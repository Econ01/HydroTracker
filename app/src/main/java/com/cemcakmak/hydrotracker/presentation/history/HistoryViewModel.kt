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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cemcakmak.hydrotracker.data.database.entities.DailySummary
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.models.WeekStartDay
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

/**
 * Statistics displayed below the weekly bar chart.
 */
data class WeeklyHistoryStats(
    val totalIntake: Double = 0.0,
    val averageIntake: Double = 0.0,
    val bestDayIntake: Double = 0.0
)

/**
 * Statistics displayed below the monthly calendar heatmap.
 */
data class MonthlyHistoryStats(
    val daysTracked: Int = 0,
    val goalsMet: Int = 0,
    val successRate: Double = 0.0
)

/**
 * Statistics displayed below the yearly heatmap.
 */
data class YearlyHistoryStats(
    val daysTracked: Int = 0,
    val goalsMet: Int = 0,
    val totalIntake: Double = 0.0
)

/**
 * Complete UI state for the History screen.
 *
 * The [summaries] list always contains only the data for the currently selected period range,
 * so chart sections receive a small, pre-filtered dataset.
 */
data class HistoryUiState(
    val selectedPeriod: TimePeriod = TimePeriod.WEEKLY,
    val weekOffset: Int = 0,
    val monthOffset: Int = 0,
    val yearOffset: Int = 0,
    val summaries: List<DailySummary> = emptyList(),
    val weeklyStats: WeeklyHistoryStats = WeeklyHistoryStats(),
    val monthlyStats: MonthlyHistoryStats = MonthlyHistoryStats(),
    val yearlyStats: YearlyHistoryStats = YearlyHistoryStats(),
    val selectedDate: String? = null,
    val selectedDateEntries: List<WaterIntakeEntry> = emptyList()
)

/**
 * ViewModel for the History screen.
 *
 * Owns period selection, navigation offsets, and date-range calculation. Queries only the
 * summaries that fall inside the active range, and pre-computes the statistics shown below
 * each chart.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    waterIntakeRepository: WaterIntakeRepository,
    userRepository: UserRepository
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow(TimePeriod.WEEKLY)
    private val weekOffset = MutableStateFlow(0)
    private val monthOffset = MutableStateFlow(0)
    private val yearOffset = MutableStateFlow(0)
    private val selectedDate = MutableStateFlow<String?>(null)

    /**
     * Emits the active date range whenever the period, an offset, or the user's preferred
     * week start day changes.
     */
    private val dateRange = combine(
        selectedPeriod,
        weekOffset,
        monthOffset,
        yearOffset,
        userRepository.themePreferences
    ) { period, currentWeekOffset, currentMonthOffset, currentYearOffset, themePreferences ->
        computeDateRange(
            period = period,
            weekOffset = currentWeekOffset,
            monthOffset = currentMonthOffset,
            yearOffset = currentYearOffset,
            weekStartDay = themePreferences.weekStartDay
        )
    }

    private val summariesForRange = dateRange
        .flatMapLatest { (startDate, endDate) ->
            waterIntakeRepository.getSummariesForRange(startDate, endDate)
        }

    private val selectedDateEntries = selectedDate
        .flatMapLatest { date ->
            if (date == null) flowOf(emptyList()) else waterIntakeRepository.getEntriesForDate(date)
        }

    /**
     * Complete UI state for the History screen. The list of summaries is always scoped to the
     * currently selected date range, and [selectedDateEntries] reflects the entries for the
     * day tapped by the user.
     */
    val uiState: StateFlow<HistoryUiState> = combine(
        summariesForRange,
        selectedDateEntries
    ) { summaries, entries ->
        HistoryUiState(
            selectedPeriod = selectedPeriod.value,
            weekOffset = weekOffset.value,
            monthOffset = monthOffset.value,
            yearOffset = yearOffset.value,
            summaries = summaries,
            weeklyStats = calculateWeeklyStats(summaries),
            monthlyStats = calculateMonthlyStats(summaries),
            yearlyStats = calculateYearlyStats(summaries),
            selectedDate = selectedDate.value,
            selectedDateEntries = entries
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HistoryUiState()
        )

    /** Switches the active period and resets all navigation offsets and the selected day. */
    fun selectPeriod(period: TimePeriod) {
        selectedPeriod.value = period
        weekOffset.value = 0
        monthOffset.value = 0
        yearOffset.value = 0
        selectedDate.value = null
    }

    /** Moves one step backward within the current period type and clears the selected day. */
    fun previousPeriod() {
        when (selectedPeriod.value) {
            TimePeriod.WEEKLY -> weekOffset.value--
            TimePeriod.MONTHLY -> monthOffset.value--
            TimePeriod.YEARLY -> yearOffset.value--
        }
        selectedDate.value = null
    }

    /** Moves one step forward within the current period type and clears the selected day. */
    fun nextPeriod() {
        when (selectedPeriod.value) {
            TimePeriod.WEEKLY -> weekOffset.value++
            TimePeriod.MONTHLY -> monthOffset.value++
            TimePeriod.YEARLY -> yearOffset.value++
        }
        selectedDate.value = null
    }

    /** Selects a calendar date and loads its entries underneath the chart. */
    fun selectDate(date: String) {
        selectedDate.value = date
    }

    private fun computeDateRange(
        period: TimePeriod,
        weekOffset: Int,
        monthOffset: Int,
        yearOffset: Int,
        weekStartDay: WeekStartDay
    ): Pair<String, String> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val (start, end) = when (period) {
            TimePeriod.WEEKLY -> getWeekDateRange(weekOffset, weekStartDay)
            TimePeriod.MONTHLY -> getMonthDateRange(monthOffset)
            TimePeriod.YEARLY -> getYearDateRange(yearOffset)
        }
        return formatter.format(start) to formatter.format(end)
    }
}

/**
 * Factory for creating [HistoryViewModel] with its repository dependencies.
 */
class HistoryViewModelFactory(
    private val waterIntakeRepository: WaterIntakeRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            return HistoryViewModel(waterIntakeRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

private fun calculateWeeklyStats(summaries: List<DailySummary>): WeeklyHistoryStats {
    val daysWithData = summaries.filter { it.totalIntake > 0.0 }
    val totalIntake = daysWithData.sumOf { it.totalIntake }
    val averageIntake = if (daysWithData.isNotEmpty()) totalIntake / daysWithData.size else 0.0
    val bestDayIntake = daysWithData.maxOfOrNull { it.totalIntake } ?: 0.0
    return WeeklyHistoryStats(totalIntake, averageIntake, bestDayIntake)
}

private fun calculateMonthlyStats(summaries: List<DailySummary>): MonthlyHistoryStats {
    val daysTracked = summaries.size
    val goalsMet = summaries.count { it.goalAchieved }
    val successRate = if (daysTracked > 0) (goalsMet.toDouble() / daysTracked) * 100.0 else 0.0
    return MonthlyHistoryStats(daysTracked, goalsMet, successRate)
}

private fun calculateYearlyStats(summaries: List<DailySummary>): YearlyHistoryStats {
    val daysTracked = summaries.size
    val goalsMet = summaries.count { it.goalAchieved }
    val totalIntake = summaries.sumOf { it.totalIntake }
    return YearlyHistoryStats(daysTracked, goalsMet, totalIntake)
}

internal fun getWeekDateRange(
    weekOffset: Int,
    weekStartDay: WeekStartDay = WeekStartDay.SYSTEM
): Pair<LocalDate, LocalDate> {
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
