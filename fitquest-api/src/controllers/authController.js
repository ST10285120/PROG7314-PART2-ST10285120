/**
 * Verifies an SSO credential, creates or matches the user, and issues a FitQuest JWT.
 */
const User = require('../models/User');
const UserPreferences = require('../models/UserPreferences');
const { verifySsoToken, exchangeGitHubCode, verifyGitHubToken } = require('../services/ssoService');
const { issueToken } = require('../services/jwtService');
const { toUserResponse } = require('../utils/userResponse');

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
    return res.status(401).json({ error: 'SSO verification failed' });
  }

  let user = await User.findOne({
    ssoProvider: profile.ssoProvider,
    ssoSubjectId: profile.ssoSubjectId,
  });

  let isNewUser = false;
  if (!user) {
    try {
      user = await User.create({
        username: profile.username,
        email: profile.email,
        ssoProvider: profile.ssoProvider,
        ssoSubjectId: profile.ssoSubjectId,
      });
    } catch (err) {
      // Emails are unique, so a second provider with the same email cannot make a new
      // account. Accounts are not merged automatically, as that could hand one over.
      if (err.code === 11000) {
        return res.status(409).json({ error: 'This email already uses another sign-in method' });
      }
      throw err;
    }
    await UserPreferences.create({ userId: user._id });
    isNewUser = true;
  }

  const token = issueToken(user);
  return res.status(isNewUser ? 201 : 200).json({
    token,
    user: toUserResponse(user),
  });
}

module.exports = { ssoSignIn };
