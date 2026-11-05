const mongoose = require('mongoose');
const Workout = require('../models/Workout');
const Exercise = require('../models/Exercise');
const { findPriorBest, isPersonalRecord, completeOnce } = require('../services/workoutService');

const MAX_WEIGHT_KG = 1000;
const MAX_REPS = 1000;
const MUSCLE_GROUP_HISTORY_DAYS = 30;
const SUGGESTED_FOCUS_GROUPS = 2;
const SUGGESTED_EXERCISE_COUNT = 5;

const isId = (value) => mongoose.isValidObjectId(value);

/** Returns an error message for a bad set, or null when it is valid. */
function validateSet({ setNumber, reps, weightKg, rpe }) {
  if (!Number.isInteger(setNumber) || setNumber < 1) return 'setNumber must be a positive integer';
  if (!Number.isInteger(reps) || reps < 0 || reps > MAX_REPS) return 'reps is out of range';
  if (typeof weightKg !== 'number' || weightKg < 0 || weightKg > MAX_WEIGHT_KG) {
    return 'weightKg is out of range';
  }
  if (rpe !== undefined && rpe !== null && !(rpe >= 1 && rpe <= 10)) return 'rpe must be 1 to 10';
  return null;
}

async function createWorkout(req, res) {
  const { title, startedAt } = req.body;
  if (!title) return res.status(400).json({ error: 'title is required' });

  const workout = await Workout.create({
    userId: req.user._id,
    title,
    startedAt: startedAt || new Date(),
  });
  return res.status(201).json({ workoutId: workout._id });
}

async function addExercise(req, res) {
  const { id } = req.params;
  const { exerciseId, orderIndex } = req.body;

  if (!isId(id)) return res.status(404).json({ error: 'Workout not found' });
  if (!isId(exerciseId)) return res.status(400).json({ error: 'exerciseId is invalid' });

  const workout = await Workout.findOne({ _id: id, userId: req.user._id });
  if (!workout) return res.status(404).json({ error: 'Workout not found' });

  workout.exercises.push({ exerciseId, orderIndex, sets: [] });
  await workout.save();

  const we = workout.exercises[workout.exercises.length - 1];
  return res.status(201).json({ workoutExerciseId: we._id });
}

async function logSet(req, res) {
  const { id, weId } = req.params;
  const { setNumber, reps, weightKg, rpe } = req.body;

  if (setNumber === undefined || reps === undefined || weightKg === undefined) {
    return res.status(400).json({ error: 'setNumber, reps and weightKg are required' });
  }
  const invalid = validateSet(req.body);
  if (invalid) return res.status(400).json({ error: invalid });
  if (!isId(id)) return res.status(404).json({ error: 'Workout not found' });

  const workout = await Workout.findOne({ _id: id, userId: req.user._id });
  if (!workout) return res.status(404).json({ error: 'Workout not found' });

  const we = isId(weId) ? workout.exercises.id(weId) : null;
  if (!we) return res.status(404).json({ error: 'WorkoutExercise not found' });

  const priorBest = await findPriorBest(req.user._id, we.exerciseId, workout._id);
  const personalRecord = isPersonalRecord(weightKg, priorBest, we.sets);

  we.sets.push({ setNumber, reps, weightKg, rpe: rpe ?? null, isPersonalRecord: personalRecord });
  await workout.save();

  const newSet = we.sets[we.sets.length - 1];
  return res.status(201).json({ setId: newSet._id, isPersonalRecord: personalRecord });
}

async function completeWorkout(req, res) {
  const { id } = req.params;
  if (!isId(id)) return res.status(404).json({ error: 'Workout not found' });

  const result = await completeOnce(id, req.user, req.body.completedAt);
  if (!result) return res.status(404).json({ error: 'Workout not found' });

  const { alreadyCompleted, ...body } = result;
  return res.json(body);
}

/** Finds the workout for this device-generated id, creating it on first sight. */
async function findOrCreateByClientId(userId, { clientId, title, startedAt }) {
  const existing = await Workout.findOne({ userId, clientId });
  if (existing) return existing;
  try {
    return await Workout.create({ userId, clientId, title, startedAt: startedAt || new Date() });
  } catch (err) {
    // Two syncs raced and the unique index stopped the second insert.
    if (err.code === 11000) return Workout.findOne({ userId, clientId });
    throw err;
  }
}

/** Returns an error message for a bad sync body, or null when it is valid. */
function validateSyncBody({ clientId, title, exercises }) {
  if (typeof clientId !== 'string' || clientId.length < 8) return 'clientId is required';
  if (typeof title !== 'string' || title.trim() === '') return 'title is required';
  if (!Array.isArray(exercises)) return 'exercises must be an array';

  for (const exercise of exercises) {
    if (typeof exercise.clientId !== 'string' || !isId(exercise.exerciseId)) {
      return 'each exercise needs a clientId and a valid exerciseId';
    }
    if (!Array.isArray(exercise.sets)) return 'each exercise needs a sets array';
    for (const set of exercise.sets) {
      if (typeof set.clientId !== 'string') return 'each set needs a clientId';
      const invalid = validateSet(set);
      if (invalid) return invalid;
    }
  }
  return null;
}

/** Applies the phone's exercises and sets. Known clientIds are skipped, missing sets removed. */
async function mergeExercises(workout, userId, exercises) {
  const priorBestByExercise = new Map();

  for (const incoming of exercises) {
    let we = workout.exercises.find((candidate) => candidate.clientId === incoming.clientId);
    if (!we) {
      workout.exercises.push({
        clientId: incoming.clientId,
        exerciseId: incoming.exerciseId,
        orderIndex: incoming.orderIndex ?? workout.exercises.length,
        sets: [],
      });
      we = workout.exercises[workout.exercises.length - 1];
    }

    // A set the phone no longer sends was deleted by the user before finishing.
    const keptIds = new Set(incoming.sets.map((set) => set.clientId));
    we.sets = we.sets.filter((set) => !set.clientId || keptIds.has(set.clientId));

    const key = String(incoming.exerciseId);
    for (const set of [...incoming.sets].sort((a, b) => a.setNumber - b.setNumber)) {
      if (we.sets.some((candidate) => candidate.clientId === set.clientId)) continue;

      if (!priorBestByExercise.has(key)) {
        priorBestByExercise.set(key, await findPriorBest(userId, incoming.exerciseId, workout._id));
      }
      we.sets.push({
        clientId: set.clientId,
        setNumber: set.setNumber,
        reps: set.reps,
        weightKg: set.weightKg,
        rpe: set.rpe ?? null,
        isPersonalRecord: isPersonalRecord(set.weightKg, priorBestByExercise.get(key), we.sets),
      });
    }
  }
}

/**
 * Upserts a whole workout from the phone (POST /api/workouts/sync).
 *
 * The phone records everything in Room first, then sends the full workout. Because
 * every workout, exercise and set has a client-generated UUID, the same request can
 * be replayed after a dropped connection with no duplicates and no extra XP.
 */
async function syncWorkout(req, res) {
  const invalid = validateSyncBody(req.body);
  if (invalid) return res.status(400).json({ error: invalid });

  const { clientId, title, startedAt, completedAt, exercises } = req.body;
  const workout = await findOrCreateByClientId(req.user._id, { clientId, title, startedAt });

  // A finished workout is final; its XP has already been awarded from these sets.
  if (!workout.completedAt) {
    await mergeExercises(workout, req.user._id, exercises);
    await workout.save();
  }

  const completion = completedAt ? await completeOnce(workout._id, req.user, completedAt) : null;

  return res.json({
    workoutId: workout._id,
    completion: completion
      ? {
          xpEarned: completion.xpEarned,
          newLevel: completion.newLevel,
          streak: completion.streak,
          badgesEarned: completion.badgesEarned,
        }
      : null,
    sets: workout.exercises.flatMap((we) =>
      we.sets
        .filter((set) => set.clientId)
        .map((set) => ({ clientId: set.clientId, isPersonalRecord: set.isPersonalRecord }))
    ),
  });
}

/** Completed workouts with exercise names, used to restore history on a new phone. */
async function workoutHistory(req, res) {
  const limit = Math.min(Math.max(Number(req.query.limit) || 100, 1), 200);
  const workouts = await Workout.find({ userId: req.user._id, completedAt: { $ne: null } })
    .sort({ completedAt: -1 })
    .limit(limit)
    .populate('exercises.exerciseId');

  return res.json(
    workouts.map((workout) => ({
      id: workout._id,
      clientId: workout.clientId ?? null,
      title: workout.title,
      startedAt: workout.startedAt,
      completedAt: workout.completedAt,
      totalVolumeKg: workout.totalVolumeKg,
      xpEarned: workout.xpEarned,
      exercises: workout.exercises
        .filter((we) => we.exerciseId)
        .map((we) => ({
          id: we._id,
          clientId: we.clientId ?? null,
          exerciseId: we.exerciseId._id,
          name: we.exerciseId.name,
          orderIndex: we.orderIndex,
          sets: we.sets.map((set) => ({
            id: set._id,
            clientId: set.clientId ?? null,
            setNumber: set.setNumber,
            reps: set.reps,
            weightKg: set.weightKg,
            rpe: set.rpe,
            isPersonalRecord: set.isPersonalRecord,
          })),
        })),
    }))
  );
}

/** Newest training time per muscle group for this user's recent workouts. */
async function lastTrainedByMuscleGroup(userId) {
  const since = new Date(Date.now() - MUSCLE_GROUP_HISTORY_DAYS * 24 * 60 * 60 * 1000);
  const workouts = await Workout.find({ userId, completedAt: { $gte: since } }).populate(
    'exercises.exerciseId'
  );

  const lastTrained = new Map();
  for (const workout of workouts) {
    const trainedAt = workout.completedAt.getTime();
    for (const we of workout.exercises) {
      const group = we.exerciseId && we.exerciseId.muscleGroup;
      if (group && trainedAt > (lastTrained.get(group) || 0)) lastTrained.set(group, trainedAt);
    }
  }
  return lastTrained;
}

function shuffle(items) {
  return [...items].sort(() => Math.random() - 0.5);
}

/**
 * Rules-based suggestion. Muscle groups are ranked by how long they have rested, the
 * two most rested become the focus, and groups inside the recovery window are avoided
 * unless everything is still recovering.
 */
async function suggestedWorkout(req, res) {
  const requestedHours = Number(req.query.recoveryHours);
  const recoveryHours = requestedHours > 0 && requestedHours <= 168 ? requestedHours : 48;
  const recoveryCutoff = Date.now() - recoveryHours * 60 * 60 * 1000;

  const allExercises = await Exercise.find({ type: 'STRENGTH' });
  const lastTrained = await lastTrainedByMuscleGroup(req.user._id);

  const groups = [...new Set(allExercises.map((exercise) => exercise.muscleGroup))];
  const recovered = groups.filter((group) => (lastTrained.get(group) || 0) < recoveryCutoff);
  const focusMuscleGroups = shuffle(recovered.length > 0 ? recovered : groups)
    .sort((a, b) => (lastTrained.get(a) || 0) - (lastTrained.get(b) || 0))
    .slice(0, SUGGESTED_FOCUS_GROUPS);

  const pool = allExercises.filter((exercise) => focusMuscleGroups.includes(exercise.muscleGroup));
  const chosen = shuffle(pool).slice(0, SUGGESTED_EXERCISE_COUNT);

  return res.json({
    title: 'Suggested Workout',
    focusMuscleGroups,
    exercises: chosen.map((exercise) => ({
      exerciseId: exercise._id,
      name: exercise.name,
      muscleGroup: exercise.muscleGroup,
    })),
  });
}

module.exports = {
  createWorkout,
  addExercise,
  logSet,
  completeWorkout,
  syncWorkout,
  workoutHistory,
  suggestedWorkout,
};
