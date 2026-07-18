package com.cemcakmak.hydrotracker.presentation.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.cemcakmak.hydrotracker.R
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private sealed interface ReorderSlot<out T> {
    data class RowSlot<T>(
        val value: T,
        val pinned: Boolean,
        val hidden: Boolean,
        val posInGroup: Int,
        val groupSize: Int
    ) : ReorderSlot<T>

    data object HiddenLabel : ReorderSlot<Nothing>
}

/**
 * In-house drag-to-reorder grouped list, built on stock Compose. Owns the grouped-card look and supports pinned and hidden rows.
 *
 * @param items full ordered list (pinned + visible + hidden, classified by [isPinned]/[isHidden]).
 * @param onReorder fired on drop with the new visible order.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ReorderableGroupedColumn(
    items: List<T>,
    key: (T) -> Any,
    onReorder: (visibleOrder: List<T>) -> Unit,
    modifier: Modifier = Modifier,
    isPinned: (T) -> Boolean = { false },
    isHidden: (T) -> Boolean = { false },
    onClick: ((T) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(),
    hiddenLabel: String? = "Hidden",
    header: (LazyListScope.() -> Unit)? = null,
    footer: (LazyListScope.() -> Unit)? = null,
    content: @Composable RowScope.(T) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val currentOnReorder by rememberUpdatedState(onReorder)

    val pinned = items.filter { isPinned(it) }
    val hidden = items.filter { !isPinned(it) && isHidden(it) }

    val orderState = remember { mutableStateOf<List<Any>>(emptyList()) }

    val visible = items.filter { !isPinned(it) && !isHidden(it) }
        .sortedBy { item ->
            val i = orderState.value.indexOf(key(item))
            if (i >= 0) i else Int.MAX_VALUE
        }
    val visibleKeys by rememberUpdatedState(visible.map(key))
    val currentVisible by rememberUpdatedState(visible)

    val settleSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val dragState = remember(lazyListState) {
        DragDropState(
            lazyListState = lazyListState,
            scope = scope,
            settleSpec = settleSpec,
            orderedKeys = { visibleKeys },
            onMove = { from, to ->
                orderState.value = visibleKeys.toMutableList().apply { add(to, removeAt(from)) }
            },
            onSwapHaptic = { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) }
        )
    }

    val incomingVisibleKeys = items.filter { !isPinned(it) && !isHidden(it) }.map(key)
    LaunchedEffect(incomingVisibleKeys, dragState.draggingItemKey) {
        if (dragState.draggingItemKey == null) {
            orderState.value = incomingVisibleKeys
        }
    }

    val mainSize = pinned.size + visible.size
    val placementSpec = MaterialTheme.motionScheme.slowSpatialSpec<IntOffset>()

    val slots = buildList {
        pinned.forEachIndexed { i, item ->
            add(ReorderSlot.RowSlot(item, pinned = true, hidden = false, posInGroup = i, groupSize = mainSize))
        }
        visible.forEachIndexed { i, item ->
            add(ReorderSlot.RowSlot(item, pinned = false, hidden = false, posInGroup = pinned.size + i, groupSize = mainSize))
        }
        if (hidden.isNotEmpty()) {
            if (hiddenLabel != null) add(ReorderSlot.HiddenLabel)
            hidden.forEachIndexed { i, item ->
                add(ReorderSlot.RowSlot(item, pinned = false, hidden = true, posInGroup = i, groupSize = hidden.size))
            }
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier,
        contentPadding = contentPadding
    ) {
        header?.invoke(this)

        items(
            items = slots,
            key = { slot ->
                when (slot) {
                    is ReorderSlot.RowSlot -> key(slot.value)
                    ReorderSlot.HiddenLabel -> "__reorderable_hidden_label__"
                }
            },
            contentType = { slot -> if (slot is ReorderSlot.RowSlot) "row" else "label" }
        ) { slot ->
            when (slot) {
                ReorderSlot.HiddenLabel -> {
                    androidx.compose.material3.Text(
                        text = hiddenLabel.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .animateItem(placementSpec = placementSpec)
                            .padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                    )
                }

                is ReorderSlot.RowSlot -> {
                    val item = slot.value
                    val itemKey = key(item)
                    val active = dragState.isActive(itemKey)
                    val rowModifier = Modifier
                        .then(if (active) Modifier else Modifier.animateItem(placementSpec = placementSpec))
                        .zIndex(if (active) 1f else 0f)
                    GroupedRow(
                        index = slot.posInGroup,
                        size = slot.groupSize,
                        dragging = active,
                        dimmed = slot.hidden,
                        translationProvider = { dragState.translationFor(itemKey) },
                        onClick = if (slot.pinned) null else onClick?.let { cb -> { cb(item) } },
                        trailing = {
                            when {
                                slot.pinned -> Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = stringResource(R.string.cd_pinned),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )

                                slot.hidden -> Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = stringResource(R.string.cd_hidden),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )

                                else -> Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.drag_handle_filled),
                                    contentDescription = stringResource(R.string.cd_reorder),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .pointerInput(itemKey) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    dragState.onDragStart(itemKey)
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragState.onDrag(dragAmount.y)
                                                },
                                                onDragEnd = {
                                                    dragState.onDragEnd()
                                                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                                    currentOnReorder(currentVisible)
                                                },
                                                onDragCancel = {
                                                    dragState.onDragEnd()
                                                    currentOnReorder(currentVisible)
                                                }
                                            )
                                        }
                                )
                            }
                        },
                        modifier = rowModifier,
                        content = { content(item) }
                    )
                }
            }
        }

        footer?.invoke(this)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GroupedRow(
    index: Int,
    size: Int,
    dragging: Boolean,
    dimmed: Boolean,
    translationProvider: () -> Float,
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val shape = rememberAnimatedGroupShape(index, size)
    val scale by animateFloatAsState(
        targetValue = if (dragging) 1.03f else 1f,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "reorderDragScale"
    )
    val tonalElevation by animateDpAsState(
        targetValue = when {
            dimmed -> 0.dp
            dragging -> 6.dp
            else -> 2.dp
        },
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "reorderTonalElevation"
    )
    val shadowElevation by animateDpAsState(
        targetValue = if (dragging) 6.dp else 0.dp,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "reorderShadowElevation"
    )
    val surfaceModifier = modifier
        .fillMaxWidth()
        .graphicsLayer {
            translationY = translationProvider()
            scaleX = scale
            scaleY = scale
        }
        .then(if (dimmed) Modifier.alpha(0.6f) else Modifier)
        .padding(bottom = 2.dp)

    val inner: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
            trailing()
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = shape,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            modifier = surfaceModifier
        ) { inner() }
    } else {
        Surface(
            shape = shape,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            modifier = surfaceModifier
        ) { inner() }
    }
}

/** Grouped-card shape whose four corner radii spring toward the target for [index]/[size]. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun rememberAnimatedGroupShape(index: Int, size: Int): Shape {
    val target = groupCorners(index, size)
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Dp>()
    val topStart by animateDpAsState(target.topStart, spec, label = "ts")
    val topEnd by animateDpAsState(target.topEnd, spec, label = "te")
    val bottomStart by animateDpAsState(target.bottomStart, spec, label = "bs")
    val bottomEnd by animateDpAsState(target.bottomEnd, spec, label = "be")
    return RoundedCornerShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomStart = bottomStart,
        bottomEnd = bottomEnd
    )
}

private class DragDropState(
    private val lazyListState: LazyListState,
    private val scope: CoroutineScope,
    private val settleSpec: FiniteAnimationSpec<Float>,
    private val orderedKeys: () -> List<Any>,
    private val onMove: (from: Int, to: Int) -> Unit,
    private val onSwapHaptic: () -> Unit
) {
    var draggingItemKey by mutableStateOf<Any?>(null)
        private set
    private var initialOffset = 0
    private var dragDelta by mutableFloatStateOf(0f)
    private var settleJob: Job? = null

    private fun infoFor(key: Any?) =
        lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }

    fun isActive(key: Any): Boolean = key == draggingItemKey

    fun translationFor(key: Any): Float =
        if (key == draggingItemKey) infoFor(key)?.let { (initialOffset + dragDelta) - it.offset } ?: 0f
        else 0f

    fun onDragStart(key: Any) {
        val info = infoFor(key) ?: return
        settleJob?.cancel()
        draggingItemKey = key
        initialOffset = info.offset
        dragDelta = 0f
    }

    fun onDrag(amountY: Float) {
        val dragging = draggingItemKey ?: return
        dragDelta += amountY
        val info = infoFor(dragging) ?: return
        val translation = (initialOffset + dragDelta) - info.offset
        val center = info.offset + translation + info.size / 2f
        val keys = orderedKeys()
        val fromIdx = keys.indexOf(dragging)
        if (fromIdx < 0) return
        val target = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { candidate ->
            candidate.key != dragging &&
                candidate.key in keys &&
                center.toInt() in candidate.offset..(candidate.offset + candidate.size)
        } ?: return
        val toIdx = keys.indexOf(target.key)
        if (toIdx >= 0 && toIdx != fromIdx) {
            onMove(fromIdx, toIdx)
            onSwapHaptic()
        }
    }

    fun onDragEnd() {
        val key = draggingItemKey ?: return
        val info = infoFor(key)
        if (info == null) {
            draggingItemKey = null
            return
        }
        val targetDelta = (info.offset - initialOffset).toFloat()
        settleJob = scope.launch {
            Animatable(dragDelta).animateTo(targetDelta, settleSpec) { dragDelta = value }
            draggingItemKey = null
        }
    }
}

internal data class GroupCorners(
    val topStart: Dp,
    val topEnd: Dp,
    val bottomStart: Dp,
    val bottomEnd: Dp
)

/** Per-corner radii for a card at [index] within a grouped list of [size] items. */
internal fun groupCorners(index: Int, size: Int): GroupCorners {
    val outer = 30.dp
    val inner = 6.dp
    return when {
        size == 1 -> GroupCorners(outer, outer, outer, outer)
        index == 0 -> GroupCorners(outer, outer, inner, inner)
        index == size - 1 -> GroupCorners(inner, inner, outer, outer)
        else -> GroupCorners(inner, inner, inner, inner)
    }
}

internal fun getGroupShape(index: Int, size: Int): Shape {
    val c = groupCorners(index, size)
    return RoundedCornerShape(
        topStart = c.topStart,
        topEnd = c.topEnd,
        bottomStart = c.bottomStart,
        bottomEnd = c.bottomEnd
    )
}
