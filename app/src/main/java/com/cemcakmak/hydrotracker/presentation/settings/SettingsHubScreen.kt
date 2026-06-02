package com.cemcakmak.hydrotracker.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.EnterExitState
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.vectorResource

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.presentation.common.NavigationRoutes
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHubScreen(
    wasPop: Boolean = false,
    developerOptionsEnabled: Boolean = false,
    onNavigateTo: (NavigationRoutes) -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val haptics = LocalHapticFeedback.current

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

    // Scroll haptic logic
    var settingsWasExpanded by remember { mutableStateOf(true) }
    var settingsWasCollapsed by remember { mutableStateOf(false) }

    LaunchedEffect(scrollBehavior.state) {
        snapshotFlow { scrollBehavior.state.collapsedFraction }
            .collect { fraction ->
                val isExpanded = fraction == 0f
                val isCollapsed = fraction == 1f

                if (isExpanded && !settingsWasExpanded) {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
                if (isCollapsed && !settingsWasCollapsed) {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                }

                settingsWasExpanded = isExpanded
                settingsWasCollapsed = isCollapsed
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .blur(blur),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Settings") },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 16.dp)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val categories = buildList {
                add(
                    SettingsCategory(
                        title = "Appearance",
                        description = "Theme, colors and AMOLED mode",
                        icon = { Icon(ImageVector.vectorResource(R.drawable.palette_filled), contentDescription = null) },
                        route = NavigationRoutes.SettingsAppearance
                    )
                )
                add(
                    SettingsCategory(
                        title = "Display & Locale",
                        description = "Week start day and locale",
                        icon = { Icon(ImageVector.vectorResource(R.drawable.event_filled), contentDescription = null) },
                        route = NavigationRoutes.SettingsDisplay
                    )
                )
                add(
                    SettingsCategory(
                        title = "Hydration & Health",
                        description = "Water goal calculation and Health Connect",
                        icon = { Icon(ImageVector.vectorResource(R.drawable.water_filled), contentDescription = null) },
                        route = NavigationRoutes.SettingsHydration
                    )
                )
                add(
                    SettingsCategory(
                        title = "Quick Add Customization",
                        description = "Container presets and beverage types",
                        icon = { Icon(ImageVector.vectorResource(R.drawable.checklist_filled), contentDescription = null) },
                        route = NavigationRoutes.SettingsContainers
                    )
                )
                add(
                    SettingsCategory(
                        title = "Notifications",
                        description = "Hydration reminders and permissions",
                        icon = { Icon(ImageVector.vectorResource(R.drawable.notifications_filled), contentDescription = null) },
                        route = NavigationRoutes.SettingsNotifications
                    )
                )
                add(
                    SettingsCategory(
                        title = "Support Development",
                        description = "Donate and support the app",
                        icon = { Icon(ImageVector.vectorResource(R.drawable.heart_smile_filled), contentDescription = null) },
                        route = NavigationRoutes.SettingsSupport
                    )
                )
                add(
                    SettingsCategory(
                        title = "About",
                        description = "Sources, privacy policy and license",
                        icon = { Icon(ImageVector.vectorResource(R.drawable.info_filled), contentDescription = null) },
                        route = NavigationRoutes.SettingsAbout
                    )
                )
                if (developerOptionsEnabled) {
                    add(
                        SettingsCategory(
                            title = "Developer Options",
                            description = "Debug tools and testing",
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
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onNavigateTo(category.route)
        }
    ) {
        ListItem(
            headlineContent = { Text(category.title) },
            supportingContent = { Text(category.description) },
            leadingContent = category.icon,
            colors = ListItemColors(
                leadingIconColor = MaterialTheme.colorScheme.primary,
                headlineColor = ListItemDefaults.colors().headlineColor,
                supportingTextColor = ListItemDefaults.colors().supportingTextColor,
                containerColor = ListItemDefaults.colors().containerColor,
                overlineColor = ListItemDefaults.colors().overlineColor,
                trailingIconColor = ListItemDefaults.colors().trailingIconColor,
                disabledHeadlineColor = ListItemDefaults.colors().disabledHeadlineColor,
                disabledLeadingIconColor = ListItemDefaults.colors().disabledLeadingIconColor,
                disabledTrailingIconColor = ListItemDefaults.colors().disabledTrailingIconColor
            )
        )
    }
}

private data class SettingsCategory(
    val title: String,
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

@Preview(showBackground = true)
@Composable
fun SettingsHubInteractivePreview() {
    HydroTrackerTheme {
        var selectedRoute by remember { mutableStateOf<NavigationRoutes?>(null) }

        val title = when (selectedRoute) {
            NavigationRoutes.SettingsAppearance -> "Appearance"
            NavigationRoutes.SettingsDisplay -> "Display & Locale"
            NavigationRoutes.SettingsHydration -> "Hydration & Health"
            NavigationRoutes.SettingsContainers -> "Quick Add Customization"
            NavigationRoutes.SettingsNotifications -> "Notifications"
            NavigationRoutes.SettingsSupport -> "Support Development"
            NavigationRoutes.SettingsAbout -> "About"
            NavigationRoutes.SettingsDeveloper -> "Developer Options"
            else -> ""
        }

        if (selectedRoute == null) {
            SettingsHubScreen(
                developerOptionsEnabled = true,
                onNavigateTo = { route -> selectedRoute = route }
            )
        } else {
            PlaceholderScreen(
                title = title,
                onNavigateBack = { selectedRoute = null }
            )
        }
    }
}
