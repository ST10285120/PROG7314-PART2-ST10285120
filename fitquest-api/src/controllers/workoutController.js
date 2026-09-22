const mongoose = require('mongoose');
const Workout = require('../models/Workout');
const Exercise = require('../models/Exercise');
const User = require('../models/User');
const {
  calculateWorkoutXp,
  calculateTotalVolume,
  updateStreak,
  levelForXp,
  evaluateBadges,
} = require('../services/gamificationService');

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

  const workout = await Workout.findOne({ _id: id, userId: req.user._id });
  if (!workout) return res.status(404).json({ error: 'Workout not found' });

  const we = workout.exercises.id(weId);
  if (!we) return res.status(404).json({ error: 'WorkoutExercise not found' });

  const priorBest = await Workout.aggregate([
    { $match: { userId: req.user._id, _id: { $ne: workout._id } } },
    { $unwind: '$exercises' },
    { $match: { 'exercises.exerciseId': we.exerciseId } },
    { $unwind: '$exercises.sets' },
    { $group: { _id: null, maxWeight: { $max: '$exercises.sets.weightKg' } } },
  ]);
  const bestSoFar = priorBest[0] ? priorBest[0].maxWeight : null;
  const isPersonalRecord = bestSoFar !== null && weightKg > bestSoFar;

  we.sets.push({ setNumber, reps, weightKg, rpe: rpe ?? null, isPersonalRecord });
  await workout.save();

  const newSet = we.sets[we.sets.length - 1];
  return res.status(201).json({ setId: newSet._id, isPersonalRecord });
}

async function completeWorkout(req, res) {
  const { id } = req.params;
  const { completedAt } = req.body;

  const workout = await Workout.findOne({ _id: id, userId: req.user._id });
  if (!workout) return res.status(404).json({ error: 'Workout not found' });

  workout.completedAt = completedAt || new Date();
  workout.totalVolumeKg = calculateTotalVolume(workout);
  workout.xpEarned = calculateWorkoutXp(workout);
  await workout.save();

  const user = req.user;
  user.xpTotal += workout.xpEarned;
  user.level = levelForXp(user.xpTotal);

  const { currentStreak, longestStreak, lastWorkoutDate } = updateStreak(user, workout.completedAt);
  user.currentStreak = currentStreak;
  user.longestStreak = longestStreak;
  user.lastWorkoutDate = lastWorkoutDate;
  await user.save();

  const newBadges = await evaluateBadges(user);

  return res.json({
    xpEarned: workout.xpEarned,
    newLevel: user.level,
    streak: user.currentStreak,
    badgesEarned: newBadges.map((b) => ({ badgeId: b._id, name: b.name, description: b.description })),
  });
}

async function suggestedWorkout(req, res) {
  const recoveryHours = Number(req.query.recoveryHours) || 48;
  const cutoff = new Date(Date.now() - recoveryHours * 60 * 60 * 1000);

  const recentWorkouts = await Workout.find({
    userId: req.user._id,
    completedAt: { $gte: cutoff },
  }).populate('exercises.exerciseId');

  const recentlyTrained = new Set();
  for (const w of recentWorkouts) {
    for (const we of w.exercises) {
      if (we.exerciseId && we.exerciseId.muscleGroup) recentlyTrained.add(we.exerciseId.muscleGroup);
    }
  }

  const allExercises = await Exercise.find({ type: 'STRENGTH' });
  const candidates = allExercises.filter((ex) => !recentlyTrained.has(ex.muscleGroup));
  const pool = candidates.length >= 5 ? candidates : allExercises;

  const shuffled = [...pool].sort(() => Math.random() - 0.5).slice(0, 5);
  return res.json({
    title: 'Suggested Workout',
    exercises: shuffled.map((ex) => ({ exerciseId: ex._id, name: ex.name, muscleGroup: ex.muscleGroup })),
  });
}

module.exports = { createWorkout, addExercise, logSet, completeWorkout, suggestedWorkout };
