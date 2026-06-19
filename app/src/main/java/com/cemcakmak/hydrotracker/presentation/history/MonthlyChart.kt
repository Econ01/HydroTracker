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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.database.entities.DailySummary
import com.cemcakmak.hydrotracker.data.models.DateFormatPattern
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.data.models.WeekStartDay
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

@Composable
internal fun MonthlyChartSection(
    summaries: List<DailySummary>,
    monthOffset: Int,
    weekStartDay: WeekStartDay = WeekStartDay.SYSTEM,
    volumeUnit: VolumeUnit,
    dateFormat: DateFormatPattern = DateFormatPattern.SYSTEM
) {
    var selectedSummary by remember { mutableStateOf<DailySummary?>(null) }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val filteredSummaries = filterSummariesByPeriod(summaries, TimePeriod.MONTHLY, weekOffset = 0, monthOffset, 0)

        if (filteredSummaries.isNotEmpty()) {
            val haptics = LocalHapticFeedback.current
            // Monthly heatmap-style visualization
            MonthlyHeatmap(
                summaries = filteredSummaries,
                onCellClick = { summary -> selectedSummary = summary
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)},
                weekStartDay = weekStartDay
            )

            // Inline detail panel with animation
            AnimatedVisibility(
                visible = selectedSummary != null,
                enter = slideInVertically(
                    initialOffsetY = { -it / 2 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { -it / 2 },
                    animationSpec = tween(200)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                selectedSummary?.let { summary ->
                    InlineDetailPanel(
                        data = ChartDetailData(
                            date = summary.date,
                            amount = summary.totalIntake,
                            goal = summary.dailyGoal,
                            goalPercentage = summary.goalPercentage
                        ),
                        onDismiss = { selectedSummary = null },
                        volumeUnit = volumeUnit,
                        dateFormat = dateFormat
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val totalDays = filteredSummaries.size.toDouble()
                val goalAchievedDays = filteredSummaries.count { it.goalAchieved }
                val successRate = (goalAchievedDays / totalDays) * 100.0

                AnimatedStatItem(
                    label = stringResource(R.string.history_stat_days_tracked),
                    targetValue = totalDays,
                    formatValue = { it.toInt().toString() }
                )

                VerticalDivider(
                    modifier = Modifier
                        .height(34.dp)
                        .width(2.dp)
                )

                AnimatedStatItem(
                    label = stringResource(R.string.history_stat_goals_met),
                    targetValue = goalAchievedDays.toDouble(),
                    hapticsEnabled = true,
                    formatValue = { it.toInt().toString() }
                )

                VerticalDivider(
                    modifier = Modifier
                        .height(34.dp)
                        .width(2.dp)
                )

                AnimatedStatItem(
                    label = stringResource(R.string.history_stat_success_rate),
                    targetValue = successRate,
                    formatValue = { stringResource(R.string.percent_format, it.toInt()) }
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.history_empty_month),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MonthlyHeatmap(
    summaries: List<DailySummary>,
    onCellClick: (DailySummary) -> Unit,
    weekStartDay: WeekStartDay = WeekStartDay.SYSTEM
) {
    // Create a map for quick lookup and determine the month being displayed
    val summaryMap = summaries.associateBy { it.date }

    // Get the month/year being displayed
    val monthYear = if (summaries.isNotEmpty()) {
        val firstDate = LocalDate.parse(summaries.first().date)
        firstDate.withDayOfMonth(1)
    } else {
        LocalDate.now().withDayOfMonth(1)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Use the user's preferred week start day (resolve SYSTEM to the device locale)
        val weekFields = WeekFields.of(weekStartDay.resolve(), 1)

        // Get first day of month and its day of week
        val firstDayOfMonth = monthYear.withDayOfMonth(1)
        val lastDayOfMonth = monthYear.withDayOfMonth(monthYear.lengthOfMonth())

        // Find the first day to display (might be from previous month)
        val startOfCalendar = firstDayOfMonth.with(weekFields.dayOfWeek(), 1)

        // Find the last day to display (might be from next month)
        val endOfCalendar = lastDayOfMonth.with(weekFields.dayOfWeek(), 7)

        // Generate all days for the calendar grid
        val calendarDays = mutableListOf<LocalDate>()
        var currentDate = startOfCalendar
        while (!currentDate.isAfter(endOfCalendar)) {
            calendarDays.add(currentDate)
            currentDate = currentDate.plusDays(1)
        }

        // Group into weeks
        val weeks = calendarDays.chunked(7)

        // Day headers based on week start day
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val startDay = weekStartDay.resolve()
            val startIndex = orderedWeekDays.indexOf(startDay)
            val dayHeaders = (orderedWeekDays.drop(startIndex) + orderedWeekDays.take(startIndex))
                .map { shortDayNameResIds.getValue(it) }

            dayHeaders.forEach { dayNameResId ->
                Text(
                    text = stringResource(dayNameResId),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Calendar weeks
        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                week.forEach { date ->
                    val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    val summary = summaryMap[dateString]
                    val isCurrentMonth = date.month == monthYear.month

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(MaterialTheme.shapes.small)
                                .clickable(enabled = summary != null && isCurrentMonth) {
                                    summary?.let { onCellClick(it) }
                                }
                                .background(
                                    when {
                                        !isCurrentMonth -> Color.Transparent
                                        summary == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        summary.goalAchieved -> MaterialTheme.colorScheme.primary
                                        summary.goalPercentage >= 0.8f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        summary.goalPercentage >= 0.6f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        summary.goalPercentage >= 0.4f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                        summary.goalPercentage >= 0.2f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCurrentMonth) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        summary == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                        summary.goalPercentage > 0.5f -> MaterialTheme.colorScheme.onPrimary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val orderedWeekDays = listOf(
    DayOfWeek.SUNDAY,
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY
)

private val shortDayNameResIds = mapOf(
    DayOfWeek.SUNDAY to R.string.weekday_short_sun,
    DayOfWeek.MONDAY to R.string.weekday_short_mon,
    DayOfWeek.TUESDAY to R.string.weekday_short_tue,
    DayOfWeek.WEDNESDAY to R.string.weekday_short_wed,
    DayOfWeek.THURSDAY to R.string.weekday_short_thu,
    DayOfWeek.FRIDAY to R.string.weekday_short_fri,
    DayOfWeek.SATURDAY to R.string.weekday_short_sat
)

@Preview(showBackground = true, name = "Monthly Chart")
@Composable
private fun MonthlyChartSectionPreview() {
    val today = LocalDate.now()
    val dailyGoal = 2700.0
    val startOfMonth = today.withDayOfMonth(1)
    val daysInMonth = today.lengthOfMonth()
    val sampleSummaries = (0 until daysInMonth).map { dayIndex ->
        val date = startOfMonth.plusDays(dayIndex.toLong())
        val totalIntake = when (dayIndex % 7) {
            0 -> dailyGoal * 1.10
            1 -> dailyGoal * 0.95
            2 -> dailyGoal * 0.75
            3 -> dailyGoal * 1.05
            4 -> dailyGoal * 0.50
            5 -> dailyGoal * 0.85
            else -> dailyGoal * 1.20
        }
        val entryCount = 4 + (dayIndex % 3)
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
        MonthlyChartSection(
            summaries = sampleSummaries,
            monthOffset = 0,
            volumeUnit = VolumeUnit.MILLILITRES,
            dateFormat = DateFormatPattern.SYSTEM
        )
    }
}
