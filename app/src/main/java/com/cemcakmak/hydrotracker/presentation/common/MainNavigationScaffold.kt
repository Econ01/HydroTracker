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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
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
                    backStack = backStack,
                    currentKey = currentKey,
                    userProfileImagePath = userProfileImagePath
                )
            }
        },
        content = content
    )
}

@Composable
private fun HydroNavigationBar(
    backStack: NavBackStack<NavKey>,
    currentKey: NavigationRoutes,
    userProfileImagePath: String? = null
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        NavigationItem.entries.forEach { item ->
            val isSelected = currentKey == item.key

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
                    if (!isSelected) {
                        backStack.apply {
                            clear()
                            add(NavigationRoutes.Home)
                            if (item.key != NavigationRoutes.Home) {
                                add(item.key)
                            }
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
    val key: NavigationRoutes,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    HOME(
        key = NavigationRoutes.Home,
        label = "Home",
        icon = Icons.Filled.Home,
        selectedIcon = Icons.Filled.Home
    ),
    HISTORY(
        key = NavigationRoutes.History,
        label = "History",
        icon = Icons.Filled.Analytics,
        selectedIcon = Icons.Filled.Analytics
    ),
    PROFILE(
        key = NavigationRoutes.Profile,
        label = "Profile",
        icon = Icons.Filled.Person,
        selectedIcon = Icons.Filled.Person
    ),
    SETTINGS(
        key = NavigationRoutes.Settings,
        label = "Settings",
        icon = Icons.Filled.Settings,
        selectedIcon = Icons.Filled.Settings
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
    val backStack = rememberNavBackStack(NavigationRoutes.Home)
    HydroNavigationBar(
        backStack = backStack,
        currentKey = NavigationRoutes.Home,
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
