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

package com.cemcakmak.hydrotracker.presentation.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.compose.ui.res.vectorResource

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.ActivityLevel
import com.cemcakmak.hydrotracker.data.models.AgeGroup
import com.cemcakmak.hydrotracker.data.models.EdgeEffect
import com.cemcakmak.hydrotracker.data.models.Gender
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import com.cemcakmak.hydrotracker.data.update.UpdateStatus
import com.cemcakmak.hydrotracker.presentation.common.LocalNavAnimatedVisibilityScope
import com.cemcakmak.hydrotracker.presentation.common.LocalSharedTransitionScope
import com.cemcakmak.hydrotracker.presentation.common.MainNavigationScaffold
import com.cemcakmak.hydrotracker.presentation.common.MainTabTopAppBar
import com.cemcakmak.hydrotracker.presentation.common.NavigationRoutes
import com.cemcakmak.hydrotracker.presentation.common.effect.backdropSource
import com.cemcakmak.hydrotracker.presentation.common.effect.rememberBackdropBlurState
import com.cemcakmak.hydrotracker.presentation.settings.profile.ProfileAvatar
import com.cemcakmak.hydrotracker.presentation.settings.profile.ProfileSettingsScreen
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHubScreen(
    userProfile: UserProfile? = null,
    themePreferences: ThemePreferences = ThemePreferences(),
    wasPop: Boolean = false,
    developerOptionsEnabled: Boolean = false,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    updateStatus: UpdateStatus = UpdateStatus.Idle,
    onNavigateTo: (NavigationRoutes) -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val isPreview = LocalInspectionMode.current
    val shouldApplyDepth = !isPreview && wasPop

    val blur by if (shouldApplyDepth) {
        val animatedContentScope = LocalNavAnimatedContentScope.current
        animatedContentScope.transition.animateDp(
            transitionSpec = { tween(400) },
            label = "enterBlur"
        ) { state ->
            if (state == EnterExitState.PreEnter) 8.dp else 0.dp
        }
    } else {
        remember { mutableStateOf(0.dp) }
    }

    // Depth scrim that clears in sync with the blur as the hub comes forward.
    val scrimAlpha by if (shouldApplyDepth) {
        val animatedContentScope = LocalNavAnimatedContentScope.current
        animatedContentScope.transition.animateFloat(
            transitionSpec = { tween(400) },
            label = "enterScrim"
        ) { state ->
            if (state == EnterExitState.PreEnter) 0.4f else 0f
        }
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    // Black on light surfaces, white on dark/AMOLED so the receding page still reads.
    val scrimColor = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        Color.White
    } else {
        Color.Black
    }

    val edgeEffectStyle = themePreferences.edgeEffect.let {
        if (it == EdgeEffect.BLURRED && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            EdgeEffect.TRANSPARENT
        } else {
            it
        }
    }

    // Backdrop captured for the frosted top band
    val backdropState = rememberBackdropBlurState()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .blur(blur),
        topBar = {
            MainTabTopAppBar(
                titleResId = R.string.nav_settings,
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
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

                if (userProfile != null) {
                    ProfileSettingsCategoryCard(
                        userProfile = userProfile,
                        onNavigateTo = { onNavigateTo(NavigationRoutes.SettingsProfile) }
                    )
                }

                val categories = buildList {
                    add(
                        SettingsCategory(
                            title = stringResource(R.string.screen_appearance_title),
                            description = stringResource(R.string.settings_appearance_desc),
                            icon = { Icon(ImageVector.vectorResource(R.drawable.palette_filled), contentDescription = null) },
                            route = NavigationRoutes.SettingsAppearance
                        )
                    )
                    add(
                        SettingsCategory(
                            title = stringResource(R.string.screen_display_locale_title),
                            description = stringResource(R.string.settings_display_desc),
                            icon = { Icon(ImageVector.vectorResource(R.drawable.event_filled), contentDescription = null) },
                            route = NavigationRoutes.SettingsDisplay
                        )
                    )
                    add(
                        SettingsCategory(
                            title = stringResource(R.string.screen_hydration_title),
                            description = stringResource(R.string.settings_hydration_desc),
                            icon = { Icon(ImageVector.vectorResource(R.drawable.water_filled), contentDescription = null) },
                            route = NavigationRoutes.SettingsHydration
                        )
                    )
                    add(
                        SettingsCategory(
                            title = stringResource(R.string.screen_quickadd_title),
                            description = stringResource(R.string.settings_quickadd_desc),
                            icon = { Icon(ImageVector.vectorResource(R.drawable.checklist_filled), contentDescription = null) },
                            route = NavigationRoutes.SettingsContainers
                        )
                    )
                    add(
                        SettingsCategory(
                            title = stringResource(R.string.screen_notifications_title),
                            description = stringResource(R.string.settings_notifications_desc),
                            icon = { Icon(ImageVector.vectorResource(R.drawable.notifications_filled), contentDescription = null) },
                            route = NavigationRoutes.SettingsNotifications
                        )
                    )
                    add(
                        SettingsCategory(
                            title = stringResource(R.string.screen_data_management_title),
                            description = stringResource(R.string.settings_data_management_desc),
                            icon = { Icon(ImageVector.vectorResource(R.drawable.save_fill), contentDescription = null) },
                            route = NavigationRoutes.SettingsDataManagement
                        )
                    )
                    val isUpdateAvailable = updateStatus is UpdateStatus.Available
                    add(
                        SettingsCategory(
                            title = if (isUpdateAvailable) stringResource(R.string.settings_about_update_available) else stringResource(R.string.screen_about_title),
                            titleColor = if (isUpdateAvailable) MaterialTheme.colorScheme.tertiary else null,
                            description = stringResource(R.string.settings_about_desc),
                            icon = { Icon(ImageVector.vectorResource(R.drawable.info_filled), contentDescription = null) },
                            route = NavigationRoutes.SettingsAbout
                        )
                    )
                    if (developerOptionsEnabled) {
                        add(
                            SettingsCategory(
                                title = stringResource(R.string.screen_developer_title),
                                description = stringResource(R.string.settings_developer_desc),
                                icon = { Icon(ImageVector.vectorResource(R.drawable.code_blocks_filled), contentDescription = null) },
                                route = NavigationRoutes.SettingsDeveloper
                            )
                        )
                    }
                }

                Column {
                    categories.forEachIndexed { index, category ->
                        SettingsCategoryCard(
                            index = index,
                            category = category,
                            totalSize = categories.size,
                            onNavigateTo = onNavigateTo
                        )
                    }
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
    // Oversized so it still blankets the screen while NavDisplay scales the entry to 0.8,
    // leaving no bright ring. Sits above the hub but inside the incoming entry, so the
    // foreground page stays bright.
    if (scrimAlpha > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(1.3f)
                .background(scrimColor.copy(alpha = scrimAlpha))
        )
    }
}

@Composable
private fun ProfileSettingsCategoryCard(
    userProfile: UserProfile,
    onNavigateTo: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current

    val nameModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                rememberSharedContentState(key = "profile-name-${userProfile.name}"),
                animatedVisibilityScope = animatedVisibilityScope,
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
            )
        }
    } else {
        Modifier
    }

    SettingsGroupCard(
        index = 0,
        size = 1,
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
            onNavigateTo()
        }
    ) {
        ListItem(
            supportingContent = { Text(text = userProfile.name, modifier = nameModifier) },
            leadingContent = {
                    ProfileAvatar(
                    profileImagePath = userProfile.profileImagePath,
                    name = userProfile.name,
                    size = 44.dp
                )
            }
        ) {
            Text(text = stringResource(R.string.nav_profile))
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    index: Int,
    category: SettingsCategory,
    totalSize: Int,
    onNavigateTo: (NavigationRoutes) -> Unit
) {
    val shape = getShapeForIndex(
        index = index,
        size = totalSize,
        outerRadius = 24.dp,
        innerRadius = 6.dp
    )

    val haptics = LocalHapticFeedback.current

    Surface(
        shape = shape,
        tonalElevation = 2.dp,
        modifier = Modifier
            .padding(bottom = 2.dp),
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            onNavigateTo(category.route)
        }
    ) {
        ListItem(
            supportingContent = { Text(category.description) },
            leadingContent = category.icon,
            colors = ListItemDefaults.colors(
                leadingContentColor = MaterialTheme.colorScheme.primary,
                contentColor = category.titleColor ?: ListItemDefaults.colors().contentColor
            )
        ) {
            Text(category.title)
        }
    }
}

private data class SettingsCategory(
    val title: String,
    val titleColor: Color? = null,
    val description: String,
    val icon: @Composable () -> Unit,
    val route: NavigationRoutes
)

private fun getShapeForIndex(
    index: Int,
    size: Int,
    outerRadius: Dp,
    innerRadius: Dp
): Shape {
    return when {
        size == 1 -> RoundedCornerShape(outerRadius)
        index == 0 -> RoundedCornerShape(
            topStart = outerRadius,
            topEnd = outerRadius,
            bottomStart = innerRadius,
            bottomEnd = innerRadius
        )
        index == size - 1 -> RoundedCornerShape(
            topStart = innerRadius,
            topEnd = innerRadius,
            bottomStart = outerRadius,
            bottomEnd = outerRadius
        )
        else -> RoundedCornerShape(innerRadius)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true)
@Composable
fun SettingsHubInteractivePreview() {
    HydroTrackerTheme {
        var selectedRoute by remember { mutableStateOf<NavigationRoutes?>(null) }

        if (selectedRoute == null) {
            val backStack = rememberNavBackStack(NavigationRoutes.Settings)

            MainNavigationScaffold(
                backStack = backStack,
                currentKey = NavigationRoutes.Settings,
                content = { paddingValues ->
                    SettingsHubScreen(
                        userProfile = UserProfile(
                            name = "Preview User",
                            gender = Gender.OTHER,
                            ageGroup = AgeGroup.YOUNG_ADULT_18_30,
                            activityLevel = ActivityLevel.MODERATE,
                            wakeUpTime = "07:00",
                            sleepTime = "23:00",
                            dailyWaterGoal = 2500.0,
                            reminderInterval = 120
                        ),
                        developerOptionsEnabled = true,
                        paddingValues = paddingValues,
                        onNavigateTo = { route -> selectedRoute = route }
                    )
                }
            )
        } else {
            val onNavigateBack = { selectedRoute = null }
            when (selectedRoute) {
                NavigationRoutes.SettingsAppearance -> AppearanceScreen(onNavigateBack = onNavigateBack)
                NavigationRoutes.SettingsDisplay -> DisplayLocaleScreen(onNavigateBack = onNavigateBack)
                NavigationRoutes.SettingsHydration -> HydrationHealthScreen(onNavigateBack = onNavigateBack)
                NavigationRoutes.SettingsContainers -> QuickAddCustomizationScreen(
                    onNavigateToContainerPresets = {},
                    onNavigateToBeverageTypes = {},
                    onNavigateBack = onNavigateBack
                )
                NavigationRoutes.SettingsNotifications -> NotificationsScreen(
                    onNavigateToReminderInterval = {},
                    onNavigateBack = onNavigateBack
                )
                NavigationRoutes.SettingsSupport -> SupportDevelopmentScreen(onNavigateBack = onNavigateBack)
                NavigationRoutes.SettingsAbout -> AboutScreen(
                    onNavigateToUpdates = {},
                    onNavigateBack = onNavigateBack,
                    onNavigateToLicenses = {}
                )
                NavigationRoutes.SettingsDeveloper -> DeveloperOptionsScreen(
                    onNavigateBack = onNavigateBack,
                    onNavigateToOnboarding = {},
                    onNavigateToHapticsTest = {},
                    onNavigateToHapticsLab = {}
                )
                NavigationRoutes.SettingsProfile -> ProfileSettingsScreen(
                    userProfile = UserProfile(
                        name = "Preview User",
                        gender = Gender.OTHER,
                        ageGroup = AgeGroup.YOUNG_ADULT_18_30,
                        activityLevel = ActivityLevel.MODERATE,
                        wakeUpTime = "07:00",
                        sleepTime = "23:00",
                        dailyWaterGoal = 2500.0,
                        reminderInterval = 120
                    ),
                    themePreferences = ThemePreferences(),
                    userRepository = UserRepository(LocalContext.current),
                    todayEntryCount = 5,
                    daysTracked = 12,
                    todayGoalProgress = 0.65f,
                    onNavigateToCrop = {},
                    onNavigateBack = onNavigateBack
                )
                else -> {}
            }
        }
    }
}
