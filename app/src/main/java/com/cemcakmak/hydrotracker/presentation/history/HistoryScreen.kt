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

package com.cemcakmak.hydrotracker.presentation.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
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
import com.cemcakmak.hydrotracker.data.database.entities.DailySummary
import com.cemcakmak.hydrotracker.data.models.DateFormatPattern
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.models.WeekStartDay
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.presentation.common.BlurMorph
import com.cemcakmak.hydrotracker.presentation.common.rememberAnimatedDouble
import com.cemcakmak.hydrotracker.utils.DateTimeFormatters
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryScreen(
    summaries: List<DailySummary>,
    themePreferences: ThemePreferences = ThemePreferences(),
    userProfile: UserProfile? = null
) {
    val haptics = LocalHapticFeedback.current

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
    // State for different time periods
    var selectedPeriod by remember { mutableStateOf(TimePeriod.WEEKLY) }

    // Navigation state for current week/month/year
    var currentWeekOffset by remember { mutableIntStateOf(0) } // 0 = current week, -1 = previous week, etc.
    var currentMonthOffset by remember { mutableIntStateOf(0) } // 0 = current month, -1 = previous month, etc.
    var currentYearOffset by remember { mutableIntStateOf(0) } // 0 = current year, -1 = previous year, etc.

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
                    .padding(innerPadding),
            ) {
                // Period Selector
                item {
                    PeriodSelector(
                        selectedPeriod = selectedPeriod,
                        onPeriodSelected = {
                            selectedPeriod = it
                            // Reset navigation when switching between periods
                            currentWeekOffset = 0
                            currentMonthOffset = 0
                            currentYearOffset = 0
                        },
                        currentWeekOffset = currentWeekOffset,
                        currentMonthOffset = currentMonthOffset,
                        currentYearOffset = currentYearOffset,
                        onWeekOffsetChanged = { currentWeekOffset = it },
                        onMonthOffsetChanged = { currentMonthOffset = it },
                        onYearOffsetChanged = { currentYearOffset = it },
                        weekStartDay = themePreferences.weekStartDay,
                        dateFormat = themePreferences.dateFormat
                    )
                }

                // Main Chart Section
                item {
                    when (selectedPeriod) {
                        TimePeriod.WEEKLY -> {
                            WeeklyChartSection(
                                weekOffset = currentWeekOffset,
                                summaries = summaries,
                                weekStartDay = themePreferences.weekStartDay,
                                volumeUnit = volumeUnit,
                                dateFormat = themePreferences.dateFormat
                            )
                        }
                        TimePeriod.MONTHLY -> {
                            MonthlyChartSection(
                                summaries = summaries,
                                monthOffset = currentMonthOffset,
                                weekStartDay = themePreferences.weekStartDay,
                                volumeUnit = volumeUnit,
                                dateFormat = themePreferences.dateFormat
                            )
                        }
                        TimePeriod.YEARLY -> {
                            YearlyChartSection(
                                summaries = summaries,
                                yearOffset = currentYearOffset,
                                volumeUnit = volumeUnit
                            )
                        }
                    }
                }
            }
        }
    }
}

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
    onWeekOffsetChanged: (Int) -> Unit,
    onMonthOffsetChanged: (Int) -> Unit,
    onYearOffsetChanged: (Int) -> Unit,
    weekStartDay: WeekStartDay,
    dateFormat: DateFormatPattern
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
                    when (selectedPeriod) {
                        TimePeriod.WEEKLY -> onWeekOffsetChanged(currentWeekOffset - 1)
                        TimePeriod.MONTHLY -> onMonthOffsetChanged(currentMonthOffset - 1)
                        TimePeriod.YEARLY -> onYearOffsetChanged(currentYearOffset - 1)
                    }
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
                    when (selectedPeriod) {
                        TimePeriod.WEEKLY -> onWeekOffsetChanged(currentWeekOffset + 1)
                        TimePeriod.MONTHLY -> onMonthOffsetChanged(currentMonthOffset + 1)
                        TimePeriod.YEARLY -> onYearOffsetChanged(currentYearOffset + 1)
                    }
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

data class ChartDetailData(
    val date: String,
    val amount: Double,
    val goal: Double?,
    val goalPercentage: Float?
)

@Composable
internal fun InlineDetailPanel(
    data: ChartDetailData,
    onDismiss: () -> Unit,
    volumeUnit: VolumeUnit,
    dateFormat: DateFormatPattern
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateTimeFormatters.formatDate(LocalDate.parse(data.date), dateFormat),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                val haptics = LocalHapticFeedback.current
                FilledIconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onDismiss()
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(),
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_close),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Content in a more compact layout
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Water amount - prominent display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.history_detail_water_intake),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = VolumeUnitConverter.format(context, data.amount, volumeUnit),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Goal information (if available)
                data.goal?.let { goal ->
                    data.goalPercentage?.let { percentage ->
                        // Compact progress display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(
                                        R.string.history_detail_goal,
                                        VolumeUnitConverter.format(context, goal, volumeUnit)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = stringResource(
                                        R.string.history_detail_progress,
                                        (percentage * 100).toInt()
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Compact progress bar
                        LinearProgressIndicator(
                            progress = { percentage.coerceAtMost(1.0f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(MaterialTheme.shapes.small),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
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

@Composable
internal fun getPeriodTitle(period: TimePeriod): String {
    return when (period) {
        TimePeriod.WEEKLY -> stringResource(R.string.history_weekly_overview)
        TimePeriod.MONTHLY -> stringResource(R.string.history_monthly_overview)
        TimePeriod.YEARLY -> stringResource(R.string.history_yearly_overview)
    }
}


internal fun getWeekDateRange(weekOffset: Int, weekStartDay: WeekStartDay = WeekStartDay.SYSTEM): Pair<LocalDate, LocalDate> {
    val today = LocalDate.now()
    val weekFields = WeekFields.of(weekStartDay.resolve(), 1)

    // Get the start of current week first
    val currentWeekStart = today.with(weekFields.dayOfWeek(), 1)

    // Then apply the offset to get the target week
    val targetWeekStart = currentWeekStart.plusWeeks(weekOffset.toLong())
    val targetWeekEnd = targetWeekStart.plusDays(6)

    return Pair(targetWeekStart, targetWeekEnd)
}

private fun getMonthDateRange(monthOffset: Int): Pair<LocalDate, LocalDate> {
    val today = LocalDate.now()
    val targetMonth = today.plusMonths(monthOffset.toLong())
    val startOfMonth = targetMonth.withDayOfMonth(1)
    val endOfMonth = targetMonth.withDayOfMonth(targetMonth.lengthOfMonth())
    return Pair(startOfMonth, endOfMonth)
}

private fun getYearDateRange(yearOffset: Int): Pair<LocalDate, LocalDate> {
    val today = LocalDate.now()
    val targetYear = today.plusYears(yearOffset.toLong())
    val startOfYear = targetYear.withDayOfYear(1)
    val endOfYear = targetYear.withDayOfYear(targetYear.lengthOfYear())
    return Pair(startOfYear, endOfYear)
}

internal fun filterSummariesByPeriod(
    summaries: List<DailySummary>,
    period: TimePeriod,
    weekOffset: Int,
    monthOffset: Int,
    yearOffset: Int = 0,
    weekStartDay: WeekStartDay = WeekStartDay.SYSTEM
): List<DailySummary> {
    val (startDate, endDate) = when (period) {
        TimePeriod.WEEKLY -> getWeekDateRange(weekOffset, weekStartDay)
        TimePeriod.MONTHLY -> getMonthDateRange(monthOffset)
        TimePeriod.YEARLY -> getYearDateRange(yearOffset)
    }

    return summaries.filter { summary ->
        val summaryDate = LocalDate.parse(summary.date)
        summaryDate in startDate..endDate
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
        DailySummary(
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

    HydroTrackerTheme {
        HistoryScreen(
            summaries = sampleSummaries
        )
    }
}

