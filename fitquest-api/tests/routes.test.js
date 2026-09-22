require('./setup');
process.env.JWT_SECRET = 'test-secret';

jest.mock('../src/services/ssoService', () => ({
  verifySsoToken: jest.fn(),
  exchangeGitHubCode: jest.fn(),
  verifyGitHubToken: jest.fn(),
}));

const request = require('supertest');
const createApp = require('../src/app');
const { verifySsoToken, exchangeGitHubCode, verifyGitHubToken } = require('../src/services/ssoService');
const Exercise = require('../src/models/Exercise');

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

describe('GET /health', () => {
  it('returns ok', async () => {
    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('ok');
  });
});
