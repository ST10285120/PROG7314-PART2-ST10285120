package com.fitquest.app.data.repository

import android.util.Log
import com.fitquest.app.data.remote.FitQuestApi
import com.fitquest.app.data.remote.dto.ProgressPhotoDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/** Progress photos. The API stores the bytes in blob storage and checks ownership. */
class PhotoRepository(private val api: FitQuestApi) {

    suspend fun listPhotos(): Result<List<ProgressPhotoDto>> = try {
        val response = api.listPhotos()
        val body = response.body()
        if (response.isSuccessful && body != null) Result.success(body)
        else Result.failure(Exception("Failed to load photos (HTTP ${response.code()})"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun uploadPhoto(jpegBytes: ByteArray): Result<ProgressPhotoDto> = try {
        val response = api.uploadPhoto(jpegBytes.toRequestBody("image/jpeg".toMediaType()))
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Log.i(TAG, "Uploaded a ${jpegBytes.size} byte photo")
            Result.success(body)
        } else {
            Result.failure(Exception("Upload failed (HTTP ${response.code()})"))
        }
    } catch (e: Exception) {
        Log.e(TAG, "Photo upload failed", e)
        Result.failure(e)
    }

    suspend fun deletePhoto(photoId: String): Result<Unit> = try {
        val response = api.deletePhoto(photoId)
        if (response.isSuccessful) Result.success(Unit)
        else Result.failure(Exception("Delete failed (HTTP ${response.code()})"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    private companion object {
        const val TAG = "PhotoRepository"
    }
}
