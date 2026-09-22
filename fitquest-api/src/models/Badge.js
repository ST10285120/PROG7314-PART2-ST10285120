const mongoose = require('mongoose');

const badgeSchema = new mongoose.Schema({
  name: { type: String, required: true },
  description: { type: String, required: true },
  iconAsset: { type: String, default: 'badge_default' },
  criteriaType: {
    type: String,
    enum: ['STREAK_DAYS', 'WORKOUT_COUNT', 'PERSONAL_RECORD_COUNT'],
    required: true,
  },
  criteriaValue: { type: Number, required: true },
});

module.exports = mongoose.model('Badge', badgeSchema);
