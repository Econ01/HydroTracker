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

package com.cemcakmak.hydrotracker.presentation.statistics

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import java.time.LocalTime

/**
 * A single beverage slice shown in the breakdown doughnut chart.
 *
 * The [key] is the raw beverage identifier stored on WaterIntakeEntry.beverageType. The UI maps
 * this key to a localized display name and icon via the active beverage list.
 */
data class BeverageBreakdownItem(
    val key: String,
    val displayName: String,
    @DrawableRes val iconRes: Int,
    val color: Color,
    val count: Int,
    val effectiveAmount: Double,
    val percentage: Double
)

/**
 * Container usage summary for a single container type.
 */
data class ContainerUsageItem(
    val name: String,
    val count: Int,
    val volume: Double,
    @DrawableRes val iconRes: Int
)

/**
 * Imported Health Connect data summarized by source app.
 */
data class HealthConnectImportStats(
    val totalImportedCount: Int,
    val totalImportedVolume: Double,
    val sourceBreakdown: List<HealthConnectSourceStat>
)

/**
 * Volume imported from a single external Health Connect source.
 */
data class HealthConnectSourceStat(
    val sourceName: String,
    val count: Int,
    val volume: Double,
    val percentage: Double
)

/**
 * Complete UI state for the all-time Statistics screen.
 */
data class StatisticsUiState(
    val isLoading: Boolean = true,
    val hasData: Boolean = false,
    val isHealthConnectEnabled: Boolean = false,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val goalSuccessRate: Double = 0.0,
    val totalTrackedDays: Int = 0,
    val averageDailyIntake: Double = 0.0,
    val totalIntake: Double = 0.0,
    val dailyGoal: Double = 0.0,
    val averageDrinkSize: Double = 0.0,
    val largestDrink: Double = 0.0,
    val averageFirstDrinkTime: LocalTime? = null,
    val averageLastDrinkTime: LocalTime? = null,
    val averageIntervalBetweenDrinksMinutes: Double = 0.0,
    val beverageBreakdown: List<BeverageBreakdownItem> = emptyList(),
    val containerUsage: List<ContainerUsageItem> = emptyList(),
    val rawIntake: Double = 0.0,
    val effectiveIntake: Double = 0.0,
    val hydrationMultiplierEffect: Double = 0.0,
    val healthConnectStats: HealthConnectImportStats? = null
)
