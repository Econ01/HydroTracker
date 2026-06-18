/*
 *
 *  * HydroTracker - A modern and private water intake tracking application
 *  * Copyright (c) 2026 Ali Cem Çakmak
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.cemcakmak.hydrotracker.presentation.settings.profile.crop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.cemcakmak.hydrotracker.utils.ImageUtils
import java.io.File
import java.io.FileOutputStream

/**
 * Helpers for the profile-picture cropper.
 */
object CropImageUtils {

    /** Maximum dimension of the bitmap kept in memory while cropping. */
    private const val MAX_SOURCE_DIMENSION = 4096

    /** Final cropped profile picture size in pixels. */
    const val CROP_OUTPUT_SIZE = 1024

    /** WebP compression quality (0–100). */
    private const val WEBP_QUALITY = 90

    private const val CROPPED_IMAGE_FILENAME = "profile_image.webp"

    /**
     * Loads a rotated, downsampled bitmap from [imageUri].
     *
     * The image is decoded so that its larger edge is at most [MAX_SOURCE_DIMENSION]
     * pixels, then EXIF orientation is applied so the bitmap is already upright.
     *
     * @return the upright bitmap, or null if decoding fails.
     */
    fun loadRotatedBitmap(context: Context, imageUri: Uri): Bitmap? {
        return try {
            val (width, height) = readImageBounds(context, imageUri) ?: return null

            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(width, height)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val original = decodeBitmap(context, imageUri, options) ?: return null
            correctImageOrientation(context, imageUri, original)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Rotates [bitmap] by the given number of degrees.
     *
     * @return a new bitmap; the input bitmap is not modified.
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap

        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Crops a square region from [bitmap] and resizes it to [CROP_OUTPUT_SIZE].
     *
     * @param srcRect the source rectangle in bitmap coordinates.
     * @return the resized square bitmap, or null if the rectangle is invalid.
     */
    fun cropAndResize(bitmap: Bitmap, srcRect: android.graphics.Rect): Bitmap? {
        val clamped = clampRect(bitmap.width, bitmap.height, srcRect)
        if (clamped.width() <= 0 || clamped.height() <= 0) return null

        val cropped = Bitmap.createBitmap(bitmap, clamped.left, clamped.top, clamped.width(), clamped.height())
        return cropped.scale(CROP_OUTPUT_SIZE, CROP_OUTPUT_SIZE, true).also {
            cropped.recycle()
        }
    }

    /**
     * Saves [bitmap] as the user's profile image.
     *
     * The bitmap is compressed to WebP at [WEBP_QUALITY] and written to the app's
     * private files' directory. Any in-memory cache is cleared so the new image is
     * picked up immediately.
     *
     * @return the absolute path of the saved file, or null on failure.
     */
    fun saveCroppedProfileImage(context: Context, bitmap: Bitmap): String? {
        return try {
            val file = File(context.filesDir, CROPPED_IMAGE_FILENAME)
            FileOutputStream(file).use { out ->
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                bitmap.compress(format, WEBP_QUALITY, out)
            }
            ImageUtils.clearProfileImageCache()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun readImageBounds(context: Context, imageUri: Uri): Pair<Int, Int>? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(imageUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            if (options.outWidth > 0 && options.outHeight > 0) {
                options.outWidth to options.outHeight
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeBitmap(context: Context, imageUri: Uri, options: BitmapFactory.Options): Bitmap? {
        return try {
            context.contentResolver.openInputStream(imageUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun correctImageOrientation(context: Context, imageUri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            context.contentResolver.openInputStream(imageUri)?.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                    else -> bitmap
                }
            } ?: bitmap
        } catch (_: Exception) {
            bitmap
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var inSampleSize = 1
        while (width / inSampleSize > MAX_SOURCE_DIMENSION || height / inSampleSize > MAX_SOURCE_DIMENSION) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    private fun clampRect(bitmapWidth: Int, bitmapHeight: Int, rect: android.graphics.Rect): android.graphics.Rect {
        val left = rect.left.coerceIn(0, bitmapWidth)
        val top = rect.top.coerceIn(0, bitmapHeight)
        val right = rect.right.coerceIn(left, bitmapWidth)
        val bottom = rect.bottom.coerceIn(top, bitmapHeight)
        return android.graphics.Rect(left, top, right, bottom)
    }
}
