package com.dev.hydrotracker.presentation.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.hydrotracker.data.models.UserProfile
import com.dev.hydrotracker.notifications.NotificationPermissionManager
import com.dev.hydrotracker.notifications.HydroNotificationScheduler
import kotlinx.coroutines.launch

/**
 * Notification Settings Section for the Settings Screen
 */
@Composable
fun NotificationSettingsSection(
    userProfile: UserProfile?,
    onRequestPermission: () -> Unit,
    isVisible: Boolean = true
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(NotificationPermissionManager.hasNotificationPermission(context))
    }

    var hasExactAlarmPermission by remember {
        mutableStateOf(NotificationPermissionManager.hasExactAlarmPermission(context))
    }

    var isNotificationsEnabled by remember {
        mutableStateOf(hasPermission && userProfile?.isOnboardingCompleted == true)
    }

    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Function to refresh permission status
    val refreshPermissions = {
        hasPermission = NotificationPermissionManager.hasNotificationPermission(context)
        hasExactAlarmPermission = NotificationPermissionManager.hasExactAlarmPermission(context)
        isNotificationsEnabled = hasPermission && hasExactAlarmPermission && userProfile?.isOnboardingCompleted == true
    }

    // Update states when userProfile changes or refresh is triggered
    LaunchedEffect(userProfile, refreshTrigger) {
        refreshPermissions()
    }

    // Listen for when app regains focus to refresh permissions (same logic as Health Connect)
    androidx.compose.runtime.DisposableEffect(Unit) {
        val activity = context as? androidx.activity.ComponentActivity
        val listener = object : android.app.Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: android.app.Activity) {
                if (activity == context) {
                    // Refresh permissions when returning to this screen
                    refreshTrigger++
                }
            }
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityStarted(activity: android.app.Activity) {}
            override fun onActivityStopped(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        }

        activity?.application?.registerActivityLifecycleCallbacks(listener)
        onDispose {
            activity?.application?.unregisterActivityLifecycleCallbacks(listener)
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            initialOffsetY = { it / 2 }
        ) + fadeIn(animationSpec = tween(600, delayMillis = 300))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(5.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Hydration Reminders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Permission Status Card
                NotificationPermissionCard(
                    hasPermission = hasPermission,
                    hasExactAlarmPermission = hasExactAlarmPermission,
                    onRequestPermission = {
                        onRequestPermission()
                        // Update state after permission request
                        hasPermission = NotificationPermissionManager.hasNotificationPermission(context)
                        hasExactAlarmPermission = NotificationPermissionManager.hasExactAlarmPermission(context)
                        if (hasPermission && hasExactAlarmPermission && userProfile != null) {
                            isNotificationsEnabled = true
                            HydroNotificationScheduler.startNotifications(context, userProfile)
                        }
                    },
                    onRequestExactAlarmPermission = {
                        NotificationPermissionManager.requestExactAlarmPermission(context)
                    }
                )

                // Notification Settings (only show if all permissions granted)
                if (hasPermission && hasExactAlarmPermission && userProfile != null) {
                    NotificationControlsCard(
                        userProfile = userProfile,
                        isEnabled = isNotificationsEnabled,
                        onToggleNotifications = { enabled ->
                            isNotificationsEnabled = enabled
                            coroutineScope.launch {
                                if (enabled) {
                                    HydroNotificationScheduler.startNotifications(context, userProfile)
                                } else {
                                    HydroNotificationScheduler.stopNotifications(context)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationPermissionCard(
    hasPermission: Boolean,
    hasExactAlarmPermission: Boolean,
    onRequestPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit
) {
    val allPermissionsGranted = hasPermission && hasExactAlarmPermission
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (allPermissionsGranted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Main permission status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (allPermissionsGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (allPermissionsGranted) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    },
                    modifier = Modifier.size(24.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (allPermissionsGranted) "Notifications Ready" else "Permissions Required",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (allPermissionsGranted) {
                            "You'll receive hydration reminders"
                        } else {
                            "Grant permissions to enable notifications"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Individual permission buttons
            if (!allPermissionsGranted) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                
                if (!hasPermission) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Notification Permission",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Allow")
                        }
                    }
                }
                
                if (!hasExactAlarmPermission) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Alarm Scheduling",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = onRequestExactAlarmPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Allow")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationControlsCard(
    userProfile: UserProfile,
    isEnabled: Boolean,
    onToggleNotifications: (Boolean) -> Unit
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Main toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hydration Reminders",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Get reminded every ${userProfile.reminderInterval} minutes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleNotifications,
                    thumbContent = if (isEnabled) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    }
                )
            }

            if (isEnabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Reminder details
                NotificationDetailRow(
                    icon = Icons.Default.Schedule,
                    label = "Reminder Frequency",
                    value = "Every ${userProfile.reminderInterval} minutes"
                )

                NotificationDetailRow(
                    icon = Icons.Default.WbSunny,
                    label = "Active Hours",
                    value = "${userProfile.wakeUpTime} - ${userProfile.sleepTime}"
                )

                NotificationDetailRow(
                    icon = Icons.Default.Style,
                    label = "Reminder Style",
                    value = userProfile.reminderStyle.getDisplayName()
                )

                // Next notification info (refreshes every time the composable recomposes)
                val nextNotificationTime = remember(userProfile, isEnabled) {
                    if (isEnabled) {
                        HydroNotificationScheduler.getNextScheduledTime(context, userProfile)
                    } else {
                        null
                    }
                }

                if (nextNotificationTime != null) {
                    NotificationDetailRow(
                        icon = Icons.Default.Schedule,
                        label = "Next Reminder",
                        value = nextNotificationTime
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationDetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}