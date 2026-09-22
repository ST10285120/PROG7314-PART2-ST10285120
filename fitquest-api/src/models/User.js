const mongoose = require('mongoose');

const userSchema = new mongoose.Schema(
  {
    username: { type: String, required: true, trim: true },
    email: { type: String, required: true, unique: true, lowercase: true, trim: true },
    ssoProvider: { type: String, enum: ['GOOGLE', 'GITHUB'], required: true },
    ssoSubjectId: { type: String, required: true },
    xpTotal: { type: Number, default: 0 },
    level: { type: Number, default: 1 },
    currentStreak: { type: Number, default: 0 },
    longestStreak: { type: Number, default: 0 },
    lastWorkoutDate: { type: Date, default: null },
  },
  { timestamps: { createdAt: 'createdAt', updatedAt: 'updatedAt' } }
);

userSchema.index({ ssoProvider: 1, ssoSubjectId: 1 }, { unique: true });

module.exports = mongoose.model('User', userSchema);
