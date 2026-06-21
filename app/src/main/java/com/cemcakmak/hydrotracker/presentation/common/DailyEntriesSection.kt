package com.cemcakmak.hydrotracker.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.data.models.ActivityLevel
import com.cemcakmak.hydrotracker.data.models.AgeGroup
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.models.Gender
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme

/**
 * A reusable card that lists daily water intake entries.
 *
 * Each row supports a Gmail-style swipe-to-edit and swipe-to-delete gesture, and the
 * section only renders when [entries] is not empty.
 *
 * @param entries The list of entries to display for the selected day.
 * @param userProfile Profile used for volume formatting and goal context.
 * @param themePreferences Theme preferences used for time/date formatting.
 * @param onEdit Called when the user swipes an entry to edit it.
 * @param onDelete Called when the user confirms deletion of an entry.
 * @param modifier Modifier applied to the outer card.
 * @param title Optional section title. Defaults to the "Recent entries" label.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DailyEntriesSection(
    entries: List<WaterIntakeEntry>,
    userProfile: UserProfile,
    themePreferences: ThemePreferences,
    onEdit: (WaterIntakeEntry) -> Unit,
    onDelete: (WaterIntakeEntry) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.home_section_recent_entries)
) {
    if (entries.isEmpty()) return

    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var pendingDeleteEntry by remember { mutableStateOf<WaterIntakeEntry?>(null) }

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
                AnimatedVisibility(
                    visible = pendingDeleteEntry?.id != entry.id,
                    enter = fadeIn() + expandVertically(),
                    exit = slideOutHorizontally(targetOffsetX = { -it }) + shrinkVertically()
                ) {
                    val swipeState = rememberSwipeActionState()

                    SwipeActionItem(
                        state = swipeState,
                        startAction = SwipeActionConfig(
                            icon = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.cd_edit_entry),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        endAction = SwipeActionConfig(
                            icon = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.cd_delete_entry),
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        onStartAction = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onEdit(entry)
                            coroutineScope.launch {
                                swipeState.animateTo(SwipeActionAnchor.Center)
                            }
                        },
                        onEndAction = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            pendingDeleteEntry = entry
                        }
                    ) {
                        DailyGroupCard(
                            index = index,
                            size = entries.size,
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

    pendingDeleteEntry?.let { entryToDelete ->
        DailyEntryDeleteDialog(
            entry = entryToDelete,
            userProfile = userProfile,
            onConfirm = {
                onDelete(entryToDelete)
                pendingDeleteEntry = null
            },
            onDismiss = {
                pendingDeleteEntry = null
            }
        )
    }
}

@Composable
internal fun DailyGroupCard(
    index: Int,
    size: Int,
    content: @Composable () -> Unit
) {
    val shape = getGroupShape(index, size)

    Surface(
        shape = shape,
        tonalElevation = 2.dp,
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
    val preset = remember(entry.containerType) {
        ContainerPreset.getDefaultPresets().firstOrNull { it.name == entry.containerType }
    }
    val containerLabel = when {
        preset?.labelResId != 0 && preset?.labelResId != null -> stringResource(preset.labelResId)
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
                shape = MaterialTheme.shapes.extraExtraLarge,
                color = if (entry.beverageType != BeverageType.WATER.name) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        preset?.iconRes != null -> {
                            Icon(
                                painter = painterResource(preset.iconRes),
                                contentDescription = containerLabel
                            )
                        }
                        preset?.icon != null -> {
                            Icon(
                                imageVector = preset.icon,
                                contentDescription = containerLabel
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.water_filled),
                                contentDescription = containerLabel
                            )
                        }
                    }
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (entry.beverageType != BeverageType.WATER.name) {
                        Text(
                            text = stringResource(
                                R.string.home_beverage_effective_name,
                                beverageLabel
                            ),
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    Text(
                        text = containerLabel,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMediumEmphasized
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
                    shape = MaterialTheme.shapes.extraExtraLarge,
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
                        color = MaterialTheme.colorScheme.tertiary
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .padding(vertical = 2.dp),
                            text = stringResource(
                                R.string.home_beverage_effective_amount,
                                entry.getFormattedEffectiveAmount(context, userProfile.volumeUnit)
                            ),
                            style = MaterialTheme.typography.titleMediumEmphasized
                        )
                    }
                }
            }
        }
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
