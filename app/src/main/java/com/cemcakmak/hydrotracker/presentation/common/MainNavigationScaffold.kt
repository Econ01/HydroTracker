// MainNavigationScaffold.kt
// Location: app/src/main/java/com/cemcakmak/hydrotracker/presentation/common/MainNavigationScaffold.kt

package com.cemcakmak.hydrotracker.presentation.common

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.presentation.home.HomeTopAppBar
import com.cemcakmak.hydrotracker.presentation.settings.SettingsTopAppBar
import com.cemcakmak.hydrotracker.utils.ImageUtils
import java.io.File

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScaffold(
    backStack: NavBackStack<NavKey>,
    currentKey: NavigationRoutes,
    userProfileImagePath: String? = null,
    userProfile: UserProfile? = null,
    waterIntakeRepository: WaterIntakeRepository? = null,
    snackbarHostState: SnackbarHostState,
    fabExpanded: Boolean = true,
    onNavigateToSettings: () -> Unit = {},
    onAddCustomClick: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val shouldShowBottomBar = currentKey in setOf(
        NavigationRoutes.Home,
        NavigationRoutes.History,
        NavigationRoutes.Profile,
        NavigationRoutes.Settings
    )

    // Scroll behaviors remembered per route so collapsed state survives tab switches
    val homeScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val settingsScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val appearanceScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val nestedScrollModifier = when (currentKey) {
        NavigationRoutes.Home -> Modifier.nestedScroll(homeScrollBehavior.nestedScrollConnection)
        NavigationRoutes.Settings -> Modifier.nestedScroll(settingsScrollBehavior.nestedScrollConnection)
        NavigationRoutes.SettingsAppearance -> Modifier.nestedScroll(appearanceScrollBehavior.nestedScrollConnection)
        else -> Modifier
    }

    val haptics = LocalHapticFeedback.current

    Scaffold(
        modifier = nestedScrollModifier,
        topBar = {
            AnimatedContent(
                targetState = currentKey,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith
                    fadeOut(animationSpec = tween(200))
                },
                label = "top_bar_crossfade"
            ) { route ->
                when (route) {
                    NavigationRoutes.Home -> HomeTopAppBar(
                        scrollBehavior = homeScrollBehavior,
                        userProfile = userProfile,
                        waterIntakeRepository = waterIntakeRepository,
                        onNavigateToSettings = onNavigateToSettings
                    )
                    NavigationRoutes.History -> HistoryTopAppBar()
                    NavigationRoutes.Profile -> ProfileTopAppBar()
                    NavigationRoutes.Settings -> SettingsTopAppBar(scrollBehavior = settingsScrollBehavior)
                    NavigationRoutes.SettingsAppearance -> LargeFlexibleTopAppBar(
                        title = { Text("Appearance") },
                        navigationIcon = {
                            IconButton(onClick = { backStack.removeLastOrNull() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    )
                    else -> {}
                }
            }
        },
        bottomBar = {
            if (shouldShowBottomBar) {
                HydroNavigationBar(
                    currentKey = currentKey,
                    userProfileImagePath = userProfileImagePath,
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
                            contentDescription = "Add Custom Amount"
                        )
                    },
                    text = {
                        Text(
                            text = "Add Custom",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTopAppBar() {
    TopAppBar(
        title = {
            Text(
                text = "History & Statistics",
                fontWeight = FontWeight.Bold
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTopAppBar() {
    TopAppBar(
        title = {
            Text(
                text = "Profile",
                fontWeight = FontWeight.Bold
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HydroNavigationBar(
    currentKey: NavigationRoutes,
    userProfileImagePath: String? = null,
    onTabSelected: (NavigationRoutes) -> Unit = {}
) {
    ShortNavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        NavigationItem.entries.forEach { item ->
            val isSelected = currentKey == item.key
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
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text(item.label) } },
                        state = tooltipState
                    ) {
                        if (item == NavigationItem.PROFILE) {
                            ProfileIcon(
                                profileImagePath = userProfileImagePath,
                                isSelected = isSelected,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = ImageVector.vectorResource(
                                    if (isSelected) item.selectedIconRes else item.iconRes
                                ),
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMediumEmphasized
                    )
                },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        onTabSelected(item.key)
                    }
                },
                colors = ShortNavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    selectedIndicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

enum class NavigationItem(
    val key: NavigationRoutes,
    val label: String,
    @get:DrawableRes val iconRes: Int,
    @get:DrawableRes val selectedIconRes: Int
) {
    HOME(
        key = NavigationRoutes.Home,
        label = "Home",
        iconRes = R.drawable.home,
        selectedIconRes = R.drawable.home_filled
    ),
    HISTORY(
        key = NavigationRoutes.History,
        label = "History",
        iconRes = R.drawable.leaderboard,
        selectedIconRes = R.drawable.leaderboard_filled
    ),
    PROFILE(
        key = NavigationRoutes.Profile,
        label = "Profile",
        iconRes = R.drawable.person,
        selectedIconRes = R.drawable.person_filled
    ),
    SETTINGS(
        key = NavigationRoutes.Settings,
        label = "Settings",
        iconRes = R.drawable.settings,
        selectedIconRes = R.drawable.settings_filled
    )
}

/**
 * Profile Icon that shows user's profile picture or default icon
 */
@Composable
fun ProfileIcon(
    profileImagePath: String?,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var profileBitmap by remember(profileImagePath) { mutableStateOf<android.graphics.Bitmap?>(null) }

    // Load the image when profileImagePath changes
    LaunchedEffect(profileImagePath) {
        profileBitmap = if (profileImagePath != null && File(profileImagePath).exists()) {
            ImageUtils.loadProfileImageBitmap(context)
        } else {
            null
        }
    }

    if (profileBitmap != null) {
        // Show profile picture
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = profileBitmap!!.asImageBitmap(),
                contentDescription = "Profile Photo",
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        CircleShape
                    ),
                contentScale = ContentScale.Crop
            )
        }
    } else {
        // Fall back to default icon
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = "Profile",
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
fun MainNavigationScaffoldPreview() {
    val backStack = rememberNavBackStack(NavigationRoutes.Home)
    MainNavigationScaffold(
        backStack = backStack,
        currentKey = NavigationRoutes.Home,
        snackbarHostState = remember { SnackbarHostState() },
        content = { _ ->
            Text(text = "Sample Content")
        }
    )
}
