package com.cemcakmak.hydrotracker.presentation.settings

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.database.repository.WaterProgress
import com.cemcakmak.hydrotracker.data.models.ActivityLevel
import com.cemcakmak.hydrotracker.data.models.AgeGroup
import com.cemcakmak.hydrotracker.data.models.Gender
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import com.cemcakmak.hydrotracker.health.HealthConnectManager
import com.cemcakmak.hydrotracker.health.HealthConnectSyncManager
import com.cemcakmak.hydrotracker.notifications.HydroNotificationScheduler
import com.cemcakmak.hydrotracker.notifications.HydroNotificationService
import com.cemcakmak.hydrotracker.notifications.NotificationPermissionManager
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val HC_DEBUG_TAG = "HealthConnectDebug"

/**
 * Developer / debug tools
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeveloperOptionsScreen(
    wasPop: Boolean = false,
    userProfile: UserProfile? = null,
    userRepository: UserRepository? = null,
    waterIntakeRepository: WaterIntakeRepository? = null,
    onNavigateBack: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToHapticsTest: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val isPreview = LocalInspectionMode.current
    val shouldApplyDepth = !isPreview && wasPop

    val blur by if (shouldApplyDepth) {
        val animatedContentScope = LocalNavAnimatedContentScope.current
        animatedContentScope.transition.animateDp(
            transitionSpec = { tween(400) },
            label = "developerOptionsEnterBlur"
        ) { state ->
            if (state == EnterExitState.PreEnter) 8.dp else 0.dp
        }
    } else {
        remember { mutableStateOf(0.dp) }
    }

    // Depth scrim that clears in sync with the blur as the page comes forward.
    val scrimAlpha by if (shouldApplyDepth) {
        val animatedContentScope = LocalNavAnimatedContentScope.current
        animatedContentScope.transition.animateFloat(
            transitionSpec = { tween(400) },
            label = "developerOptionsEnterScrim"
        ) { state ->
            if (state == EnterExitState.PreEnter) 0.4f else 0f
        }
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val scrimColor = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        Color.White
    } else {
        Color.Black
    }

    // Title of the action currently running, drives its inline spinner. Only one at a time.
    var busy by remember { mutableStateOf<String?>(null) }

    var showResetDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showInjectDialog by remember { mutableStateOf(false) }
    var injectDays by remember { mutableIntStateOf(30) }
    var isInjecting by remember { mutableStateOf(false) }

    // Runs [block] off the click, toasts its returned message (or the error), and clears the spinner.
    fun runWithToast(key: String, block: suspend () -> String) {
        if (busy != null) return
        busy = key
        scope.launch {
            val message = try {
                block()
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            busy = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().blur(blur)) {
            SettingsDetailScaffold(
                title = "Developer Options",
                onNavigateBack = onNavigateBack
            ) {
                // Data
                DevSection("Data", modifier = Modifier.padding(top = 24.dp)) {
                    DeveloperActionCard(
                        index = 0,
                        size = 3,
                        title = "Reset Onboarding",
                        description = "Clear user data and restart onboarding",
                        icon = ImageVector.vectorResource(R.drawable.reset_exposure_filled),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            showResetDialog = true
                        }
                    )
                    DeveloperActionCard(
                        index = 1,
                        size = 3,
                        title = "Clear All Data",
                        description = "Remove all stored preferences and water data",
                        icon = ImageVector.vectorResource(R.drawable.delete_fill),
                        isLoading = busy == "Clear All Data",
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            showClearDialog = true
                        }
                    )
                    DeveloperActionCard(
                        index = 2,
                        size = 3,
                        title = "Inject Sample Data",
                        description = "Generate realistic intake history",
                        icon = ImageVector.vectorResource(R.drawable.data_object_filled),
                        showChevron = true,
                        isLoading = isInjecting,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            showInjectDialog = true
                        }
                    )
                }

                // Haptics
                DevSection("Haptics") {
                    DeveloperActionCard(
                        index = 0,
                        size = 1,
                        title = "Test Haptics Engine",
                        description = "Compare system and custom haptic feedback",
                        icon = ImageVector.vectorResource(R.drawable.mobile_vibrate_filled),
                        showChevron = true,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onNavigateToHapticsTest()
                        }
                    )
                }

                // Health Connect testing (only when supported AND sync enabled)
                if (isPreview ||
                    (HealthConnectManager.isVersionSupported() && userProfile?.healthConnectSyncEnabled == true)
                ) {
                    DevSection("Health Connect testing") {
                        val hcActions = listOf(
                            DevAction(
                                title = "Test Write",
                                description = "Write a 250 ml test entry to Health Connect",
                                icon = ImageVector.vectorResource(R.drawable.cloud_upload_filled)
                            ) {
                                runWithToast("Test Write") {
                                    val entry = WaterIntakeEntry(
                                        amount = 250.0,
                                        timestamp = System.currentTimeMillis(),
                                        date = LocalDate.now().toString(),
                                        containerType = "Debug Test",
                                        containerVolume = 250.0,
                                        note = "Health Connect Debug Test Entry"
                                    )
                                    val result = HealthConnectManager.writeHydrationRecord(context, entry)
                                    result.fold(
                                        onSuccess = {
                                            Log.i(HC_DEBUG_TAG, "Test write result: $it")
                                            "Health Connect write succeeded"
                                        },
                                        onFailure = {
                                            Log.e(HC_DEBUG_TAG, "Test write failed", it)
                                            "Health Connect write failed: ${it.message}"
                                        }
                                    )
                                }
                            },
                            DevAction(
                                title = "Test Read",
                                description = "Read recent hydration records from Health Connect",
                                ImageVector.vectorResource(R.drawable.cloud_download_filled)
                            ) {
                                runWithToast("Test Read") {
                                    val since = Instant.now().minus(1, ChronoUnit.DAYS)
                                    val result = HealthConnectManager.readHydrationRecords(context, since)
                                    result.fold(
                                        onSuccess = { records ->
                                            Log.i(HC_DEBUG_TAG, "Found ${records.size} records since yesterday")
                                            records.forEach { record ->
                                                Log.d(HC_DEBUG_TAG, "Record: ${record.volume.inMilliliters}ml at ${record.startTime}")
                                            }
                                            "Read ${records.size} record(s) from Health Connect"
                                        },
                                        onFailure = {
                                            Log.e(HC_DEBUG_TAG, "Test read failed", it)
                                            "Health Connect read failed: ${it.message}"
                                        }
                                    )
                                }
                            },
                            DevAction(
                                title = "Test Import",
                                description = "Import external hydration data (last 7 days)",
                                icon = ImageVector.vectorResource(R.drawable.directory_sync_filled)
                            ) {
                                if (busy != null) return@DevAction
                                if (userRepository == null || waterIntakeRepository == null) {
                                    Toast.makeText(context, "Import unavailable: repositories missing", Toast.LENGTH_LONG).show()
                                    return@DevAction
                                }
                                busy = "Test Import"
                                val since = Instant.now().minus(7, ChronoUnit.DAYS)
                                Log.i(HC_DEBUG_TAG, "🔄 Starting import test for last 7 days...")
                                HealthConnectSyncManager.importExternalHydrationData(context, userRepository, waterIntakeRepository, since) { imported, errors ->
                                    Log.i(HC_DEBUG_TAG, "📊 Import test result: $imported imported, $errors errors")
                                    scope.launch {
                                        Toast.makeText(
                                            context,
                                            "Import complete: $imported imported, $errors error(s)",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        busy = null
                                    }
                                }
                            },
                            DevAction(
                                title = "Check Status",
                                description = "Verify Health Connect availability and permissions",
                                icon = ImageVector.vectorResource(R.drawable.health_and_safety_filled)
                            ) {
                                runWithToast("Check Status") {
                                    val available = HealthConnectManager.isAvailable(context)
                                    val hasPermissions = HealthConnectManager.hasPermissions(context)
                                    val status = HealthConnectManager.getStatusMessage(context)
                                    Log.i(HC_DEBUG_TAG, "=== Health Connect Status Check ===")
                                    Log.i(HC_DEBUG_TAG, "Available: $available")
                                    Log.i(HC_DEBUG_TAG, "Has Permissions: $hasPermissions")
                                    Log.i(HC_DEBUG_TAG, "Status: $status")
                                    HealthConnectManager.debugPermissions()
                                    "$status (available: $available, permissions: $hasPermissions)"
                                }
                            }
                        )
                        DevActionList(hcActions, busy)
                    }
                }

                // Notifications
                DevSection("Notifications") {
                    val notifActions = buildList {
                        add(
                            DevAction(
                                title = "Send Test Notification",
                                description = "Show a real hydration reminder",
                                icon = ImageVector.vectorResource(R.drawable.notifications_filled)
                            ) {
                                runWithToast("Send Test Notification") {
                                    val profile = userProfile ?: fallbackProfile()
                                    val progress = try {
                                        waterIntakeRepository?.getTodayProgress()?.first() ?: fallbackProgress(profile)
                                    } catch (_: Exception) {
                                        fallbackProgress(profile)
                                    }
                                    HydroNotificationService(context).showTestNotification(profile, progress)
                                    "Test notification sent"
                                }
                            }
                        )
                        add(
                            DevAction(
                                title = "Check Notification Permission",
                                description = "Display current permission status",
                                icon = ImageVector.vectorResource(R.drawable.security_filled)
                            ) {
                                runWithToast("Check Notification Permission") {
                                    val granted = NotificationPermissionManager.hasNotificationPermission(context)
                                    "Notification permission: ${if (granted) "GRANTED" else "DENIED"}"
                                }
                            }
                        )
                        if (userProfile != null) {
                            add(
                                DevAction(
                                    title = "Restart Notification Schedule",
                                    description = "Cancel and reschedule all notifications",
                                    icon = ImageVector.vectorResource(R.drawable.restart_alt_filled)
                                ) {
                                    runWithToast("Restart Notification Schedule") {
                                        HydroNotificationScheduler.rescheduleNotifications(context, userProfile)
                                        "Notifications rescheduled"
                                    }
                                }
                            )
                            add(
                                DevAction(
                                    title = "Next Scheduled Notification",
                                    description = "Show when the next reminder will fire",
                                    icon = ImageVector.vectorResource(R.drawable.schedule_filled)
                                ) {
                                    runWithToast("Next Scheduled Notification") {
                                        val nextTime = HydroNotificationScheduler.getNextScheduledTime(context, userProfile)
                                        nextTime?.let { "Next notification: $it" }
                                            ?: "No notification scheduled (outside waking hours or goal achieved)"
                                    }
                                }
                            )
                        }
                        add(
                            DevAction(
                                title = "System Alarm Info",
                                description = "Check if exact alarm scheduling is available",
                                icon = ImageVector.vectorResource(R.drawable.alarm_on_filled)
                            ) {
                                runWithToast("System Alarm Info") {
                                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                    val canSchedule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        alarmManager.canScheduleExactAlarms()
                                    } else true
                                    val nextAlarm = alarmManager.nextAlarmClock
                                    buildString {
                                        append("Exact alarms: ${if (canSchedule) "allowed" else "blocked"}")
                                        append(" • API ${Build.VERSION.SDK_INT}")
                                        if (nextAlarm != null) {
                                            append(" • next system alarm ${java.util.Date(nextAlarm.triggerTime)}")
                                        } else {
                                            append(" • no system alarms")
                                        }
                                    }
                                }
                            }
                        )
                        if (userProfile != null) {
                            add(
                                DevAction(
                                    title = "Enable 1-Minute Test Interval",
                                    description = "Temporarily fire reminders every minute",
                                    icon = ImageVector.vectorResource(R.drawable.timer_filled)
                                ) {
                                    runWithToast("Enable 1-Minute Test Interval") {
                                        val testProfile = userProfile.copy(reminderInterval = 1)
                                        HydroNotificationScheduler.rescheduleNotifications(context, testProfile)
                                        "1-minute interval enabled. Remember to reset when done."
                                    }
                                }
                            )
                        }
                        add(
                            DevAction(
                                title = "Stop All Notifications",
                                description = "Cancel all scheduled notifications",
                                icon = ImageVector.vectorResource(R.drawable.notifications_paused_filled)
                            ) {
                                runWithToast("Stop All Notifications") {
                                    HydroNotificationScheduler.stopNotifications(context)
                                    "All notifications stopped"
                                }
                            }
                        )
                    }
                    DevActionList(notifActions, busy)
                }

                // Status
                DevSection("Status") {
                    val onboardingCompleted = userProfile?.isOnboardingCompleted == true
                    StatusInfoCard(
                        index = 0,
                        size = if (userProfile != null) 2 else 1,
                        title = "App state",
                        lines = listOf(
                            "Onboarding completed" to onboardingCompleted.toString(),
                            "User profile exists" to (userProfile != null).toString(),
                            "Daily goal" to (userProfile?.let { "${it.dailyWaterGoal.toInt()} ml" } ?: "—")
                        )
                    )
                    if (userProfile != null) {
                        val permission = if (isPreview) "—" else
                            if (NotificationPermissionManager.hasNotificationPermission(context)) "Granted" else "Denied"
                        val shouldEnable = if (isPreview) "—" else
                            HydroNotificationScheduler.shouldEnableNotifications(context, userProfile).toString()
                        StatusInfoCard(
                            index = 1,
                            size = 2,
                            title = "Notification state",
                            lines = listOf(
                                "Permission" to permission,
                                "Should enable" to shouldEnable,
                                "Reminder interval" to "${userProfile.reminderInterval} min",
                                "Reminder style" to userProfile.reminderStyle.getDisplayName(),
                                "Active hours" to "${userProfile.wakeUpTime} – ${userProfile.sleepTime}"
                            )
                        )
                    }
                }
            }
        }

        if (scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.3f)
                    .background(scrimColor.copy(alpha = scrimAlpha))
            )
        }
    }

    // Dialogs
    if (showResetDialog) {
        ConfirmActionDialog(
            title = "Reset onboarding?",
            message = "This clears your profile and preferences, then restarts the onboarding flow.",
            confirmLabel = "Reset",
            onConfirm = {
                showResetDialog = false
                userRepository?.resetOnboarding()
                onNavigateToOnboarding()
            },
            onDismiss = { showResetDialog = false }
        )
    }

    if (showClearDialog) {
        ConfirmActionDialog(
            title = "Clear all data?",
            message = "This permanently removes all stored preferences and water intake data. This cannot be undone.",
            confirmLabel = "Clear",
            onConfirm = {
                showClearDialog = false
                runWithToast("Clear All Data") {
                    userRepository?.clearUserProfile()
                    waterIntakeRepository?.clearAllData()
                    "All data cleared"
                }
            },
            onDismiss = { showClearDialog = false }
        )
    }

    if (showInjectDialog) {
        InjectSampleDataDialog(
            selectedDays = injectDays,
            onDaysChange = { injectDays = it },
            isInjecting = isInjecting,
            onConfirm = {
                if (waterIntakeRepository == null) {
                    Toast.makeText(context, "Injection unavailable: repository missing", Toast.LENGTH_LONG).show()
                    showInjectDialog = false
                } else {
                    isInjecting = true
                    scope.launch {
                        val result = try {
                            waterIntakeRepository.injectDebugData(injectDays)
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                        isInjecting = false
                        showInjectDialog = false
                        val message = if (result.isSuccess) {
                            "Injected $injectDays days of sample data"
                        } else {
                            "Error: ${result.exceptionOrNull()?.message}"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDismiss = { showInjectDialog = false }
        )
    }
}

@Composable
private fun DevSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionHeader(title)
        Column(content = content)
    }
}

private class DevAction(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val showChevron: Boolean = false,
    val onClick: () -> Unit
)

@Composable
private fun DevActionList(actions: List<DevAction>, busy: String?) {
    actions.forEachIndexed { index, action ->
        DeveloperActionCard(
            index = index,
            size = actions.size,
            title = action.title,
            description = action.description,
            icon = action.icon,
            showChevron = action.showChevron,
            isLoading = busy == action.title,
            onClick = action.onClick
        )
    }
}

@Composable
private fun DeveloperActionCard(
    index: Int,
    size: Int,
    title: String,
    description: String,
    icon: ImageVector,
    isLoading: Boolean = false,
    showChevron: Boolean = false,
    onClick: () -> Unit
) {
    SettingsGroupCard(index = index, size = size, onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusInfoCard(
    index: Int,
    size: Int,
    title: String,
    lines: List<Pair<String, String>>
) {
    SettingsGroupCard(index = index, size = size) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            lines.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onConfirm()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onDismiss()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InjectSampleDataDialog(
    selectedDays: Int,
    onDaysChange: (Int) -> Unit,
    isInjecting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    AlertDialog(
        onDismissRequest = { if (!isInjecting) onDismiss() },
        title = { Text("Inject sample data") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Generate realistic water intake history. Choose a range:")
                val options = listOf(30 to "Last 30 days", 90 to "Last 90 days")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEachIndexed { index, (value, label) ->
                        SelectableOptionCard(
                            index = index,
                            size = options.size,
                            selected = selectedDays == value,
                            unselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            tonalElevation = 0.dp,
                            onClick = {
                                if (!isInjecting) {
                                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                                    onDaysChange(value)
                                }
                            }
                        ) { contentColor ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onConfirm()
                },
                enabled = !isInjecting
            ) {
                if (isInjecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Inject")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (!isInjecting) {
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onDismiss()
                    }
                },
                enabled = !isInjecting
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun fallbackProfile(): UserProfile = UserProfile(
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

private fun fallbackProgress(profile: UserProfile): WaterProgress = WaterProgress(
    currentIntake = 1134.0,
    dailyGoal = profile.dailyWaterGoal,
    progress = 0.42f,
    isGoalAchieved = false,
    remainingAmount = (profile.dailyWaterGoal - 1134.0).coerceAtLeast(0.0)
)

@Preview(showBackground = true)
@Composable
fun DeveloperOptionsScreenPreview() {
    val sampleProfile = UserProfile(
        name = "Preview",
        gender = Gender.MALE,
        ageGroup = AgeGroup.ADULT_31_50,
        activityLevel = ActivityLevel.MODERATE,
        wakeUpTime = "07:00",
        sleepTime = "23:00",
        dailyWaterGoal = 2500.0,
        reminderInterval = 60,
        isOnboardingCompleted = true,
        healthConnectSyncEnabled = true
    )
    HydroTrackerTheme {
        DeveloperOptionsScreen(userProfile = sampleProfile)
    }
}
