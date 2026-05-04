package com.cemcakmak.hydrotracker.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.cemcakmak.hydrotracker.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing user data persistence
 */
class UserRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "hydrotracker_prefs",
        Context.MODE_PRIVATE
    )

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(false)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _beveragePreferences = MutableStateFlow(loadBeveragePreferences())
    val beveragePreferences: StateFlow<BeveragePreferences> = _beveragePreferences.asStateFlow()

    init {
        // Add debug logging
        println("UserRepository: Initializing...")
        val currentCompletionStatus = prefs.getBoolean("onboarding_completed", false)
        println("UserRepository: Current onboarding_completed = $currentCompletionStatus")

        // Migration: Ensure existing users have new theme preferences
        migratePreferencesIfNeeded()

        loadUserProfile()
        // Ensure we emit the initial state immediately
        _isOnboardingCompleted.value = prefs.getBoolean("onboarding_completed", false)

        println("UserRepository: Initialization complete. Final status = ${_isOnboardingCompleted.value}")
    }

    /**
     * Migrate SharedPreferences for existing users when new features are added
     */
    private fun migratePreferencesIfNeeded() {
        val isExistingUser = prefs.getBoolean("onboarding_completed", false)

        if (isExistingUser) {
            println("UserRepository: Migrating preferences for existing user...")

            // Migration for usePureBlack (added in version 0.9.3)
            if (!prefs.contains("use_pure_black")) {
                println("UserRepository: Adding missing use_pure_black preference")
                prefs.edit().putBoolean("use_pure_black", false).apply()
            }

            // Migration for hydrationStandard (added in version 0.9.4)
            if (!prefs.contains("hydration_standard")) {
                println("UserRepository: Adding missing hydration_standard preference - defaulting to EFSA")
                prefs.edit().putString("hydration_standard", HydrationStandard.EFSA.name).apply()
            }

            println("UserRepository: Migration complete")
        }
    }

    /**
     * Save user profile to SharedPreferences
     */
    fun saveUserProfile(profile: UserProfile) {
        println("UserRepository: Saving user profile...")
        println("UserRepository: Profile onboarding completed = ${profile.isOnboardingCompleted}")

        prefs.edit().apply {
            putString("name", profile.name)
            putString("gender", profile.gender.name)
            putString("age_group", profile.ageGroup.name)
            putString("activity_level", profile.activityLevel.name)
            putString("wake_up_time", profile.wakeUpTime)
            putString("sleep_time", profile.sleepTime)
            putFloat("daily_water_goal", profile.dailyWaterGoal.toFloat())
            putInt("reminder_interval", profile.reminderInterval)
            putBoolean("onboarding_completed", profile.isOnboardingCompleted)
            putString("reminder_style", profile.reminderStyle.name)
            putString("hydration_standard", profile.hydrationStandard.name)
            putBoolean("health_connect_sync_enabled", profile.healthConnectSyncEnabled)

            // Optional fields
            profile.weight?.let { putFloat("weight", it.toFloat()) }
            profile.preferredThemeColor?.let { putString("preferred_theme_color", it) }
            profile.profileImagePath?.let { putString("profile_image_path", it) }
            putBoolean("use_system_theme", profile.useSystemTheme)

            apply()
        }

        println("UserRepository: Profile saved to SharedPreferences")

        // Immediately update StateFlows
        _userProfile.value = profile
        _isOnboardingCompleted.value = profile.isOnboardingCompleted

        println("UserRepository: StateFlows updated - isOnboardingCompleted = ${_isOnboardingCompleted.value}")

        // Verify the save worked
        val savedValue = prefs.getBoolean("onboarding_completed", false)
        println("UserRepository: Verification - SharedPreferences onboarding_completed = $savedValue")
    }

    /**
     * Load user profile from SharedPreferences
     */
    private fun loadUserProfile() {
        try {
            val isCompleted = prefs.getBoolean("onboarding_completed", false)
            _isOnboardingCompleted.value = isCompleted

            if (isCompleted) {
                val genderName = prefs.getString("gender", null)
                val ageGroupName = prefs.getString("age_group", null)
                val activityLevelName = prefs.getString("activity_level", null)

                if (genderName != null && ageGroupName != null && activityLevelName != null) {
                    val profile = UserProfile(
                        name = prefs.getString("name", "User") ?: "User", // Default name for existing users
                        profileImagePath = prefs.getString("profile_image_path", null),
                        gender = Gender.valueOf(genderName),
                        ageGroup = AgeGroup.valueOf(ageGroupName),
                        activityLevel = ActivityLevel.valueOf(activityLevelName),
                        wakeUpTime = prefs.getString("wake_up_time", "07:00") ?: "07:00",
                        sleepTime = prefs.getString("sleep_time", "23:00") ?: "23:00",
                        dailyWaterGoal = prefs.getFloat("daily_water_goal", 2700f).toDouble(),
                        reminderInterval = prefs.getInt("reminder_interval", 120),
                        isOnboardingCompleted = true,
                        weight = prefs.getFloat("weight", 0f).let { if (it > 0) it.toDouble() else null },
                        preferredThemeColor = prefs.getString("preferred_theme_color", null),
                        useSystemTheme = prefs.getBoolean("use_system_theme", true),
                        reminderStyle = ReminderStyle.valueOf(
                            prefs.getString("reminder_style", ReminderStyle.GENTLE.name) ?: ReminderStyle.GENTLE.name
                        ),
                        hydrationStandard = HydrationStandard.valueOf(
                            prefs.getString("hydration_standard", HydrationStandard.EFSA.name) ?: HydrationStandard.EFSA.name
                        ),
                        healthConnectSyncEnabled = prefs.getBoolean("health_connect_sync_enabled", false)
                    )

                    _userProfile.value = profile
                } else {
                    // Data is corrupted, reset onboarding
                    _isOnboardingCompleted.value = false
                    clearUserProfile()
                }
            }
        } catch (e: Exception) {
            // Handle any corrupted data by resetting
            _isOnboardingCompleted.value = false
            clearUserProfile()
        }
    }

    /**
     * Clear user profile (for testing or reset purposes)
     */
    fun clearUserProfile() {
        println("UserRepository: Clearing all user data...")
        prefs.edit().clear().apply()
        _userProfile.value = null
        _isOnboardingCompleted.value = false
        println("UserRepository: User data cleared. Onboarding status = ${_isOnboardingCompleted.value}")
    }

    /**
     * Force reset onboarding for testing
     */
    fun resetOnboarding() {
        println("UserRepository: Resetting onboarding...")
        prefs.edit().putBoolean("onboarding_completed", false).apply()
        _isOnboardingCompleted.value = false
        _userProfile.value = null
        println("UserRepository: Onboarding reset complete")
    }

    /**
     * Update theme preferences
     */
    fun updateThemePreferences(themePreferences: ThemePreferences) {
        prefs.edit().apply {
            putBoolean("use_dynamic_color", themePreferences.useDynamicColor)
            putString("dark_mode", themePreferences.darkMode.name)
            putString("color_source", themePreferences.colorSource.name)
            putString("week_start_day", themePreferences.weekStartDay.name)
            putBoolean("use_pure_black", themePreferences.usePureBlack)
            putString("app_font", themePreferences.appFont.name)
            apply()
        }
    }

    /**
     * Load theme preferences
     */
    fun loadThemePreferences(): ThemePreferences {
        return ThemePreferences(
            useDynamicColor = prefs.getBoolean("use_dynamic_color", true),
            darkMode = DarkModePreference.valueOf(
                prefs.getString("dark_mode", DarkModePreference.SYSTEM.name) ?: DarkModePreference.SYSTEM.name
            ),
            colorSource = ColorSource.valueOf(
                prefs.getString("color_source", ColorSource.DYNAMIC_COLOR.name) ?: ColorSource.DYNAMIC_COLOR.name
            ),
            weekStartDay = WeekStartDay.valueOf(
                prefs.getString("week_start_day", WeekStartDay.MONDAY.name) ?: WeekStartDay.MONDAY.name
            ),
            usePureBlack = prefs.getBoolean("use_pure_black", false),
            appFont = AppFont.valueOf(
                prefs.getString("app_font", AppFont.ROBOTO.name) ?: AppFont.ROBOTO.name
            )
        )
    }
    
    /**
     * Save developer options enabled state
     */
    fun saveDeveloperOptionsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("developer_options_enabled", enabled).apply()
    }
    
    /**
     * Load developer options enabled state
     */
    fun loadDeveloperOptionsEnabled(): Boolean {
        return prefs.getBoolean("developer_options_enabled", false)
    }

    private fun loadBeveragePreferences(): BeveragePreferences {
        val orderStr = prefs.getString("beverage_order", null)
        val hiddenStr = prefs.getString("beverage_hidden", null)
        if (orderStr == null && hiddenStr == null) return BeveragePreferences.default()
        val ordered = orderStr?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        val hidden = hiddenStr?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        return BeveragePreferences(orderedVisible = ordered, hidden = hidden)
    }

    fun saveBeveragePreferences(beveragePreferences: BeveragePreferences) {
        prefs.edit().apply {
            putString("beverage_order", beveragePreferences.orderedVisible.joinToString(","))
            putString("beverage_hidden", beveragePreferences.hidden.joinToString(","))
            apply()
        }
        _beveragePreferences.value = beveragePreferences
    }
}