const express = require('express');
const authGuard = require('../middleware/authGuard');

const { ssoSignIn } = require('../controllers/authController');
const { listExercises } = require('../controllers/exerciseController');
const {
  createWorkout,
  addExercise,
  logSet,
  completeWorkout,
  suggestedWorkout,
} = require('../controllers/workoutController');
const {
  getPreferences,
  updatePreferences,
  getProgress,
  getBadges,
} = require('../controllers/userController');

const router = express.Router();

router.post('/auth/sso', ssoSignIn);

router.use(authGuard);

router.get('/exercises', listExercises);

router.get('/users/me/preferences', getPreferences);
router.put('/users/me/preferences', updatePreferences);
router.get('/users/me/progress', getProgress);
router.get('/users/me/badges', getBadges);

router.get('/workouts/suggested', suggestedWorkout);
router.post('/workouts', createWorkout);
router.post('/workouts/:id/exercises', addExercise);
router.post('/workouts/:id/exercises/:weId/sets', logSet);
router.patch('/workouts/:id/complete', completeWorkout);

module.exports = router;
