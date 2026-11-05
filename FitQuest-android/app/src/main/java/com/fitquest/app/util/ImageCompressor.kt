package com.fitquest.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Shrinks a picked image before upload, which keeps it under the API's 5 MB limit and
 * saves mobile data. Returns null if the image cannot be read, and never throws.
 */
object ImageCompressor {

    private const val TAG = "ImageCompressor"
    private const val MAX_DIMENSION = 1280
    private const val JPEG_QUALITY = 85

    fun toJpeg(context: Context, uri: Uri): ByteArray? = try {
        val bitmap = decode(context, uri)
        ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            output.toByteArray()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Could not read the selected image", e)
        null
    }

    private fun decode(context: Context, uri: Uri): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder also applies EXIF rotation, so photos stay upright.
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.setTargetSampleSize(sampleSize(max(info.size.width, info.size.height)))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(max(bounds.outWidth, bounds.outHeight))
        }
        return context.contentResolver.openInputStream(uri).use {
            requireNotNull(BitmapFactory.decodeStream(it, null, options))
        }
    }

    private fun sampleSize(largestSide: Int): Int {
        var size = 1
        while (largestSide / (size * 2) >= MAX_DIMENSION) size *= 2
        return size
    }
}
