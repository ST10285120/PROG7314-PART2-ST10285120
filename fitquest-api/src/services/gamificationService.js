const Badge = require('../models/Badge');
const UserBadge = require('../models/UserBadge');
const Workout = require('../models/Workout');

const XP_PER_SET = 5;
const XP_PER_PERSONAL_RECORD = 25;
const XP_WORKOUT_COMPLETION_BONUS = 20;
const XP_PER_LEVEL = 200;
const STREAK_GRACE_HOURS = 36;

function msBetween(a, b) {
  return Math.abs(new Date(a).getTime() - new Date(b).getTime());
}

function isSameCalendarDay(a, b) {
  const da = new Date(a);
  const db = new Date(b);
  return (
    da.getUTCFullYear() === db.getUTCFullYear() &&
    da.getUTCMonth() === db.getUTCMonth() &&
    da.getUTCDate() === db.getUTCDate()
  );
}

function calculateWorkoutXp(workout) {
  let xp = XP_WORKOUT_COMPLETION_BONUS;
  for (const we of workout.exercises) {
    for (const set of we.sets) {
      xp += XP_PER_SET;
      if (set.isPersonalRecord) xp += XP_PER_PERSONAL_RECORD;
    }
  }
  return xp;
}

function calculateTotalVolume(workout) {
  let volume = 0;
  for (const we of workout.exercises) {
    for (const set of we.sets) {
      volume += set.reps * set.weightKg;
    }
  }
  return volume;
}

function updateStreak(user, workoutDate) {
  const now = new Date(workoutDate);
  let { currentStreak, longestStreak, lastWorkoutDate } = user;

  if (!lastWorkoutDate) {
    currentStreak = 1;
  } else if (isSameCalendarDay(lastWorkoutDate, now)) {
  } else {
    const gapMs = msBetween(lastWorkoutDate, now);
    const gapHours = gapMs / (1000 * 60 * 60);
    if (gapHours <= 24 + STREAK_GRACE_HOURS) {
      currentStreak += 1;
    } else {
      currentStreak = 1;
    }
  }

  longestStreak = Math.max(longestStreak || 0, currentStreak);
  return { currentStreak, longestStreak, lastWorkoutDate: now };
}

function levelForXp(xpTotal) {
  return Math.floor(xpTotal / XP_PER_LEVEL) + 1;
}

async function evaluateBadges(user) {
  const [allBadges, alreadyEarned, workoutCount, personalRecordCount] = await Promise.all([
    Badge.find({}),
    UserBadge.find({ userId: user._id }).select('badgeId'),
    Workout.countDocuments({ userId: user._id, completedAt: { $ne: null } }),
    Workout.aggregate([
      { $match: { userId: user._id } },
      { $unwind: '$exercises' },
      { $unwind: '$exercises.sets' },
      { $match: { 'exercises.sets.isPersonalRecord': true } },
      { $count: 'count' },
    ]),
  ]);

  const earnedIds = new Set(alreadyEarned.map((ub) => ub.badgeId.toString()));
  const prCount = personalRecordCount[0] ? personalRecordCount[0].count : 0;
  const newlyAwarded = [];

  for (const badge of allBadges) {
    if (earnedIds.has(badge._id.toString())) continue;

    let meetsCriteria = false;
    if (badge.criteriaType === 'STREAK_DAYS') meetsCriteria = user.currentStreak >= badge.criteriaValue;
    if (badge.criteriaType === 'WORKOUT_COUNT') meetsCriteria = workoutCount >= badge.criteriaValue;
    if (badge.criteriaType === 'PERSONAL_RECORD_COUNT') meetsCriteria = prCount >= badge.criteriaValue;

    if (meetsCriteria) {
      await UserBadge.create({ userId: user._id, badgeId: badge._id });
      newlyAwarded.push(badge);
    }
  }

  return newlyAwarded;
}

module.exports = {
  calculateWorkoutXp,
  calculateTotalVolume,
  updateStreak,
  levelForXp,
  evaluateBadges,
  XP_PER_SET,
  XP_PER_PERSONAL_RECORD,
  XP_WORKOUT_COMPLETION_BONUS,
  XP_PER_LEVEL,
};
