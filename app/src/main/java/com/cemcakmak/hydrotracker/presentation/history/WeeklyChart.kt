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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.database.dao.DailyTotal
import com.cemcakmak.hydrotracker.data.database.entities.DailySummary
import com.cemcakmak.hydrotracker.data.models.DateFormatPattern
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.data.models.WeekStartDay
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import java.time.format.DateTimeFormatter

@Composable
internal fun WeeklyChartSection(
    weekOffset: Int,
    summaries: List<DailySummary> = emptyList(),
    weekStartDay: WeekStartDay = WeekStartDay.SYSTEM,
    volumeUnit: VolumeUnit,
    dateFormat: DateFormatPattern = DateFormatPattern.SYSTEM
) {
    val context = LocalContext.current
    var selectedDayData by remember { mutableStateOf<DailyTotal?>(null) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = getCurrentPeriodText(TimePeriod.WEEKLY, weekOffset, 0, 0, weekStartDay, dateFormat),
                style = MaterialTheme.typography.titleLargeEmphasized
            )

            // Filter summaries for the selected week and convert to DailyTotal format
            val filteredSummaries = filterSummariesByPeriod(summaries, TimePeriod.WEEKLY, weekOffset, 0, 0, weekStartDay)

            // Create a complete week with all 7 days, filling in missing days with 0 data
            val (startOfWeek) = getWeekDateRange(weekOffset, weekStartDay)
            val filteredDailyTotals = mutableListOf<DailyTotal>()

            for (i in 0..6) {
                val currentDate = startOfWeek.plusDays(i.toLong())
                val dateString = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val summary = filteredSummaries.find { it.date == dateString }

                filteredDailyTotals.add(
                    DailyTotal(
                        date = dateString,
                        totalAmount = summary?.totalIntake ?: 0.0,
                        entryCount = summary?.entryCount ?: 0
                    )
                )
            }

            if (filteredDailyTotals.isNotEmpty()) {
                val haptics = LocalHapticFeedback.current
                // Simple bar chart representation
                WeeklyBarChart(
                    dailyTotals = filteredDailyTotals,
                    onBarClick = { dayTotal -> selectedDayData = dayTotal
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)},
                    volumeUnit = volumeUnit
                )

                // Inline detail panel with animation
                AnimatedVisibility(
                    visible = selectedDayData != null,
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
                    selectedDayData?.let { dayData ->
                        InlineDetailPanel(
                            data = ChartDetailData(
                                date = dayData.date,
                                amount = dayData.totalAmount,
                                goal = null,
                                goalPercentage = null
                            ),
                            onDismiss = { selectedDayData = null },
                            volumeUnit = volumeUnit,
                            dateFormat = dateFormat
                        )
                    }
                }

                // Period-specific summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val totalAmount = filteredDailyTotals.sumOf { it.totalAmount }
                    val daysWithData = filteredDailyTotals.count { it.totalAmount > 0.0 }
                    val avgAmount = if (daysWithData > 0) totalAmount / daysWithData else 0.0
                    val bestAmount = filteredDailyTotals.maxOfOrNull { it.totalAmount } ?: 0.0

                    ChartStatItem(
                        label = stringResource(R.string.history_stat_total),
                        value = VolumeUnitConverter.format(context, totalAmount, volumeUnit)
                    )
                    ChartStatItem(
                        label = stringResource(R.string.history_stat_average),
                        value = VolumeUnitConverter.format(context, avgAmount, volumeUnit)
                    )
                    ChartStatItem(
                        label = stringResource(R.string.history_stat_best_day),
                        value = VolumeUnitConverter.format(context, bestAmount, volumeUnit)
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
                        text = stringResource(R.string.history_empty_week),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyBarChart(
    dailyTotals: List<DailyTotal>,
    onBarClick: (DailyTotal) -> Unit,
    volumeUnit: VolumeUnit
) {
    val context = LocalContext.current
    if (dailyTotals.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.history_empty_generic),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxAmount = dailyTotals.maxOfOrNull { it.totalAmount } ?: 1.0

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Bar chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            dailyTotals.forEach { dayTotal ->
                val height = ((dayTotal.totalAmount / maxAmount) * 120).dp

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height)
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onBarClick(dayTotal) }
                            .background(
                                color = MaterialTheme.colorScheme.primary
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (height > 30.dp) {
                            Text(
                                text = VolumeUnitConverter.format(context, dayTotal.totalAmount, volumeUnit),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Day labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            dailyTotals.forEach { dayTotal ->
                Text(
                    text = dayTotal.date.takeLast(2), // Show day number
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Weekly Chart")
@Composable
private fun WeeklyChartSectionPreview() {
    val dailyGoal = 2700.0
    val startOfWeek = getWeekDateRange(0, WeekStartDay.SYSTEM).first
    val sampleSummaries = (0..6).map { dayIndex ->
        val date = startOfWeek.plusDays(dayIndex.toLong())
        val totalIntake = when (dayIndex) {
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
        WeeklyChartSection(
            weekOffset = 0,
            summaries = sampleSummaries,
            volumeUnit = VolumeUnit.MILLILITRES,
            dateFormat = DateFormatPattern.SYSTEM
        )
    }
}
