// MainNavigationScaffold.kt
// Location: app/src/main/java/com/cemcakmak/hydrotracker/presentation/common/MainNavigationScaffold.kt

package com.cemcakmak.hydrotracker.presentation.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.utils.ImageUtils
import java.io.File

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainNavigationScaffold(
    backStack: NavBackStack<NavKey>,
    currentKey: NavigationRoutes,
    userProfileImagePath: String? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val shouldShowBottomBar = currentKey in setOf(
        NavigationRoutes.Home,
        NavigationRoutes.History,
        NavigationRoutes.Profile,
        NavigationRoutes.Settings
    )

    Scaffold(
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
        }
    ) { paddingValues ->
        // Intentionally not applying paddingValues here — inner screens have their own
        // Scaffold/TopAppBar and handle insets themselves. Passing paddingValues through
        // satisfies the Compose inspection without causing double insets.
        content(paddingValues)
    }
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
fun MainNavigationScaffoldPreview() {
    val backStack = rememberNavBackStack(NavigationRoutes.Home)
    MainNavigationScaffold(
        backStack = backStack,
        currentKey = NavigationRoutes.Home,
        content = { _ ->
            Text(text = "Sample Content")
        }
    )
}

@Preview
@Composable
fun HydroNavigationBarPreview() {
    HydroNavigationBar(
        currentKey = NavigationRoutes.Home,
        userProfileImagePath = null,
        onTabSelected = {}
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
