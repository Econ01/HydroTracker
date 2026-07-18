package com.cemcakmak.hydrotracker.presentation.common

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Anchor points for a swipeable row.
 */
enum class SwipeActionAnchor {
    Start,
    Center,
    End
}

/**
 * Describes a single swipe action: icon, accessibility label, and colours.
 *
 * @property icon Icon shown inside the growing pill.
 * @property contentDescription Accessibility label for the action.
 * @property containerColor Background colour of the pill.
 * @property contentColor Tint colour for the icon.
 * @property shape Shape of the revealed pill. Defaults to a full capsule.
 */
data class SwipeActionConfig(
    val icon: ImageVector,
    val contentDescription: String,
    val containerColor: Color,
    val contentColor: Color,
    val shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)
)

/**
 * Creates and remembers an [AnchoredDraggableState] configured for a single swipe action
 * in each direction.
 *
 * @param maxOffset Maximum horizontal distance the row can be dragged in either direction.
 */
@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("RestrictedApi")
@Composable
fun rememberSwipeActionState(
    maxOffset: Dp = LocalConfiguration.current.screenWidthDp.dp
): AnchoredDraggableState<SwipeActionAnchor> {
    val density = LocalDensity.current
    val maxOffsetPx = with(density) { maxOffset.toPx() }

    return remember(maxOffsetPx) {
        AnchoredDraggableState(
            initialValue = SwipeActionAnchor.Center,
            anchors = DraggableAnchors {
                SwipeActionAnchor.Start at -maxOffsetPx
                SwipeActionAnchor.Center at 0f
                SwipeActionAnchor.End at maxOffsetPx
            }
        )
    }
}

/**
 * A swipeable row that reveals an icon-only pill action on each horizontal edge.
 *
 * @param state Swipe state. Hoist this if the caller needs to reset or observe the offset.
 * @param startAction Action hidden under the start edge; revealed by swiping right (start-to-end in LTR).
 * @param endAction Action hidden under the end edge; revealed by swiping left (end-to-start in LTR).
 * @param onStartAction Called when the row settles on the start action (right swipe).
 * @param onEndAction Called when the row settles on the end action (left swipe).
 * @param maxOffset Maximum drag distance used for anchors and icon-scale progress. The pill itself fills the dragged gap.
 * @param modifier Modifier applied to the outer container.
 * @param content Foreground content that can be dragged.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeActionItem(
    modifier: Modifier = Modifier,
    state: AnchoredDraggableState<SwipeActionAnchor>,
    startAction: SwipeActionConfig? = null,
    endAction: SwipeActionConfig? = null,
    onStartAction: () -> Unit = {},
    onEndAction: () -> Unit = {},
    maxOffset: Dp = LocalConfiguration.current.screenWidthDp.dp,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val pillCardGap = 2.dp
    val iconMinLeadingPadding = 20.dp

    val gapPx = with(density) { pillCardGap.toPx() }
    val minLeadingPaddingPx = with(density) { iconMinLeadingPadding.toPx() }

    // Fire actions only once the row settles at an edge — i.e. after the finger is released.
    LaunchedEffect(state.settledValue) {
        when (state.settledValue) {
            SwipeActionAnchor.End -> startAction?.let { onStartAction() }
            SwipeActionAnchor.Start -> endAction?.let { onEndAction() }
            SwipeActionAnchor.Center -> {}
        }
    }

    // Subtle tick and icon bounce the moment the drag crosses the commit threshold.
    val haptics = LocalHapticFeedback.current
    val iconScale = remember { Animatable(1f) }
    val thresholdPx = with(density) { maxOffset.toPx() } * 0.4f
    LaunchedEffect(state, thresholdPx) {
        snapshotFlow { state.requireOffset().absoluteValue >= thresholdPx }
            .distinctUntilChanged()
            .collect { past ->
                if (past) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    iconScale.snapTo(1f)
                    iconScale.animateTo(
                        targetValue = 1.4f,
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMedium,
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        )
                    )
                    iconScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            stiffness = Spring.StiffnessLow,
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        )
                    )
                }
            }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        // Background pill that gets revealed upon swipe
        Box(modifier = Modifier.matchParentSize()) {
            startAction?.let { config ->
                StartActionPill(
                    state = state,
                    gapPx = gapPx,
                    minLeadingPaddingPx = minLeadingPaddingPx,
                    iconScale = iconScale.value,
                    config = config
                )
            }
            endAction?.let { config ->
                EndActionPill(
                    state = state,
                    gapPx = gapPx,
                    minLeadingPaddingPx = minLeadingPaddingPx,
                    iconScale = iconScale.value,
                    config = config
                )
            }
        }

        // Foreground content layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Horizontal
                )
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StartActionPill(
    state: AnchoredDraggableState<SwipeActionAnchor>,
    gapPx: Float,
    minLeadingPaddingPx: Float,
    iconScale: Float,
    config: SwipeActionConfig
) {
    Box(
        modifier = Modifier
            .padding(bottom = 2.dp)
            .fillMaxHeight()
            .layout { measurable, constraints ->
                val widthPx = (state.requireOffset() - gapPx).coerceAtLeast(0f).roundToInt()
                val placeable = measurable.measure(
                    constraints.copy(minWidth = widthPx, maxWidth = widthPx)
                )
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(0, 0)
                }
            }
            .clip(config.shape)
            .background(config.containerColor),
        contentAlignment = Alignment.Center
    ) {
        ActionIcon(
            state = state,
            gapPx = gapPx,
            minLeadingPaddingPx = minLeadingPaddingPx,
            iconScale = iconScale,
            leadingStart = true,
            config = config
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EndActionPill(
    state: AnchoredDraggableState<SwipeActionAnchor>,
    gapPx: Float,
    minLeadingPaddingPx: Float,
    iconScale: Float,
    config: SwipeActionConfig
) {
    Box(
        modifier = Modifier
            .padding(bottom = 2.dp)
            .fillMaxHeight()
            .layout { measurable, constraints ->
                val widthPx = (-state.requireOffset() - gapPx).coerceAtLeast(0f).roundToInt()
                val placeable = measurable.measure(
                    constraints.copy(minWidth = widthPx, maxWidth = widthPx)
                )
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(constraints.maxWidth - placeable.width, 0)
                }
            }
            .clip(config.shape)
            .background(config.containerColor),
        contentAlignment = Alignment.Center
    ) {
        ActionIcon(
            state = state,
            gapPx = gapPx,
            minLeadingPaddingPx = minLeadingPaddingPx,
            iconScale = iconScale,
            leadingStart = false,
            config = config
        )
    }
}

/**
 * Action icon that scales on the commit threshold. Centered in the pill once it is wide enough,
 * but kept at least [minLeadingPaddingPx] from the pill's leading edge (and clipped by the pill)
 * while narrow, so it emerges from the swiped edge instead of being squeezed in the middle.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActionIcon(
    state: AnchoredDraggableState<SwipeActionAnchor>,
    gapPx: Float,
    minLeadingPaddingPx: Float,
    iconScale: Float,
    leadingStart: Boolean,
    config: SwipeActionConfig
) {
    Icon(
        imageVector = config.icon,
        contentDescription = config.contentDescription,
        tint = config.contentColor,
        modifier = Modifier.graphicsLayer {
            scaleX = iconScale
            scaleY = iconScale
            val pillWidth = (state.requireOffset().absoluteValue - gapPx).coerceAtLeast(0f)
            val centeredLeft = (pillWidth - size.width) / 2f
            val delta = (minLeadingPaddingPx - centeredLeft).coerceAtLeast(0f)
            translationX = if (leadingStart) delta else -delta
        }
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true, name = "Swipe action - resting")
@Composable
private fun SwipeActionItemRestingPreview() {
    HydroTrackerTheme {
        SwipeActionItemPreviewContent(initialAnchor = SwipeActionAnchor.Center)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true, name = "Swipe action - edit revealed")
@Composable
private fun SwipeActionItemEditPreview() {
    HydroTrackerTheme {
        SwipeActionItemPreviewContent(initialAnchor = SwipeActionAnchor.End)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true, name = "Swipe action - delete revealed")
@Composable
private fun SwipeActionItemDeletePreview() {
    HydroTrackerTheme {
        SwipeActionItemPreviewContent(initialAnchor = SwipeActionAnchor.Start)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeActionItemPreviewContent(initialAnchor: SwipeActionAnchor) {
    val state = rememberSwipeActionState()

    LaunchedEffect(initialAnchor) {
        if (initialAnchor != SwipeActionAnchor.Center) {
            state.snapTo(initialAnchor)
        }
    }

    SwipeActionItem(
        state = state,
        startAction = SwipeActionConfig(
            icon = Icons.Default.Edit,
            contentDescription = "Edit entry",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        endAction = SwipeActionConfig(
            icon = Icons.Default.Delete,
            contentDescription = "Delete entry",
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        modifier = Modifier.padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Entry content",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
