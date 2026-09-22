# FitQuest API

Hosted REST API backing the FitQuest Android app — a gamified strength-training and
habit-tracking application built for PROG7314/OPSC7312. This API is the sole source of
truth for user accounts, the exercise library, workout history, and all gamification
state (XP, levels, streaks, badges), per the design in the accompanying Planning &
Design document.

## Purpose and scope

The Android client is responsible only for presentation, offline caching, and queuing
sync requests. This API owns every piece of data that must persist beyond a single
device, and is solely responsible for all gamification business logic (XP calculation,
streak evaluation, badge-criteria checking) so that none of it can be spoofed
client-side. See `src/services/gamificationService.js` for that logic in full.

## Architectural choices

- **Node.js + Express** — lightweight routing and a middleware ecosystem that maps
  directly onto this course's web modules.
- **MongoDB (Mongoose)** — the workout → exercises → sets relationship is naturally
  document-shaped, so `WorkoutExercise` and `SetEntry` are modelled as embedded
  sub-documents inside `Workout` rather than separate collections; everything else
  (`User`, `UserPreferences`, `Exercise`, `Badge`, `UserBadge`) is its own collection.
- **SSO-only authentication** — no passwords are ever stored. The Android client signs
  in via Google Identity Services or GitHub OAuth, obtains an identity token from the
  provider, and sends it to `POST /api/auth/sso`. This API verifies that token directly
  with the provider (`src/services/ssoService.js`), creates or matches the
  corresponding `User`, and issues its own short-lived JWT for every request after that.
- **JWT bearer auth** — `src/middleware/authGuard.js` rejects any request to a
  protected route with a missing, malformed, or expired token (401), satisfying FR9.

## Project structure

```
src/
  app.js                  Express app wiring (no server start — used directly by tests)
  config/db.js            Mongoose connect/disconnect helpers
  controllers/            Route handler logic, one file per resource
  middleware/authGuard.js JWT verification middleware
  models/                 Mongoose schemas
  routes/index.js         All route definitions
  services/               SSO verification, JWT issuance, gamification engine
  utils/seed.js           Seeds the exercise library and badge definitions
server.js                 Entry point: connects to MongoDB, then starts listening
tests/                    Jest + Supertest test suite
.github/workflows/        CI (test) and CD (deploy) workflows
```

## Running locally

```bash
npm install
cp .env.example .env        # then fill in MONGO_URI, JWT_SECRET, GOOGLE_CLIENT_ID
npm run seed                 # populates the exercise library and badge definitions
npm run dev                  # starts the API on http://localhost:3000 with auto-reload
```

Getting a free `MONGO_URI`: create a free MongoDB Atlas cluster (atlas.mongodb.com),
add a database user, allow network access from anywhere (0.0.0.0/0) for development,
and copy the connection string from **Connect > Drivers**.

Getting a `GOOGLE_CLIENT_ID`: create an OAuth 2.0 Client ID of type **Android** in the
Google Cloud Console (APIs & Services > Credentials), using the app's package name and
SHA-1 signing fingerprint. The client ID must match the one configured in the Android
app's `google-services.json` / Google Sign-In setup.

## Running the tests

```bash
npm test
```

The suite has two layers:

- **`tests/gamificationService.test.js`** — pure unit tests for XP, streak, and level
  logic. These touch no database and run instantly.
- **`tests/routes.test.js`** — integration tests that exercise the full Express app
  through Supertest, using `mongodb-memory-server` to spin up a temporary, real
  MongoDB instance for the duration of the test run (no external database or network
  service needed — SSO provider calls are mocked so tests never hit Google or GitHub).

GitHub Actions runs the full suite automatically on every push and pull request via
`.github/workflows/test.yml`.

## API reference

All endpoints are prefixed with `/api`. Every route except `POST /auth/sso` requires
an `Authorization: Bearer <JWT>` header.

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/auth/sso` | Verify an SSO identity token; create or match the user; return a FitQuest JWT |
| GET | `/exercises?muscleGroup=&equipment=` | List/filter the shared exercise library |
| GET | `/users/me/preferences` | Read the signed-in user's settings |
| PUT | `/users/me/preferences` | Update units, theme, reminder time, rest-timer default |
| GET | `/users/me/progress` | Volume-by-week and personal-record data for the dashboard |
| GET | `/users/me/badges` | List badges the user has earned |
| GET | `/workouts/suggested?recoveryHours=48` | Rules-based next-workout suggestion |
| POST | `/workouts` | Start a workout |
| POST | `/workouts/:id/exercises` | Add an exercise to a workout |
| POST | `/workouts/:id/exercises/:weId/sets` | Log a set (server determines if it's a PR) |
| PATCH | `/workouts/:id/complete` | Finish a workout — triggers XP/level/streak/badge calculation |

## Deployment

`.github/workflows/deploy.yml` triggers a deploy on Render via a deploy hook whenever
`main` passes CI. Add the Render deploy hook URL as a repository secret named
`RENDER_DEPLOY_HOOK_URL` (Render dashboard > your service > Settings > Deploy Hook).
Render itself is configured to run `npm install` then `npm start`, with `MONGO_URI`,
`JWT_SECRET`, and `GOOGLE_CLIENT_ID` set as environment variables in the Render
dashboard (never committed to the repository).
