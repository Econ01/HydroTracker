/*
 *
 *  * HydroTracker - A modern and private water intake tracking application
 *  * Copyright (c) 2026 Ali Cem Çakmak
 *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.cemcakmak.hydrotracker.presentation.history

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.vectorResource
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.data.models.ActivityLevel
import com.cemcakmak.hydrotracker.data.models.AgeGroup
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.models.DateFormatPattern
import com.cemcakmak.hydrotracker.data.models.Gender
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.models.WeekStartDay
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.presentation.common.BeverageOption
import com.cemcakmak.hydrotracker.presentation.common.BlurMorph
import com.cemcakmak.hydrotracker.presentation.common.DailyEntriesSection
import com.cemcakmak.hydrotracker.presentation.common.EditWaterDialog
import com.cemcakmak.hydrotracker.presentation.common.rememberAnimatedDouble
import com.cemcakmak.hydrotracker.presentation.common.toOption
import com.cemcakmak.hydrotracker.utils.DateTimeFormatters
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    themePreferences: ThemePreferences = ThemePreferences(),
    userProfile: UserProfile? = null,
    activeBeverages: List<BeverageOption> = BeverageType.getAllSorted().map { it.toOption() },
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onPeriodSelected: (TimePeriod) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onDaySelected: (String) -> Unit,
    onUpdateEntry: (WaterIntakeEntry, WaterIntakeEntry) -> Unit,
    onDeleteEntry: (WaterIntakeEntry) -> Unit
) {
    val haptics = LocalHapticFeedback.current

    var entryToEdit by remember { mutableStateOf<WaterIntakeEntry?>(null) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // Scroll haptic logic
    var historyWasExpanded by remember { mutableStateOf(true) }
    var historyWasCollapsed by remember { mutableStateOf(false) }

    LaunchedEffect(scrollBehavior.state) {
        snapshotFlow { scrollBehavior.state.collapsedFraction }
            .collect { fraction ->
                val isExpanded = fraction == 0f
                val isCollapsed = fraction == 1f

                if (isExpanded && !historyWasExpanded) {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
                if (isCollapsed && !historyWasCollapsed) {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                }

                historyWasExpanded = isExpanded
                historyWasCollapsed = isCollapsed
            }
    }

    val volumeUnit = userProfile?.volumeUnit ?: VolumeUnit.MILLILITRES

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeFlexibleTopAppBar(
                    title = { Text(stringResource(R.string.nav_history)) },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        scrolledContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(top = innerPadding.calculateTopPadding()),
            ) {
                // Period Selector
                item {
                    PeriodSelector(
                        selectedPeriod = uiState.selectedPeriod,
                        onPeriodSelected = onPeriodSelected,
                        currentWeekOffset = uiState.weekOffset,
                        currentMonthOffset = uiState.monthOffset,
                        currentYearOffset = uiState.yearOffset,
                        onPreviousPeriod = onPreviousPeriod,
                        onNextPeriod = onNextPeriod,
                        weekStartDay = themePreferences.weekStartDay,
                        dateFormat = themePreferences.dateFormat
                    )
                }

                // Main Chart Section
                item {
                    AnimatedContent(
                        targetState = uiState.selectedPeriod,
                        modifier = Modifier.fillMaxWidth(),
                        transitionSpec = {
                            val direction = when {
                                targetState.ordinal > initialState.ordinal -> 1
                                targetState.ordinal < initialState.ordinal -> -1
                                else -> 0
                            }
                            val slideDuration = 600

                            (slideInHorizontally(tween(slideDuration, easing = EaseOutCubic)) { fullWidth ->
                                fullWidth * direction
                            } + fadeIn(tween(slideDuration, easing = EaseOutCubic)))
                                .togetherWith(
                                    slideOutHorizontally(tween(slideDuration, easing = EaseOutCubic)) { fullWidth ->
                                        -fullWidth * direction
                                    } + fadeOut(tween(slideDuration, easing = EaseOutCubic))
                                )
                                .using(SizeTransform(clip = false))
                        },
                        label = "historyPeriodTransition"
                    ) { period ->
                        val blur by transition.animateDp(
                            transitionSpec = { tween(600, easing = EaseOutCubic) },
                            label = "historyPeriodBlur"
                        ) { enterExit ->
                            if (enterExit == EnterExitState.Visible) 0.dp else 10.dp
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .blur(blur, BlurredEdgeTreatment.Unbounded)
                        ) {
                            when (period) {
                                TimePeriod.WEEKLY -> {
                                    WeeklyChartSection(
                                        weekOffset = uiState.weekOffset,
                                        summaries = uiState.summaries,
                                        stats = uiState.weeklyStats,
                                        weekStartDay = themePreferences.weekStartDay,
                                        volumeUnit = volumeUnit,
                                        animationDelayMillis = CHART_ANIMATION_DELAY_MILLIS,
                                        onDaySelected = onDaySelected
                                    )
                                }
                                TimePeriod.MONTHLY -> {
                                    MonthlyChartSection(
                                        summaries = uiState.summaries,
                                        stats = uiState.monthlyStats,
                                        weekStartDay = themePreferences.weekStartDay,
                                        animationDelayMillis = CHART_ANIMATION_DELAY_MILLIS,
                                        onDaySelected = onDaySelected
                                    )
                                }
                                TimePeriod.YEARLY -> {
                                    YearlyChartSection(
                                        summaries = uiState.summaries,
                                        stats = uiState.yearlyStats,
                                        volumeUnit = volumeUnit,
                                        animationDelayMillis = CHART_ANIMATION_DELAY_MILLIS
                                    )
                                }
                            }
                        }
                    }
                }

                // Selected day entries
                item {
                    SelectedDayEntries(
                        selectedDate = uiState.selectedDate,
                        entries = uiState.selectedDateEntries,
                        userProfile = userProfile,
                        themePreferences = themePreferences,
                        onEdit = { entry ->
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            entryToEdit = entry
                        },
                        onDelete = { entry ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDeleteEntry(entry)
                        }
                    )
                }
            }
        }

        // Edit entry dialogue
        entryToEdit?.let { entry ->
            EditWaterDialog(
                entry = entry,
                themePreferences = themePreferences,
                volumeUnit = userProfile?.volumeUnit ?: VolumeUnit.MILLILITRES,
                onDismiss = { entryToEdit = null },
                onConfirm = { updatedEntry ->
                    onUpdateEntry(entry, updatedEntry)
                    entryToEdit = null
                },
                beverages = activeBeverages
            )
        }
    }
}

private const val CHART_ANIMATION_DELAY_MILLIS = 200

enum class TimePeriod(@param:StringRes val displayNameResId: Int) {
    WEEKLY(R.string.history_period_weekly),
    MONTHLY(R.string.history_period_monthly),
    YEARLY(R.string.history_period_yearly)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PeriodSelector(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit,
    currentWeekOffset: Int,
    currentMonthOffset: Int,
    currentYearOffset: Int,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    weekStartDay: WeekStartDay,
    dateFormat: DateFormatPattern
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val haptics = LocalHapticFeedback.current

        // Period Type Selection with ToggleButton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimePeriod.entries.forEach { period ->
                val isSelected = selectedPeriod == period

                ToggleButton(
                    modifier = Modifier.weight(1f),
                    checked = isSelected,
                    onCheckedChange = { onPeriodSelected(period)
                        haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)}
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(period.displayNameResId),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Navigation Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = {
                    onPreviousPeriod()
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                },
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.filledIconButtonColors()
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.arrow_back_filled),
                    contentDescription = stringResource(
                        R.string.cd_previous_period,
                        stringResource(selectedPeriod.displayNameResId)
                    )
                )
            }

            data class PeriodTitleState(
                val period: TimePeriod,
                val weekOffset: Int,
                val monthOffset: Int,
                val yearOffset: Int
            )

            BlurMorph(
                targetState = PeriodTitleState(
                    selectedPeriod,
                    currentWeekOffset,
                    currentMonthOffset,
                    currentYearOffset
                )
            ) { state, blurModifier ->
                Text(
                    modifier = Modifier
                        .then(blurModifier)
                        .padding(horizontal = 12.dp)
                        .padding(vertical = 8.dp),
                    text = getCurrentPeriodText(
                        state.period,
                        state.weekOffset,
                        state.monthOffset,
                        state.yearOffset,
                        weekStartDay,
                        dateFormat
                    ),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    textAlign = TextAlign.Center
                )
            }

            IconButton(
                onClick = {
                    onNextPeriod()
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                },
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.filledIconButtonColors(),
                enabled = when (selectedPeriod) {
                    TimePeriod.WEEKLY -> currentWeekOffset < 0
                    TimePeriod.MONTHLY -> currentMonthOffset < 0
                    TimePeriod.YEARLY -> currentYearOffset < 0
                }
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.arrow_forward_filled),
                    contentDescription = stringResource(
                        R.string.cd_next_period,
                        stringResource(selectedPeriod.displayNameResId)
                    )
                )
            }
        }
    }
}

@Composable
internal fun AnimatedStatItem(
    label: String,
    targetValue: Double,
    hapticsEnabled: Boolean = false,
    formatValue: @Composable (Float) -> String
) {
    val animatedValue = rememberAnimatedDouble(
        targetValue = targetValue,
        hapticsEnabled = hapticsEnabled
    )
    ChartStatItem(
        label = label,
        value = formatValue(animatedValue)
    )
}

@Composable
internal fun ChartStatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
internal fun SelectedDayEntries(
    selectedDate: String?,
    entries: List<WaterIntakeEntry>,
    userProfile: UserProfile?,
    themePreferences: ThemePreferences,
    onEdit: (WaterIntakeEntry) -> Unit,
    onDelete: (WaterIntakeEntry) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        if (selectedDate == null || userProfile == null) {
            Text(
                text = stringResource(R.string.history_select_day_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
            )
        } else {
            DailyEntriesSection(
                entries = entries,
                userProfile = userProfile,
                themePreferences = themePreferences,
                onEdit = onEdit,
                onDelete = onDelete,
                title = DateTimeFormatters.formatDate(
                    LocalDate.parse(selectedDate),
                    themePreferences.dateFormat
                )
            )
        }
    }
}

@Composable
internal fun getCurrentPeriodText(
    period: TimePeriod,
    weekOffset: Int,
    monthOffset: Int,
    yearOffset: Int,
    weekStartDay: WeekStartDay = WeekStartDay.SYSTEM,
    dateFormat: DateFormatPattern = DateFormatPattern.SYSTEM
): String {
    return when (period) {
        TimePeriod.WEEKLY -> {
            val (startOfWeek, endOfWeek) = getWeekDateRange(weekOffset, weekStartDay)

            when (weekOffset) {
                0 -> stringResource(R.string.history_this_week)
                -1 -> stringResource(R.string.history_last_week)
                else -> "${DateTimeFormatters.formatDate(startOfWeek, dateFormat)} - ${DateTimeFormatters.formatDate(endOfWeek, dateFormat)}"
            }
        }
        TimePeriod.MONTHLY -> {
            val today = LocalDate.now()
            val targetMonth = today.plusMonths(monthOffset.toLong())
            when (monthOffset) {
                0 -> stringResource(R.string.history_this_month)
                -1 -> stringResource(R.string.history_last_month)
                else -> targetMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            }
        }
        TimePeriod.YEARLY -> {
            val today = LocalDate.now()
            val targetYear = today.plusYears(yearOffset.toLong())
            when (yearOffset) {
                0 -> stringResource(R.string.history_this_year)
                -1 -> stringResource(R.string.history_last_year)
                else -> targetYear.format(DateTimeFormatter.ofPattern("yyyy"))
            }
        }
    }
}

@Preview(showBackground = true, name = "History Screen")
@Composable
private fun HistoryScreenPreview() {
    val today = LocalDate.now()
    val dailyGoal = 2700.0
    val sampleSummaries = List(35) { index ->
        val date = today.minusDays(index.toLong())
        val totalIntake = when (index % 7) {
            0 -> dailyGoal * 1.10
            1 -> dailyGoal * 0.95
            2 -> dailyGoal * 0.75
            3 -> dailyGoal * 1.05
            4 -> dailyGoal * 0.50
            5 -> dailyGoal * 0.85
            else -> dailyGoal * 1.20
        }
        val entryCount = 4 + (index % 5)
        com.cemcakmak.hydrotracker.data.database.entities.DailySummary(
            date = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            totalIntake = totalIntake,
            dailyGoal = dailyGoal,
            goalAchieved = totalIntake >= dailyGoal,
            goalPercentage = (totalIntake / dailyGoal).toFloat(),
            entryCount = entryCount,
            firstIntakeTime = null,
            lastIntakeTime = null,
            largestIntake = totalIntake * 0.4,
            averageIntake = totalIntake / entryCount
        )
    }

    val selectedDate = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    val previewUser = UserProfile(
        name = "Preview User",
        gender = Gender.MALE,
        ageGroup = AgeGroup.ADULT_31_50,
        activityLevel = ActivityLevel.MODERATE,
        wakeUpTime = "07:00",
        sleepTime = "23:00",
        dailyWaterGoal = dailyGoal,
        reminderInterval = 60,
        volumeUnit = VolumeUnit.MILLILITRES
    )

    val sampleEntries = listOf(
        WaterIntakeEntry(
            id = 1,
            amount = 250.0,
            timestamp = System.currentTimeMillis() - 3_600_000,
            date = selectedDate,
            containerType = "Glass",
            containerVolume = 250.0,
            beverageType = BeverageType.WATER.name
        ),
        WaterIntakeEntry(
            id = 2,
            amount = 330.0,
            timestamp = System.currentTimeMillis() - 7_200_000,
            date = selectedDate,
            containerType = "Mug",
            containerVolume = 330.0,
            beverageType = BeverageType.COFFEE.name,
            beverageMultiplier = 0.95
        ),
        WaterIntakeEntry(
            id = 3,
            amount = 500.0,
            timestamp = System.currentTimeMillis() - 10_800_000,
            date = selectedDate,
            containerType = "Bottle",
            containerVolume = 500.0,
            beverageType = BeverageType.WATER.name
        )
    )

    val uiState = HistoryUiState(
        summaries = sampleSummaries,
        weeklyStats = WeeklyHistoryStats(
            totalIntake = sampleSummaries.take(7).sumOf { it.totalIntake },
            averageIntake = sampleSummaries.take(7).map { it.totalIntake }.average(),
            bestDayIntake = sampleSummaries.take(7).maxOfOrNull { it.totalIntake } ?: 0.0
        ),
        monthlyStats = MonthlyHistoryStats(
            daysTracked = sampleSummaries.size,
            goalsMet = sampleSummaries.count { it.goalAchieved },
            successRate = (sampleSummaries.count { it.goalAchieved }.toDouble() / sampleSummaries.size) * 100.0
        ),
        yearlyStats = YearlyHistoryStats(
            daysTracked = sampleSummaries.size,
            goalsMet = sampleSummaries.count { it.goalAchieved },
            totalIntake = sampleSummaries.sumOf { it.totalIntake }
        ),
        selectedDate = selectedDate,
        selectedDateEntries = sampleEntries
    )

    HydroTrackerTheme {
        HistoryScreen(
            uiState = uiState,
            themePreferences = ThemePreferences(),
            userProfile = previewUser,
            onPeriodSelected = {},
            onPreviousPeriod = {},
            onNextPeriod = {},
            onDaySelected = {},
            onUpdateEntry = { _, _ -> },
            onDeleteEntry = {}
        )
    }
}
