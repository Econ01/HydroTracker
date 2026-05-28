package com.cemcakmak.hydrotracker.presentation.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.presentation.common.NavigationRoutes
import com.cemcakmak.hydrotracker.ui.icons.tabler.TablerIcons
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHubScreen(
    developerOptionsEnabled: Boolean = false,
    onNavigateTo: (NavigationRoutes) -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Settings") },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val categories = buildList {
                add(
                    SettingsCategory(
                        title = "Appearance",
                        description = "Theme, colors and AMOLED mode",
                        icon = { Icon(TablerIcons.PaletteFilled, contentDescription = null) },
                        route = NavigationRoutes.SettingsAppearance
                    )
                )
                add(
                    SettingsCategory(
                        title = "Display & Locale",
                        description = "Week start day and locale",
                        icon = { Icon(TablerIcons.CalendarEventFilled, contentDescription = null) },
                        route = NavigationRoutes.SettingsDisplay
                    )
                )
                add(
                    SettingsCategory(
                        title = "Hydration & Health",
                        description = "Water goal calculation and Health Connect",
                        icon = { Icon(TablerIcons.HeartFilled, contentDescription = null) },
                        route = NavigationRoutes.SettingsHydration
                    )
                )
                add(
                    SettingsCategory(
                        title = "Quick Add Customization",
                        description = "Container presets and beverage types",
                        icon = { Icon(TablerIcons.EditFilled, contentDescription = null) },
                        route = NavigationRoutes.SettingsContainers
                    )
                )
                add(
                    SettingsCategory(
                        title = "Notifications",
                        description = "Hydration reminders and permissions",
                        icon = { Icon(TablerIcons.BellFilled, contentDescription = null) },
                        route = NavigationRoutes.SettingsNotifications
                    )
                )
                add(
                    SettingsCategory(
                        title = "Support Development",
                        description = "Donate and support the app",
                        icon = { Icon(TablerIcons.HeartFilled, contentDescription = null) },
                        route = NavigationRoutes.SettingsSupport
                    )
                )
                add(
                    SettingsCategory(
                        title = "About",
                        description = "Sources, privacy policy and license",
                        icon = { Icon(TablerIcons.InfoCircleFilled, contentDescription = null) },
                        route = NavigationRoutes.SettingsAbout
                    )
                )
                if (developerOptionsEnabled) {
                    add(
                        SettingsCategory(
                            title = "Developer Options",
                            description = "Debug tools and testing",
                            icon = { Icon(TablerIcons.CodeCircleFilled, contentDescription = null) },
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
                        isVisible = isVisible,
                        totalSize = categories.size,
                        onNavigateTo = onNavigateTo
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    index: Int,
    category: SettingsCategory,
    isVisible: Boolean,
    totalSize: Int,
    onNavigateTo: (NavigationRoutes) -> Unit
) {
    val offsetY = remember { Animatable(150f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay((index * 80).milliseconds)
            launch {
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(300)
                )
            }
        }
    }

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
            .padding(bottom = 4.dp)
            .graphicsLayer {
                translationY = offsetY.value
                this.alpha = alpha.value
            },
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onNavigateTo(category.route)
        }
    ) {
        ListItem(
            headlineContent = { Text(category.title) },
            supportingContent = { Text(category.description) },
            leadingContent = category.icon,
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
