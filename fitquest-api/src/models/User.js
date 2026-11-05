/**
 * A FitQuest account. XP, level and streak are only changed by the server.
 */
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
    // Blob name of the profile picture; bytes live in blob storage, not MongoDB.
    avatarBlobName: { type: String, default: null },
    // FCM registration tokens, one per signed-in device.
    deviceTokens: { type: [String], default: [] },
    // Custom routines (FR2). Embedded because they are always read and saved together.
    routines: {
      type: [
        {
          _id: false,
          id: { type: String, required: true },
          name: { type: String, required: true },
          exercises: {
            type: [
              {
                _id: false,
                exerciseId: { type: String, required: true },
                name: { type: String, required: true },
                muscleGroup: { type: String, required: true },
              },
            ],
            default: [],
          },
        },
      ],
      default: [],
    },
  },
  { timestamps: { createdAt: 'createdAt', updatedAt: 'updatedAt' } }
);

userSchema.index({ ssoProvider: 1, ssoSubjectId: 1 }, { unique: true });

module.exports = mongoose.model('User', userSchema);
