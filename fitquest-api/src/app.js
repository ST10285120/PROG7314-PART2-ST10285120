/**
 * Builds the Express app without listening, so tests can use it directly.
 */
const express = require('express');
const cors = require('cors');
const routes = require('./routes');

function createApp() {
  const app = express();
  app.use(cors());
  app.use(express.json());
  // Express 5 leaves req.body undefined when a request has no JSON body.
  app.use((req, res, next) => {
    if (req.body === undefined) req.body = {};
    next();
  });

  app.get('/health', (req, res) => res.json({ status: 'ok' }));
  app.use('/api', routes);

  // Last-resort handler. Body-parser errors carry their own status (400 for bad JSON,
  // 413 for an oversized photo); anything else is an unexpected 500.
  app.use((err, req, res, next) => {
    const status = err.status || err.statusCode || 500;
    if (status >= 500) console.error(err);
    const message = status >= 500 ? 'Internal server error' : err.message;
    res.status(status).json({ error: message });
  });

  return app;
}

module.exports = createApp;
