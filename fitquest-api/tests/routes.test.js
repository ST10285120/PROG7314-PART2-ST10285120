require('./setup');
process.env.JWT_SECRET = 'test-secret';

jest.mock('../src/services/ssoService', () => ({
  verifySsoToken: jest.fn(),
  exchangeGitHubCode: jest.fn(),
  verifyGitHubToken: jest.fn(),
}));

jest.mock('../src/services/blobStorageService', () => ({
  isConfigured: jest.fn(() => true),
  newBlobName: jest.fn((folder) => `${folder}/test-blob.jpg`),
  uploadBlob: jest.fn(),
  downloadBlob: jest.fn(),
  deleteBlob: jest.fn(async () => {}),
}));

const request = require('supertest');
const createApp = require('../src/app');
const { verifySsoToken, exchangeGitHubCode, verifyGitHubToken } = require('../src/services/ssoService');
const Exercise = require('../src/models/Exercise');
const Badge = require('../src/models/Badge');
const User = require('../src/models/User');
const blobStorage = require('../src/services/blobStorageService');

const app = createApp();

async function signInTestUser() {
  verifySsoToken.mockResolvedValueOnce({
    ssoProvider: 'GOOGLE',
    ssoSubjectId: 'google-subject-123',
    email: 'test@example.com',
    username: 'Test User',
  });
  const res = await request(app)
    .post('/api/auth/sso')
    .send({ provider: 'GOOGLE', idToken: 'fake-token' });
  return res.body.token;
}

describe('POST /api/auth/sso', () => {
  it('creates a new account on first sign-in and returns 201 with a token', async () => {
    verifySsoToken.mockResolvedValueOnce({
      ssoProvider: 'GOOGLE',
      ssoSubjectId: 'abc123',
      email: 'new@example.com',
      username: 'New User',
    });
    const res = await request(app)
      .post('/api/auth/sso')
      .send({ provider: 'GOOGLE', idToken: 'fake' });

    expect(res.status).toBe(201);
    expect(res.body.token).toBeTruthy();
    expect(res.body.user.email).toBe('new@example.com');
  });

  it('matches the existing account on a second sign-in with the same identity', async () => {
    verifySsoToken.mockResolvedValue({
      ssoProvider: 'GOOGLE',
      ssoSubjectId: 'same-id',
      email: 'repeat@example.com',
      username: 'Repeat User',
    });
    const first = await request(app).post('/api/auth/sso').send({ provider: 'GOOGLE', idToken: 'x' });
    const second = await request(app).post('/api/auth/sso').send({ provider: 'GOOGLE', idToken: 'x' });

    expect(first.status).toBe(201);
    expect(second.status).toBe(200);
    expect(second.body.user.id).toBe(first.body.user.id);
  });

  it('returns 401 when the SSO token fails verification', async () => {
    verifySsoToken.mockRejectedValueOnce(new Error('bad token'));
    const res = await request(app).post('/api/auth/sso').send({ provider: 'GOOGLE', idToken: 'bad' });
    expect(res.status).toBe(401);
  });

  it('returns 400 for an unsupported provider', async () => {
    const res = await request(app).post('/api/auth/sso').send({ provider: 'FACEBOOK', idToken: 'x' });
    expect(res.status).toBe(400);
  });

  it('returns 400 when GITHUB is used with idToken instead of code', async () => {
    const res = await request(app).post('/api/auth/sso').send({ provider: 'GITHUB', idToken: 'x' });
    expect(res.status).toBe(400);
  });

  it('exchanges a GitHub authorization code for a token and signs the user in', async () => {
    exchangeGitHubCode.mockResolvedValueOnce('gh-access-token');
    verifyGitHubToken.mockResolvedValueOnce({
      ssoProvider: 'GITHUB',
      ssoSubjectId: 'gh-42',
      email: 'ghuser@example.com',
      username: 'GitHub User',
    });
    const res = await request(app).post('/api/auth/sso').send({ provider: 'GITHUB', code: 'auth-code-123' });

    expect(exchangeGitHubCode).toHaveBeenCalledWith('auth-code-123');
    expect(res.status).toBe(201);
    expect(res.body.user.email).toBe('ghuser@example.com');
  });

  it('returns 409 when a second provider uses the same email', async () => {
    verifySsoToken.mockResolvedValueOnce({
      ssoProvider: 'GOOGLE',
      ssoSubjectId: 'google-1',
      email: 'both@example.com',
      username: 'Both',
    });
    await request(app).post('/api/auth/sso').send({ provider: 'GOOGLE', idToken: 'x' });

    exchangeGitHubCode.mockResolvedValueOnce('gh-token');
    verifyGitHubToken.mockResolvedValueOnce({
      ssoProvider: 'GITHUB',
      ssoSubjectId: 'gh-1',
      email: 'both@example.com',
      username: 'Both',
    });
    const res = await request(app).post('/api/auth/sso').send({ provider: 'GITHUB', code: 'c' });

    expect(res.status).toBe(409);
  });

  it('returns 401 when the GitHub code exchange fails', async () => {
    exchangeGitHubCode.mockRejectedValueOnce(new Error('bad code'));
    const res = await request(app).post('/api/auth/sso').send({ provider: 'GITHUB', code: 'expired' });
    expect(res.status).toBe(401);
  });
});

describe('authGuard (FR9)', () => {
  it('rejects requests with no Authorization header', async () => {
    const res = await request(app).get('/api/users/me/preferences');
    expect(res.status).toBe(401);
  });

  it('rejects requests with an invalid token', async () => {
    const res = await request(app)
      .get('/api/users/me/preferences')
      .set('Authorization', 'Bearer not-a-real-token');
    expect(res.status).toBe(401);
  });

  it('accepts requests with a valid token', async () => {
    const token = await signInTestUser();
    const res = await request(app)
      .get('/api/users/me/preferences')
      .set('Authorization', `Bearer ${token}`);
    expect(res.status).toBe(200);
  });
});

describe('User preferences', () => {
  it('returns default preferences for a new user', async () => {
    const token = await signInTestUser();
    const res = await request(app)
      .get('/api/users/me/preferences')
      .set('Authorization', `Bearer ${token}`);
    expect(res.body.units).toBe('KG');
    expect(res.body.theme).toBe('SYSTEM');
  });

  it('persists updated preferences', async () => {
    const token = await signInTestUser();
    const update = await request(app)
      .put('/api/users/me/preferences')
      .set('Authorization', `Bearer ${token}`)
      .send({ units: 'LB', theme: 'DARK' });
    expect(update.status).toBe(200);
    expect(update.body.units).toBe('LB');
    expect(update.body.theme).toBe('DARK');

    const reread = await request(app)
      .get('/api/users/me/preferences')
      .set('Authorization', `Bearer ${token}`);
    expect(reread.body.units).toBe('LB');
  });
});

describe('Workout logging flow (FR3, FR5)', () => {
  it('logs sets and awards XP, level and streak on completion', async () => {
    const token = await signInTestUser();
    const auth = (req) => req.set('Authorization', `Bearer ${token}`);

    const exercise = await Exercise.create({
      name: 'Test Bench Press',
      muscleGroup: 'CHEST',
      equipment: 'Barbell',
      type: 'STRENGTH',
    });

    const created = await auth(request(app).post('/api/workouts')).send({ title: 'Push Day' });
    expect(created.status).toBe(201);
    const workoutId = created.body.workoutId;

    const addEx = await auth(request(app).post(`/api/workouts/${workoutId}/exercises`)).send({
      exerciseId: exercise._id,
      orderIndex: 0,
    });
    expect(addEx.status).toBe(201);
    const weId = addEx.body.workoutExerciseId;

    const setRes = await auth(
      request(app).post(`/api/workouts/${workoutId}/exercises/${weId}/sets`)
    ).send({ setNumber: 1, reps: 8, weightKg: 60 });
    expect(setRes.status).toBe(201);
    expect(setRes.body.isPersonalRecord).toBe(false);

    const complete = await auth(request(app).patch(`/api/workouts/${workoutId}/complete`)).send({});
    expect(complete.status).toBe(200);
    expect(complete.body.xpEarned).toBe(25);
    expect(complete.body.streak).toBe(1);
    expect(complete.body.newLevel).toBe(1);
  });

  it('rejects logging sets on another user\'s workout', async () => {
    const tokenA = await signInTestUser();
    exchangeGitHubCode.mockResolvedValueOnce('gh-access-token');
    verifyGitHubToken.mockResolvedValueOnce({
      ssoProvider: 'GITHUB',
      ssoSubjectId: 'gh-1',
      email: 'other@example.com',
      username: 'Other User',
    });
    const signInB = await request(app).post('/api/auth/sso').send({ provider: 'GITHUB', code: 'auth-code' });
    const tokenB = signInB.body.token;

    const created = await request(app)
      .post('/api/workouts')
      .set('Authorization', `Bearer ${tokenA}`)
      .send({ title: 'User A Workout' });

    const res = await request(app)
      .post(`/api/workouts/${created.body.workoutId}/exercises`)
      .set('Authorization', `Bearer ${tokenB}`)
      .send({ exerciseId: '507f1f77bcf86cd799439011', orderIndex: 0 });

    expect(res.status).toBe(404);
  });
});

describe('Regression fixes from Part 2 feedback', () => {
  async function completedWorkout(token, exerciseId, sets) {
    const auth = (req) => req.set('Authorization', `Bearer ${token}`);
    const created = await auth(request(app).post('/api/workouts')).send({ title: 'Day' });
    const id = created.body.workoutId;
    const we = await auth(request(app).post(`/api/workouts/${id}/exercises`)).send({ exerciseId, orderIndex: 0 });
    for (const [index, weightKg] of sets.entries()) {
      await auth(request(app).post(`/api/workouts/${id}/exercises/${we.body.workoutExerciseId}/sets`))
        .send({ setNumber: index + 1, reps: 5, weightKg });
    }
    await auth(request(app).patch(`/api/workouts/${id}/complete`)).send({});
    return id;
  }

  it('awards XP only once when the same workout is completed twice', async () => {
    const token = await signInTestUser();
    const exercise = await Exercise.create({ name: 'Squat', muscleGroup: 'LEGS', equipment: 'Barbell', type: 'STRENGTH' });
    const id = await completedWorkout(token, exercise._id, [100]);

    const again = await request(app)
      .patch(`/api/workouts/${id}/complete`)
      .set('Authorization', `Bearer ${token}`)
      .send({});
    const profile = await request(app).get('/api/users/me').set('Authorization', `Bearer ${token}`);

    expect(again.status).toBe(200);
    expect(again.body.xpEarned).toBe(25);
    expect(profile.body.xpTotal).toBe(25);
  });

  it('returns a populated muscle group split and PRs by exercise name', async () => {
    const token = await signInTestUser();
    const exercise = await Exercise.create({ name: 'Squat', muscleGroup: 'LEGS', equipment: 'Barbell', type: 'STRENGTH' });
    await completedWorkout(token, exercise._id, [100]);

    const res = await request(app).get('/api/users/me/progress').set('Authorization', `Bearer ${token}`);

    expect(res.body.muscleGroupSplit.LEGS).toBe(500);
    expect(res.body.prsByExercise.Squat).toBe(100);
    expect(res.body.totalWorkouts).toBe(1);
  });

  it('starts personal records again and keeps workouts and XP', async () => {
    const token = await signInTestUser();
    const auth = (req) => req.set('Authorization', `Bearer ${token}`);
    const exercise = await Exercise.create({ name: 'Squat', muscleGroup: 'LEGS', equipment: 'Barbell', type: 'STRENGTH' });
    await completedWorkout(token, exercise._id, [100]);
    await completedWorkout(token, exercise._id, [110]);
    const before = await auth(request(app).get('/api/users/me'));

    const reset = await auth(request(app).post('/api/users/me/personal-records/reset'));
    const afterReset = await auth(request(app).get('/api/users/me/progress'));
    const after = await auth(request(app).get('/api/users/me'));
    const history = await auth(request(app).get('/api/workouts/history'));

    expect(reset.status).toBe(200);
    expect(afterReset.body.prsByExercise).toEqual({});
    expect(afterReset.body.totalWorkouts).toBe(2);
    expect(after.body.xpTotal).toBe(before.body.xpTotal);
    const flags = history.body.flatMap((w) => w.exercises.flatMap((e) => e.sets.map((s) => s.isPersonalRecord)));
    expect(flags).toEqual([false, false]);

    await completedWorkout(token, exercise._id, [90]);
    const firstBack = await auth(request(app).get('/api/workouts/history'));
    expect(firstBack.body[0].exercises[0].sets[0].isPersonalRecord).toBe(false);

    await completedWorkout(token, exercise._id, [95]);
    const secondBack = await auth(request(app).get('/api/workouts/history'));
    const progress = await auth(request(app).get('/api/users/me/progress'));
    const newest = secondBack.body.find((w) => w.exercises[0].sets[0].weightKg === 95);
    expect(newest.exercises[0].sets[0].isPersonalRecord).toBe(true);
    expect(progress.body.prsByExercise.Squat).toBe(95);
  });
});

describe('POST /api/workouts/sync (offline replay)', () => {
  const body = (exerciseId, completedAt) => ({
    clientId: 'workout-uuid-1',
    title: 'Offline Day',
    startedAt: '2026-03-01T08:00:00.000Z',
    completedAt,
    exercises: [
      {
        clientId: 'we-uuid-1',
        exerciseId,
        orderIndex: 0,
        sets: [
          { clientId: 'set-uuid-1', setNumber: 1, reps: 5, weightKg: 100 },
          { clientId: 'set-uuid-2', setNumber: 2, reps: 5, weightKg: 100 },
        ],
      },
    ],
  });

  it('stores a full workout and returns the authoritative result', async () => {
    const token = await signInTestUser();
    const exercise = await Exercise.create({ name: 'Row', muscleGroup: 'BACK', equipment: 'Barbell', type: 'STRENGTH' });

    const res = await request(app)
      .post('/api/workouts/sync')
      .set('Authorization', `Bearer ${token}`)
      .send(body(exercise._id, '2026-03-01T09:00:00.000Z'));

    expect(res.status).toBe(200);
    expect(res.body.completion.xpEarned).toBe(30);
    expect(res.body.sets).toHaveLength(2);
  });

  it('does not duplicate data or XP when the same sync is replayed', async () => {
    const token = await signInTestUser();
    const exercise = await Exercise.create({ name: 'Row', muscleGroup: 'BACK', equipment: 'Barbell', type: 'STRENGTH' });
    const payload = body(exercise._id, '2026-03-01T09:00:00.000Z');
    const send = () => request(app).post('/api/workouts/sync').set('Authorization', `Bearer ${token}`).send(payload);

    await send();
    const replay = await send();
    const history = await request(app).get('/api/workouts/history').set('Authorization', `Bearer ${token}`);
    const profile = await request(app).get('/api/users/me').set('Authorization', `Bearer ${token}`);

    expect(replay.status).toBe(200);
    expect(history.body).toHaveLength(1);
    expect(history.body[0].exercises[0].sets).toHaveLength(2);
    expect(history.body[0].exercises[0].name).toBe('Row');
    expect(profile.body.xpTotal).toBe(30);
  });

  it('adds new sets when an unfinished workout is synced again', async () => {
    const token = await signInTestUser();
    const exercise = await Exercise.create({ name: 'Row', muscleGroup: 'BACK', equipment: 'Barbell', type: 'STRENGTH' });
    const payload = body(exercise._id);
    const send = (data) => request(app).post('/api/workouts/sync').set('Authorization', `Bearer ${token}`).send(data);

    await send(payload);
    payload.exercises[0].sets.push({ clientId: 'set-uuid-3', setNumber: 3, reps: 5, weightKg: 110 });
    const res = await send(payload);

    expect(res.body.sets).toHaveLength(3);
    expect(res.body.completion).toBeNull();
  });

  it('removes a set the user deleted before finishing', async () => {
    const token = await signInTestUser();
    const exercise = await Exercise.create({ name: 'Row', muscleGroup: 'BACK', equipment: 'Barbell', type: 'STRENGTH' });
    const payload = body(exercise._id);
    const send = (data) => request(app).post('/api/workouts/sync').set('Authorization', `Bearer ${token}`).send(data);

    await send(payload);
    payload.exercises[0].sets.pop();
    const res = await send(payload);

    expect(res.body.sets).toHaveLength(1);
  });

  it('rejects invalid input with 400', async () => {
    const token = await signInTestUser();
    const res = await request(app)
      .post('/api/workouts/sync')
      .set('Authorization', `Bearer ${token}`)
      .send({ clientId: 'x', title: '', exercises: 'nope' });
    expect(res.status).toBe(400);
  });
});

describe('Badges, notifications and photos', () => {
  it('lists locked badges with progress', async () => {
    const token = await signInTestUser();
    await Badge.create({ name: '10 Workouts', description: 'd', criteriaType: 'WORKOUT_COUNT', criteriaValue: 10 });

    const res = await request(app).get('/api/badges').set('Authorization', `Bearer ${token}`);

    expect(res.body[0]).toMatchObject({ name: '10 Workouts', target: 10, progress: 0, earnedAt: null });
  });

  it('stores a device token only once', async () => {
    const token = await signInTestUser();
    const send = () => request(app)
      .post('/api/users/me/device-tokens')
      .set('Authorization', `Bearer ${token}`)
      .send({ token: 'fcm-token' });

    await send();
    const res = await send();
    const user = await User.findOne({ email: 'test@example.com' });

    expect(res.status).toBe(204);
    expect(user.deviceTokens).toEqual(['fcm-token']);
  });

  it('uploads a JPEG progress photo and lists it', async () => {
    const token = await signInTestUser();
    const jpeg = Buffer.from([0xff, 0xd8, 0xff, 0xe0]);

    const upload = await request(app)
      .post('/api/users/me/photos')
      .set('Authorization', `Bearer ${token}`)
      .set('Content-Type', 'image/jpeg')
      .send(jpeg);
    const list = await request(app).get('/api/users/me/photos').set('Authorization', `Bearer ${token}`);

    expect(upload.status).toBe(201);
    expect(list.body).toHaveLength(1);
    expect(blobStorage.uploadBlob).toHaveBeenCalled();
  });

  it('rejects a file that only claims to be a JPEG', async () => {
    const token = await signInTestUser();
    const res = await request(app)
      .post('/api/users/me/avatar')
      .set('Authorization', `Bearer ${token}`)
      .set('Content-Type', 'image/jpeg')
      .send(Buffer.from('not an image'));
    expect(res.status).toBe(400);
  });
});

describe('Routines and account deletion', () => {
  const routine = (exerciseId) => ({
    id: 'routine-1',
    name: 'Push Day',
    exercises: [{ exerciseId: String(exerciseId), name: 'Bench', muscleGroup: 'CHEST' }],
  });

  it('saves routines and returns them on a later read', async () => {
    const token = await signInTestUser();
    const exercise = await Exercise.create({ name: 'Bench', muscleGroup: 'CHEST', equipment: 'Barbell', type: 'STRENGTH' });

    const saved = await request(app)
      .put('/api/users/me/routines')
      .set('Authorization', `Bearer ${token}`)
      .send({ routines: [routine(exercise._id)] });
    const read = await request(app).get('/api/users/me/routines').set('Authorization', `Bearer ${token}`);

    expect(saved.status).toBe(200);
    expect(read.body.routines[0].name).toBe('Push Day');
    expect(read.body.routines[0].exercises).toHaveLength(1);
  });

  it('rejects a routine with no exercises', async () => {
    const token = await signInTestUser();
    const res = await request(app)
      .put('/api/users/me/routines')
      .set('Authorization', `Bearer ${token}`)
      .send({ routines: [{ id: 'r', name: 'Empty', exercises: [] }] });
    expect(res.status).toBe(400);
  });

  it('deletes the account and its data, and the old token stops working', async () => {
    const token = await signInTestUser();
    await request(app).post('/api/workouts').set('Authorization', `Bearer ${token}`).send({ title: 'Day' });

    const res = await request(app).delete('/api/users/me').set('Authorization', `Bearer ${token}`);
    const after = await request(app).get('/api/users/me').set('Authorization', `Bearer ${token}`);

    expect(res.status).toBe(204);
    expect(after.status).toBe(401);
    expect(await User.countDocuments({ email: 'test@example.com' })).toBe(0);
  });
});

describe('GET /health', () => {
  it('returns ok', async () => {
    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('ok');
  });
});
