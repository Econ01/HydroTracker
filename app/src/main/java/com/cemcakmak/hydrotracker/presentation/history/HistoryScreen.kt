// ===== FILE: HistoryScreen.kt =====
// Location: app/src/main/java/com/cemcakmak/hydrotracker/presentation/history/HistoryScreen.kt

package com.cemcakmak.hydrotracker.presentation.history

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.database.entities.DailySummary
import com.cemcakmak.hydrotracker.data.models.WeekStartDay
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.utils.WaterCalculator
import java.text.SimpleDateFormat
import java.util.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryScreen(
    waterIntakeRepository: WaterIntakeRepository,
    themePreferences: ThemePreferences = ThemePreferences(),
    onNavigateBack: () -> Unit = {}
) {
    // State for different time periods
    var selectedPeriod by remember { mutableStateOf(TimePeriod.WEEKLY) }
    
    // Navigation state for current week/month/year
    var currentWeekOffset by remember { mutableIntStateOf(0) } // 0 = current week, -1 = previous week, etc.
    var currentMonthOffset by remember { mutableIntStateOf(0) } // 0 = current month, -1 = previous month, etc.
    var currentYearOffset by remember { mutableIntStateOf(0) } // 0 = current year, -1 = previous year, etc.

    // Collect ALL historical data from repository (not just last 30 days)
    val allSummaries by waterIntakeRepository.getAllSummaries().collectAsState(
        initial = emptyList()
    )



    // Animation states
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "History & Statistics",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
        ) {
            // Period Selector
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { -it / 3 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(animationSpec = tween(500))
                ) {
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
                        weekStartDay = themePreferences.weekStartDay
                    )
                }
            }

            // Main Chart Section
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(animationSpec = tween(700, delayMillis = 200))
                ) {
                    when (selectedPeriod) {
                        TimePeriod.WEEKLY -> {
                            WeeklyChartSection(
                                selectedPeriod = selectedPeriod,
                                weekOffset = currentWeekOffset,
                                summaries = allSummaries,
                                weekStartDay = themePreferences.weekStartDay
                            )
                        }
                        TimePeriod.MONTHLY -> {
                            MonthlyChartSection(
                                summaries = allSummaries,
                                selectedPeriod = selectedPeriod,
                                monthOffset = currentMonthOffset,
                                weekStartDay = themePreferences.weekStartDay
                            )
                        }
                        TimePeriod.YEARLY -> {
                            YearlyChartSection(
                                summaries = allSummaries,
                                selectedPeriod = selectedPeriod,
                                yearOffset = currentYearOffset
                            )
                        }
                    }
                }
            }

            // Statistics Overview
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(animationSpec = tween(600, delayMillis = 300))
                ) {
                    StatisticsGrid(
                        summaries = allSummaries
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Goal Achievement
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(animationSpec = tween(600, delayMillis = 400))
                ) {
                    GoalAchievementSection(
                        summaries = allSummaries,
                        selectedPeriod = selectedPeriod,
                        weekOffset = currentWeekOffset,
                        monthOffset = currentMonthOffset,
                        yearOffset = currentYearOffset,
                        weekStartDay = themePreferences.weekStartDay
                    )
                }
            }

            // Bottom spacer for navigation bar
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

enum class TimePeriod(val displayName: String, val description: String) {
    WEEKLY("Weekly", "Week view"),
    MONTHLY("Monthly", "Month view"),
    YEARLY("Yearly", "Year view")
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
    weekStartDay: WeekStartDay
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Period Type Selection with ToggleButton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimePeriod.entries.forEach { period ->
                    val isSelected = selectedPeriod == period

                    val haptics = LocalHapticFeedback.current
                    
                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = { onPeriodSelected(period)
                            haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)},
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = period.displayName,
                                style = MaterialTheme.typography.labelLargeEmphasized
                            )
                        }
                    }
                }
            }

            // Haptic feedback for button clicks
            val haptics = LocalHapticFeedback.current
            
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
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    },
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledIconButtonColors()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous ${selectedPeriod.displayName.lowercase()}"
                    )
                }
                
                Text(
                    text = getCurrentPeriodText(selectedPeriod, currentWeekOffset, currentMonthOffset, currentYearOffset, weekStartDay),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                IconButton(
                    onClick = {
                        when (selectedPeriod) {
                            TimePeriod.WEEKLY -> onWeekOffsetChanged(currentWeekOffset + 1)
                            TimePeriod.MONTHLY -> onMonthOffsetChanged(currentMonthOffset + 1)
                            TimePeriod.YEARLY -> onYearOffsetChanged(currentYearOffset + 1)
                        }
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
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
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next ${selectedPeriod.displayName.lowercase()}"
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyChartSection(
    selectedPeriod: TimePeriod,
    weekOffset: Int,
    summaries: List<DailySummary> = emptyList(),
    weekStartDay: WeekStartDay = WeekStartDay.MONDAY
) {
    var selectedDayData by remember { mutableStateOf<com.cemcakmak.hydrotracker.data.database.dao.DailyTotal?>(null) }
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
                text = getCurrentPeriodText(selectedPeriod, weekOffset, 0, 0, weekStartDay),
                style = MaterialTheme.typography.titleLargeEmphasized
            )

            // Filter summaries for the selected week and convert to DailyTotal format
            val filteredSummaries = filterSummariesByPeriod(summaries, selectedPeriod, weekOffset, 0, 0, weekStartDay)
            
            // Create a complete week with all 7 days, filling in missing days with 0 data
            val (startOfWeek) = getWeekDateRange(weekOffset, weekStartDay)
            val filteredDailyTotals = mutableListOf<com.cemcakmak.hydrotracker.data.database.dao.DailyTotal>()
            
            for (i in 0..6) {
                val currentDate = startOfWeek.plusDays(i.toLong())
                val dateString = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val summary = filteredSummaries.find { it.date == dateString }
                
                filteredDailyTotals.add(
                    com.cemcakmak.hydrotracker.data.database.dao.DailyTotal(
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
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)}
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
                            onDismiss = { selectedDayData = null }
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
                    
                    WeeklyStatItem(
                        label = "Total",
                        value = WaterCalculator.formatWaterAmount(totalAmount)
                    )
                    WeeklyStatItem(
                        label = "Average",
                        value = WaterCalculator.formatWaterAmount(avgAmount)
                    )
                    WeeklyStatItem(
                        label = "Best Day",
                        value = WaterCalculator.formatWaterAmount(bestAmount)
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
                        text = "No data available for this week",
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
    dailyTotals: List<com.cemcakmak.hydrotracker.data.database.dao.DailyTotal>,
    onBarClick: (com.cemcakmak.hydrotracker.data.database.dao.DailyTotal) -> Unit
) {
    if (dailyTotals.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No data available",
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
                                text = WaterCalculator.formatWaterAmount(dayTotal.totalAmount),
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

@Composable
private fun WeeklyStatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun YearlyChartSection(
    summaries: List<DailySummary>,
    selectedPeriod: TimePeriod,
    yearOffset: Int
) {
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
                text = getPeriodTitle(selectedPeriod),
                style = MaterialTheme.typography.titleLargeEmphasized
            )

            val filteredSummaries = filterSummariesByPeriod(summaries, selectedPeriod, weekOffset = 0, monthOffset = 0, yearOffset = yearOffset)
            
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
                    WeeklyStatItem(
                        label = "Days Tracked",
                        value = "$totalDays"
                    )
                    WeeklyStatItem(
                        label = "Goals Met",
                        value = "$goalAchievedDays"
                    )
                    WeeklyStatItem(
                        label = "Total Intake",
                        value = "${(totalIntake / 1000).toInt()}L"
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
                        text = "No data available for this year",
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
                text = "Less",
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
                text = "More",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun MonthlyChartSection(
    summaries: List<DailySummary>,
    selectedPeriod: TimePeriod,
    monthOffset: Int,
    weekStartDay: WeekStartDay = WeekStartDay.MONDAY
) {
    var selectedSummary by remember { mutableStateOf<DailySummary?>(null) }
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
                text = getPeriodTitle(selectedPeriod),
                style = MaterialTheme.typography.titleLargeEmphasized
            )

            val filteredSummaries = filterSummariesByPeriod(summaries, selectedPeriod, weekOffset = 0, monthOffset, 0)
            
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
                            onDismiss = { selectedSummary = null }
                        )
                    }
                }

                // Monthly stats
                val totalDays = filteredSummaries.size
                val goalAchievedDays = filteredSummaries.count { it.goalAchieved }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    WeeklyStatItem(
                        label = "Days Tracked",
                        value = "$totalDays"
                    )
                    WeeklyStatItem(
                        label = "Goals Met",
                        value = "$goalAchievedDays"
                    )
                    WeeklyStatItem(
                        label = "Success Rate",
                        value = "${((goalAchievedDays.toFloat() / totalDays) * 100).toInt()}%"
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
                        text = "No data available for the last 30 days",
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
private fun MonthlyHeatmap(
    summaries: List<DailySummary>,
    onCellClick: (DailySummary) -> Unit,
    weekStartDay: WeekStartDay = WeekStartDay.MONDAY
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
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MonthlyCalendarGrid(
            monthYear = monthYear,
            summaryMap = summaryMap,
            onCellClick = onCellClick,
            weekStartDay = weekStartDay
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Less",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf(0.1f, 0.25f, 0.4f, 0.6f, 0.8f, 1.0f).forEach { alpha ->
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                    )
                }
            }
            Text(
                text = "More",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthlyCalendarGrid(
    monthYear: LocalDate,
    summaryMap: Map<String, DailySummary>,
    onCellClick: (DailySummary) -> Unit,
    weekStartDay: WeekStartDay = WeekStartDay.MONDAY
) {
    // Use the user's preferred week start day
    val weekFields = WeekFields.of(weekStartDay.dayOfWeek, 1)
    
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

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Day headers based on week start day
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val dayHeaders = if (weekStartDay == WeekStartDay.SUNDAY) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            } else {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            }
            
            dayHeaders.forEach { dayName ->
                Text(
                    text = dayName,
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

@Composable
private fun StatisticsGrid(
    summaries: List<DailySummary>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Statistics",
            style = MaterialTheme.typography.titleLargeEmphasized
        )

        // Calculate statistics from ALL data (not filtered)
        val currentStreak = calculateStreak(summaries.sortedByDescending { it.date })
        val totalIntake = summaries.sumOf { it.totalIntake }
        val totalDays = summaries.size
        
        // Calculate daily average from all data
        val dailyAverage = if (totalDays > 0) totalIntake / totalDays else 0.0
        
        
        // Grid of stat cards - consistent across all periods
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocalFireDepartment,
                    title = "Current Streak",
                    value = currentStreak.toString(),
                    subtitle = "days",
                    color = MaterialTheme.colorScheme.error,
                )

                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Stars,
                    title = "Daily Average",
                    value = WaterCalculator.formatWaterAmount(dailyAverage).split(" ")[0],
                    subtitle = WaterCalculator.formatWaterAmount(dailyAverage).split(" ")[1],
                    color = MaterialTheme.colorScheme.secondary
                )

                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.WaterDrop,
                    title = "Total Intake",
                    value = "${(totalIntake / 1000).toInt()}",
                    subtitle = "liters",
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    color: Color,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    color = color
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun GoalAchievementSection(
    summaries: List<DailySummary>,
    selectedPeriod: TimePeriod,
    weekOffset: Int,
    monthOffset: Int,
    yearOffset: Int,
    weekStartDay: WeekStartDay = WeekStartDay.MONDAY
) {
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
                text = "Goal Achievement",
                style = MaterialTheme.typography.titleLargeEmphasized,
            )

            val filteredSummaries = filterSummariesByPeriod(summaries, selectedPeriod, weekOffset, monthOffset, yearOffset, weekStartDay)
            
            if (filteredSummaries.isNotEmpty()) {
                val achievementRate = filteredSummaries.count { it.goalAchieved }.toFloat() / filteredSummaries.size

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Large achievement percentage
                    Text(
                        text = "${(achievementRate * 100).toInt()}%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "of days you met your goal",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Achievement progress bar
                    LinearWavyProgressIndicator(
                        progress = { achievementRate },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        stroke = WavyProgressIndicatorDefaults.linearIndicatorStroke,
                        trackStroke = WavyProgressIndicatorDefaults.linearTrackStroke,
                        amplitude = WavyProgressIndicatorDefaults.indicatorAmplitude,
                        wavelength = WavyProgressIndicatorDefaults.LinearDeterminateWavelength,
                        waveSpeed = WavyProgressIndicatorDefaults.LinearDeterminateWavelength
                    )
                }
            } else {
                Text(
                    text = "Start tracking your water intake to see your goal achievement rate!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

// Helper functions
private fun calculateStreak(summaries: List<DailySummary>): Int {
    var streak = 0
    val sortedSummaries = summaries.sortedByDescending { it.date }

    for (summary in sortedSummaries) {
        if (summary.goalAchieved) {
            streak++
        } else {
            break
        }
    }
    return streak
}




data class ChartDetailData(
    val date: String,
    val amount: Double,
    val goal: Double?,
    val goalPercentage: Float?
)


@Composable
private fun InlineDetailPanel(
    data: ChartDetailData,
    onDismiss: () -> Unit,
) {
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
                        text = formatDisplayDate(data.date),
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
                            contentDescription = "Close",
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
                            text = "Water Intake",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = WaterCalculator.formatWaterAmount(data.amount),
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
                                        text = "Goal: ${WaterCalculator.formatWaterAmount(goal)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Progress: ${(percentage * 100).toInt()}%",
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

private fun formatDisplayDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (_: Exception) {
        dateString
    }
}



private fun getCurrentPeriodText(
    period: TimePeriod, 
    weekOffset: Int, 
    monthOffset: Int, 
    yearOffset: Int,
    weekStartDay: WeekStartDay = WeekStartDay.MONDAY
): String {
    return when (period) {
        TimePeriod.WEEKLY -> {
            val (startOfWeek, endOfWeek) = getWeekDateRange(weekOffset, weekStartDay)
            
            when (weekOffset) {
                0 -> "This Week"
                -1 -> "Last Week"
                else -> "${startOfWeek.format(DateTimeFormatter.ofPattern("MMM d"))} - ${endOfWeek.format(DateTimeFormatter.ofPattern("MMM d"))}"
            }
        }
        TimePeriod.MONTHLY -> {
            val today = LocalDate.now()
            val targetMonth = today.plusMonths(monthOffset.toLong())
            when (monthOffset) {
                0 -> "This Month"
                -1 -> "Last Month"
                else -> targetMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            }
        }
        TimePeriod.YEARLY -> {
            val today = LocalDate.now()
            val targetYear = today.plusYears(yearOffset.toLong())
            when (yearOffset) {
                0 -> "This Year"
                -1 -> "Last Year"
                else -> targetYear.format(DateTimeFormatter.ofPattern("yyyy"))
            }
        }
    }
}

private fun getPeriodTitle(period: TimePeriod): String {
    return when (period) {
        TimePeriod.WEEKLY -> "Weekly Overview"
        TimePeriod.MONTHLY -> "Monthly Overview"
        TimePeriod.YEARLY -> "Yearly Overview"
    }
}


private fun getWeekDateRange(weekOffset: Int, weekStartDay: WeekStartDay = WeekStartDay.MONDAY): Pair<LocalDate, LocalDate> {
    val today = LocalDate.now()
    val weekFields = WeekFields.of(weekStartDay.dayOfWeek, 1)
    
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

private fun filterSummariesByPeriod(
    summaries: List<DailySummary>,
    period: TimePeriod,
    weekOffset: Int,
    monthOffset: Int,
    yearOffset: Int = 0,
    weekStartDay: WeekStartDay = WeekStartDay.MONDAY
): List<DailySummary> {
    val (startDate, endDate) = when (period) {
        TimePeriod.WEEKLY -> getWeekDateRange(weekOffset, weekStartDay)
        TimePeriod.MONTHLY -> getMonthDateRange(monthOffset)
        TimePeriod.YEARLY -> getYearDateRange(yearOffset)
    }
    
    return summaries.filter { summary ->
        val summaryDate = LocalDate.parse(summary.date)
        summaryDate >= startDate && summaryDate <= endDate
    }
}

