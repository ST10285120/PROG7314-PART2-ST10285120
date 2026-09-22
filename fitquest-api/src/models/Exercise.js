const mongoose = require('mongoose');

const exerciseSchema = new mongoose.Schema({
  name: { type: String, required: true },
  muscleGroup: {
    type: String,
    enum: ['CHEST', 'BACK', 'LEGS', 'SHOULDERS', 'ARMS', 'CORE'],
    required: true,
  },
  equipment: { type: String, required: true },
  type: { type: String, enum: ['STRENGTH', 'CARDIO'], required: true },
});

module.exports = mongoose.model('Exercise', exerciseSchema);
