const mongoose = require('mongoose');

const setEntrySchema = new mongoose.Schema(
  {
    setNumber: { type: Number, required: true },
    reps: { type: Number, required: true },
    weightKg: { type: Number, required: true },
    rpe: { type: Number, min: 1, max: 10, default: null },
    isPersonalRecord: { type: Boolean, default: false },
  },
  { _id: true }
);

const workoutExerciseSchema = new mongoose.Schema(
  {
    exerciseId: { type: mongoose.Schema.Types.ObjectId, ref: 'Exercise', required: true },
    orderIndex: { type: Number, required: true },
    sets: { type: [setEntrySchema], default: [] },
  },
  { _id: true }
);

const workoutSchema = new mongoose.Schema(
  {
    userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    title: { type: String, required: true },
    startedAt: { type: Date, required: true, default: Date.now },
    completedAt: { type: Date, default: null },
    totalVolumeKg: { type: Number, default: 0 },
    xpEarned: { type: Number, default: 0 },
    exercises: { type: [workoutExerciseSchema], default: [] },
  },
  { timestamps: true }
);

module.exports = mongoose.model('Workout', workoutSchema);
