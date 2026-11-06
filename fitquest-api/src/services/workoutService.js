/**
 * Workout rules shared by the per-set endpoints and the offline sync endpoint, so
 * both give the same personal records and award XP in exactly the same way.
 */
const mongoose = require('mongoose');
const Workout = require('../models/Workout');
const {
  calculateWorkoutXp,
  calculateTotalVolume,
  updateStreak,
  levelForXp,
  evaluateBadges,
} = require('./gamificationService');
const { sendToUser } = require('./pushService');

/** The heaviest weight this user lifted for an exercise in any other workout since the last reset. */
async function findPriorBest(user, exerciseId, excludeWorkoutId) {
  const match = { userId: user._id, _id: { $ne: excludeWorkoutId } };
  if (user.recordsResetAt) match.startedAt = { $gte: user.recordsResetAt };

  const result = await Workout.aggregate([
    { $match: match },
    { $unwind: '$exercises' },
    { $match: { 'exercises.exerciseId': new mongoose.Types.ObjectId(String(exerciseId)) } },
    { $unwind: '$exercises.sets' },
    { $group: { _id: null, maxWeight: { $max: '$exercises.sets.weightKg' } } },
  ]);
  return result[0] ? result[0].maxWeight : null;
}

/**
 * A set is a PR when it beats all earlier lifts of that exercise. With no earlier
 * lift there is nothing to beat, so the first ever set is not a PR.
 */
function isPersonalRecord(weightKg, priorBest, setsThisWorkout) {
  const weights = setsThisWorkout.map((set) => set.weightKg);
  if (priorBest !== null) weights.push(priorBest);
  return weights.length > 0 && weightKg > Math.max(...weights);
}

/** Clears every record flag and makes later lifts ignore workouts from before now. */
async function resetPersonalRecords(user) {
  await Workout.updateMany(
    { userId: user._id, 'exercises.sets.isPersonalRecord': true },
    { $set: { 'exercises.$[].sets.$[record].isPersonalRecord': false } },
    { arrayFilters: [{ 'record.isPersonalRecord': true }] }
  );
  user.recordsResetAt = new Date();
  await user.save();
  return user.recordsResetAt;
}

/**
 * Completes a workout exactly once. The conditional update means a retried request,
 * or two arriving together, cannot award XP twice: only one can move completedAt
 * from null to a date. Repeats get the stored result and no new badges.
 */
async function completeOnce(workoutId, user, completedAt) {
  const workout = await Workout.findOneAndUpdate(
    { _id: workoutId, userId: user._id, completedAt: null },
    { $set: { completedAt: completedAt ? new Date(completedAt) : new Date() } },
    { returnDocument: 'after' }
  );

  if (!workout) {
    const existing = await Workout.findOne({ _id: workoutId, userId: user._id });
    if (!existing) return null;
    return {
      xpEarned: existing.xpEarned,
      newLevel: user.level,
      streak: user.currentStreak,
      badgesEarned: [],
      alreadyCompleted: true,
    };
  }

  workout.totalVolumeKg = calculateTotalVolume(workout);
  workout.xpEarned = calculateWorkoutXp(workout);
  await workout.save();

  const previousLevel = user.level;
  user.xpTotal += workout.xpEarned;
  user.level = levelForXp(user.xpTotal);
  Object.assign(user, updateStreak(user, workout.completedAt));
  await user.save();

  const newBadges = await evaluateBadges(user);
  notifyAchievements(user, previousLevel, newBadges);

  return {
    xpEarned: workout.xpEarned,
    newLevel: user.level,
    streak: user.currentStreak,
    badgesEarned: newBadges.map((badge) => ({
      badgeId: badge._id,
      name: badge.name,
      description: badge.description,
    })),
    alreadyCompleted: false,
  };
}

/** Fire and forget: sendToUser handles its own errors. */
function notifyAchievements(user, previousLevel, newBadges) {
  for (const badge of newBadges) {
    sendToUser(user, { type: 'BADGE', badgeName: badge.name });
  }
  if (user.level > previousLevel) {
    sendToUser(user, { type: 'LEVEL_UP', level: String(user.level) });
  }
}

module.exports = { findPriorBest, isPersonalRecord, resetPersonalRecords, completeOnce };
