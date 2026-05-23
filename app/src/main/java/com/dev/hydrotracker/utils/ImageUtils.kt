package com.dev.hydrotracker.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Utility class for handling profile image operations
 * Provides image compression, storage, and retrieval functionality
 */
object ImageUtils {
    
    private const val PROFILE_IMAGE_FILENAME = "profile_image.jpg"
    private const val MAX_IMAGE_SIZE = 300 // Max width/height in pixels
    private const val JPEG_QUALITY = 85 // Compression quality (0-100)
    
    /**
     * Save an image URI to local storage with compression
     * @param context Application context
     * @param imageUri URI of the image to save
     * @return Local file path if successful, null if failed
     */
    fun saveProfileImage(context: Context, imageUri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            inputStream?.use { stream ->
                // Decode the image
                val originalBitmap = BitmapFactory.decodeStream(stream)
                    ?: return null
                
                // Apply rotation correction if needed
                val correctedBitmap = correctImageOrientation(context, imageUri, originalBitmap)
                
                // Compress and resize the image
                val compressedBitmap = compressImage(correctedBitmap)
                
                // Save to internal storage
                val file = getProfileImageFile(context)
                saveBitmapToFile(compressedBitmap, file)
                
                // Clean up
                if (correctedBitmap != originalBitmap) {
                    correctedBitmap.recycle()
                }
                compressedBitmap.recycle()
                
                file.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get the profile image file
     */
    private fun getProfileImageFile(context: Context): File {
        return File(context.filesDir, PROFILE_IMAGE_FILENAME)
    }
    
    /**
     * Check if profile image exists
     */
    fun profileImageExists(context: Context): Boolean {
        return getProfileImageFile(context).exists()
    }
    
    /**
     * Get profile image path if it exists
     */
    fun getProfileImagePath(context: Context): String? {
        val file = getProfileImageFile(context)
        return if (file.exists()) file.absolutePath else null
    }
    
    /**
     * Delete the current profile image
     */
    fun deleteProfileImage(context: Context): Boolean {
        return try {
            val file = getProfileImageFile(context)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Compress and resize image to reduce file size
     */
    private fun compressImage(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Calculate new dimensions
        val maxDimension = MAX_IMAGE_SIZE.toFloat()
        val ratio = if (width > height) {
            maxDimension / width
        } else {
            maxDimension / height
        }
        
        return if (ratio < 1.0f) {
            // Resize if image is larger than max dimensions
            val newWidth = (width * ratio).toInt()
            val newHeight = (height * ratio).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            // Image is already small enough
            bitmap
        }
    }
    
    /**
     * Correct image orientation based on EXIF data
     */
    private fun correctImageOrientation(context: Context, imageUri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            inputStream?.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                    else -> bitmap
                }
            } ?: bitmap
        } catch (e: Exception) {
            // If we can't read EXIF data, return original bitmap
            bitmap
        }
    }
    
    /**
     * Rotate bitmap by specified degrees
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    /**
     * Save bitmap to file
     */
    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
    }
    
    /**
     * Load profile image as bitmap if it exists
     */
    fun loadProfileImageBitmap(context: Context): Bitmap? {
        return try {
            val file = getProfileImageFile(context)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get file size of profile image in bytes
     */
    fun getProfileImageSize(context: Context): Long {
        return try {
            val file = getProfileImageFile(context)
            if (file.exists()) file.length() else 0L
        } catch (e: Exception) {
            0L
        }
    }
}