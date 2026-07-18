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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.net.toUri

/**
 * Full-screen circular cropper for profile pictures.
 *
 * The user can pinch to zoom, drag to pan, double-tap to toggle zoom, rotate
 * 90°, reset the transform, and confirm the crop. The cropped image is saved
 * as a 1024×1024 WebP file and the resulting URI is delivered to
 * [onCropCompleted].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropProfileImageScreen(
    sourceUri: Uri,
    onCropCompleted: (Uri?) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var decodedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(sourceUri) {
        withContext(Dispatchers.IO) {
            decodedBitmap = CropImageUtils.loadRotatedBitmap(context, sourceUri)

            // Clean up the temporary camera file if it came from our cache directory.
            if (sourceUri.scheme == "file") {
                sourceUri.path?.let { path ->
                    if (path.startsWith(context.cacheDir.absolutePath)) {
                        File(path).delete()
                    }
                }
            }
        }
        isLoading = false
        if (decodedBitmap == null) {
            onNavigateBack()
        }
    }

    val state = remember(decodedBitmap) {
        decodedBitmap?.let { bitmap ->
            CropImageState(
                initialBitmap = bitmap,
                cropWindowFraction = CROP_WINDOW_FRACTION,
                minCropWindowPx = with(density) { MIN_CROP_WINDOW_DP.dp.toPx() },
                maxCropWindowPx = with(density) { MAX_CROP_WINDOW_DP.dp.toPx() }
            )
        }
    }

    LaunchedEffect(state, containerSize) {
        if (containerSize.width > 0 && containerSize.height > 0) {
            state?.updateContainerSize(
                containerSize.width.toFloat(),
                containerSize.height.toFloat()
            )
        }
    }

    BackHandler { onNavigateBack() }

    CropProfileImageContent(
        state = state,
        isLoading = isLoading,
        onContainerSizeChanged = { containerSize = it },
        onCancel = onNavigateBack,
        onRotateLeft = { state?.rotateCounterClockwise() },
        onRotateRight = { state?.rotateClockwise() },
        onReset = { state?.reset() },
        onDone = {
            state?.let {
                cropAndSave(
                    context = context,
                    scope = scope,
                    state = it,
                    onCropCompleted = onCropCompleted
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CropProfileImageContent(
    state: CropImageState?,
    isLoading: Boolean,
    onContainerSizeChanged: (IntSize) -> Unit,
    onCancel: () -> Unit,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val saveLabel = stringResource(R.string.crop_save)
    val cancelLabel = stringResource(R.string.crop_cancel)
    val rotateLeftLabel = stringResource(R.string.crop_rotate_left)
    val rotateRightLabel = stringResource(R.string.crop_rotate_right)
    val resetLabel = stringResource(R.string.crop_reset)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.profile_crop_title)) },
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .onSizeChanged { onContainerSizeChanged(it) },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading || state == null) {
                CircularProgressIndicator()
            } else {
                // Toolbar
                HorizontalFloatingToolbar(
                    expanded = true,
                    floatingActionButton = {
                        TooltipBox(
                            positionProvider =
                                TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Above
                                ),
                            tooltip = {
                                PlainTooltip(
                                    modifier = Modifier.semantics {
                                        liveRegion = LiveRegionMode.Assertive
                                        paneTitle = saveLabel
                                    }
                                ) {
                                    Text(saveLabel)
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            FloatingToolbarDefaults.StandardFloatingActionButton(
                                onClick = {
                                    onDone()
                                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                }
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.save_fill),
                                    contentDescription = null
                                )
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).offset(y = -ScreenOffset).zIndex(1f),
                    content = {
                        TooltipBox(
                            positionProvider =
                                TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Above
                                ),
                            tooltip = {
                                PlainTooltip(
                                    modifier =
                                        Modifier.semantics {
                                            liveRegion = LiveRegionMode.Assertive
                                            paneTitle = cancelLabel
                                        }
                                ) {
                                    Text(cancelLabel)
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            IconButton(
                                onClick = {
                                    onCancel()
                                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                                }
                            ) {
                                Icon(imageVector = ImageVector.vectorResource(R.drawable.cancel_filled), contentDescription = cancelLabel)
                            }
                        }
                        TooltipBox(
                            positionProvider =
                                TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Above
                                ),
                            tooltip = {
                                PlainTooltip(
                                    modifier =
                                        Modifier.semantics {
                                            liveRegion = LiveRegionMode.Assertive
                                            paneTitle = rotateLeftLabel
                                        }
                                ) {
                                    Text(rotateLeftLabel)
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            IconButton(
                                onClick = {
                                    onRotateLeft()
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                }
                            ) {
                                Icon(imageVector = ImageVector.vectorResource(R.drawable.rotate_90_degrees_ccw_filled), contentDescription = rotateLeftLabel)
                            }
                        }
                        TooltipBox(
                            positionProvider =
                                TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Above
                                ),
                            tooltip = {
                                PlainTooltip(
                                    modifier =
                                        Modifier.semantics {
                                            liveRegion = LiveRegionMode.Assertive
                                            paneTitle = rotateRightLabel
                                        }
                                ) {
                                    Text(rotateRightLabel)
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            IconButton(
                                onClick = {
                                    onRotateRight()
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                }
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.rotate_90_degrees_cw_filled),
                                    contentDescription = rotateRightLabel,
                                )
                            }
                        }
                        TooltipBox(
                            positionProvider =
                                TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Above
                                ),
                            tooltip = {
                                PlainTooltip(
                                    modifier =
                                        Modifier.semantics {
                                            liveRegion = LiveRegionMode.Assertive
                                            paneTitle = resetLabel
                                        }
                                ) {
                                    Text(resetLabel)
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            IconButton(
                                onClick = {
                                    onReset()
                                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                                }
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.refresh_filled),
                                    contentDescription = resetLabel,
                                )
                            }
                        }
                    },
                )

                Image(
                    bitmap = state.imageBitmap,
                    contentDescription = stringResource(R.string.cd_crop_image),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = state.scale,
                            scaleY = state.scale,
                            translationX = state.offset.x,
                            translationY = state.offset.y
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    state.toggleZoom()
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                state.onTransform(centroid, pan, zoom)
                            }
                        },
                    contentScale = ContentScale.None
                )

                CropOverlay(
                    cropWindowSizePx = state.cropWindowSizePx,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun CropOverlay(
    cropWindowSizePx: Float,
    modifier: Modifier = Modifier
) {
    val overlayColor = MaterialTheme.colorScheme.surface.copy(alpha = OVERLAY_ALPHA)
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = BORDER_ALPHA)

    Box(
        modifier = modifier.drawBehind {
            if (cropWindowSizePx <= 0f) return@drawBehind

            val radius = cropWindowSizePx / 2f
            val cropRect = Rect(
                left = center.x - radius,
                top = center.y - radius,
                right = center.x + radius,
                bottom = center.y + radius
            )

            val path = Path().apply {
                addRect(Rect(Offset.Zero, size))
                addOval(cropRect)
                fillType = PathFillType.EvenOdd
            }

            drawPath(path, color = overlayColor)

            drawCircle(
                color = borderColor,
                radius = radius,
                style = Stroke(width = BORDER_WIDTH_DP.dp.toPx())
            )
        }
    )
}

@SuppressLint("UseKtx")
private fun cropAndSave(
    context: Context,
    scope: CoroutineScope,
    state: CropImageState,
    onCropCompleted: (Uri?) -> Unit
) {
    state.isProcessing = true

    scope.launch(Dispatchers.Default) {
        val cropped = state.crop()
        val path = cropped?.let { CropImageUtils.saveCroppedProfileImage(context, it) }

        withContext(Dispatchers.Main) {
            state.isProcessing = false

            if (path != null) {
                onCropCompleted(path.toUri())
            } else {
                Toast.makeText(
                    context,
                    R.string.error_saving_image,
                    Toast.LENGTH_SHORT
                ).show()
                onCropCompleted(null)
            }
        }
    }
}

@Preview
@Composable
private fun CropProfileImageScreenPreview() {
    HydroTrackerTheme {
        val density = LocalDensity.current
        val bitmap = remember { createSampleBitmap() }
        val state = remember(bitmap) {
            CropImageState(
                initialBitmap = bitmap,
                cropWindowFraction = CROP_WINDOW_FRACTION,
                minCropWindowPx = with(density) { MIN_CROP_WINDOW_DP.dp.toPx() },
                maxCropWindowPx = with(density) { MAX_CROP_WINDOW_DP.dp.toPx() }
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            LaunchedEffect(Unit) {
                state.updateContainerSize(
                    constraints.maxWidth.toFloat(),
                    constraints.maxHeight.toFloat()
                )
            }

            CropProfileImageContent(
                state = state,
                isLoading = false,
                onContainerSizeChanged = {
                    state.updateContainerSize(it.width.toFloat(), it.height.toFloat())
                },
                onCancel = {},
                onRotateLeft = { state.rotateCounterClockwise() },
                onRotateRight = { state.rotateClockwise() },
                onReset = { state.reset() },
                onDone = {}
            )
        }
    }
}

private fun createSampleBitmap(): Bitmap {
    val size = 1024
    val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply { isAntiAlias = true }

    val gradient = LinearGradient(
        0f, 0f, size.toFloat(), size.toFloat(),
        "#2196F3".toColorInt(),
        "#9C27B0".toColorInt(),
        Shader.TileMode.CLAMP
    )
    paint.shader = gradient
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
    paint.shader = null

    paint.color = android.graphics.Color.WHITE
    paint.alpha = 180
    canvas.drawCircle(size * 0.3f, size * 0.3f, size * 0.15f, paint)
    canvas.drawCircle(size * 0.7f, size * 0.6f, size * 0.2f, paint)
    canvas.drawCircle(size * 0.5f, size * 0.8f, size * 0.1f, paint)

    return bitmap
}

private const val CROP_WINDOW_FRACTION = 0.7f
private const val MIN_CROP_WINDOW_DP = 240
private const val MAX_CROP_WINDOW_DP = 360
private const val OVERLAY_ALPHA = 0.6f
private const val BORDER_ALPHA = 0.8f
private const val BORDER_WIDTH_DP = 2
