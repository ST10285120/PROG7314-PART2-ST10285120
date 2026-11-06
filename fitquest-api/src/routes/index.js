/**
 * Maps every REST endpoint to its controller.
 */
const express = require('express');
const authGuard = require('../middleware/authGuard');

const { ssoSignIn } = require('../controllers/authController');
const { listExercises } = require('../controllers/exerciseController');
const workouts = require('../controllers/workoutController');
const users = require('../controllers/userController');

const router = express.Router();

// Raw bytes for photo uploads. The size limit is enforced before any handler runs.
const imageBody = express.raw({ type: ['image/jpeg', 'image/png'], limit: '5mb' });

router.post('/auth/sso', ssoSignIn);

// Everything below this line requires a valid JWT (FR9).
router.use(authGuard);

router.get('/exercises', listExercises);
router.get('/badges', users.getBadgeCatalog);

router.get('/users/me', users.getProfile);
router.delete('/users/me', users.deleteAccount);
router.get('/users/me/routines', users.getRoutines);
router.put('/users/me/routines', users.saveRoutines);
router.get('/users/me/preferences', users.getPreferences);
router.put('/users/me/preferences', users.updatePreferences);
router.get('/users/me/progress', users.getProgress);
router.get('/users/me/badges', users.getBadges);
router.post('/users/me/device-tokens', users.registerDeviceToken);
router.post('/users/me/notifications/test', users.sendTestNotification);

router.get('/users/me/avatar', users.getAvatar);
router.post('/users/me/avatar', imageBody, users.uploadAvatar);
router.get('/users/me/photos', users.listPhotos);
router.post('/users/me/photos', imageBody, users.uploadPhoto);
router.get('/users/me/photos/:id/content', users.getPhotoContent);
router.delete('/users/me/photos/:id', users.deletePhoto);

// Fixed paths go before the :id routes so "sync" is never read as a workout id.
router.get('/workouts/suggested', workouts.suggestedWorkout);
router.get('/workouts/history', workouts.workoutHistory);
router.post('/workouts/sync', workouts.syncWorkout);
router.post('/workouts', workouts.createWorkout);
router.post('/workouts/:id/exercises', workouts.addExercise);
router.post('/workouts/:id/exercises/:weId/sets', workouts.logSet);
router.patch('/workouts/:id/complete', workouts.completeWorkout);

module.exports = router;
