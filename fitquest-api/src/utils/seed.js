require('dotenv').config();
const { connectDb, disconnectDb } = require('../config/db');
const Exercise = require('../models/Exercise');
const Badge = require('../models/Badge');

const exercises = [
  { name: 'Barbell Bench Press', muscleGroup: 'CHEST', equipment: 'Barbell', type: 'STRENGTH' },
  { name: 'Incline Dumbbell Press', muscleGroup: 'CHEST', equipment: 'Dumbbell', type: 'STRENGTH' },
  { name: 'Push-Up', muscleGroup: 'CHEST', equipment: 'Bodyweight', type: 'STRENGTH' },
  { name: 'Barbell Back Squat', muscleGroup: 'LEGS', equipment: 'Barbell', type: 'STRENGTH' },
  { name: 'Romanian Deadlift', muscleGroup: 'LEGS', equipment: 'Barbell', type: 'STRENGTH' },
  { name: 'Walking Lunge', muscleGroup: 'LEGS', equipment: 'Dumbbell', type: 'STRENGTH' },
  { name: 'Deadlift', muscleGroup: 'BACK', equipment: 'Barbell', type: 'STRENGTH' },
  { name: 'Pull-Up', muscleGroup: 'BACK', equipment: 'Bodyweight', type: 'STRENGTH' },
  { name: 'Bent-Over Row', muscleGroup: 'BACK', equipment: 'Barbell', type: 'STRENGTH' },
  { name: 'Overhead Press', muscleGroup: 'SHOULDERS', equipment: 'Barbell', type: 'STRENGTH' },
  { name: 'Lateral Raise', muscleGroup: 'SHOULDERS', equipment: 'Dumbbell', type: 'STRENGTH' },
  { name: 'Barbell Curl', muscleGroup: 'ARMS', equipment: 'Barbell', type: 'STRENGTH' },
  { name: 'Tricep Dip', muscleGroup: 'ARMS', equipment: 'Bodyweight', type: 'STRENGTH' },
  { name: 'Plank', muscleGroup: 'CORE', equipment: 'Bodyweight', type: 'STRENGTH' },
  { name: 'Hanging Leg Raise', muscleGroup: 'CORE', equipment: 'Bodyweight', type: 'STRENGTH' },
];

const badges = [
  { name: '3-Day Streak', description: 'Logged a workout 3 days in a row.', criteriaType: 'STREAK_DAYS', criteriaValue: 3 },
  { name: '7-Day Streak', description: 'Logged a workout 7 days in a row.', criteriaType: 'STREAK_DAYS', criteriaValue: 7 },
  { name: '30-Day Streak', description: 'Logged a workout 30 days in a row.', criteriaType: 'STREAK_DAYS', criteriaValue: 30 },
  { name: 'First Steps', description: 'Completed your first workout.', criteriaType: 'WORKOUT_COUNT', criteriaValue: 1 },
  { name: '10 Workouts', description: 'Completed 10 workouts.', criteriaType: 'WORKOUT_COUNT', criteriaValue: 10 },
  { name: '50 Workouts', description: 'Completed 50 workouts.', criteriaType: 'WORKOUT_COUNT', criteriaValue: 50 },
  { name: 'First PR', description: 'Set your first personal record.', criteriaType: 'PERSONAL_RECORD_COUNT', criteriaValue: 1 },
  { name: '10 PRs', description: 'Set 10 personal records.', criteriaType: 'PERSONAL_RECORD_COUNT', criteriaValue: 10 },
];

async function seed() {
  await connectDb(process.env.MONGO_URI);

  await Exercise.deleteMany({});
  await Exercise.insertMany(exercises);
  console.log(`Seeded ${exercises.length} exercises`);

  await Badge.deleteMany({});
  await Badge.insertMany(badges);
  console.log(`Seeded ${badges.length} badges`);

  await disconnectDb();
}

seed().catch((err) => {
  console.error('Seed failed:', err);
  process.exit(1);
});
