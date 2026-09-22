const mongoose = require('mongoose');

const userPreferencesSchema = new mongoose.Schema(
  {
    userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true, unique: true },
    units: { type: String, enum: ['KG', 'LB'], default: 'KG' },
    theme: { type: String, enum: ['LIGHT', 'DARK', 'SYSTEM'], default: 'SYSTEM' },
    reminderTime: { type: String, default: '18:00' },
    restTimerDefaultSec: { type: Number, default: 90 },
  },
  { timestamps: true }
);

module.exports = mongoose.model('UserPreferences', userPreferencesSchema);
