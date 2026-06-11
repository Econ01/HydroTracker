package com.cemcakmak.hydrotracker.presentation.settings

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.text.format.DateFormat
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.DarkModePreference
import com.cemcakmak.hydrotracker.data.models.ReminderStyle
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.notifications.HydroNotificationScheduler
import com.cemcakmak.hydrotracker.notifications.NotificationContentProvider
import com.cemcakmak.hydrotracker.notifications.NotificationPermissionManager
import com.cemcakmak.hydrotracker.presentation.common.BlurMorph
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.utils.WaterCalculator
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotificationsScreen(
    wasPop: Boolean = false,
    onNavigateToReminderInterval: () -> Unit = {},
    themePreferences: ThemePreferences = ThemePreferences(),
    userProfile: UserProfile? = null,
    onNavigateBack: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onUserProfileUpdate: (UserProfile) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    
    val isPreview = LocalInspectionMode.current
    val shouldApplyDepth = !isPreview && wasPop

    val blur by if (shouldApplyDepth) {
        val animatedContentScope = LocalNavAnimatedContentScope.current
        animatedContentScope.transition.animateDp(
            transitionSpec = { tween(400) },
            label = "notificationsEnterBlur"
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
            label = "notificationsEnterBlur"
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

        // Permission state
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var hasNotificationPermission by remember {
        mutableStateOf(if (isPreview) true else NotificationPermissionManager.hasNotificationPermission(context))
    }
    var hasExactAlarmPermission by remember {
        mutableStateOf(if (isPreview) true else NotificationPermissionManager.hasExactAlarmPermission(context))
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
        if (isPreview) return@DisposableEffect onDispose { }
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
        if (!isPreview) {
            hasNotificationPermission = NotificationPermissionManager.hasNotificationPermission(context)
            hasExactAlarmPermission = NotificationPermissionManager.hasExactAlarmPermission(context)
        }
        isRemindersEnabled = hasNotificationPermission && hasExactAlarmPermission && userProfile?.isOnboardingCompleted == true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().blur(blur)) {
            SettingsDetailScaffold(
                title = stringResource(R.string.screen_notifications_title),
                onNavigateBack = onNavigateBack
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

                // Notification preview
                if (userProfile != null) {
                    NotificationPreviewCard(
                        currentStyle = userProfile.reminderStyle,
                        themePreferences = themePreferences
                    )
                }

                // Reminders toggle
                if (userProfile != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsSectionHeader(stringResource(R.string.notif_reminders_header))
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
                                            ImageVector.vectorResource(R.drawable.notifications)
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.notif_hydration_reminders),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isRemindersEnabled) stringResource(R.string.notif_reminders_active) else stringResource(R.string.notif_reminders_paused),
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
                                        if (!isPreview) {
                                            coroutineScope.launch {
                                                if (enabled) {
                                                    HydroNotificationScheduler.startNotifications(context, userProfile)
                                                } else {
                                                    HydroNotificationScheduler.stopNotifications(context)
                                                }
                                            }
                                        }
                                    },
                                    thumbContent = if (isRemindersEnabled) {
                                        {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.check_filled),
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
                            if (isRemindersEnabled && !isPreview) {
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

                    val nextTime = remember(userProfile, isRemindersEnabled) {
                        when {
                            !isRemindersEnabled -> null
                            isPreview -> "Preview mode"
                            else -> HydroNotificationScheduler.getNextScheduledTime(context, userProfile)
                        }
                    }

                    ScheduleSection(
                        intervalMinutes = userProfile.reminderInterval,
                        nextTime = nextTime,
                        onConfigureClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onNavigateToReminderInterval()
                        }
                    )
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


    // Time pickers
    if (showWakeUpPicker && userProfile != null) {
        TimePickerBottomSheet(
            title = stringResource(R.string.notif_wake_up_time),
            initialTime = userProfile.wakeUpTime,
            onConfirm = { newTime ->
                if (newTime != userProfile.wakeUpTime) {
                    val newInterval = WaterCalculator.calculateReminderInterval(
                        wakeUpTime = newTime,
                        sleepTime = userProfile.sleepTime,
                        dailyGoal = userProfile.dailyWaterGoal,
                        reminderIntervalMode = userProfile.reminderIntervalMode,
                        customReminderInterval = userProfile.customReminderInterval
                    )
                    val updated = userProfile.copy(
                        wakeUpTime = newTime,
                        reminderInterval = newInterval
                    )
                    onUserProfileUpdate(updated)
                    if (isRemindersEnabled && !isPreview) {
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
            title = stringResource(R.string.notif_sleep_time),
            initialTime = userProfile.sleepTime,
            onConfirm = { newTime ->
                if (newTime != userProfile.sleepTime) {
                    val newInterval = WaterCalculator.calculateReminderInterval(
                        wakeUpTime = userProfile.wakeUpTime,
                        sleepTime = newTime,
                        dailyGoal = userProfile.dailyWaterGoal,
                        reminderIntervalMode = userProfile.reminderIntervalMode,
                        customReminderInterval = userProfile.customReminderInterval
                    )
                    val updated = userProfile.copy(
                        sleepTime = newTime,
                        reminderInterval = newInterval
                    )
                    onUserProfileUpdate(updated)
                    if (isRemindersEnabled && !isPreview) {
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
private fun NotificationPreviewCard(
    currentStyle: ReminderStyle,
    themePreferences: ThemePreferences
) {
    val context = LocalContext.current
    val isDark = when (themePreferences.darkMode) {
        DarkModePreference.DARK -> true
        DarkModePreference.LIGHT -> false
        DarkModePreference.SYSTEM -> isSystemInDarkTheme()
    }

    val border = if (themePreferences.usePureBlack && isDark) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    } else {
        null
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        shape = RoundedCornerShape(size = 30.dp),
        tonalElevation = 2.dp,
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BlurMorph(targetState = currentStyle) { style, blurModifier ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(blurModifier),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Style name
                    Text(
                        text = stringResource(style.labelResId),
                        style = MaterialTheme.typography.headlineSmall
                    )

                    // Style description
                    Text(
                        text = when (style) {
                            ReminderStyle.GENTLE -> stringResource(R.string.notif_style_gentle_desc)
                            ReminderStyle.MOTIVATING -> stringResource(R.string.notif_style_motivating_desc)
                            ReminderStyle.MINIMAL -> stringResource(R.string.notif_style_minimal_desc)
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(R.string.notif_sample_notification),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelMediumEmphasized,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            BlurMorph(targetState = currentStyle) { style, blurModifier ->
                val preview = remember(style) {
                    NotificationContentProvider.getNotificationPreview(context, style)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(blurModifier),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = preview.title,
                        style = MaterialTheme.typography.titleSmallEmphasized
                    )
                    Text(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        text = preview.message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(R.string.notif_fun_fact),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelMediumEmphasized,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            BlurMorph(targetState = currentStyle) { style, blurModifier ->
                val preview = remember(style) {
                    NotificationContentProvider.getNotificationPreview(context, style)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(blurModifier),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = preview.extraContent,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
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
        SettingsSectionHeader(stringResource(R.string.notif_permissions_header))
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
                                text = stringResource(R.string.notif_permission_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.notif_permission_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onRequestNotificationPermission()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.action_allow))
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
                                text = stringResource(R.string.notif_exact_alarm_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.notif_exact_alarm_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onRequestExactAlarmPermission()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.action_allow))
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionHeader(stringResource(R.string.notif_reminder_style_header))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReminderStyle.entries.forEach { style ->
                val isSelected = currentStyle == style

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
                        Crossfade(
                            targetState = isSelected,
                            animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
                            label = "darkModeToggleIcon_${style.name}"
                        ) { selected ->
                            Icon(
                                imageVector = when (style) {
                                    ReminderStyle.GENTLE -> if (selected) ImageVector.vectorResource(R.drawable.spa_filled) else ImageVector.vectorResource(R.drawable.spa)
                                    ReminderStyle.MOTIVATING -> if (selected) ImageVector.vectorResource(R.drawable.bolt_filled) else ImageVector.vectorResource(R.drawable.bolt)
                                    ReminderStyle.MINIMAL -> if (selected) ImageVector.vectorResource(R.drawable.abc_filled) else ImageVector.vectorResource(R.drawable.abc)
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = when (style) {
                                ReminderStyle.GENTLE -> stringResource(R.string.notif_style_gentle_short)
                                ReminderStyle.MOTIVATING -> stringResource(R.string.notif_style_motivating_short)
                                ReminderStyle.MINIMAL -> stringResource(R.string.notif_style_minimal_short)
                            },
                            style = if (isSelected) {
                                MaterialTheme.typography.labelLargeEmphasized
                            } else {
                                MaterialTheme.typography.labelLarge
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionHeader(stringResource(R.string.notif_active_hours_header))
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
                        imageVector = ImageVector.vectorResource(R.drawable.sunny_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.notif_wake_up),
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
                        imageVector = ImageVector.vectorResource(R.drawable.bedtime_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.notif_sleep),
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
private fun ScheduleSection(
    intervalMinutes: Int,
    nextTime: String?,
    onConfigureClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SettingsSectionHeader(stringResource(R.string.notif_schedule_header))
        Column {
            val totalSize = if (nextTime != null) 3 else 2

            SettingsGroupCard(
                index = 0,
                size = totalSize,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onConfigureClick()
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
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.notif_configure_schedule),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.notif_configure_schedule_desc),
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

            SettingsGroupCard(index = 1, size = totalSize) {
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
                            text = stringResource(R.string.notif_reminder_interval),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = pluralStringResource(R.plurals.every_x_minutes, intervalMinutes, intervalMinutes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (nextTime != null) {
                SettingsGroupCard(index = 2, size = totalSize) {
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
                                text = stringResource(R.string.notif_scheduled_for),
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerBottomSheet(
    title: String,
    initialTime: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 7
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val timeState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = DateFormat.is24HourFormat(context)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        TimePickerSheetContent(
            title = title,
            timeState = timeState,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerSheetContent(
    title: String,
    timeState: TimePickerState,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    var lastHour by remember { mutableIntStateOf(timeState.hour) }
    var lastMinute by remember { mutableIntStateOf(timeState.minute) }

    LaunchedEffect(timeState.hour) {
        if (timeState.hour != lastHour) {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            lastHour = timeState.hour
        }
    }
    LaunchedEffect(timeState.minute) {
        if (timeState.minute != lastMinute) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastMinute = timeState.minute
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        TimePicker(state = timeState)

        val cancelInteractionSource = remember { MutableInteractionSource() }
        val saveInteractionSource = remember { MutableInteractionSource() }

        LaunchedEffect(cancelInteractionSource) {
            cancelInteractionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    is PressInteraction.Release -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    else -> {  }
                }
            }
        }

        LaunchedEffect(saveInteractionSource) {
            saveInteractionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    is PressInteraction.Release -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    else -> {  }
                }
            }
        }

        ButtonGroup(
            modifier = Modifier.fillMaxWidth(),
            overflowIndicator = {}
        ) {
            val scope = this
            customItem(
                buttonGroupContent = {
                    FilledTonalButton(
                        onClick = {
                            onDismiss()
                        },
                        shapes = ButtonDefaults.shapes(),
                        interactionSource = cancelInteractionSource,
                        modifier = with(scope) {
                            Modifier
                                .weight(1f)
                                .height(56.dp)
                                .animateWidth(interactionSource = cancelInteractionSource)
                        }
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.cancel_filled),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_cancel),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                },
                menuContent = {}
            )

            customItem(
                buttonGroupContent = {
                    Button(
                        onClick = {
                            val formatted = String.format(
                                Locale.getDefault(),
                                "%02d:%02d",
                                timeState.hour,
                                timeState.minute
                            )
                            onConfirm(formatted)
                        },
                        shapes = ButtonDefaults.shapes(),
                        interactionSource = saveInteractionSource,
                        modifier = with(scope) {
                            Modifier
                                .weight(1f)
                                .height(56.dp)
                                .animateWidth(interactionSource = saveInteractionSource)
                        }
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.save_fill),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_save),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                },
                menuContent = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationsScreenPreview() {
    var previewProfile by remember {
        mutableStateOf(
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
        )
    }

    HydroTrackerTheme {
        NotificationsScreen(
            userProfile = previewProfile,
            onUserProfileUpdate = { previewProfile = it },
            onNavigateToReminderInterval = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun WakeUpTimePickerPreview() {
    HydroTrackerTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BottomSheetDefaults.DragHandle()
                val timeState = rememberTimePickerState(
                    initialHour = 7,
                    initialMinute = 0,
                    is24Hour = true
                )
                TimePickerSheetContent(
                    title = "Wake up time",
                    timeState = timeState,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun SleepTimePickerPreview() {
    HydroTrackerTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BottomSheetDefaults.DragHandle()
                val timeState = rememberTimePickerState(
                    initialHour = 23,
                    initialMinute = 0,
                    is24Hour = true
                )
                TimePickerSheetContent(
                    title = "Sleep time",
                    timeState = timeState,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
    }
}
