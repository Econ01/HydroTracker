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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.database.entities.DailySummary
import com.cemcakmak.hydrotracker.presentation.common.EntryAnimationDefaults
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.ui.theme.extendedColorScheme
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun YearlyChartSection(
    summaries: List<DailySummary>,
    stats: YearlyHistoryStats,
    volumeUnit: VolumeUnit,
    animationDelayMillis: Int = 0
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (summaries.isNotEmpty()) {
            // Yearly visualization - all days of the year
            YearlyHeatmap(
                summaries = summaries,
                animationDelayMillis = animationDelayMillis
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedStatItem(
                    label = stringResource(R.string.history_stat_days_tracked),
                    targetValue = stats.daysTracked.toDouble(),
                    hapticsEnabled = true,
                    formatValue = { it.toInt().toString() },
                    entryDelayMillis = EntryAnimationDefaults.DELAY_MS
                )

                VerticalDivider(
                    modifier = Modifier
                        .height(34.dp)
                        .width(2.dp)
                )

                AnimatedStatItem(
                    label = stringResource(R.string.history_stat_goals_met),
                    targetValue = stats.goalsMet.toDouble(),
                    formatValue = { it.toInt().toString() },
                    entryDelayMillis = EntryAnimationDefaults.DELAY_MS
                )

                VerticalDivider(
                    modifier = Modifier
                        .height(34.dp)
                        .width(2.dp)
                )

                AnimatedStatItem(
                    label = stringResource(R.string.history_stat_total_intake),
                    targetValue = stats.totalIntake,
                    formatValue = { VolumeUnitConverter.format(context, it.toDouble(), volumeUnit) },
                    entryDelayMillis = EntryAnimationDefaults.DELAY_MS
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
                    text = stringResource(R.string.history_empty_year),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun YearlyHeatmap(
    summaries: List<DailySummary>,
    animationDelayMillis: Int = 0
) {
    // Create a map for quick lookup of summaries by date
    val summaryMap = summaries.associateBy { it.date }

    // Generate all days of the year
    val yearToShow = if (summaries.isNotEmpty()) {
        LocalDate.parse(summaries.first().date).year
    } else {
        LocalDate.now().year
    }

    val startOfYear = LocalDate.of(yearToShow, 1, 1)
    val daysInYear = if (startOfYear.isLeapYear) 366 else 365

    // Generate all dates in the year
    val allDates = (0 until daysInYear).map { dayIndex ->
        startOfYear.plusDays(dayIndex.toLong())
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dynamic grid layout
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 12.dp),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            items(allDates) { date ->
                // Get the year being displayed
                val monthYear = if (summaries.isNotEmpty()) {
                    val firstDate = LocalDate.parse(summaries.first().date)
                    firstDate.withDayOfYear(1)
                } else {
                    LocalDate.now().withDayOfYear(1)
                }

                val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val summary = summaryMap[dateString]
                val isCurrentYear = date.year == monthYear.year
                val isToday = date == LocalDate.now()

                val (animatedScale, cellData) = rememberAnimatedDay(
                    targetDate = date,
                    targetIsCurrentYear = isCurrentYear,
                    targetSummary = summary,
                    animationDelayMillis = animationDelayMillis
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(animatedScale),
                        shape = MaterialShapes.Sunny.toShape(),
                        color = if (isToday) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            when {
                                !cellData.isCurrentMonth -> Color.Transparent
                                cellData.summary == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                cellData.summary.goalAchieved -> MaterialTheme.extendedColorScheme.success
                                cellData.summary.goalPercentage >= 0.8f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                cellData.summary.goalPercentage >= 0.6f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                cellData.summary.goalPercentage >= 0.4f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                cellData.summary.goalPercentage >= 0.2f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            }
                        }
                    ) { }
                }
            }
        }
    }
}

@Composable
fun rememberAnimatedDay(
    targetDate: LocalDate,
    targetIsCurrentYear: Boolean,
    targetSummary: DailySummary?,
    animationSpec: AnimationSpec<Float> = MaterialTheme.motionScheme.slowSpatialSpec(),
    animationDelayMillis: Int = 0
): Pair<Float, CellData> {
    var displayData by remember {
        mutableStateOf(CellData(targetDate, targetIsCurrentYear, targetSummary))
    }

    var hasAnimated by remember { mutableStateOf(false) }
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(targetDate) {
        if (!hasAnimated) {
            val enterDelay = animationDelayMillis + (targetDate.dayOfYear - 1) * 2L
            if (enterDelay > 0) delay(enterDelay.milliseconds)
        }

        if (displayData.date != targetDate && animatable.value > 0f) {
            animatable.animateTo(0f, animationSpec = animationSpec)

            // Swap data safely
            displayData = CellData(targetDate, targetIsCurrentYear, targetSummary)
        }

        animatable.animateTo(
            targetValue = 1f,
            animationSpec = animationSpec
        )

        hasAnimated = true
    }

    return Pair(animatable.value, displayData)
}


@Preview(showBackground = true, name = "Yearly Chart")
@Composable
private fun YearlyChartSectionPreview() {
    val startOfYear = LocalDate.now().withDayOfYear(1)
    val dailyGoal = 2700.0
    val sampleSummaries = (0 until 365 step 7).map { dayIndex ->
        val date = startOfYear.plusDays(dayIndex.toLong())
        val weekIndex = dayIndex / 7
        val totalIntake = when (weekIndex % 7) {
            0 -> dailyGoal * 1.10
            1 -> dailyGoal * 0.95
            2 -> dailyGoal * 0.75
            3 -> dailyGoal * 1.05
            4 -> dailyGoal * 0.50
            5 -> dailyGoal * 0.85
            else -> dailyGoal * 1.20
        }
        val entryCount = 4 + (weekIndex % 3)
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
        YearlyChartSection(
            summaries = sampleSummaries,
            stats = YearlyHistoryStats(
                daysTracked = sampleSummaries.size,
                goalsMet = sampleSummaries.count { it.goalAchieved },
                totalIntake = sampleSummaries.sumOf { it.totalIntake }
            ),
            volumeUnit = VolumeUnit.MILLILITRES
        )
    }
}
