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

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.rememberNavBackStack
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.ActivityLevel
import com.cemcakmak.hydrotracker.data.models.AgeGroup
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.models.EdgeEffect
import com.cemcakmak.hydrotracker.data.models.Gender
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.TimeFormat
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.presentation.common.BeverageOption
import com.cemcakmak.hydrotracker.presentation.common.MainNavigationScaffold
import com.cemcakmak.hydrotracker.presentation.common.MainTabTopAppBar
import com.cemcakmak.hydrotracker.presentation.common.NavigationRoutes
import com.cemcakmak.hydrotracker.presentation.common.effect.BackdropBlurState
import com.cemcakmak.hydrotracker.presentation.common.effect.BackdropBlurStyle
import com.cemcakmak.hydrotracker.presentation.common.effect.BackdropProgressive
import com.cemcakmak.hydrotracker.presentation.common.effect.backdropBlur
import com.cemcakmak.hydrotracker.presentation.common.effect.backdropSource
import com.cemcakmak.hydrotracker.presentation.common.effect.rememberBackdropBlurState
import com.cemcakmak.hydrotracker.presentation.common.shapes.SquircleShape
import com.cemcakmak.hydrotracker.presentation.common.toOption
import com.cemcakmak.hydrotracker.presentation.statistics.components.BeverageDonutChart
import com.cemcakmak.hydrotracker.presentation.statistics.components.ContainerUsageCard
import com.cemcakmak.hydrotracker.presentation.statistics.components.OverviewCard
import com.cemcakmak.hydrotracker.presentation.statistics.components.HeroStatItem
import com.cemcakmak.hydrotracker.presentation.common.EntryAnimationDefaults
import com.cemcakmak.hydrotracker.presentation.statistics.components.TotalIntakePill
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.utils.NumberFormatters
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatisticsScreen(
    uiState: StatisticsUiState,
    themePreferences: ThemePreferences = ThemePreferences(),
    userProfile: UserProfile? = null,
    activeBeverages: List<BeverageOption> = BeverageType.getAllSorted().map { it.toOption() },
    paddingValues: PaddingValues
) {
    val volumeUnit = userProfile?.volumeUnit ?: VolumeUnit.MILLILITRES
    val timeFormat = themePreferences.timeFormat

    val edgeEffectStyle = themePreferences.edgeEffect.let {
        if (it == EdgeEffect.BLURRED && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            EdgeEffect.TRANSPARENT
        } else {
            it
        }
    }

    val backdropState = rememberBackdropBlurState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MainTabTopAppBar(
                titleResId = R.string.nav_statistics,
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (edgeEffectStyle == EdgeEffect.BLURRED) {
                            Modifier
                                .backdropSource(backdropState)
                                .background(MaterialTheme.colorScheme.surface)
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(innerPadding.calculateTopPadding()))

                when {
                    uiState.isLoading -> StatisticsLoadingState()
                    !uiState.hasData -> StatisticsEmptyState()
                    else -> StatisticsContent(
                        uiState = uiState,
                        volumeUnit = volumeUnit,
                        timeFormat = timeFormat,
                        activeBeverages = activeBeverages
                    )
                }

                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
            }

            TopEdgeEffect(
                style = edgeEffectStyle,
                backdropState = backdropState,
                paddingValues = innerPadding,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatisticsContent(
    uiState: StatisticsUiState,
    volumeUnit: VolumeUnit,
    timeFormat: TimeFormat,
    activeBeverages: List<BeverageOption>
) {
    val context = LocalContext.current
    val resolvedBeverages = resolveBeverageDisplayNames(uiState.beverageBreakdown, activeBeverages)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TotalIntakePill(
            label = stringResource(R.string.statistics_label_total_intake),
            totalIntake = uiState.totalIntake,
            volumeUnit = volumeUnit,
            entryDelayMillis = EntryAnimationDefaults.DELAY_MS
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val averageDailyIntakeDisplayUnit = VolumeUnitConverter.selectDisplayUnit(uiState.averageDailyIntake, volumeUnit)
            HeroStatItem(
                label = stringResource(R.string.statistics_label_average_daily_intake),
                value = uiState.averageDailyIntake,
                shape = SquircleShape(
                    topStart = CornerSize(20.dp),
                    topEnd = CornerSize(10.dp),
                    bottomStart = CornerSize(40.dp),
                    bottomEnd = CornerSize(10.dp)
                ),
                hapticsEnabled = false,
                tooltipText = VolumeUnitConverter.format(context, uiState.averageDailyIntake, volumeUnit),
                formatValue = { VolumeUnitConverter.formatValue(it.toDouble(), averageDailyIntakeDisplayUnit) },
                suffix = stringResource(averageDailyIntakeDisplayUnit.shortLabelResId),
                entryDelayMillis = EntryAnimationDefaults.DELAY_MS
            )

            HeroStatItem(
                label = stringResource(R.string.statistics_hero_success_rate),
                value = uiState.goalSuccessRate,
                shape = SquircleShape(
                    topStart = CornerSize(10.dp),
                    topEnd = CornerSize(10.dp),
                    bottomStart = CornerSize(10.dp),
                    bottomEnd = CornerSize(10.dp)
                ),
                hapticsEnabled = false,
                tooltipText = stringResource(R.string.percent_format, uiState.goalSuccessRate.toInt()),
                formatValue = { it.toInt().toString() },
                suffix = "%",
                entryDelayMillis = EntryAnimationDefaults.DELAY_MS
            )

            HeroStatItem(
                label = stringResource(R.string.statistics_hero_days_tracked),
                value = uiState.totalTrackedDays.toDouble(),
                shape = SquircleShape(
                    topStart = CornerSize(10.dp),
                    topEnd = CornerSize(20.dp),
                    bottomStart = CornerSize(10.dp),
                    bottomEnd = CornerSize(40.dp)
                ),
                hapticsEnabled = false,
                tooltipText = uiState.totalTrackedDays.toString(),
                formatValue = { NumberFormatters.formatCompactCount(it.toDouble()) },
                entryDelayMillis = EntryAnimationDefaults.DELAY_MS
            )
        }
    }

    SectionHeader(title = stringResource(R.string.statistics_section_overview))
    OverviewCard(
        currentStreak = uiState.currentStreak,
        successRate = uiState.goalSuccessRate,
        daysTracked = uiState.totalTrackedDays,
        averageFirstDrinkTime = uiState.averageFirstDrinkTime,
        averageLastDrinkTime = uiState.averageLastDrinkTime,
        averageIntervalMinutes = uiState.averageIntervalBetweenDrinksMinutes,
        averageDrinkSize = uiState.averageDrinkSize,
        largestDrink = uiState.largestDrink,
        timeFormat = timeFormat,
        volumeUnit = volumeUnit
    )

    SectionHeader(title = stringResource(R.string.statistics_section_beverages))
    BeverageDonutChart(
        items = resolvedBeverages,
        volumeUnit = volumeUnit,
        entryDelayMillis = EntryAnimationDefaults.DELAY_MS
    )

    SectionHeader(title = stringResource(R.string.statistics_section_containers))
    ContainerUsageCard(
        items = uiState.containerUsage,
        volumeUnit = volumeUnit
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatisticsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.analytics_filled),
            contentDescription = null,
            modifier = Modifier,
            tint = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = stringResource(R.string.statistics_empty_title),
            style = MaterialTheme.typography.titleLargeEmphasized,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.statistics_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun StatisticsLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ContainedLoadingIndicator()
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun TopEdgeEffect(
    style: EdgeEffect,
    backdropState: BackdropBlurState,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    val bandHeight = paddingValues.calculateTopPadding()

    when (style) {
        EdgeEffect.TRANSPARENT -> Unit
        EdgeEffect.SCRIM -> {
            val scrimColor = MaterialTheme.colorScheme.surface
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(bandHeight + 20.dp)
                    .drawBehind {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(scrimColor, Color.Transparent)
                            )
                        )
                    }
            )
        }
        EdgeEffect.BLURRED -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(bandHeight)
                        .backdropBlur(
                            state = backdropState,
                            style = BackdropBlurStyle(
                                blurRadius = 20.dp,
                                progressive = BackdropProgressive(
                                    startFraction = 0f,
                                    endFraction = 1f
                                ),
                                tint = MaterialTheme.colorScheme.surface.copy(0.4f)
                            )
                        )
                )
            }
        }
    }
}

private fun resolveBeverageDisplayNames(
    items: List<BeverageBreakdownItem>,
    activeBeverages: List<BeverageOption>
): List<BeverageBreakdownItem> {
    val optionByKey = activeBeverages.associateBy { it.storageKey }
    return items.map { item ->
        val option = optionByKey[item.key]
        if (option != null) {
            item.copy(
                displayName = option.displayName,
                iconRes = option.iconResFilled
            )
        } else {
            item
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true, name = "Statistics Screen")
@Composable
private fun StatisticsScreenPreview() {
    val previewUser = UserProfile(
        name = "Preview User",
        gender = Gender.MALE,
        ageGroup = AgeGroup.ADULT_31_50,
        activityLevel = ActivityLevel.MODERATE,
        wakeUpTime = "07:00",
        sleepTime = "23:00",
        dailyWaterGoal = 2500.0,
        reminderInterval = 120,
        volumeUnit = VolumeUnit.MILLILITRES
    )

    val uiState = StatisticsUiState(
        isLoading = false,
        hasData = true,
        isHealthConnectEnabled = true,
        currentStreak = 888,
        longestStreak = 12,
        goalSuccessRate = 88.0,
        totalTrackedDays = 888,
        averageDailyIntake = 2100.0,
        totalIntake = 71400000.0,
        dailyGoal = 2500.0,
        averageDrinkSize = 280.0,
        largestDrink = 750.0,
        averageIntervalBetweenDrinksMinutes = 145.0,
        beverageBreakdown = listOf(
            BeverageBreakdownItem(
                key = BeverageType.WATER.name,
                displayName = "Water",
                iconRes = BeverageType.WATER.iconResFilled,
                color = Color(0xFF0077BE),
                count = 120,
                effectiveAmount = 42000.0,
                percentage = 58.8
            ),
            BeverageBreakdownItem(
                key = BeverageType.COFFEE.name,
                displayName = "Coffee",
                iconRes = BeverageType.COFFEE.iconResFilled,
                color = Color(0xFF4CAF50),
                count = 45,
                effectiveAmount = 9500.0,
                percentage = 13.3
            ),
            BeverageBreakdownItem(
                key = BeverageType.TEA.name,
                displayName = "Tea",
                iconRes = BeverageType.TEA.iconResFilled,
                color = Color(0xFFFF9800),
                count = 30,
                effectiveAmount = 7200.0,
                percentage = 10.1
            )
        ),
        containerUsage = listOf(
            ContainerUsageItem("Water Bottle", 80, 40000.0, R.drawable.water_bottle_filled),
            ContainerUsageItem("Coffee Mug", 45, 9000.0, R.drawable.coffee_filled),
            ContainerUsageItem("Tea Cup", 30, 7200.0, R.drawable.tea_filled)
        ),
        rawIntake = 68000.0,
        effectiveIntake = 71400.0,
        hydrationMultiplierEffect = 5.0,
        healthConnectStats = HealthConnectImportStats(
            totalImportedCount = 12,
            totalImportedVolume = 3500.0,
            sourceBreakdown = listOf(
                HealthConnectSourceStat("Samsung Health", 8, 2500.0, 71.4),
                HealthConnectSourceStat("Google Fit", 4, 1000.0, 28.6)
            )
        )
    )

    HydroTrackerTheme {
        val backStack = rememberNavBackStack(NavigationRoutes.Statistics)

        MainNavigationScaffold(
            backStack = backStack,
            currentKey = NavigationRoutes.Statistics,
            content = { paddingValues ->
                StatisticsScreen(
                    uiState = uiState,
                    themePreferences = ThemePreferences(),
                    userProfile = previewUser,
                    activeBeverages = BeverageType.getAllSorted().map { it.toOption() },
                    paddingValues = paddingValues
                )
            }
        )
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
private  fun StatisticsLoadingStatePreview() {
    val uiState = StatisticsUiState(
        isLoading = true
    )
    HydroTrackerTheme {
        val backStack = rememberNavBackStack(NavigationRoutes.Statistics)

        MainNavigationScaffold(
            backStack = backStack,
            currentKey = NavigationRoutes.Statistics,
            content = { paddingValues ->
                StatisticsScreen(
                    uiState = uiState,
                    themePreferences = ThemePreferences(),
                    activeBeverages = BeverageType.getAllSorted().map { it.toOption() },
                    paddingValues = paddingValues
                )
            }
        )
    }
}

@Preview(showBackground = true, name = "Empty State")
@Composable
private  fun StatisticsEmptyStatePreview() {
    val uiState = StatisticsUiState(
        isLoading = false
    )
    HydroTrackerTheme {
        val backStack = rememberNavBackStack(NavigationRoutes.Statistics)

        MainNavigationScaffold(
            backStack = backStack,
            currentKey = NavigationRoutes.Statistics,
            content = { paddingValues ->
                StatisticsScreen(
                    uiState = uiState,
                    themePreferences = ThemePreferences(),
                    activeBeverages = BeverageType.getAllSorted().map { it.toOption() },
                    paddingValues = paddingValues
                )
            }
        )
    }
}

