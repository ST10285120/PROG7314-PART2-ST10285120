/**
 * Sends real-time notifications with the FCM HTTP v1 API. Messages are data-only,
 * so the app builds the text itself in the user's chosen language.
 *
 * A failed push must never fail the request that caused it, so callers can fire
 * and forget; every error is logged here.
 *
 * Reference: Google (2026) Send a message using FCM HTTP v1 API.
 * https://firebase.google.com/docs/cloud-messaging/send-message
 */
const { JWT } = require('google-auth-library');

const FCM_SCOPE = 'https://www.googleapis.com/auth/firebase.messaging';

function loadServiceAccount() {
  const encoded = process.env.FIREBASE_SERVICE_ACCOUNT_BASE64;
  if (!encoded) return null;
  return JSON.parse(Buffer.from(encoded, 'base64').toString('utf8'));
}

async function sendToToken(account, accessToken, deviceToken, data) {
  const res = await fetch(
    `https://fcm.googleapis.com/v1/projects/${account.project_id}/messages:send`,
    {
      method: 'POST',
      headers: { Authorization: `Bearer ${accessToken}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        message: { token: deviceToken, data, android: { priority: 'high' } },
      }),
    }
  );
  return res.status;
}

/** Sends data to all of a user's devices; returns how many accepted it. */
async function sendToUser(user, data) {
  try {
    const account = loadServiceAccount();
    const tokens = user.deviceTokens || [];
    if (!account || tokens.length === 0) return 0;

    const client = new JWT({
      email: account.client_email,
      key: account.private_key,
      scopes: [FCM_SCOPE],
    });
    const { token: accessToken } = await client.getAccessToken();

    const statuses = await Promise.all(
      tokens.map((deviceToken) => sendToToken(account, accessToken, deviceToken, data))
    );

    // FCM answers 404 for a token whose app was uninstalled, so stop using it.
    const staleTokens = tokens.filter((_, index) => statuses[index] === 404);
    if (staleTokens.length > 0) {
      await user.updateOne({ $pull: { deviceTokens: { $in: staleTokens } } });
    }
    return statuses.filter((status) => status === 200).length;
  } catch (err) {
    console.error('Push notification failed:', err.message);
    return 0;
  }
}

module.exports = { sendToUser };
