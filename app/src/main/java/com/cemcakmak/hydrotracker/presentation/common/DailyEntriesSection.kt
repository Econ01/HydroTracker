package com.cemcakmak.hydrotracker.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.data.models.ActivityLevel
import com.cemcakmak.hydrotracker.data.models.AgeGroup
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.models.Gender
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.presentation.common.dialogs.DailyEntryDeleteDialog
import com.cemcakmak.hydrotracker.presentation.common.shapes.PillShape
import com.cemcakmak.hydrotracker.presentation.common.shapes.SquircleShape
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.ui.theme.rememberBeverageColorRoles
import com.cemcakmak.hydrotracker.utils.ContainerIconMapper
import kotlin.time.Duration.Companion.milliseconds

/**
 * A reusable card that lists daily water intake entries.
 *
 * Each row supports a Gmail-style swipe-to-edit and swipe-to-delete gesture, and the
 * section only renders when [entries] is not empty.
 *
 * @param modifier Modifier applied to the outer card.
 * @param entries The list of entries to display for the selected day.
 * @param userProfile Profile used for volume formatting and goal context.
 * @param themePreferences Theme preferences used for time/date formatting.
 * @param tonalElevation Tonal elevation of the surface
 * @param cascadeEntries When true, rows reveal one-by-one (staggered).
 * @param onEdit Called when the user swipes an entry to edit it.
 * @param onDelete Called when the user confirms deletion of an entry.
 * @param title Optional section title. Defaults to the "Recent entries" label.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DailyEntriesSection(
    modifier: Modifier = Modifier,
    entries: List<WaterIntakeEntry>,
    userProfile: UserProfile,
    themePreferences: ThemePreferences,
    tonalElevation: Dp = 2.dp,
    cascadeEntries: Boolean = true,
    onEdit: (WaterIntakeEntry) -> Unit,
    onDelete: (WaterIntakeEntry) -> Unit,
    title: String = stringResource(R.string.home_section_recent_entries)
) {
    if (entries.isEmpty()) return

    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    // Drives the confirmation dialogue (set when a left-swipe commits; row returns to centre meanwhile).
    var dialogEntry by remember { mutableStateOf<WaterIntakeEntry?>(null) }
    // Set on confirm; the matching row plays the collapse, then onDelete fires when it settles.
    var confirmedDeleteEntry by remember { mutableStateOf<WaterIntakeEntry?>(null) }
    // Clear the marker only once the entry has actually left the list, so neighbours don't briefly
    // revert their corners in the window between the collapse settling and the data updating.
    LaunchedEffect(entries, confirmedDeleteEntry) {
        if (confirmedDeleteEntry?.let { c -> entries.none { it.id == c.id } } == true) {
            confirmedDeleteEntry = null
        }
    }

    val sizeSpec = MaterialTheme.motionScheme.slowSpatialSpec<IntSize>()
    val fadeSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
    // Group shape is driven by the surviving rows so a neighbour starts morphing
    // its corners the instant deletion is confirmed. In sync with the collapse.
    val survivorIndexById = entries
        .filterNot { it.id == confirmedDeleteEntry?.id }
        .withIndex()
        .associate { (i, e) -> e.id to i }
    val survivorCount = survivorIndexById.size

    val rowStates = remember { mutableStateMapOf<Long, MutableTransitionState<Boolean>>() }
    val orderedIds = entries.map { it.id }
    LaunchedEffect(orderedIds) {
        if (!cascadeEntries) return@LaunchedEffect
        for (id in orderedIds) {
            val state = snapshotFlow { rowStates[id] }.filterNotNull().first()
            if (state.currentState || state.targetState) continue
            state.targetState = true
            delay(200L.milliseconds)
        }
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            modifier = Modifier.padding(vertical = 16.dp),
            text = title,
            style = MaterialTheme.typography.titleLargeEmphasized
        )

        entries.forEachIndexed { index, entry ->
            key(entry.id) {
                val visibleState = remember { MutableTransitionState(!cascadeEntries) }
                DisposableEffect(Unit) {
                    rowStates[entry.id] = visibleState
                    onDispose { rowStates.remove(entry.id) }
                }
                // Start the collapse once this entry's deletion is confirmed.
                LaunchedEffect(confirmedDeleteEntry) {
                    if (confirmedDeleteEntry?.id == entry.id) visibleState.targetState = false
                }
                // Remove from the data only after the collapse animation has settled and only when
                // this row is the confirmed-delete target. A freshly created row is also idle+hidden
                // (before the cascade reveals it), so without this guard every row would self-delete.
                // The marker is cleared separately once the entry leaves [entries].
                LaunchedEffect(visibleState.isIdle) {
                    if (visibleState.isIdle && !visibleState.currentState &&
                        confirmedDeleteEntry?.id == entry.id
                    ) {
                        onDelete(entry)
                    }
                }

                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = fadeIn(fadeSpec) + expandVertically(sizeSpec),
                    exit = shrinkVertically(sizeSpec) + fadeOut(fadeSpec)
                ) {
                    BoxWithConstraints {

                        val swipeState = rememberSwipeActionState(maxOffset = maxWidth)

                        SwipeActionItem(
                            state = swipeState,
                            startAction = SwipeActionConfig(
                                icon = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.cd_edit_entry),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            endAction = SwipeActionConfig(
                                icon = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.cd_delete_entry),
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            onStartAction = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEdit(entry)
                                coroutineScope.launch { swipeState.animateTo(SwipeActionAnchor.Center) }
                            },
                            onEndAction = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                dialogEntry = entry
                                coroutineScope.launch { swipeState.animateTo(SwipeActionAnchor.Center) }
                            }
                        ) {
                            // A surviving row uses its position among survivors; the row currently
                            // collapsing keeps its original position so its own corners hold steady.
                            val survivorIndex = survivorIndexById[entry.id]
                            DailyGroupCard(
                                index = survivorIndex ?: index,
                                size = if (survivorIndex != null) survivorCount else entries.size,
                                tonalElevation = tonalElevation
                            ) {
                                DailyEntryItem(
                                    entry = entry,
                                    userProfile = userProfile,
                                    themePreferences = themePreferences
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    dialogEntry?.let { entryToDelete ->
        DailyEntryDeleteDialog(
            entry = entryToDelete,
            userProfile = userProfile,
            onConfirm = {
                confirmedDeleteEntry = entryToDelete
                dialogEntry = null
            },
            onDismiss = {
                dialogEntry = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DailyGroupCard(
    index: Int,
    size: Int,
    tonalElevation: Dp = 2.dp,
    content: @Composable () -> Unit
) {
    val shape = getShapeForIndex(index, size)

    Surface(
        shape = shape,
        tonalElevation = tonalElevation,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
    ) { content() }
}

/**
 * A single daily intake row. Swipe actions are provided by [DailyEntriesSection].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyEntryItem(
    entry: WaterIntakeEntry,
    userProfile: UserProfile,
    themePreferences: ThemePreferences
) {
    val context = LocalContext.current
    val containerIcon = remember(entry.iconType, entry.iconName, entry.containerVolume) {
        ContainerIconMapper.getIconByName(entry.iconType, entry.iconName)
            ?: ContainerIconMapper.getIconForVolume(entry.containerVolume)
    }
    val containerLabel = when {
        entry.containerType == "Custom" -> stringResource(R.string.home_option_custom)
        else -> entry.containerType
    }
    val beverageEnum = remember(entry.beverageType) {
        BeverageType.entries.find { it.name == entry.beverageType }
    }
    val beverageLabel = if (beverageEnum != null) {
        stringResource(beverageEnum.labelResId)
    } else {
        entry.beverageType
    }
    val beverageColors = rememberBeverageColorRoles(
        beverageKey = entry.beverageType,
        useBeverageColors = themePreferences.useBeverageColors
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading icon
            Surface(
                modifier = Modifier.size(height = 56.dp, width = 34.dp),
                shape = PillShape,
                color = beverageColors.color
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(containerIcon.checkedRes),
                        contentDescription = containerLabel,
                        tint = beverageColors.onColor
                    )
                }
            }

            // Main content
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .padding(start = 16.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                // Title
                Text(
                    text = containerLabel,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMediumEmphasized
                )

                if (entry.beverageType != BeverageType.WATER.name) {
                    Text(
                        text = stringResource(
                            R.string.home_beverage_effective_name,
                            beverageLabel
                        ),
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = beverageColors.color
                    )
                }

                // timestamp
                Text(
                    text = entry.getFormattedTime(context, themePreferences.timeFormat),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

            }

            // Trailing content
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.Top)
            ) {
                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .padding(vertical = 2.dp),
                        text = entry.getFormattedAmount(context, userProfile.volumeUnit),
                        style = MaterialTheme.typography.titleMediumEmphasized
                    )
                }

                if (entry.beverageType != BeverageType.WATER.name) {
                    Surface(
                        shape = MaterialTheme.shapes.extraExtraLarge,
                        color = beverageColors.color
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .padding(vertical = 2.dp),
                            text = stringResource(
                                R.string.home_beverage_effective_amount,
                                entry.getFormattedEffectiveAmount(context, userProfile.volumeUnit)
                            ),
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            color = beverageColors.onColor
                        )
                    }
                }
            }
        }
    }
}

private fun getShapeForIndex(
    index: Int,
    size: Int,
    outerRadius: Dp = 30.dp,
    innerRadius: Dp = 6.dp
): Shape {
    return when {
        size == 1 -> SquircleShape(
            topStart = CornerSize(outerRadius),
            topEnd = CornerSize(outerRadius),
            bottomStart = CornerSize(outerRadius),
            bottomEnd = CornerSize(outerRadius)
        )
        index == 0 -> SquircleShape(
            topStart = CornerSize(outerRadius),
            topEnd = CornerSize(outerRadius),
            bottomStart = CornerSize(innerRadius),
            bottomEnd = CornerSize(innerRadius)
        )
        index == size - 1 -> SquircleShape(
            topStart = CornerSize(innerRadius),
            topEnd = CornerSize(innerRadius),
            bottomStart = CornerSize(outerRadius),
            bottomEnd = CornerSize(outerRadius)
        )
        else -> RoundedCornerShape(innerRadius)
    }
}


@Preview(showBackground = true)
@Composable
private fun DailyEntriesSectionPreview() {
    val previewUser = UserProfile(
        name = "Preview User",
        gender = Gender.MALE,
        ageGroup = AgeGroup.ADULT_31_50,
        activityLevel = ActivityLevel.MODERATE,
        wakeUpTime = "07:00",
        sleepTime = "23:00",
        dailyWaterGoal = 2500.0,
        reminderInterval = 60
    )
    val previewTheme = ThemePreferences()

    HydroTrackerTheme {
        DailyEntriesSection(
            entries = listOf(
                WaterIntakeEntry(
                    id = 1,
                    amount = 250.0,
                    timestamp = System.currentTimeMillis() - 3_600_000,
                    date = "2026-06-20",
                    containerType = "Glass",
                    containerVolume = 250.0,
                    beverageType = BeverageType.WATER.name
                ),
                WaterIntakeEntry(
                    id = 2,
                    amount = 330.0,
                    timestamp = System.currentTimeMillis() - 7_200_000,
                    date = "2026-06-20",
                    containerType = "Mug",
                    containerVolume = 330.0,
                    beverageType = BeverageType.COFFEE.name,
                    beverageMultiplier = 0.95
                ),
                WaterIntakeEntry(
                    id = 3,
                    amount = 500.0,
                    timestamp = System.currentTimeMillis() - 10_800_000,
                    date = "2026-06-20",
                    containerType = "Bottle",
                    containerVolume = 500.0,
                    beverageType = BeverageType.WATER.name
                )
            ),
            userProfile = previewUser,
            themePreferences = previewTheme,
            onEdit = {},
            onDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyEntryItemPreview() {
    val previewUser = UserProfile(
        name = "Preview User",
        gender = Gender.FEMALE,
        ageGroup = AgeGroup.YOUNG_ADULT_18_30,
        activityLevel = ActivityLevel.LIGHT,
        wakeUpTime = "07:00",
        sleepTime = "23:00",
        dailyWaterGoal = 2000.0,
        reminderInterval = 60
    )
    val previewTheme = ThemePreferences()

    HydroTrackerTheme {
        DailyEntryItem(
            entry = WaterIntakeEntry(
                id = 4,
                amount = 250.0,
                timestamp = System.currentTimeMillis(),
                date = "2026-06-20",
                containerType = "Glass",
                containerVolume = 250.0,
                beverageType = BeverageType.TEA.name,
                beverageMultiplier = 0.9
            ),
            userProfile = previewUser,
            themePreferences = previewTheme
        )
    }
}
