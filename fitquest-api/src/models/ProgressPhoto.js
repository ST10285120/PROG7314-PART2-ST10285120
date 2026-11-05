const mongoose = require('mongoose');

/**
 * Metadata for a progress photo. The image itself is in blob storage; MongoDB keeps
 * only the blob name, following the usual split between structured data and files.
 */
const progressPhotoSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true, index: true },
  blobName: { type: String, required: true },
  contentType: { type: String, required: true },
  takenAt: { type: Date, default: Date.now },
});

module.exports = mongoose.model('ProgressPhoto', progressPhotoSchema);
