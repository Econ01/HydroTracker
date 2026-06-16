package com.cemcakmak.hydrotracker.presentation.settings.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cemcakmak.hydrotracker.R
import com.cemcakmak.hydrotracker.data.models.ActivityLevel
import com.cemcakmak.hydrotracker.data.models.Gender
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import com.cemcakmak.hydrotracker.notifications.HydroNotificationScheduler
import com.cemcakmak.hydrotracker.presentation.common.BlurMorph
import com.cemcakmak.hydrotracker.presentation.settings.SettingsDetailScaffold
import com.cemcakmak.hydrotracker.presentation.settings.SettingsGroupCard
import com.cemcakmak.hydrotracker.presentation.settings.SettingsSectionHeader
import com.cemcakmak.hydrotracker.ui.theme.HydroTrackerTheme
import com.cemcakmak.hydrotracker.utils.WaterCalculator
import kotlinx.coroutines.launch

/**
 * Profile settings screen
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileSettingsScreen(
    userProfile: UserProfile,
    themePreferences: ThemePreferences,
    userRepository: UserRepository,
    todayEntryCount: Int,
    daysTracked: Int,
    todayGoalProgress: Float,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var showGenderSheet by remember { mutableStateOf(false) }
    var showActivitySheet by remember { mutableStateOf(false) }
    var showGoalSheet by remember { mutableStateOf(false) }
    var showPhotoSheet by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }

    fun saveProfile(updatedProfile: UserProfile) {
        coroutineScope.launch {
            val needsNotificationReschedule =
                userProfile.wakeUpTime != updatedProfile.wakeUpTime ||
                userProfile.sleepTime != updatedProfile.sleepTime ||
                userProfile.reminderInterval != updatedProfile.reminderInterval

            userRepository.saveUserProfile(updatedProfile)

            if (needsNotificationReschedule && updatedProfile.isOnboardingCompleted) {
                HydroNotificationScheduler.rescheduleNotifications(context, updatedProfile)
            }
        }
    }

    SettingsDetailScaffold(
        title = stringResource(R.string.nav_profile),
        onNavigateBack = onNavigateBack
    ) {
        ProfileHeroPreview(
            userProfile = userProfile,
            themePreferences = themePreferences,
            todayEntryCount = todayEntryCount,
            daysTracked = daysTracked,
            todayGoalProgress = todayGoalProgress,
            onEditProfilePicture = { showPhotoSheet = true },
            onEditUsername = { showUsernameDialog = true }
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSectionHeader(
                stringResource(R.string.profile_section_personal)
            )

            Column {
                SettingsGroupCard(
                    index = 0,
                    size = 1,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        showGenderSheet = true
                    }
                ) {
                    ProfileSettingRow(
                        icon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.person_filled),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        title = stringResource(R.string.profile_label_gender),
                        value = stringResource(userProfile.gender.labelResId)
                    )
                }
            }
        }


        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsSectionHeader(
                stringResource(R.string.profile_section_daily_goals)
            )

            Column {
                SettingsGroupCard(
                    index = 0,
                    size = 2,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        showGoalSheet = true
                    }
                ) {
                    ProfileSettingRow(
                        icon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.water_drop_filled),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        title = stringResource(R.string.profile_label_daily_goal),
                        value = WaterCalculator.formatWaterAmount(
                            context,
                            userProfile.dailyWaterGoal,
                            userProfile.volumeUnit
                        )
                    )
                }

                SettingsGroupCard(
                    index = 1,
                    size = 2,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        showActivitySheet = true
                    }
                ) {
                    ProfileSettingRow(
                        icon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.fitness_center_filled),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        title = stringResource(R.string.profile_label_activity_level),
                        value = stringResource(userProfile.activityLevel.labelResId)
                    )
                }
            }
        }
    }

    if (showGenderSheet) {
        GenderBottomSheet(
            currentGender = userProfile.gender,
            onGenderChange = { newGender ->
                if (newGender != userProfile.gender) {
                    val newGoal = WaterCalculator.calculateDailyWaterGoal(
                        gender = newGender,
                        activityLevel = userProfile.activityLevel,
                        weight = userProfile.weight,
                        hydrationStandard = userProfile.hydrationStandard
                    )
                    saveProfile(
                        userProfile.copy(
                            gender = newGender,
                            dailyWaterGoal = newGoal
                        )
                    )
                }
            },
            onDismiss = { showGenderSheet = false }
        )
    }

    if (showActivitySheet) {
        ActivityLevelBottomSheet(
            currentLevel = userProfile.activityLevel,
            onActivityLevelChange = { newLevel ->
                if (newLevel != userProfile.activityLevel) {
                    val newGoal = WaterCalculator.calculateDailyWaterGoal(
                        gender = userProfile.gender,
                        activityLevel = newLevel,
                        weight = userProfile.weight,
                        hydrationStandard = userProfile.hydrationStandard
                    )
                    saveProfile(
                        userProfile.copy(
                            activityLevel = newLevel,
                            dailyWaterGoal = newGoal
                        )
                    )
                }
            },
            onDismiss = { showActivitySheet = false }
        )
    }

    if (showGoalSheet) {
        DailyGoalBottomSheet(
            currentGoalMl = userProfile.dailyWaterGoal,
            volumeUnit = userProfile.volumeUnit,
            onGoalChange = { newGoalMl ->
                if (newGoalMl != userProfile.dailyWaterGoal) {
                    saveProfile(userProfile.copy(dailyWaterGoal = newGoalMl))
                }
            },
            onDismiss = { showGoalSheet = false }
        )
    }

    if (showPhotoSheet) {
        UpdateProfilePictureBottomSheet(
            onImageSelected = { uri ->
                saveProfile(userProfile.copy(profileImagePath = uri?.toString()))
                showPhotoSheet = false
            },
            onDismiss = { showPhotoSheet = false }
        )
    }

    if (showUsernameDialog) {
        UsernameEditDialog(
            currentName = userProfile.name,
            onDismiss = { showUsernameDialog = false },
            onConfirm = { newName ->
                if (newName != userProfile.name) {
                    saveProfile(userProfile.copy(name = newName))
                }
                showUsernameDialog = false
            }
        )
    }
}

@Composable
private fun ProfileSettingRow(
    icon: @Composable () -> Unit,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            BlurMorph(targetState = value) { state, blurModifier ->
                Text(
                    modifier = blurModifier,
                    text = state,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.keyboard_arrow_up_filled),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val previewProfile = UserProfile(
    name = "Preview User",
    gender = Gender.MALE,
    ageGroup = com.cemcakmak.hydrotracker.data.models.AgeGroup.YOUNG_ADULT_18_30,
    activityLevel = ActivityLevel.MODERATE,
    wakeUpTime = "07:00",
    sleepTime = "23:00",
    dailyWaterGoal = 2500.0,
    reminderInterval = 120
)

@Preview(showBackground = true, name = "Profile Settings - Light")
@Composable
fun ProfileSettingsScreenPreviewLight() {
    HydroTrackerTheme {
        ProfileSettingsScreen(
            userProfile = previewProfile,
            themePreferences = ThemePreferences(),
            userRepository = UserRepository(LocalContext.current),
            todayEntryCount = 5,
            daysTracked = 12,
            todayGoalProgress = 0.65f,
            onNavigateBack = {}
        )
    }
}
