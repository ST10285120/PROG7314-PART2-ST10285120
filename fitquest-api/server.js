require('dotenv').config();
const createApp = require('./src/app');
const { connectDb } = require('./src/config/db');

const PORT = process.env.PORT || 3000;
const MONGO_URI = process.env.MONGO_URI;

async function start() {
  if (!MONGO_URI) {
    console.error('MONGO_URI is not set. Copy .env.example to .env and fill it in.');
    process.exit(1);
  }
  await connectDb(MONGO_URI);
  console.log('Connected to MongoDB');

  const app = createApp();
  app.listen(PORT, () => {
    console.log(`FitQuest API listening on port ${PORT}`);
  });
}

start().catch((err) => {
  console.error('Failed to start server:', err);
  process.exit(1);
});
