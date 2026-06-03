package com.cemcakmak.hydrotracker.presentation.settings

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.ReminderStyle
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.notifications.HydroNotificationScheduler
import com.cemcakmak.hydrotracker.notifications.NotificationPermissionManager
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.utils.WaterCalculator
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotificationsScreen(
    userProfile: UserProfile? = null,
    onNavigateBack: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onUserProfileUpdate: (UserProfile) -> Unit = {},
    paddingValues: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val inPreview = LocalInspectionMode.current

    // Permission state
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var hasNotificationPermission by remember {
        mutableStateOf(if (inPreview) true else NotificationPermissionManager.hasNotificationPermission(context))
    }
    var hasExactAlarmPermission by remember {
        mutableStateOf(if (inPreview) true else NotificationPermissionManager.hasExactAlarmPermission(context))
    }

    val allPermissionsGranted = hasNotificationPermission && hasExactAlarmPermission

    // Reminders enabled state (runtime toggle, not persisted)
    var isRemindersEnabled by remember {
        mutableStateOf(
            allPermissionsGranted && userProfile?.isOnboardingCompleted == true
        )
    }

    // Time picker sheet state
    var showWakeUpPicker by remember { mutableStateOf(false) }
    var showSleepPicker by remember { mutableStateOf(false) }

    // Refresh permissions when returning from system settings
    DisposableEffect(Unit) {
        if (inPreview) return@DisposableEffect onDispose { }
        val activity = context as? androidx.activity.ComponentActivity
        val listener = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (activity == context) refreshTrigger++
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        }
        activity?.application?.registerActivityLifecycleCallbacks(listener)
        onDispose { activity?.application?.unregisterActivityLifecycleCallbacks(listener) }
    }

    LaunchedEffect(refreshTrigger, userProfile) {
        if (!inPreview) {
            hasNotificationPermission = NotificationPermissionManager.hasNotificationPermission(context)
            hasExactAlarmPermission = NotificationPermissionManager.hasExactAlarmPermission(context)
        }
        isRemindersEnabled = hasNotificationPermission && hasExactAlarmPermission && userProfile?.isOnboardingCompleted == true
    }

    SettingsDetailScaffold(
        title = "Notifications",
        onNavigateBack = onNavigateBack,
        paddingValues = paddingValues
    ) {
        // Permission status banner
        if (!allPermissionsGranted) {
            PermissionStatusBanner(
                hasNotificationPermission = hasNotificationPermission,
                hasExactAlarmPermission = hasExactAlarmPermission,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onRequestExactAlarmPermission = {
                    NotificationPermissionManager.requestExactAlarmPermission(context)
                }
            )
        }

        // Reminders toggle
        if (userProfile != null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsSectionHeader("Reminders")
                SettingsGroupCard(index = 0, size = 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Crossfade(
                            targetState = isRemindersEnabled,
                            animationSpec = tween(400),
                            label = "reminderIcon"
                        ) { enabled ->
                            Icon(
                                imageVector = if (enabled) {
                                    ImageVector.vectorResource(R.drawable.notifications_filled)
                                } else {
                                    Icons.Default.Notifications
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hydration reminders",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isRemindersEnabled) "Reminders are active" else "Reminders are paused",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isRemindersEnabled,
                            onCheckedChange = { enabled ->
                                val hapticType = if (enabled) {
                                    HapticFeedbackType.ToggleOn
                                } else {
                                    HapticFeedbackType.ToggleOff
                                }
                                haptics.performHapticFeedback(hapticType)
                                isRemindersEnabled = enabled
                                coroutineScope.launch {
                                    if (enabled) {
                                        HydroNotificationScheduler.startNotifications(context, userProfile)
                                    } else {
                                        HydroNotificationScheduler.stopNotifications(context)
                                    }
                                }
                            },
                            thumbContent = if (isRemindersEnabled) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }

            // Reminder style
            ReminderStyleSection(
                currentStyle = userProfile.reminderStyle,
                onStyleChange = { newStyle ->
                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    val updated = userProfile.copy(reminderStyle = newStyle)
                    onUserProfileUpdate(updated)
                    if (isRemindersEnabled) {
                        HydroNotificationScheduler.rescheduleNotifications(context, updated)
                    }
                }
            )

            // Active hours
            ActiveHoursSection(
                wakeUpTime = userProfile.wakeUpTime,
                sleepTime = userProfile.sleepTime,
                onWakeUpClick = { showWakeUpPicker = true },
                onSleepClick = { showSleepPicker = true }
            )

            // Frequency
            FrequencySection(
                intervalMinutes = userProfile.reminderInterval
            )

            // Next reminder
            if (isRemindersEnabled) {
                NextReminderSection(
                    userProfile = userProfile
                )
            }
        }
    }

    // Time pickers
    if (showWakeUpPicker && userProfile != null) {
        TimePickerBottomSheet(
            title = "Wake up time",
            initialTime = userProfile.wakeUpTime,
            onConfirm = { newTime ->
                if (newTime != userProfile.wakeUpTime) {
                    val newInterval = WaterCalculator.calculateReminderInterval(
                        wakeUpTime = newTime,
                        sleepTime = userProfile.sleepTime,
                        dailyGoal = userProfile.dailyWaterGoal
                    )
                    val updated = userProfile.copy(
                        wakeUpTime = newTime,
                        reminderInterval = newInterval
                    )
                    onUserProfileUpdate(updated)
                    if (isRemindersEnabled) {
                        HydroNotificationScheduler.rescheduleNotifications(context, updated)
                    }
                }
                showWakeUpPicker = false
            },
            onDismiss = { showWakeUpPicker = false }
        )
    }

    if (showSleepPicker && userProfile != null) {
        TimePickerBottomSheet(
            title = "Sleep time",
            initialTime = userProfile.sleepTime,
            onConfirm = { newTime ->
                if (newTime != userProfile.sleepTime) {
                    val newInterval = WaterCalculator.calculateReminderInterval(
                        wakeUpTime = userProfile.wakeUpTime,
                        sleepTime = newTime,
                        dailyGoal = userProfile.dailyWaterGoal
                    )
                    val updated = userProfile.copy(
                        sleepTime = newTime,
                        reminderInterval = newInterval
                    )
                    onUserProfileUpdate(updated)
                    if (isRemindersEnabled) {
                        HydroNotificationScheduler.rescheduleNotifications(context, updated)
                    }
                }
                showSleepPicker = false
            },
            onDismiss = { showSleepPicker = false }
        )
    }
}

@Composable
private fun PermissionStatusBanner(
    hasNotificationPermission: Boolean,
    hasExactAlarmPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier.padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionHeader("Permissions")
        Column {
            if (!hasNotificationPermission) {
                SettingsGroupCard(index = 0, size = if (!hasExactAlarmPermission) 2 else 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification permission",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Required to send hydration reminders",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onRequestNotificationPermission()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Allow")
                        }
                    }
                }
            }
            if (!hasExactAlarmPermission) {
                SettingsGroupCard(
                    index = if (!hasNotificationPermission) 1 else 0,
                    size = if (!hasNotificationPermission) 2 else 1
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exact alarm permission",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Required for precise reminder timing",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onRequestExactAlarmPermission()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReminderStyleSection(
    currentStyle: ReminderStyle,
    onStyleChange: (ReminderStyle) -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier.padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionHeader("Reminder style")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReminderStyle.entries.forEach { style ->
                val isSelected = currentStyle == style
                val icon = when (style) {
                    ReminderStyle.GENTLE -> Icons.Default.Spa
                    ReminderStyle.MOTIVATING -> Icons.Default.NotificationsActive
                    ReminderStyle.MINIMAL -> Icons.Default.Schedule
                }
                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = {
                        onStyleChange(style)
                        haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = style.getDisplayName(),
                            style = if (isSelected) {
                                MaterialTheme.typography.labelLargeEmphasized
                            } else {
                                MaterialTheme.typography.labelLarge
                            }
                        )
                        Text(
                            text = when (style) {
                                ReminderStyle.GENTLE -> "Soft & caring"
                                ReminderStyle.MOTIVATING -> "Energetic"
                                ReminderStyle.MINIMAL -> "Simple"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveHoursSection(
    wakeUpTime: String,
    sleepTime: String,
    onWakeUpClick: () -> Unit,
    onSleepClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier.padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionHeader("Active hours")
        Column {
            SettingsGroupCard(
                index = 0,
                size = 2,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onWakeUpClick()
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Wake up",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = wakeUpTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            SettingsGroupCard(
                index = 1,
                size = 2,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onSleepClick()
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sleep",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = sleepTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FrequencySection(
    intervalMinutes: Int
) {
    Column(
        modifier = Modifier.padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionHeader("Frequency")
        SettingsGroupCard(index = 0, size = 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reminder interval",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Every $intervalMinutes minutes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NextReminderSection(
    userProfile: UserProfile
) {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    val nextTime = remember(userProfile) {
        if (inPreview) "Preview mode" else HydroNotificationScheduler.getNextScheduledTime(context, userProfile)
    }

    if (nextTime != null) {
        Column(
            modifier = Modifier.padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsSectionHeader("Next reminder")
            SettingsGroupCard(index = 0, size = 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Scheduled for",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = nextTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerBottomSheet(
    title: String,
    initialTime: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val haptics = LocalHapticFeedback.current
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 7
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val timeState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            TimePicker(state = timeState)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        val formatted = String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            timeState.hour,
                            timeState.minute
                        )
                        onConfirm(formatted)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationsScreenPreview() {
    val previewProfile = remember {
        UserProfile(
            name = "Preview",
            gender = com.cemcakmak.hydrotracker.data.models.Gender.MALE,
            ageGroup = com.cemcakmak.hydrotracker.data.models.AgeGroup.ADULT_31_50,
            activityLevel = com.cemcakmak.hydrotracker.data.models.ActivityLevel.MODERATE,
            wakeUpTime = "07:00",
            sleepTime = "23:00",
            dailyWaterGoal = 2500.0,
            reminderInterval = 60,
            isOnboardingCompleted = true,
            reminderStyle = ReminderStyle.GENTLE
        )
    }

    HydroTrackerTheme {
        NotificationsScreen(
            userProfile = previewProfile,
            onUserProfileUpdate = {}
        )
    }
}
