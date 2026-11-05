const mongoose = require('mongoose');

/**
 * A single logged set. clientId is the UUID generated on the phone, which makes
 * offline replays idempotent: a retried sync matches the existing set instead of
 * creating a second one.
 */
const setEntrySchema = new mongoose.Schema(
  {
    clientId: { type: String },
    setNumber: { type: Number, required: true },
    reps: { type: Number, required: true },
    weightKg: { type: Number, required: true },
    rpe: { type: Number, min: 1, max: 10, default: null },
    isPersonalRecord: { type: Boolean, default: false },
  },
  { _id: true }
);

/** Join between a workout and an exercise from the shared library. */
const workoutExerciseSchema = new mongoose.Schema(
  {
    clientId: { type: String },
    exerciseId: { type: mongoose.Schema.Types.ObjectId, ref: 'Exercise', required: true },
    orderIndex: { type: Number, required: true },
    sets: { type: [setEntrySchema], default: [] },
  },
  { _id: true }
);

/**
 * Sets and exercises are embedded because they are always read with their workout.
 * completedAt doubles as the "gamification applied" marker: completion claims the
 * null -> date transition atomically, so XP can only be awarded once.
 */
const workoutSchema = new mongoose.Schema(
  {
    userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    clientId: { type: String },
    title: { type: String, required: true },
    startedAt: { type: Date, required: true, default: Date.now },
    completedAt: { type: Date, default: null },
    totalVolumeKg: { type: Number, default: 0 },
    xpEarned: { type: Number, default: 0 },
    exercises: { type: [workoutExerciseSchema], default: [] },
  },
  { timestamps: true }
);

// Partial index: workouts created before offline sync existed have no clientId.
workoutSchema.index(
  { userId: 1, clientId: 1 },
  { unique: true, partialFilterExpression: { clientId: { $type: 'string' } } }
);
workoutSchema.index({ userId: 1, completedAt: -1 });

module.exports = mongoose.model('Workout', workoutSchema);
