// MainNavigationScaffold.kt
// Location: app/src/main/java/com/cemcakmak/hydrotracker/presentation/common/MainNavigationScaffold.kt

package com.cemcakmak.hydrotracker.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.cemcakmak.hydrotracker.utils.ImageUtils
import java.io.File

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainNavigationScaffold(
    navController: NavController,
    currentRoute: String,
    userProfileImagePath: String? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val shouldShowBottomBar = when (currentRoute) {
        NavigationRoutes.HOME, NavigationRoutes.HISTORY, NavigationRoutes.PROFILE, NavigationRoutes.SETTINGS -> true
        else -> false
    }

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                HydroNavigationBar(
                    navController = navController,
                    currentRoute = currentRoute,
                    userProfileImagePath = userProfileImagePath
                )
            }
        },
        content = content
    )
}

@Composable
private fun HydroNavigationBar(
    navController: NavController,
    currentRoute: String,
    userProfileImagePath: String? = null
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        NavigationItem.entries.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                icon = {
                    if (item == NavigationItem.PROFILE) {
                        ProfileIcon(
                            profileImagePath = userProfileImagePath,
                            isSelected = isSelected,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp)
                        )
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
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(NavigationRoutes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

enum class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    HOME(
        route = NavigationRoutes.HOME,
        label = "Home",
        icon = Icons.Filled.Home,
        selectedIcon = Icons.Filled.Home
    ),
    HISTORY(
        route = NavigationRoutes.HISTORY,
        label = "History",
        icon = Icons.Filled.Analytics,
        selectedIcon = Icons.Filled.Analytics
    ),
    PROFILE(
        route = NavigationRoutes.PROFILE,
        label = "Profile",
        icon = Icons.Filled.Person,
        selectedIcon = Icons.Filled.Person
    ),
    SETTINGS(
        route = NavigationRoutes.SETTINGS,
        label = "Settings",
        icon = Icons.Filled.Settings,
        selectedIcon = Icons.Filled.Settings
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
fun MainNavigationScaffoldPreview() {
    val navController = rememberNavController()
    MainNavigationScaffold(
        navController = navController,
        currentRoute = NavigationRoutes.HOME,
        content = { paddingValues ->
            Text(
                text = "Sample Content",
                modifier = Modifier.size(paddingValues.calculateBottomPadding())
            )
        }
    )
}

@Preview
@Composable
fun HydroNavigationBarPreview() {
    val navController = rememberNavController()
    HydroNavigationBar(
        navController = navController,
        currentRoute = NavigationRoutes.HOME,
        userProfileImagePath = null
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
