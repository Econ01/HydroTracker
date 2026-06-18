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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.database.entities.DailySummary
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun YearlyChartSection(
    summaries: List<DailySummary>,
    yearOffset: Int,
    volumeUnit: VolumeUnit
) {
    val context = LocalContext.current
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
                text = getPeriodTitle(TimePeriod.YEARLY),
                style = MaterialTheme.typography.titleLargeEmphasized
            )

            val filteredSummaries = filterSummariesByPeriod(summaries, TimePeriod.YEARLY, weekOffset = 0, monthOffset = 0, yearOffset = yearOffset)

            if (filteredSummaries.isNotEmpty()) {
                // Yearly visualization - all days of the year
                YearlyHeatmap(
                    summaries = filteredSummaries,
                    onCellClick = { _ ->
                        // Cell click handler for future use
                    }
                )

                // Yearly stats
                val totalDays = filteredSummaries.size
                val goalAchievedDays = filteredSummaries.count { it.goalAchieved }
                val totalIntake = filteredSummaries.sumOf { it.totalIntake }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ChartStatItem(
                        label = stringResource(R.string.history_stat_days_tracked),
                        value = "$totalDays"
                    )
                    ChartStatItem(
                        label = stringResource(R.string.history_stat_goals_met),
                        value = "$goalAchievedDays"
                    )
                    ChartStatItem(
                        label = stringResource(R.string.history_stat_total_intake),
                        value = VolumeUnitConverter.format(context, totalIntake, volumeUnit)
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
}

@Composable
private fun YearlyHeatmap(
    summaries: List<DailySummary>,
    onCellClick: (DailySummary) -> Unit
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
                val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val summary = summaryMap[dateString]

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(MaterialTheme.shapes.small)
                        .clickable(enabled = summary != null) {
                            summary?.let { onCellClick(it) }
                        }
                        .background(
                            when {
                                summary == null -> MaterialTheme.colorScheme.surfaceVariant
                                summary.goalAchieved -> MaterialTheme.colorScheme.primary
                                summary.goalPercentage >= 0.8f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                summary.goalPercentage >= 0.6f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                summary.goalPercentage >= 0.4f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                summary.goalPercentage >= 0.2f -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.history_legend_less),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf(0.1f, 0.25f, 0.4f, 0.6f, 0.8f, 1.0f).forEach { alpha ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                    )
                }
            }
            Text(
                text = stringResource(R.string.history_legend_more),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
