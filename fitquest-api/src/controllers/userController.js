/**
 * Profile, settings, progress, badges, routines, photos and account deletion.
 */
const mongoose = require('mongoose');
const UserPreferences = require('../models/UserPreferences');
const UserBadge = require('../models/UserBadge');
const Badge = require('../models/Badge');
const Workout = require('../models/Workout');
const ProgressPhoto = require('../models/ProgressPhoto');
const blobStorage = require('../services/blobStorageService');
const { sendToUser } = require('../services/pushService');
const { getBadgeStats } = require('../services/gamificationService');
const { toUserResponse } = require('../utils/userResponse');

const REMINDER_TIME_PATTERN = /^([01]\d|2[0-3]):[0-5]\d$/;
const MIN_REST_SECONDS = 10;
const MAX_REST_SECONDS = 600;
const MAX_ROUTINES = 50;
const MAX_ROUTINE_NAME = 60;
const MAX_ROUTINE_EXERCISES = 30;

function toPreferencesResponse(prefs) {
  return {
    units: prefs.units,
    theme: prefs.theme,
    reminderTime: prefs.reminderTime,
    restTimerDefaultSec: prefs.restTimerDefaultSec,
  };
}

function getProfile(req, res) {
  return res.json(toUserResponse(req.user));
}

async function getPreferences(req, res) {
  let prefs = await UserPreferences.findOne({ userId: req.user._id });
  if (!prefs) {
    prefs = await UserPreferences.create({ userId: req.user._id });
  }
  return res.json(toPreferencesResponse(prefs));
}

async function updatePreferences(req, res) {
  const allowed = ['units', 'theme', 'reminderTime', 'restTimerDefaultSec'];
  const updates = {};
  for (const key of allowed) {
    if (req.body[key] !== undefined) updates[key] = req.body[key];
  }

  if (updates.units !== undefined && !['KG', 'LB'].includes(updates.units)) {
    return res.status(400).json({ error: 'units must be KG or LB' });
  }
  if (updates.theme !== undefined && !['LIGHT', 'DARK', 'SYSTEM'].includes(updates.theme)) {
    return res.status(400).json({ error: 'theme must be LIGHT, DARK or SYSTEM' });
  }
  if (updates.reminderTime !== undefined && !REMINDER_TIME_PATTERN.test(updates.reminderTime)) {
    return res.status(400).json({ error: 'reminderTime must be in HH:mm format' });
  }
  if (
    updates.restTimerDefaultSec !== undefined &&
    !(
      Number.isInteger(updates.restTimerDefaultSec) &&
      updates.restTimerDefaultSec >= MIN_REST_SECONDS &&
      updates.restTimerDefaultSec <= MAX_REST_SECONDS
    )
  ) {
    return res.status(400).json({ error: 'restTimerDefaultSec is out of range' });
  }

  const prefs = await UserPreferences.findOneAndUpdate(
    { userId: req.user._id },
    { $set: updates },
    { returnDocument: 'after', upsert: true }
  );

  return res.json(toPreferencesResponse(prefs));
}

function validateRoutines(routines) {
  if (!Array.isArray(routines) || routines.length > MAX_ROUTINES) return 'routines must be a list';
  for (const routine of routines) {
    const name = routine && routine.name;
    if (typeof routine?.id !== 'string' || routine.id.length === 0) return 'each routine needs an id';
    if (typeof name !== 'string' || name.trim() === '' || name.length > MAX_ROUTINE_NAME) {
      return 'each routine needs a name';
    }
    const exercises = routine.exercises;
    if (!Array.isArray(exercises) || exercises.length === 0 || exercises.length > MAX_ROUTINE_EXERCISES) {
      return 'each routine needs at least one exercise';
    }
    for (const exercise of exercises) {
      const hasText = typeof exercise?.name === 'string' && typeof exercise?.muscleGroup === 'string';
      if (!mongoose.isValidObjectId(exercise?.exerciseId) || !hasText) {
        return 'each exercise needs a valid exerciseId, name and muscleGroup';
      }
    }
  }
  return null;
}

function getRoutines(req, res) {
  return res.json({ routines: req.user.routines });
}

/** Replaces the whole list, so a phone that was offline can simply send what it has. */
async function saveRoutines(req, res) {
  const { routines } = req.body;
  const invalid = validateRoutines(routines);
  if (invalid) return res.status(400).json({ error: invalid });

  req.user.routines = routines;
  await req.user.save();
  return res.json({ routines: req.user.routines });
}

/**
 * Permanently removes the account and everything linked to it. Google Play requires
 * apps that create accounts to offer this. A photo that fails to delete must not leave
 * the account in place, so storage errors are logged and the deletion continues.
 */
async function deleteAccount(req, res) {
  const userId = req.user._id;
  const photos = await ProgressPhoto.find({ userId });
  const blobNames = photos.map((photo) => photo.blobName);
  if (req.user.avatarBlobName) blobNames.push(req.user.avatarBlobName);

  if (blobStorage.isConfigured()) {
    await Promise.all(
      blobNames.map((name) => blobStorage.deleteBlob(name).catch((err) => console.error(err.message)))
    );
  }

  await Promise.all([
    Workout.deleteMany({ userId }),
    UserPreferences.deleteMany({ userId }),
    UserBadge.deleteMany({ userId }),
    ProgressPhoto.deleteMany({ userId }),
  ]);
  await req.user.deleteOne();
  return res.status(204).send();
}

/** Dashboard totals, calculated from every completed workout this user has synced. */
async function getProgress(req, res) {
  const workouts = await Workout.find({
    userId: req.user._id,
    completedAt: { $ne: null },
  })
    .sort({ completedAt: 1 })
    .populate('exercises.exerciseId');

  const volumeByWeek = {};
  const prsByExercise = {};
  const muscleGroupSplit = {};

  for (const workout of workouts) {
    const weekKey = isoWeekKey(new Date(workout.completedAt));
    volumeByWeek[weekKey] = (volumeByWeek[weekKey] || 0) + workout.totalVolumeKg;

    for (const we of workout.exercises) {
      // The exercise can be missing if the library entry was deleted.
      if (!we.exerciseId) continue;
      const { name, muscleGroup } = we.exerciseId;

      for (const set of we.sets) {
        muscleGroupSplit[muscleGroup] =
          (muscleGroupSplit[muscleGroup] || 0) + set.reps * set.weightKg;
        prsByExercise[name] = Math.max(prsByExercise[name] || 0, set.weightKg);
      }
    }
  }

  return res.json({
    volumeByWeek,
    prsByExercise,
    muscleGroupSplit,
    totalWorkouts: workouts.length,
  });
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
    earned
      .filter((userBadge) => userBadge.badgeId)
      .map((userBadge) => ({
        badgeId: userBadge.badgeId._id,
        name: userBadge.badgeId.name,
        description: userBadge.badgeId.description,
        earnedAt: userBadge.earnedAt,
      }))
  );
}

/** Every badge with this user's progress, so locked badges give them a goal. */
async function getBadgeCatalog(req, res) {
  const [badges, earned, stats] = await Promise.all([
    Badge.find({}).sort({ criteriaType: 1, criteriaValue: 1 }),
    UserBadge.find({ userId: req.user._id }),
    getBadgeStats(req.user),
  ]);
  const earnedAtByBadge = new Map(earned.map((ub) => [ub.badgeId.toString(), ub.earnedAt]));

  return res.json(
    badges.map((badge) => {
      const earnedAt = earnedAtByBadge.get(badge._id.toString()) || null;
      return {
        badgeId: badge._id,
        name: badge.name,
        description: badge.description,
        earnedAt,
        target: badge.criteriaValue,
        progress: earnedAt
          ? badge.criteriaValue
          : Math.min(stats[badge.criteriaType] || 0, badge.criteriaValue),
      };
    })
  );
}

async function registerDeviceToken(req, res) {
  const { token } = req.body;
  if (typeof token !== 'string' || token.length === 0) {
    return res.status(400).json({ error: 'token is required' });
  }
  await req.user.updateOne({ $addToSet: { deviceTokens: token } });
  return res.status(204).send();
}

/** Lets the Settings screen prove the full push pipeline on demand. */
async function sendTestNotification(req, res) {
  const delivered = await sendToUser(req.user, { type: 'TEST' });
  return res.json({ delivered });
}

/** Checks the type header and the file signature, then returns the content type. */
function readImage(req) {
  const contentType = req.headers['content-type'];
  const body = req.body;
  if (!Buffer.isBuffer(body) || body.length === 0) return null;
  const isJpeg = contentType === 'image/jpeg' && body[0] === 0xff && body[1] === 0xd8;
  const isPng = contentType === 'image/png' && body[0] === 0x89 && body[1] === 0x50;
  return isJpeg || isPng ? contentType : null;
}

function storageUnavailable(res) {
  return res.status(503).json({ error: 'Photo storage is not configured' });
}

async function uploadAvatar(req, res) {
  if (!blobStorage.isConfigured()) return storageUnavailable(res);
  const contentType = readImage(req);
  if (!contentType) return res.status(400).json({ error: 'A JPEG or PNG image is required' });

  const blobName = blobStorage.newBlobName('avatars', req.user._id, contentType);
  await blobStorage.uploadBlob(blobName, req.body, contentType);

  const previousBlob = req.user.avatarBlobName;
  req.user.avatarBlobName = blobName;
  await req.user.save();

  // The new picture is saved, so a failed clean-up only leaves an unused file.
  if (previousBlob) {
    blobStorage.deleteBlob(previousBlob).catch((err) => console.error(err.message));
  }
  return res.status(201).json(toUserResponse(req.user));
}

async function sendBlob(res, blobName) {
  const { buffer, contentType } = await blobStorage.downloadBlob(blobName);
  res.set('Cache-Control', 'private, max-age=86400');
  return res.type(contentType).send(buffer);
}

async function getAvatar(req, res) {
  if (!req.user.avatarBlobName) return res.status(404).json({ error: 'No profile picture' });
  if (!blobStorage.isConfigured()) return storageUnavailable(res);
  return sendBlob(res, req.user.avatarBlobName);
}

function toPhotoResponse(photo) {
  return {
    id: photo._id,
    path: `api/users/me/photos/${photo._id}/content`,
    takenAt: photo.takenAt,
  };
}

async function listPhotos(req, res) {
  const photos = await ProgressPhoto.find({ userId: req.user._id }).sort({ takenAt: -1 });
  return res.json(photos.map(toPhotoResponse));
}

async function uploadPhoto(req, res) {
  if (!blobStorage.isConfigured()) return storageUnavailable(res);
  const contentType = readImage(req);
  if (!contentType) return res.status(400).json({ error: 'A JPEG or PNG image is required' });

  const blobName = blobStorage.newBlobName('progress', req.user._id, contentType);
  await blobStorage.uploadBlob(blobName, req.body, contentType);

  const photo = await ProgressPhoto.create({ userId: req.user._id, blobName, contentType });
  return res.status(201).json(toPhotoResponse(photo));
}

/** Loads a photo only if it belongs to the signed-in user. */
async function findOwnPhoto(req) {
  if (!mongoose.isValidObjectId(req.params.id)) return null;
  return ProgressPhoto.findOne({ _id: req.params.id, userId: req.user._id });
}

async function getPhotoContent(req, res) {
  const photo = await findOwnPhoto(req);
  if (!photo) return res.status(404).json({ error: 'Photo not found' });
  if (!blobStorage.isConfigured()) return storageUnavailable(res);
  return sendBlob(res, photo.blobName);
}

async function deletePhoto(req, res) {
  const photo = await findOwnPhoto(req);
  if (!photo) return res.status(404).json({ error: 'Photo not found' });
  if (!blobStorage.isConfigured()) return storageUnavailable(res);

  await blobStorage.deleteBlob(photo.blobName);
  await photo.deleteOne();
  return res.status(204).send();
}

module.exports = {
  getProfile,
  deleteAccount,
  getRoutines,
  saveRoutines,
  getPreferences,
  updatePreferences,
  getProgress,
  getBadges,
  getBadgeCatalog,
  registerDeviceToken,
  sendTestNotification,
  uploadAvatar,
  getAvatar,
  listPhotos,
  uploadPhoto,
  getPhotoContent,
  deletePhoto,
};
