const User = require('../models/User');
const UserPreferences = require('../models/UserPreferences');
const { verifySsoToken, exchangeGitHubCode, verifyGitHubToken } = require('../services/ssoService');
const { issueToken } = require('../services/jwtService');

async function ssoSignIn(req, res) {
  const { provider, idToken, code } = req.body;

  if (!provider) {
    return res.status(400).json({ error: 'provider is required' });
  }
  if (!['GOOGLE', 'GITHUB'].includes(provider)) {
    return res.status(400).json({ error: 'provider must be GOOGLE or GITHUB' });
  }
  if (provider === 'GOOGLE' && !idToken) {
    return res.status(400).json({ error: 'idToken is required for GOOGLE' });
  }
  if (provider === 'GITHUB' && !code) {
    return res.status(400).json({ error: 'code is required for GITHUB' });
  }

  let profile;
  try {
    if (provider === 'GOOGLE') {
      profile = await verifySsoToken('GOOGLE', idToken);
    } else {
      const accessToken = await exchangeGitHubCode(code);
      profile = await verifyGitHubToken(accessToken);
    }
  } catch (err) {
    return res.status(401).json({ error: 'SSO verification failed', detail: err.message });
  }

  let user = await User.findOne({
    ssoProvider: profile.ssoProvider,
    ssoSubjectId: profile.ssoSubjectId,
  });

  let isNewUser = false;
  if (!user) {
    user = await User.create({
      username: profile.username,
      email: profile.email,
      ssoProvider: profile.ssoProvider,
      ssoSubjectId: profile.ssoSubjectId,
    });
    await UserPreferences.create({ userId: user._id });
    isNewUser = true;
  }

  const token = issueToken(user);
  return res.status(isNewUser ? 201 : 200).json({
    token,
    user: {
      id: user._id,
      username: user.username,
      email: user.email,
      xpTotal: user.xpTotal,
      level: user.level,
      currentStreak: user.currentStreak,
    },
  });
}

module.exports = { ssoSignIn };
