// MainNavigationScaffold.kt
// Location: app/src/main/java/com/cemcakmak/hydrotracker/presentation/common/MainNavigationScaffold.kt

package com.cemcakmak.hydrotracker.presentation.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.NavBarLabelMode

/**
 * Composition local used by SettingsHubScreen to communicate its animated
 * depth-blur value to the common top app bar hosted in MainNavigationScaffold.
 */
val LocalSettingsHubBlur = compositionLocalOf<MutableState<Dp>> {
    error("LocalSettingsHubBlur must be provided by MainNavigationScaffold")
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScaffold(
    backStack: NavBackStack<NavKey>,
    currentKey: NavigationRoutes,
    snackbarHostState: SnackbarHostState,
    fabExpanded: Boolean = true,
    onAddCustomClick: () -> Unit = {},
    onTabSwitch: () -> Unit = {},
    autoHideNavBar: Boolean = false,
    navBarLabelMode: NavBarLabelMode = NavBarLabelMode.ALWAYS,
    content: @Composable (PaddingValues) -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    val shouldShowBottomBar = currentKey in setOf(
        NavigationRoutes.Home,
        NavigationRoutes.History,
        NavigationRoutes.Settings
    )

    // Auto-hide on scroll: hide the bar when scrolling down, reveal when scrolling up.
    val barVisibleByScroll = remember { mutableStateOf(true) }
    LaunchedEffect(currentKey) { barVisibleByScroll.value = true }
    val autoHideConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -0.5f) barVisibleByScroll.value = false
                else if (available.y > 0.5f) barVisibleByScroll.value = true
                return Offset.Zero
            }
        }
    }

    // Scroll behaviours remembered per route so collapsed state survives tab switches
    val historyScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val settingsScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val nestedScrollModifier = run {
        val base = when (currentKey) {
            NavigationRoutes.History -> Modifier.nestedScroll(historyScrollBehavior.nestedScrollConnection)
            NavigationRoutes.Settings -> Modifier.nestedScroll(settingsScrollBehavior.nestedScrollConnection)
            else -> Modifier
        }
        if (autoHideNavBar) base.nestedScroll(autoHideConnection) else base
    }

    val settingsBlurState = remember { mutableStateOf(0.dp) }

    CompositionLocalProvider(LocalSettingsHubBlur provides settingsBlurState) {
        Scaffold(
            modifier = nestedScrollModifier,
            topBar = {
                if (currentKey == NavigationRoutes.History || currentKey == NavigationRoutes.Settings) {
                    MainTabTopAppBar(
                        titleResId = if (currentKey == NavigationRoutes.History) {
                            R.string.nav_history
                        } else {
                            R.string.nav_settings
                        },
                        scrollBehavior = if (currentKey == NavigationRoutes.History) {
                            historyScrollBehavior
                        } else {
                            settingsScrollBehavior
                        },
                        blur = if (currentKey == NavigationRoutes.Settings) {
                            settingsBlurState.value
                        } else {
                            0.dp
                        }
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = shouldShowBottomBar && (!autoHideNavBar || barVisibleByScroll.value),
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    HydroNavigationBar(
                        currentKey = currentKey,
                        labelMode = navBarLabelMode,
                        onTabSwitch = onTabSwitch,
                        onTabSelected = { key ->
                            backStack.apply {
                                clear()
                                add(key)
                            }
                        }
                    )
                }
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = currentKey == NavigationRoutes.Home,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            onAddCustomClick()
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        },
                        expanded = fabExpanded,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.cd_add_custom_amount)
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.nav_add_custom),
                                style = MaterialTheme.typography.labelLargeEmphasized
                            )
                        }
                    )
                }
            },
            snackbarHost = { HydroSnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            content(paddingValues)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HydroNavigationBar(
    currentKey: NavigationRoutes,
    labelMode: NavBarLabelMode = NavBarLabelMode.ALWAYS,
    onTabSelected: (NavigationRoutes) -> Unit = {},
    onTabSwitch: () -> Unit = {}
) {
    ShortNavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        NavigationItem.entries.forEach { item ->
            val isSelected = currentKey == item.key
            val showLabel = labelMode == NavBarLabelMode.ALWAYS ||
                    (labelMode == NavBarLabelMode.SELECTED && isSelected)
            val tooltipState = rememberTooltipState()
            val haptics = LocalHapticFeedback.current

            LaunchedEffect(tooltipState.isVisible) {
                if (tooltipState.isVisible) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }

            ShortNavigationBarItem(
                icon = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text(stringResource(item.labelResId)) } },
                        state = tooltipState
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(
                                if (isSelected) item.selectedIconRes else item.iconRes
                            ),
                            contentDescription = stringResource(item.labelResId),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = if (showLabel) {
                    {
                        Text(
                            text = stringResource(item.labelResId),
                            style = MaterialTheme.typography.labelMediumEmphasized
                        )
                    }
                } else {
                    null
                },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        onTabSwitch()
                        onTabSelected(item.key)
                    }
                }
            )
        }
    }
}

enum class NavigationItem(
    val key: NavigationRoutes,
    @get:StringRes val labelResId: Int,
    @get:DrawableRes val iconRes: Int,
    @get:DrawableRes val selectedIconRes: Int
) {
    HOME(
        key = NavigationRoutes.Home,
        labelResId = R.string.nav_home,
        iconRes = R.drawable.home,
        selectedIconRes = R.drawable.home_filled
    ),
    HISTORY(
        key = NavigationRoutes.History,
        labelResId = R.string.nav_history,
        iconRes = R.drawable.leaderboard,
        selectedIconRes = R.drawable.leaderboard_filled
    ),
    SETTINGS(
        key = NavigationRoutes.Settings,
        labelResId = R.string.nav_settings,
        iconRes = R.drawable.settings,
        selectedIconRes = R.drawable.settings_filled
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
fun MainNavigationScaffoldPreview() {
    val backStack = rememberNavBackStack(NavigationRoutes.Home)
    MainNavigationScaffold(
        backStack = backStack,
        currentKey = NavigationRoutes.History,
        snackbarHostState = remember { SnackbarHostState() },
        content = { _ ->
            Text(text = "Sample Content")
        }
    )
}
