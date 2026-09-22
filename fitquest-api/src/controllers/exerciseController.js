const Exercise = require('../models/Exercise');

async function listExercises(req, res) {
  const filter = {};
  if (req.query.muscleGroup) filter.muscleGroup = req.query.muscleGroup;
  if (req.query.equipment) filter.equipment = req.query.equipment;

  const exercises = await Exercise.find(filter).sort({ name: 1 });
  return res.json(exercises);
}

module.exports = { listExercises };
