/**
 * Server-side XP, level, streak and badge rules, kept here so the phone cannot alter them.
 */
const Badge = require('../models/Badge');
const UserBadge = require('../models/UserBadge');
const Workout = require('../models/Workout');

const XP_PER_SET = 5;
const XP_PER_PERSONAL_RECORD = 25;
const XP_WORKOUT_COMPLETION_BONUS = 20;
const XP_PER_LEVEL = 200;
// A streak survives one missed calendar day, as specified in the Part 1 design.
const STREAK_GRACE_DAYS = 1;
const MS_PER_DAY = 24 * 60 * 60 * 1000;

/** Whole UTC calendar days from a to b. */
function calendarDaysBetween(a, b) {
  const da = new Date(a);
  const db = new Date(b);
  const utcA = Date.UTC(da.getUTCFullYear(), da.getUTCMonth(), da.getUTCDate());
  const utcB = Date.UTC(db.getUTCFullYear(), db.getUTCMonth(), db.getUTCDate());
  return Math.round((utcB - utcA) / MS_PER_DAY);
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

/**
 * Returns the streak after a workout on workoutDate.
 * Workouts synced late from offline can arrive out of order, so one dated before
 * the last counted workout leaves the streak untouched.
 */
function updateStreak(user, workoutDate) {
  const workoutAt = new Date(workoutDate);
  let { currentStreak, longestStreak, lastWorkoutDate } = user;

  if (!lastWorkoutDate) {
    currentStreak = 1;
    lastWorkoutDate = workoutAt;
  } else {
    const dayGap = calendarDaysBetween(lastWorkoutDate, workoutAt);
    if (dayGap > 0) {
      currentStreak = dayGap <= 1 + STREAK_GRACE_DAYS ? currentStreak + 1 : 1;
    }
    if (dayGap >= 0) lastWorkoutDate = workoutAt;
  }

  longestStreak = Math.max(longestStreak || 0, currentStreak);
  return { currentStreak, longestStreak, lastWorkoutDate };
}

function levelForXp(xpTotal) {
  return Math.floor(xpTotal / XP_PER_LEVEL) + 1;
}

/** Counts used to judge badge criteria and to show progress towards locked badges. */
async function getBadgeStats(user) {
  const [workoutCount, personalRecordCount] = await Promise.all([
    Workout.countDocuments({ userId: user._id, completedAt: { $ne: null } }),
    Workout.aggregate([
      { $match: { userId: user._id } },
      { $unwind: '$exercises' },
      { $unwind: '$exercises.sets' },
      { $match: { 'exercises.sets.isPersonalRecord': true } },
      { $count: 'count' },
    ]),
  ]);
  return {
    STREAK_DAYS: user.currentStreak,
    WORKOUT_COUNT: workoutCount,
    PERSONAL_RECORD_COUNT: personalRecordCount[0] ? personalRecordCount[0].count : 0,
  };
}

/** Awards every badge whose criteria are newly met and returns those badges. */
async function evaluateBadges(user) {
  const [allBadges, alreadyEarned, stats] = await Promise.all([
    Badge.find({}),
    UserBadge.find({ userId: user._id }).select('badgeId'),
    getBadgeStats(user),
  ]);

  const earnedIds = new Set(alreadyEarned.map((ub) => ub.badgeId.toString()));
  const newlyAwarded = [];

  for (const badge of allBadges) {
    if (earnedIds.has(badge._id.toString())) continue;

    const meetsCriteria = (stats[badge.criteriaType] || 0) >= badge.criteriaValue;

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
  getBadgeStats,
  XP_PER_SET,
  XP_PER_PERSONAL_RECORD,
  XP_WORKOUT_COMPLETION_BONUS,
  XP_PER_LEVEL,
};
