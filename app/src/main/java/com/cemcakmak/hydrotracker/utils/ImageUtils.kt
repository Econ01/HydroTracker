package com.cemcakmak.hydrotracker.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * Utility class for handling profile image operations
 * Provides image compression, storage, and retrieval functionality
 */
object ImageUtils {
    
    private const val PROFILE_IMAGE_FILENAME = "profile_image.jpg"
    private const val PROFILE_IMAGE_WEBP_FILENAME = "profile_image.webp"

    private val profileImageCache = mutableMapOf<String, Bitmap>()

    /**
     * Get the legacy JPEG profile image file.
     */
    private fun getProfileImageJpgFile(context: Context): File {
        return File(context.filesDir, PROFILE_IMAGE_FILENAME)
    }

    /**
     * Get the cropped WebP profile image file.
     */
    private fun getProfileImageWebpFile(context: Context): File {
        return File(context.filesDir, PROFILE_IMAGE_WEBP_FILENAME)
    }

    /**
     * Returns the existing profile image file, preferring the WebP version and
     * falling back to the legacy JPEG file.
     */
    private fun getExistingProfileImageFile(context: Context): File? {
        val webpFile = getProfileImageWebpFile(context)
        if (webpFile.exists()) return webpFile

        val jpgFile = getProfileImageJpgFile(context)
        return if (jpgFile.exists()) jpgFile else null
    }

    /**
     * Delete all profile image files and clear the in-memory cache.
     */
    fun deleteProfileImage(context: Context): Boolean {
        return try {
            val jpgDeleted = getProfileImageJpgFile(context).deleteExisting()
            val webpDeleted = getProfileImageWebpFile(context).deleteExisting()
            clearProfileImageCache()
            jpgDeleted || webpDeleted
        } catch (_: Exception) {
            false
        }
    }

    private fun File.deleteExisting(): Boolean {
        return if (exists()) delete() else false
    }

    /**
     * Load profile image as bitmap if it exists.
     * Results are cached in memory so subsequent loads are instantaneous.
     */
    fun loadProfileImageBitmap(context: Context, path: String? = null): Bitmap? {
        return try {
            val file = path?.let { File(it) } ?: getExistingProfileImageFile(context) ?: return null
            val cacheKey = file.absolutePath

            profileImageCache[cacheKey]?.let { return it }

            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)?.also { bitmap ->
                    profileImageCache[cacheKey] = bitmap
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Clear the in-memory profile image cache.
     * Call this after the profile picture is updated or deleted.
     */
    fun clearProfileImageCache() {
        profileImageCache.clear()
    }
    
}