package com.cemcakmak.hydrotracker.presentation.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.data.models.BeveragePreferences
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeverageTypesScreen(
    userRepository: UserRepository,
    onNavigateBack: () -> Unit = {}
) {
    val beveragePrefs by userRepository.beveragePreferences.collectAsState()
    val haptics = LocalHapticFeedback.current

    val orderedVisible = remember(beveragePrefs) {
        beveragePrefs.orderedVisible.mapNotNull { name ->
            BeverageType.entries.find { it.name == name }
        }
    }
    val hiddenList = remember(beveragePrefs) {
        beveragePrefs.hidden.mapNotNull { name ->
            BeverageType.entries.find { it.name == name }
        }
    }

    var combinedList by remember(beveragePrefs) {
        mutableStateOf(orderedVisible + hiddenList)
    }
    var splitCount by remember(beveragePrefs) {
        mutableIntStateOf(orderedVisible.size)
    }

    fun persist(v: List<BeverageType>, h: List<BeverageType>) {
        userRepository.saveBeveragePreferences(
            BeveragePreferences(
                orderedVisible = v.map { it.name },
                hidden = h.map { it.name }.toSet()
            )
        )
    }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fLazy = from.index
        val tLazy = to.index
        val sep = splitCount + 1  // lazy index of HIDDEN_HEADER

        // Never move WATER
        if (fLazy == 0 || tLazy == 0) return@rememberReorderableLazyListState
        // Never land on the separator itself
        if (tLazy == sep) return@rememberReorderableLazyListState

        // Drag visible item onto the placeholder that exists when hidden section is empty
        if (to.key == "HIDDEN_PLACEHOLDER") {
            val f = fLazy - 1  // placeholder only exists when all items are visible
            if (f !in combinedList.indices) return@rememberReorderableLazyListState
            val updated = combinedList.toMutableList()
            val item = updated.removeAt(f)
            val newSplit = (splitCount - 1).coerceAtLeast(0)
            updated.add(newSplit, item)
            combinedList = updated
            splitCount = newSplit
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            persist(updated.take(newSplit), updated.drop(newSplit))
            return@rememberReorderableLazyListState
        }

        // Convert lazy index to combinedList index
        fun toListIdx(i: Int): Int = if (i < sep) i - 1 else i - 2

        val f = toListIdx(fLazy)
        val t = toListIdx(tLazy).coerceIn(0, combinedList.size - 1)
        if (f !in combinedList.indices) return@rememberReorderableLazyListState

        val updated = combinedList.toMutableList().apply {
            add(t.coerceAtMost(size - 1), removeAt(f))
        }

        val crossedIntoHidden = splitCount in (f + 1)..t
        val crossedIntoVisible = splitCount in (t + 1)..f
        val newSplit = when {
            crossedIntoHidden -> (splitCount - 1).coerceAtLeast(0)
            crossedIntoVisible -> (splitCount + 1).coerceAtMost(updated.size)
            else -> splitCount
        }

        combinedList = updated
        splitCount = newSplit
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        persist(updated.take(newSplit), updated.drop(newSplit))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Beverage Types",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Drag to reorder. Drag into Hidden to hide.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(
                state = lazyListState,
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 700.dp)
            ) {
                // WATER — pinned at index 0 of visible group
                item(key = "WATER") {
                    val visibleGroupSize = 1 + splitCount
                    Surface(
                        shape = segmentShape(0, visibleGroupSize),
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
                    ) {
                        ListItem(
                            headlineContent = { Text(BeverageType.WATER.displayName) },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(BeverageType.WATER.iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }

                // Visible items, group size = 1 + splitCount
                itemsIndexed(combinedList.take(splitCount), key = { _, t -> t.name }) { idx, type ->
                    ReorderableItem(reorderState, key = type.name) { isDragging ->
                        BeverageTypeRow(
                            type = type,
                            isDragging = isDragging,
                            isHidden = false,
                            shape = segmentShape(idx + 1, 1 + splitCount),
                            modifier = Modifier.draggableHandle(
                                onDragStarted = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragStopped = {
                                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                }
                            )
                        )
                    }
                }

                // Hidden section divider — always present
                item(key = "HIDDEN_HEADER") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "Hidden",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                }

                // Hidden items — or invisible placeholder when hidden section is empty
                val hiddenItems = combinedList.drop(splitCount)
                if (hiddenItems.isEmpty()) {
                    item(key = "HIDDEN_PLACEHOLDER") {
                        ReorderableItem(reorderState, key = "HIDDEN_PLACEHOLDER") { _ ->
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            )
                        }
                    }
                } else {
                    itemsIndexed(hiddenItems, key = { _, t -> "h_${t.name}" }) { idx, type ->
                        ReorderableItem(reorderState, key = "h_${type.name}") { isDragging ->
                            BeverageTypeRow(
                                type = type,
                                isDragging = isDragging,
                                isHidden = true,
                                shape = segmentShape(idx, hiddenItems.size),
                                modifier = Modifier.draggableHandle(
                                    onDragStarted = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragStopped = {
                                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                    }
                                )
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    userRepository.saveBeveragePreferences(BeveragePreferences.default())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset to Default Order")
            }
        }
    }
}

@Composable
private fun BeverageTypeRow(
    type: BeverageType,
    isDragging: Boolean,
    isHidden: Boolean,
    shape: Shape,
    modifier: Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        label = "drag_scale"
    )
    Surface(
        shape = shape,
        tonalElevation = if (isHidden) 0.dp else 2.dp,
        modifier = Modifier.fillMaxWidth().scale(scale).padding(bottom = 2.dp)
    ) {
        ListItem(
            headlineContent = { Text(type.displayName) },
            leadingContent = {
                Icon(
                    painter = painterResource(type.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Reorder",
                    modifier = modifier.size(24.dp)
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

private fun segmentShape(index: Int, size: Int): Shape {
    val outer = 16.dp
    val inner = 4.dp
    return when {
        size == 1 -> RoundedCornerShape(outer)
        index == 0 -> RoundedCornerShape(topStart = outer, topEnd = outer, bottomStart = inner, bottomEnd = inner)
        index == size - 1 -> RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = outer, bottomEnd = outer)
        else -> RoundedCornerShape(inner)
    }
}
