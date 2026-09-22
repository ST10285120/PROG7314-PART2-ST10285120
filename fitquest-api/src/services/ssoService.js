const { OAuth2Client } = require('google-auth-library');

const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

async function verifyGoogleToken(idToken) {
  const ticket = await googleClient.verifyIdToken({
    idToken,
    audience: process.env.GOOGLE_CLIENT_ID,
  });
  const payload = ticket.getPayload();
  if (!payload || !payload.sub) {
    throw new Error('Invalid Google ID token payload');
  }
  return {
    ssoProvider: 'GOOGLE',
    ssoSubjectId: payload.sub,
    email: payload.email,
    username: payload.name || (payload.email ? payload.email.split('@')[0] : 'FitQuest User'),
  };
}

async function verifyGitHubToken(accessToken) {
  const res = await fetch('https://api.github.com/user', {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      Accept: 'application/vnd.github+json',
      'User-Agent': 'FitQuest-API',
    },
  });
  if (!res.ok) {
    throw new Error(`GitHub token verification failed with status ${res.status}`);
  }
  const profile = await res.json();
  if (!profile || !profile.id) {
    throw new Error('Invalid GitHub profile response');
  }

  let email = profile.email;
  if (!email) {
    const emailRes = await fetch('https://api.github.com/user/emails', {
      headers: {
        Authorization: `Bearer ${accessToken}`,
        Accept: 'application/vnd.github+json',
        'User-Agent': 'FitQuest-API',
      },
    });
    if (emailRes.ok) {
      const emails = await emailRes.json();
      const primary = Array.isArray(emails) ? emails.find((e) => e.primary) : null;
      email = primary ? primary.email : `${profile.id}+${profile.login}@users.noreply.github.com`;
    } else {
      email = `${profile.id}+${profile.login}@users.noreply.github.com`;
    }
  }

  return {
    ssoProvider: 'GITHUB',
    ssoSubjectId: String(profile.id),
    email,
    username: profile.name || profile.login,
  };
}

async function exchangeGitHubCode(code) {
  const res = await fetch('https://github.com/login/oauth/access_token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({
      client_id: process.env.GITHUB_CLIENT_ID,
      client_secret: process.env.GITHUB_CLIENT_SECRET,
      code,
      redirect_uri: process.env.GITHUB_REDIRECT_URI,
    }),
  });
  if (!res.ok) {
    throw new Error(`GitHub code exchange failed with status ${res.status}`);
  }
  const data = await res.json();
  if (!data.access_token) {
    throw new Error(data.error_description || 'GitHub did not return an access token');
  }
  return data.access_token;
}

async function verifySsoToken(provider, token) {
  if (provider === 'GOOGLE') return verifyGoogleToken(token);
  if (provider === 'GITHUB') return verifyGitHubToken(token);
  throw new Error(`Unsupported SSO provider: ${provider}`);
}

module.exports = { verifySsoToken, verifyGoogleToken, verifyGitHubToken, exchangeGitHubCode };
