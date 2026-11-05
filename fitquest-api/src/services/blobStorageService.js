/**
 * Stores images in a private Azure Blob Storage container using the REST API and a
 * SAS token. The phone never receives the token: uploads and downloads go through
 * this API, which checks the JWT and the owner of the photo first.
 *
 * Reference: Microsoft (2026) Put Blob and Get Blob, Azure Storage REST API.
 * https://learn.microsoft.com/en-us/rest/api/storageservices/put-blob
 */
const { randomUUID } = require('crypto');

function isConfigured() {
  return Boolean(process.env.AZURE_BLOB_CONTAINER_URL && process.env.AZURE_BLOB_SAS_TOKEN);
}

function blobUrl(blobName) {
  const container = process.env.AZURE_BLOB_CONTAINER_URL.replace(/\/$/, '');
  const sas = process.env.AZURE_BLOB_SAS_TOKEN.replace(/^\?/, '');
  const path = blobName.split('/').map(encodeURIComponent).join('/');
  return `${container}/${path}?${sas}`;
}

function newBlobName(folder, userId, contentType) {
  const extension = contentType === 'image/png' ? 'png' : 'jpg';
  return `${folder}/${userId}/${randomUUID()}.${extension}`;
}

async function uploadBlob(blobName, buffer, contentType) {
  const res = await fetch(blobUrl(blobName), {
    method: 'PUT',
    headers: {
      'x-ms-blob-type': 'BlockBlob',
      'Content-Type': contentType,
      'Content-Length': String(buffer.length),
    },
    body: buffer,
  });
  if (!res.ok) throw new Error(`Blob upload failed with status ${res.status}`);
}

async function downloadBlob(blobName) {
  const res = await fetch(blobUrl(blobName));
  if (!res.ok) throw new Error(`Blob download failed with status ${res.status}`);
  return {
    buffer: Buffer.from(await res.arrayBuffer()),
    contentType: res.headers.get('content-type') || 'application/octet-stream',
  };
}

/** A blob that is already gone counts as deleted. */
async function deleteBlob(blobName) {
  const res = await fetch(blobUrl(blobName), { method: 'DELETE' });
  if (!res.ok && res.status !== 404) {
    throw new Error(`Blob delete failed with status ${res.status}`);
  }
}

module.exports = { isConfigured, newBlobName, uploadBlob, downloadBlob, deleteBlob };
