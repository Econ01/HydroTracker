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

import android.graphics.Bitmap
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.max
import kotlin.math.min

/**
 * Holds the interactive state for the circular profile-picture cropper.
 *
 * The coordinate system used for [offset] is centred on the cropper container:
 * (0, 0) is the centre of the screen, positive X points right, and positive Y
 * points down.
 */
class CropImageState(
    initialBitmap: Bitmap,
    private val cropWindowFraction: Float,
    private val minCropWindowPx: Float,
    private val maxCropWindowPx: Float
) {
    private val originalBitmap: Bitmap = initialBitmap
    private var currentBitmap by mutableStateOf(initialBitmap)

    /** Bitmap currently displayed, already rotated to match [rotation]. */
    val imageBitmap by derivedStateOf { currentBitmap.asImageBitmap() }

    /** Width of the cropper container in pixels. */
    var containerWidthPx by mutableFloatStateOf(0f)

    /** Height of the cropper container in pixels. */
    var containerHeightPx by mutableFloatStateOf(0f)

    /** Current zoom level applied to the image. */
    var scale by mutableFloatStateOf(1f)
        private set

    /** Current pan offset in the centred container coordinate system. */
    var offset by mutableStateOf(Offset.Zero)
        private set

    /** Cumulative rotation in degrees, always one of 0, 90, 180, or 270. */
    var rotation by mutableIntStateOf(0)
        private set

    /** Whether the cropper is busy writing the final image. */
    var isProcessing by mutableStateOf(false)

    /** Diameter of the circular crop window in pixels. */
    var cropWindowSizePx by mutableFloatStateOf(0f)
        private set

    private var baseScale by mutableFloatStateOf(1f)
    private var isToggledZoom by mutableStateOf(false)

    private val containerCenter: Offset
        get() = Offset(containerWidthPx / 2f, containerHeightPx / 2f)

    /**
     * Updates the container size and recomputes the crop window size and base
     * zoom. Call this whenever the cropper layout size is known or changes.
     */
    fun updateContainerSize(width: Float, height: Float) {
        containerWidthPx = width
        containerHeightPx = height
        cropWindowSizePx = computeCropWindowSize(width, height)
        fit()
    }

    /**
     * Applies a pinch/zoom/pan gesture.
     *
     * @param centroid position of the gesture centroid in container coordinates.
     * @param pan translation delta in pixels.
     * @param zoom scale multiplier from the gesture.
     */
    fun onTransform(centroid: Offset, pan: Offset, zoom: Float) {
        val newScale = (scale * zoom).coerceAtLeast(baseScale)
        val centroidRel = centroid - containerCenter

        // Zoom around the gesture centroid, then add the pan.
        val newOffset = centroidRel * (1f - zoom) + offset * zoom + pan

        scale = newScale
        offset = clampOffset(newOffset)
        isToggledZoom = false
    }

    /** Rotates the image 90° clockwise and recentres it. */
    fun rotateClockwise() {
        applyRotation((rotation + 90) % 360)
    }

    /** Rotates the image 90° counter-clockwise and recentres it. */
    fun rotateCounterClockwise() {
        applyRotation((rotation - 90 + 360) % 360)
    }

    /** Resets zoom, pan, and rotation to the initial cover state. */
    fun reset() {
        applyRotation(0)
    }

    /**
     * Toggles between the cover-fit zoom and a 2× zoom centred on the crop window.
     */
    fun toggleZoom() {
        isToggledZoom = !isToggledZoom
        scale = if (isToggledZoom) (baseScale * 2f).coerceAtLeast(baseScale) else baseScale
        offset = Offset.Zero
    }

    /**
     * Crops the region currently underneath the circular crop window.
     *
     * The work is synchronous; callers should invoke this from a background
     * dispatcher.
     *
     * @return a [CropImageUtils.CROP_OUTPUT_SIZE] square bitmap containing the
     *         cropped image, or null if the crop rectangle is invalid.
     */
    fun crop(): Bitmap? {
        val bitmap = currentBitmap
        val imageWidth = bitmap.width.toFloat()
        val imageHeight = bitmap.height.toFloat()

        // Compute the square crop rectangle in bitmap coordinates.
        val srcSize = cropWindowSizePx / scale
        val srcX = (imageWidth * scale - cropWindowSizePx) / 2f - offset.x
        val srcY = (imageHeight * scale - cropWindowSizePx) / 2f - offset.y

        val srcRect = android.graphics.Rect(
            (srcX / scale).toInt(),
            (srcY / scale).toInt(),
            ((srcX / scale) + srcSize).toInt(),
            ((srcY / scale) + srcSize).toInt()
        )

        return CropImageUtils.cropAndResize(bitmap, srcRect)
    }

    private fun computeCropWindowSize(width: Float, height: Float): Float {
        return (min(width, height) * cropWindowFraction)
            .coerceIn(minCropWindowPx, maxCropWindowPx)
    }

    private fun fit() {
        if (cropWindowSizePx <= 0f) return

        val bmp = currentBitmap
        baseScale = max(
            cropWindowSizePx / bmp.width,
            cropWindowSizePx / bmp.height
        )
        scale = baseScale
        offset = Offset.Zero
        isToggledZoom = false
    }

    private fun applyRotation(newRotation: Int) {
        val nextBitmap = if (newRotation == 0) {
            originalBitmap
        } else {
            CropImageUtils.rotateBitmap(originalBitmap, newRotation.toFloat())
        }

        // Recycle the previous rotated bitmap to avoid piling up native memory.
        if (currentBitmap !== originalBitmap && currentBitmap !== nextBitmap && !currentBitmap.isRecycled) {
            currentBitmap.recycle()
        }

        currentBitmap = nextBitmap
        rotation = newRotation
        fit()
    }

    private fun clampOffset(value: Offset): Offset {
        val bmp = currentBitmap
        val maxX = (bmp.width * scale - cropWindowSizePx) / 2f
        val maxY = (bmp.height * scale - cropWindowSizePx) / 2f

        return Offset(
            x = value.x.coerceIn(-maxX, maxX),
            y = value.y.coerceIn(-maxY, maxY)
        )
    }
}
