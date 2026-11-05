/**
 * The public shape of a user. Keeping it in one place stops sign-in and /users/me
 * drifting apart, and guarantees deviceTokens never leave the server.
 */
function toUserResponse(user) {
  return {
    id: user._id,
    username: user.username,
    email: user.email,
    xpTotal: user.xpTotal,
    level: user.level,
    currentStreak: user.currentStreak,
    longestStreak: user.longestStreak,
    lastWorkoutDate: user.lastWorkoutDate,
    avatarPath: user.avatarBlobName
      ? `api/users/me/avatar?v=${encodeURIComponent(user.avatarBlobName)}`
      : null,
  };
}

module.exports = { toUserResponse };
