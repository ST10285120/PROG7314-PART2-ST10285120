const {
  calculateWorkoutXp,
  calculateTotalVolume,
  updateStreak,
  levelForXp,
} = require('../src/services/gamificationService');

describe('gamificationService', () => {
  describe('calculateWorkoutXp', () => {
    it('awards the completion bonus plus per-set XP', () => {
      const workout = {
        exercises: [
          { sets: [{ isPersonalRecord: false }, { isPersonalRecord: false }] },
          { sets: [{ isPersonalRecord: false }] },
        ],
      };
      expect(calculateWorkoutXp(workout)).toBe(35);
    });

    it('adds a bonus for each personal record set', () => {
      const workout = {
        exercises: [{ sets: [{ isPersonalRecord: true }, { isPersonalRecord: false }] }],
      };
      expect(calculateWorkoutXp(workout)).toBe(55);
    });

    it('returns just the completion bonus for a workout with no sets', () => {
      const workout = { exercises: [] };
      expect(calculateWorkoutXp(workout)).toBe(20);
    });
  });

  describe('calculateTotalVolume', () => {
    it('sums reps * weightKg across all sets and exercises', () => {
      const workout = {
        exercises: [
          { sets: [{ reps: 8, weightKg: 60 }, { reps: 8, weightKg: 60 }] },
          { sets: [{ reps: 10, weightKg: 20 }] },
        ],
      };
      expect(calculateTotalVolume(workout)).toBe(8 * 60 + 8 * 60 + 10 * 20);
    });
  });

  describe('updateStreak', () => {
    it('starts a new streak at 1 for a user with no prior workout', () => {
      const user = { currentStreak: 0, longestStreak: 0, lastWorkoutDate: null };
      const result = updateStreak(user, new Date('2026-01-10T18:00:00Z'));
      expect(result.currentStreak).toBe(1);
      expect(result.longestStreak).toBe(1);
    });

    it('does not increment the streak twice for the same calendar day', () => {
      const user = { currentStreak: 3, longestStreak: 5, lastWorkoutDate: new Date('2026-01-10T08:00:00Z') };
      const result = updateStreak(user, new Date('2026-01-10T20:00:00Z'));
      expect(result.currentStreak).toBe(3);
    });

    it('increments the streak for a workout the following day', () => {
      const user = { currentStreak: 3, longestStreak: 5, lastWorkoutDate: new Date('2026-01-10T18:00:00Z') };
      const result = updateStreak(user, new Date('2026-01-11T18:00:00Z'));
      expect(result.currentStreak).toBe(4);
      expect(result.longestStreak).toBe(5);
    });

    it('resets the streak to 1 after a gap beyond the grace window', () => {
      const user = { currentStreak: 6, longestStreak: 6, lastWorkoutDate: new Date('2026-01-10T18:00:00Z') };
      const result = updateStreak(user, new Date('2026-01-14T18:00:00Z'));
      expect(result.currentStreak).toBe(1);
      expect(result.longestStreak).toBe(6);
    });
  });

  describe('levelForXp', () => {
    it('starts at level 1 with zero XP', () => {
      expect(levelForXp(0)).toBe(1);
    });

    it('increases every 200 XP', () => {
      expect(levelForXp(199)).toBe(1);
      expect(levelForXp(200)).toBe(2);
      expect(levelForXp(450)).toBe(3);
    });
  });
});
