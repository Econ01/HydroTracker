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

package com.cemcakmak.hydrotracker.presentation.statistics.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.TimeFormat
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.presentation.common.shapes.PillShape
import com.cemcakmak.hydrotracker.presentation.common.shapes.SquircleShape
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.utils.DateTimeFormatters
import com.cemcakmak.hydrotracker.utils.NumberFormatters
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import java.time.LocalTime

/**
 * Overview card for the Statistics screen.
 *
 * Shows a high-level summary of the user's hydration history at the top, followed by the original
 * drinking-pattern metrics as a subsection.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OverviewCard(
    currentStreak: Int,
    successRate: Double,
    daysTracked: Int,
    averageFirstDrinkTime: LocalTime?,
    averageLastDrinkTime: LocalTime?,
    averageIntervalMinutes: Double,
    averageDrinkSize: Double,
    largestDrink: Double,
    timeFormat: TimeFormat,
    volumeUnit: VolumeUnit
) {
    val context = LocalContext.current

    val categories = buildList {
        add(
            OverviewCategory(
                iconRes = R.drawable.trophy_filled,
                label = stringResource(R.string.statistics_label_current_streak),
                value = pluralStringResource(R.plurals.statistics_streak_days, currentStreak, currentStreak)
            )
        )
        add(
            OverviewCategory(
                iconRes = R.drawable.check_filled,
                label = stringResource(R.string.statistics_label_success_rate),
                value = stringResource(R.string.percent_format, successRate.toInt())
            )
        )
        add(
            OverviewCategory(
                iconRes = R.drawable.calendar_today_filled,
                label = stringResource(R.string.statistics_label_days_tracked),
                value = NumberFormatters.formatCompactCount(daysTracked.toDouble())
            )
        )
        add(
            OverviewCategory(
                iconRes = R.drawable.automation_filled,
                label = stringResource(R.string.statistics_label_average_interval),
                value = formatInterval(averageIntervalMinutes)
            )
        )
        add(
            OverviewCategory(
                iconRes = R.drawable.glass_cup_filled,
                label = stringResource(R.string.statistics_label_average_drink_size),
                value = VolumeUnitConverter.format(context, averageDrinkSize, volumeUnit)
            )
        )
        add(
            OverviewCategory(
                iconRes = R.drawable.local_cafe_filled,
                label = stringResource(R.string.statistics_label_largest_drink),
                value = VolumeUnitConverter.format(context, largestDrink, volumeUnit)
            )
        )
        add(
            OverviewCategory(
                iconRes = R.drawable.schedule_filled,
                label = stringResource(R.string.statistics_label_first_drink),
                value = formatTime(context, averageFirstDrinkTime, timeFormat)
            )
        )
        add(
            OverviewCategory(
                iconRes = R.drawable.bedtime_filled,
                label = stringResource(R.string.statistics_label_last_drink),
                value = formatTime(context, averageLastDrinkTime, timeFormat)
            )
        )
    }

    Column {
        categories.forEachIndexed { index, category ->
            OverviewCategoryCard(
                index = index,
                category = category,
                totalSize = categories.size,
            )
        }
    }
}

private data class OverviewCategory(
    val iconRes: Int,
    val label: String,
    val value: String
)

@Composable
private fun OverviewCategoryCard(
    index: Int,
    category: OverviewCategory,
    totalSize: Int
) {
    val shape = getShapeForIndex(
        index = index,
        size = totalSize,
        outerRadius = 24.dp,
        innerRadius = 10.dp
    )

    Surface(
        shape = shape,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(bottom = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(category.iconRes),
                contentDescription = null,
                modifier = Modifier,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Row(
                modifier = Modifier
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = category.value,
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
            }
        }
    }
}

private fun getShapeForIndex(
    index: Int,
    size: Int,
    outerRadius: Dp,
    innerRadius: Dp
): Shape {
    return when {
        size == 1 -> PillShape
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

private fun formatTime(context: Context, time: LocalTime?, timeFormat: TimeFormat): String {
    return time?.let { DateTimeFormatters.formatTime(context, it, timeFormat) }
        ?: "—"
}

private fun formatInterval(minutes: Double): String {
    if (minutes <= 0.0) return "—"
    val hours = (minutes / 60).toInt()
    val mins = (minutes % 60).toInt()
    return when {
        hours > 0 && mins > 0 -> "$hours h $mins min"
        hours > 0 -> "$hours h"
        else -> "$mins min"
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true, name = "Overview Card")
@Composable
private fun OverviewCardPreview() {
    HydroTrackerTheme {
        OverviewCard(
            currentStreak = 12,
            successRate = 88.0,
            daysTracked = 8888,
            averageFirstDrinkTime = LocalTime.of(8, 30),
            averageLastDrinkTime = LocalTime.of(21, 15),
            averageIntervalMinutes = 145.0,
            averageDrinkSize = 280.0,
            largestDrink = 750.0,
            timeFormat = TimeFormat.HOUR_24,
            volumeUnit = VolumeUnit.MILLILITRES
        )
    }
}
