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

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cemcakmak.hydrotracker.data.database.entities.DailySummary
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.data.database.repository.ContainerPresetRepository
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import com.cemcakmak.hydrotracker.utils.ContainerIconMapper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * ViewModel for the all-time Statistics screen.
 *
 * Combines the full list of daily summaries (for streaks and goal achievement) with every
 * non-hidden water intake entry (for drinking patterns, beverages, containers, and Health
 * Connect imports) and emits a single immutable [StatisticsUiState].
 */
class StatisticsViewModel(
    waterIntakeRepository: WaterIntakeRepository,
    userRepository: UserRepository,
    containerPresetRepository: ContainerPresetRepository
) : ViewModel() {

    val uiState: StateFlow<StatisticsUiState> = combine(
        waterIntakeRepository.getAllSummaries(),
        waterIntakeRepository.getAllEntries(),
        userRepository.userProfile,
        containerPresetRepository.getAllPresets()
    ) { summaries, entries, profile, presets ->
        computeStatistics(summaries, entries, presets, profile?.healthConnectSyncEnabled == true, profile?.dailyWaterGoal ?: 0.0)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StatisticsUiState()
        )

    private fun computeStatistics(
        summaries: List<DailySummary>,
        entries: List<WaterIntakeEntry>,
        presets: List<ContainerPreset>,
        isHealthConnectEnabled: Boolean,
        dailyGoal: Double
    ): StatisticsUiState {
        val visibleEntries = entries.filter { !it.isHidden }

        if (summaries.isEmpty() && visibleEntries.isEmpty()) {
            return StatisticsUiState(
                isLoading = false,
                hasData = false,
                isHealthConnectEnabled = isHealthConnectEnabled,
                dailyGoal = dailyGoal
            )
        }

        val sortedSummaries = summaries.sortedBy { it.date }

        val (currentStreak, longestStreak) = calculateStreaks(sortedSummaries)
        val goalSuccessRate = if (sortedSummaries.isNotEmpty()) {
            (sortedSummaries.count { it.goalAchieved }.toDouble() / sortedSummaries.size) * 100.0
        } else 0.0

        val totalIntake = sortedSummaries.sumOf { it.totalIntake }
        val averageDailyIntake = if (sortedSummaries.isNotEmpty()) {
            totalIntake / sortedSummaries.size
        } else 0.0

        val rawIntake = visibleEntries.sumOf { it.amount }
        val effectiveIntake = visibleEntries.sumOf { it.getEffectiveHydrationAmount() }
        val hydrationMultiplierEffect = if (rawIntake > 0.0) {
            ((effectiveIntake - rawIntake) / rawIntake) * 100.0
        } else 0.0

        val averageDrinkSize = if (visibleEntries.isNotEmpty()) {
            visibleEntries.sumOf { it.amount } / visibleEntries.size
        } else 0.0
        val largestDrink = visibleEntries.maxOfOrNull { it.amount } ?: 0.0

        val (averageFirstTime, averageLastTime) = calculateAverageFirstLastTimes(visibleEntries)
        val averageInterval = calculateAverageIntervalBetweenDrinks(visibleEntries)

        return StatisticsUiState(
            isLoading = false,
            hasData = true,
            isHealthConnectEnabled = isHealthConnectEnabled,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            goalSuccessRate = goalSuccessRate,
            totalTrackedDays = sortedSummaries.size,
            averageDailyIntake = averageDailyIntake,
            totalIntake = totalIntake,
            dailyGoal = dailyGoal,
            averageDrinkSize = averageDrinkSize,
            largestDrink = largestDrink,
            averageFirstDrinkTime = averageFirstTime,
            averageLastDrinkTime = averageLastTime,
            averageIntervalBetweenDrinksMinutes = averageInterval,
            beverageBreakdown = buildBeverageBreakdown(visibleEntries),
            containerUsage = buildContainerUsage(visibleEntries, presets),
            rawIntake = rawIntake,
            effectiveIntake = effectiveIntake,
            hydrationMultiplierEffect = hydrationMultiplierEffect,
            healthConnectStats = if (isHealthConnectEnabled) buildHealthConnectStats(visibleEntries) else null
        )
    }

    private fun calculateStreaks(summaries: List<DailySummary>): Pair<Int, Int> {
        var longest = 0
        var current = 0
        var running = 0

        summaries.forEach { summary ->
            if (summary.goalAchieved) {
                running++
                if (running > longest) longest = running
            } else {
                running = 0
            }
        }

        // Current streak is the trailing run of achieved days, ending with today if today is achieved.
        val reversed = summaries.asReversed()
        for (summary in reversed) {
            if (summary.goalAchieved) {
                current++
            } else {
                break
            }
        }

        return current to longest
    }

    private fun calculateAverageFirstLastTimes(entries: List<WaterIntakeEntry>): Pair<LocalTime?, LocalTime?> {
        if (entries.isEmpty()) return null to null

        val zone = ZoneId.systemDefault()
        val entriesByDate = entries.groupBy { it.date }

        val firstTimes = entriesByDate.values.mapNotNull { dayEntries ->
            dayEntries.minByOrNull { it.timestamp }?.timestamp?.let { millis ->
                LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone).toLocalTime()
            }
        }

        val lastTimes = entriesByDate.values.mapNotNull { dayEntries ->
            dayEntries.maxByOrNull { it.timestamp }?.timestamp?.let { millis ->
                LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone).toLocalTime()
            }
        }

        return averageLocalTimes(firstTimes) to averageLocalTimes(lastTimes)
    }

    private fun averageLocalTimes(times: List<LocalTime>): LocalTime? {
        if (times.isEmpty()) return null

        // Average minutes from midnight, which is robust across midnight boundaries.
        var totalMinutes = 0L
        times.forEach { totalMinutes += it.hour * 60L + it.minute }
        val averageMinutes = (totalMinutes / times.size).toInt()
        return LocalTime.of(averageMinutes / 60, averageMinutes % 60)
    }

    private fun calculateAverageIntervalBetweenDrinks(entries: List<WaterIntakeEntry>): Double {
        if (entries.size < 2) return 0.0

        val entriesByDate = entries.groupBy { it.date }
        var intervalSum = 0L
        var intervalCount = 0

        entriesByDate.values.forEach { dayEntries ->
            val sorted = dayEntries.sortedBy { it.timestamp }
            for (i in 1 until sorted.size) {
                intervalSum += sorted[i].timestamp - sorted[i - 1].timestamp
                intervalCount++
            }
        }

        return if (intervalCount > 0) (intervalSum / intervalCount / 60_000.0) else 0.0
    }

    private fun buildBeverageBreakdown(entries: List<WaterIntakeEntry>): List<BeverageBreakdownItem> {
        if (entries.isEmpty()) return emptyList()

        val totalEffective = entries.sumOf { it.getEffectiveHydrationAmount() }
        if (totalEffective <= 0.0) return emptyList()

        val grouped = entries.groupBy { it.beverageType }
        val palette = listOf(
            Color(0xFF0077BE),
            Color(0xFF4CAF50),
            Color(0xFFFF9800),
            Color(0xFF9C27B0),
            Color(0xFFE91E63),
            Color(0xFF00BCD4),
            Color(0xFFFFEB3B),
            Color(0xFF795548),
            Color(0xFF607D8B)
        )

        return grouped.map { (key, group) ->
            val effectiveAmount = group.sumOf { it.getEffectiveHydrationAmount() }
            val percentage = (effectiveAmount / totalEffective) * 100.0
            val beverageType = BeverageType.fromStringOrDefault(key)
            val displayName = beverageType.displayName
            val iconRes = beverageType.iconResFilled

            BeverageBreakdownItem(
                key = key,
                displayName = displayName,
                iconRes = iconRes,
                color = palette[grouped.keys.indexOf(key) % palette.size],
                count = group.size,
                effectiveAmount = effectiveAmount,
                percentage = percentage
            )
        }
            .sortedByDescending { it.effectiveAmount }
    }

    private fun buildContainerUsage(
        entries: List<WaterIntakeEntry>,
        presets: List<ContainerPreset>
    ): List<ContainerUsageItem> {
        if (entries.isEmpty()) return emptyList()

        val presetByName = presets.associateBy { it.name }

        return entries
            .groupBy { it.containerType }
            .map { (name, group) ->
                val iconRes = presetByName[name]?.iconRes
                    ?: ContainerIconMapper.getIconForVolume(group.sumOf { it.amount }).checkedRes
                ContainerUsageItem(
                    name = name,
                    count = group.size,
                    volume = group.sumOf { it.amount },
                    iconRes = iconRes
                )
            }
            .sortedByDescending { it.volume }
    }

    private fun buildHealthConnectStats(entries: List<WaterIntakeEntry>): HealthConnectImportStats? {
        val externalEntries = entries.filter { it.isExternalEntry() }
        if (externalEntries.isEmpty()) return null

        val totalVolume = externalEntries.sumOf { it.getEffectiveHydrationAmount() }
        val sources = externalEntries.groupBy { extractHealthConnectSource(it.note) }

        val sourceStats = sources.map { (source, group) ->
            val volume = group.sumOf { it.getEffectiveHydrationAmount() }
            HealthConnectSourceStat(
                sourceName = source,
                count = group.size,
                volume = volume,
                percentage = if (totalVolume > 0.0) (volume / totalVolume) * 100.0 else 0.0
            )
        }.sortedByDescending { it.volume }

        return HealthConnectImportStats(
            totalImportedCount = externalEntries.size,
            totalImportedVolume = totalVolume,
            sourceBreakdown = sourceStats
        )
    }

    private fun extractHealthConnectSource(note: String?): String {
        val prefix = "Imported from "
        return if (note?.startsWith(prefix) == true) {
            note.removePrefix(prefix).trim()
        } else {
            "Health Connect"
        }
    }
}

/**
 * Factory for creating [StatisticsViewModel] with its repository dependencies.
 */
class StatisticsViewModelFactory(
    private val waterIntakeRepository: WaterIntakeRepository,
    private val userRepository: UserRepository,
    private val containerPresetRepository: ContainerPresetRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
            return StatisticsViewModel(waterIntakeRepository, userRepository, containerPresetRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

