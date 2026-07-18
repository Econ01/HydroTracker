package com.cemcakmak.hydrotracker.presentation.onboarding

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import com.cemcakmak.hydrotracker.R
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.cemcakmak.hydrotracker.data.models.*
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import com.cemcakmak.hydrotracker.notifications.NotificationPermissionManager
import com.cemcakmak.hydrotracker.notifications.HydroNotificationScheduler
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import com.cemcakmak.hydrotracker.utils.WaterCalculator
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel for managing onboarding flow state and user profile creation
 * Integrated with UserRepository for data persistence and notification system
 */
class OnboardingViewModel(
    application: Application,
    private val userRepository: UserRepository
) : AndroidViewModel(application) {

    // Current onboarding step
    private val _currentStep = MutableStateFlow(OnboardingStep.WELCOME)
    val currentStep: StateFlow<OnboardingStep> = _currentStep.asStateFlow()

    // User profile being built during onboarding
    private val _userProfile = MutableStateFlow(
        UserProfile(
            id = 1,
            name = "", // Will be set in profile setup
            profileImagePath = null,
            gender = Gender.MALE, // Default values, will be updated
            ageGroup = AgeGroup.ADULT_31_50,
            activityLevel = ActivityLevel.MODERATE,
            wakeUpTime = "07:00",
            sleepTime = "23:00",
            dailyWaterGoal = 2700.0, // Will be calculated
            reminderInterval = 120,
            isOnboardingCompleted = false
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // Animation and UI state
    private val _isAnimating = MutableStateFlow(false)
    val isAnimating: StateFlow<Boolean> = _isAnimating.asStateFlow()

    // Navigation state
    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _canGoNext = MutableStateFlow(false)
    val canGoNext: StateFlow<Boolean> = _canGoNext.asStateFlow()

    // Completion state
    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    init {
        updateNavigationState()
    }

    /** Navigate to next onboarding step with animation */
    fun nextStep() {
        if (!_canGoNext.value || _isAnimating.value) return

        viewModelScope.launch {
            _isAnimating.value = true

            val nextStep = when (_currentStep.value) {
                OnboardingStep.WELCOME -> OnboardingStep.GENDER
                OnboardingStep.GENDER -> OnboardingStep.AGE
                OnboardingStep.AGE -> OnboardingStep.ACTIVITY
                OnboardingStep.ACTIVITY -> OnboardingStep.SCHEDULE
                OnboardingStep.SCHEDULE -> OnboardingStep.PROFILE_SETUP
                OnboardingStep.PROFILE_SETUP -> OnboardingStep.GOAL
                OnboardingStep.GOAL -> OnboardingStep.COMPLETE
                OnboardingStep.COMPLETE -> OnboardingStep.COMPLETE // Stay at final step
            }

            // Calculate water goal when moving to goal step
            if (nextStep == OnboardingStep.GOAL) {
                calculateWaterGoal()
                Log.d("OnboardingViewModel", "Calculated water goal: ${_userProfile.value.dailyWaterGoal}")
                Log.d("OnboardingViewModel", "Calculated reminder interval: ${_userProfile.value.reminderInterval}")
            }

            _currentStep.value = nextStep
            updateNavigationState()

            // Simulate animation duration
            kotlinx.coroutines.delay(300.milliseconds)
            _isAnimating.value = false
        }
    }

    /** Navigate to previous onboarding step */
    fun previousStep() {
        if (!_canGoBack.value || _isAnimating.value) return

        viewModelScope.launch {
            _isAnimating.value = true

            val previousStep = when (_currentStep.value) {
                OnboardingStep.WELCOME -> OnboardingStep.WELCOME // Stay at first step
                OnboardingStep.GENDER -> OnboardingStep.WELCOME
                OnboardingStep.AGE -> OnboardingStep.GENDER
                OnboardingStep.ACTIVITY -> OnboardingStep.AGE
                OnboardingStep.SCHEDULE -> OnboardingStep.ACTIVITY
                OnboardingStep.PROFILE_SETUP -> OnboardingStep.SCHEDULE
                OnboardingStep.GOAL -> OnboardingStep.PROFILE_SETUP
                OnboardingStep.COMPLETE -> OnboardingStep.GOAL
            }

            _currentStep.value = previousStep
            updateNavigationState()

            kotlinx.coroutines.delay(300.milliseconds)
            _isAnimating.value = false
        }
    }

    /** Update user's gender selection */
    fun updateGender(gender: Gender) {
        _userProfile.value = _userProfile.value.copy(gender = gender)
        updateNavigationState()
    }

    /** Update user's age group selection */
    fun updateAgeGroup(ageGroup: AgeGroup) {
        _userProfile.value = _userProfile.value.copy(ageGroup = ageGroup)
        updateNavigationState()
    }

    /** Update user's activity level */
    fun updateActivityLevel(activityLevel: ActivityLevel) {
        _userProfile.value = _userProfile.value.copy(activityLevel = activityLevel)
        updateNavigationState()
    }

    /** Update wake-up time */
    fun updateWakeUpTime(time: String) {
        _userProfile.value = _userProfile.value.copy(wakeUpTime = time)
        updateNavigationState()
    }

    /** Update sleep time */
    fun updateSleepTime(time: String) {
        _userProfile.value = _userProfile.value.copy(sleepTime = time)
        updateNavigationState()
    }

    /** Update user's name */
    fun updateName(name: String) {
        _userProfile.value = _userProfile.value.copy(name = name)
        updateNavigationState()
    }

    /** Update user's profile image */
    fun updateProfileImage(uri: android.net.Uri?) {
        _userProfile.value = _userProfile.value.copy(profileImagePath = uri?.toString())
        updateNavigationState()
    }

    /** Calculate daily water goal and reminder interval based on user profile */
    private fun calculateWaterGoal() {
        val profile = _userProfile.value

        val dailyGoal = WaterCalculator.calculateDailyWaterGoal(
            gender = profile.gender,
            activityLevel = profile.activityLevel,
            weight = profile.weight,
            hydrationStandard = profile.hydrationStandard
        )

        val reminderInterval = WaterCalculator.calculateReminderInterval(
            wakeUpTime = profile.wakeUpTime,
            sleepTime = profile.sleepTime,
            dailyGoal = dailyGoal,
            reminderIntervalMode = profile.reminderIntervalMode,
            customReminderInterval = profile.customReminderInterval
        )

        _userProfile.value = _userProfile.value.copy(
            dailyWaterGoal = dailyGoal,
            reminderInterval = reminderInterval
        )
    }

    /** Complete onboarding and save user profile */
    fun completeOnboarding() {
        println("DEBUG: OnboardingViewModel.completeOnboarding() called")

        viewModelScope.launch {
            val context = getApplication<Application>()
            val completedProfile = _userProfile.value.copy(
                isOnboardingCompleted = true,
                volumeUnit = VolumeUnitConverter.defaultUnitForLocale(context.resources.configuration.locales.get(0))
            )

            println("DEBUG: Saving completed profile to repository.")
            // Save to shared repository
            userRepository.saveUserProfile(completedProfile)

            // Update local state
            _userProfile.value = completedProfile
            _isCompleted.value = true

            println("DEBUG: OnboardingViewModel - isCompleted set to: ${_isCompleted.value}")
            println("DEBUG: OnboardingViewModel - Profile completion status: ${completedProfile.isOnboardingCompleted}")

            // NEW: Start notifications if permission is granted
            if (NotificationPermissionManager.hasNotificationPermission(context)) {
                println("DEBUG: Starting notifications after onboarding completion")
                HydroNotificationScheduler.startNotifications(context, completedProfile)
            } else {
                println("DEBUG: Notification permission not granted, skipping notification startup")
            }
        }
    }


    /** Update navigation state based on current step and user input */
    private fun updateNavigationState() {
        val currentStep = _currentStep.value

        _canGoBack.value = currentStep != OnboardingStep.WELCOME

        _canGoNext.value = when (currentStep) {
            OnboardingStep.WELCOME -> true
            OnboardingStep.GENDER -> true // Gender is always valid (has default)
            OnboardingStep.AGE -> true // Age is always valid (has default)
            OnboardingStep.ACTIVITY -> true // Activity is always valid (has default)
            OnboardingStep.SCHEDULE -> true // Times are always valid (have defaults)
            OnboardingStep.PROFILE_SETUP -> _userProfile.value.name.isNotBlank() // Name is required
            OnboardingStep.GOAL -> true
            OnboardingStep.COMPLETE -> false // No, next step after complete
        }
    }

    /** Get progress percentage for the current step */
    fun getProgress(): Float {
        return when (_currentStep.value) {
            OnboardingStep.WELCOME -> 0.0f
            OnboardingStep.GENDER -> 1f / 7f        // ~14.3%
            OnboardingStep.AGE -> 2f / 7f            // ~28.6%
            OnboardingStep.ACTIVITY -> 3f / 7f       // ~42.9%
            OnboardingStep.SCHEDULE -> 4f / 7f       // ~57.1%
            OnboardingStep.PROFILE_SETUP -> 5f / 7f  // ~71.4%
            OnboardingStep.GOAL -> 6f / 7f           // ~85.7%
            OnboardingStep.COMPLETE -> 1.0f          // 100%
        }
    }

    /** String resource for the current step's title. Resolved with stringResource() in the UI. */
    @StringRes
    fun getStepTitleRes(): Int {
        return when (_currentStep.value) {
            OnboardingStep.WELCOME -> R.string.onboarding_welcome_title
            OnboardingStep.GENDER -> R.string.onboarding_gender_title
            OnboardingStep.AGE -> R.string.onboarding_age_title
            OnboardingStep.ACTIVITY -> R.string.onboarding_activity_title
            OnboardingStep.SCHEDULE -> R.string.onboarding_schedule_title
            OnboardingStep.PROFILE_SETUP -> R.string.onboarding_profile_title
            OnboardingStep.GOAL -> R.string.onboarding_goal_title
            OnboardingStep.COMPLETE -> R.string.onboarding_complete_title
        }
    }

    /** String resource for the current step's description. Resolved with stringResource() in the UI. */
    @StringRes
    fun getStepDescriptionRes(): Int {
        return when (_currentStep.value) {
            OnboardingStep.WELCOME -> R.string.onboarding_welcome_description
            OnboardingStep.GENDER -> R.string.onboarding_gender_description
            OnboardingStep.AGE -> R.string.onboarding_age_description
            OnboardingStep.ACTIVITY -> R.string.onboarding_activity_description
            OnboardingStep.SCHEDULE -> R.string.onboarding_schedule_description
            OnboardingStep.PROFILE_SETUP -> R.string.onboarding_profile_description
            OnboardingStep.GOAL -> R.string.onboarding_goal_description
            OnboardingStep.COMPLETE -> R.string.onboarding_complete_description
        }
    }
}