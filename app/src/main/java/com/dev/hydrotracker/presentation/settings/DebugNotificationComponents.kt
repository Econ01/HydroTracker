// DebugNotificationComponents.kt
// Location: app/src/main/java/com/cemcakmak/hydrotracker/presentation/settings/DebugNotificationComponents.kt

package com.dev.hydrotracker.presentation.settings

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.hydrotracker.data.models.UserProfile
import com.dev.hydrotracker.data.models.Gender
import com.dev.hydrotracker.data.models.AgeGroup
import com.dev.hydrotracker.data.models.ActivityLevel
import com.dev.hydrotracker.data.database.repository.WaterProgress
import com.dev.hydrotracker.data.database.repository.WaterIntakeRepository
import com.dev.hydrotracker.notifications.HydroNotificationService
import com.dev.hydrotracker.notifications.HydroNotificationScheduler
import com.dev.hydrotracker.notifications.NotificationPermissionManager
import com.dev.hydrotracker.presentation.common.showSuccessSnackbar
import com.dev.hydrotracker.presentation.common.showStackedSuccessSnackbar
import com.dev.hydrotracker.presentation.common.showStackedErrorSnackbar
import com.dev.hydrotracker.presentation.common.showStackedWarningSnackbar
import com.dev.hydrotracker.presentation.common.showStackedInfoSnackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

/**
 * Debug notification tools for the Settings screen
 * Provides testing and debugging capabilities for the notification system
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DebugNotificationSection(
    userProfile: UserProfile?,
    waterIntakeRepository: WaterIntakeRepository,
    snackbarHostState: SnackbarHostState,
    isVisible: Boolean = true
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            initialOffsetY = { it / 2 }
        ) + fadeIn(animationSpec = tween(600, delayMillis = 500))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Notification Debug Tools",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                Text(
                    text = "Test and debug the notification system. These tools help verify that notifications are working correctly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )

                // Test Notification Button
                DebugActionButton(
                    title = "Send Test Notification",
                    description = "Show a real hydration reminder with random content",
                    icon = Icons.Default.NotificationImportant,
                    snackbarHostState = snackbarHostState,
                    onClick = {
                        if (userProfile != null) {
                            // Get current water progress for realistic test
                            coroutineScope.launch {
                                val currentProgress = try {
                                    waterIntakeRepository.getTodayProgress().first()
                                } catch (e: Exception) {
                                    // Fallback progress if repository fails
                                    WaterProgress(
                                        currentIntake = 1134.0,
                                        dailyGoal = userProfile.dailyWaterGoal,
                                        progress = 0.42f,
                                        isGoalAchieved = false,
                                        remainingAmount = userProfile.dailyWaterGoal - 1134.0
                                    )
                                }

                                val notificationService = HydroNotificationService(context)
                                notificationService.showTestNotification(userProfile, currentProgress)
                            }
                        } else {
                            // Fallback if no user profile
                            val notificationService = HydroNotificationService(context)
                            val fallbackProfile = UserProfile(
                                name = "Test User",
                                gender = Gender.MALE,
                                ageGroup = AgeGroup.ADULT_31_50,
                                activityLevel = ActivityLevel.MODERATE,
                                wakeUpTime = "07:00",
                                sleepTime = "23:00",
                                dailyWaterGoal = 2700.0,
                                reminderInterval = 120,
                                isOnboardingCompleted = true
                            )
                            val fallbackProgress = WaterProgress(
                                currentIntake = 1134.0,
                                dailyGoal = 2700.0,
                                progress = 0.42f,
                                isGoalAchieved = false,
                                remainingAmount = 1566.0
                            )
                            notificationService.showTestNotification(fallbackProfile, fallbackProgress)
                        }
                    },
                    confirmationMessage = "Test notification sent successfully!"
                )

                // Permission Status Button
                DebugInfoButton(
                    title = "Check Notification Permission",
                    description = "Display current notification permission status",
                    icon = Icons.Default.Security,
                    onClick = {
                        coroutineScope.launch {
                            val hasPermission = NotificationPermissionManager.hasNotificationPermission(context)
                            snackbarHostState.showSnackbar(
                                message = "Notification Permission: ${if (hasPermission) "GRANTED" else "DENIED"}",
                                duration = SnackbarDuration.Long
                            )
                        }
                    }
                )

                // Restart Notifications Button
                if (userProfile != null) {
                    DebugActionButton(
                        title = "Restart Notification Schedule",
                        description = "Cancel and reschedule all notifications",
                        icon = Icons.Default.RestartAlt,
                        snackbarHostState = snackbarHostState,
                        onClick = {
                            HydroNotificationScheduler.rescheduleNotifications(context, userProfile)
                        },
                        confirmationMessage = "Notifications rescheduled successfully!"
                    )

                    // Next Notification Info
                    DebugInfoButton(
                        title = "Next Scheduled Notification",
                        description = "Show when the next notification is scheduled",
                        icon = Icons.Default.Schedule,
                        onClick = {
                            coroutineScope.launch {
                                val nextTime = HydroNotificationScheduler.getNextScheduledTime(context, userProfile)
                                val message = if (nextTime != null) {
                                    "Next notification: $nextTime"
                                } else {
                                    "No notification scheduled (may be outside waking hours or goal achieved)"
                                }
                                snackbarHostState.showSnackbar(
                                    message = message,
                                    duration = SnackbarDuration.Long
                                )
                            }
                        }
                    )

                    // Debug System Info Button
                    DebugInfoButton(
                        title = "System Alarm Info",
                        description = "Check if exact alarm scheduling is available",
                        icon = Icons.Default.AlarmOn,
                        onClick = {
                            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                            val canSchedule = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                alarmManager.canScheduleExactAlarms()
                            } else {
                                true
                            }
                            
                            coroutineScope.launch {
                                val message = buildString {
                                    append("Can schedule exact alarms: $canSchedule\n")
                                    append("Android version: ${android.os.Build.VERSION.SDK_INT}\n")
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                        val nextAlarm = alarmManager.nextAlarmClock
                                        if (nextAlarm != null) {
                                            append("Next system alarm: ${java.util.Date(nextAlarm.triggerTime)}")
                                        } else {
                                            append("No system alarms scheduled")
                                        }
                                    }
                                }
                                snackbarHostState.showSnackbar(
                                    message = message,
                                    duration = SnackbarDuration.Long
                                )
                            }
                        }
                    )
                }

                // 1-Minute Debug Interval Button
                if (userProfile != null) {
                    DebugActionButton(
                        title = "Enable 1-Minute Test Interval",
                        description = "Temporarily set notification interval to 1 minute for testing",
                        icon = Icons.Default.Timer,
                        snackbarHostState = snackbarHostState,
                        onClick = {
                            val testProfile = userProfile.copy(reminderInterval = 1)
                            HydroNotificationScheduler.rescheduleNotifications(context, testProfile)
                        },
                        confirmationMessage = "Notifications set to 1-minute intervals! Remember to reset when done testing."
                    )
                }

                // Stop All Notifications Button
                DebugActionButton(
                    title = "Stop All Notifications",
                    description = "Cancel all scheduled notifications and clear notification tray",
                    icon = Icons.Default.NotificationsOff,
                    snackbarHostState = snackbarHostState,
                    onClick = {
                        HydroNotificationScheduler.stopNotifications(context)
                    },
                    confirmationMessage = "All notifications stopped successfully!"
                )

                // Test Stacking Snackbars Button
                DebugActionButton(
                    title = "Test Stacking Snackbars",
                    description = "Show multiple snackbars quickly to test stacking",
                    icon = Icons.Default.Layers,
                    snackbarHostState = snackbarHostState,
                    onClick = {
                        // Show multiple stacked snackbars
                        showStackedSuccessSnackbar("First success message!")
                        showStackedInfoSnackbar("Second info message!")
                        showStackedWarningSnackbar("Third warning message!")
                        showStackedErrorSnackbar("Fourth error message!")
                    },
                    confirmationMessage = "Stacked snackbars shown!"
                )

                // Current Status Card
                if (userProfile != null) {
                    NotificationStatusCard(userProfile = userProfile)
                }
            }
        }
    }
}

@Composable
private fun DebugActionButton(
    title: String,
    description: String,
    icon: ImageVector,
    snackbarHostState: SnackbarHostState,
    onClick: () -> Unit,
    confirmationMessage: String
) {
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "debug_button_press"
    )

    Card(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Show confirmation snackbar
    LaunchedEffect(isPressed) {
        if (isPressed) {
            coroutineScope.launch {
                snackbarHostState.showSuccessSnackbar(
                    message = confirmationMessage
                )
            }
            delay(150)
            isPressed = false
        }
    }
}

@Composable
private fun DebugInfoButton(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun NotificationStatusCard(userProfile: UserProfile) {
    val context = LocalContext.current
    val hasPermission = NotificationPermissionManager.hasNotificationPermission(context)
    val shouldEnable = HydroNotificationScheduler.shouldEnableNotifications(context, userProfile)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Current Notification Status",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Permission: ${if (hasPermission) "Granted" else "Denied"}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Should Enable: ${if (shouldEnable) "Yes" else "No"}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Reminder Interval: ${userProfile.reminderInterval} minutes",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Reminder Style: ${userProfile.reminderStyle.getDisplayName()}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Active Hours: ${userProfile.wakeUpTime} - ${userProfile.sleepTime}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}