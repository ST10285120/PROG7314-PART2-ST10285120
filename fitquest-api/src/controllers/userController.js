const UserPreferences = require('../models/UserPreferences');
const UserBadge = require('../models/UserBadge');
const Workout = require('../models/Workout');

async function getPreferences(req, res) {
  let prefs = await UserPreferences.findOne({ userId: req.user._id });
  if (!prefs) {
    prefs = await UserPreferences.create({ userId: req.user._id });
  }
  return res.json({
    units: prefs.units,
    theme: prefs.theme,
    reminderTime: prefs.reminderTime,
    restTimerDefaultSec: prefs.restTimerDefaultSec,
  });
}

async function updatePreferences(req, res) {
  const allowed = ['units', 'theme', 'reminderTime', 'restTimerDefaultSec'];
  const updates = {};
  for (const key of allowed) {
    if (req.body[key] !== undefined) updates[key] = req.body[key];
  }

  const prefs = await UserPreferences.findOneAndUpdate(
    { userId: req.user._id },
    { $set: updates },
    { new: true, upsert: true }
  );

  return res.json({
    units: prefs.units,
    theme: prefs.theme,
    reminderTime: prefs.reminderTime,
    restTimerDefaultSec: prefs.restTimerDefaultSec,
  });
}
async function getProgress(req, res) {
  const workouts = await Workout.find({
    userId: req.user._id,
    completedAt: { $ne: null },
  }).sort({ completedAt: 1 });

  const volumeByWeek = {};
  const prsByExercise = {};
  const muscleGroupVolume = {};

  for (const w of workouts) {
    const d = new Date(w.completedAt);
    const weekKey = isoWeekKey(d);
    volumeByWeek[weekKey] = (volumeByWeek[weekKey] || 0) + w.totalVolumeKg;

    for (const we of w.exercises) {
      for (const set of we.sets) {
        if (set.isPersonalRecord) {
          const exId = we.exerciseId.toString();
          const prev = prsByExercise[exId] || 0;
          prsByExercise[exId] = Math.max(prev, set.weightKg);
        }
      }
    }
  }

  return res.json({ volumeByWeek, prsByExercise, muscleGroupSplit: muscleGroupVolume });
}

function isoWeekKey(date) {
  const d = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
  d.setUTCDate(d.getUTCDate() + 4 - (d.getUTCDay() || 7));
  const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
  const weekNo = Math.ceil(((d - yearStart) / 86400000 + 1) / 7);
  return `${d.getUTCFullYear()}-W${String(weekNo).padStart(2, '0')}`;
}

async function getBadges(req, res) {
  const earned = await UserBadge.find({ userId: req.user._id }).populate('badgeId');
  return res.json(
    earned.map((ub) => ({
      badgeId: ub.badgeId._id,
      name: ub.badgeId.name,
      description: ub.badgeId.description,
      earnedAt: ub.earnedAt,
    }))
  );
}

module.exports = { getPreferences, updatePreferences, getProgress, getBadges };
