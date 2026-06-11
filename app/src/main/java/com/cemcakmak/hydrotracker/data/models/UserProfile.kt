package com.cemcakmak.hydrotracker.data.models

import androidx.annotation.StringRes
import com.cemcakmak.hydrotracker.R
import kotlinx.serialization.Serializable

/**
 * User profile data model
 */
@Serializable
data class UserProfile(
    val id: Int = 1,
    val name: String, // Required username (max 15 characters)
    val profileImagePath: String? = null, // Optional local file path to profile image
    val gender: Gender,
    val ageGroup: AgeGroup,
    val weight: Double? = null, // in kg (optional for more precise calculation)
    val activityLevel: ActivityLevel,
    val wakeUpTime: String, // HH:mm format (e.g., "07:00")
    val sleepTime: String, // HH:mm format (e.g., "23:00")
    val dailyWaterGoal: Double, // calculated target in milliliters
    val reminderInterval: Int, // reminder frequency in minutes
    val isOnboardingCompleted: Boolean = false,
    val preferredThemeColor: String? = null, // For custom colour themes
    val useSystemTheme: Boolean = true, // Material 3 dynamic colour
    val reminderStyle: ReminderStyle = ReminderStyle.GENTLE,
    val hydrationStandard: HydrationStandard = HydrationStandard.EFSA, // Default to EFSA
    val healthConnectSyncEnabled: Boolean = false, // Health Connect data sync setting
    val dayEndMode: DayEndMode = DayEndMode.SLEEP_TIME,
    val reminderIntervalMode: ReminderIntervalMode = ReminderIntervalMode.AUTOMATIC,
    val customReminderInterval: Int = 60
)

@Serializable
enum class Gender(
    @param:StringRes val labelResId: Int,
    @param:StringRes val greetingResId: Int
) {
    MALE(R.string.gender_male, R.string.gender_greeting_male),
    FEMALE(R.string.gender_female, R.string.gender_greeting_female),
    OTHER(R.string.gender_other, R.string.gender_greeting_other);

    // Transitional: prefer labelResId/greetingResId with stringResource() in Compose.
    // These String accessors are kept until all call sites migrate, then removed.
    fun getDisplayName(): String {
        return when (this) {
            MALE -> "Male"
            FEMALE -> "Female"
            OTHER -> "Prefer not to say"
        }
    }

    // Use inclusive, compassionate language
    fun getPersonalizedGreeting(): String {
        return when (this) {
            MALE -> "Stay hydrated, champion!"
            FEMALE -> "Keep glowing with hydration!"
            OTHER -> "You've got this!"
        }
    }
}

@Serializable
enum class AgeGroup(
    @param:StringRes val labelResId: Int,
    @param:StringRes val motivationResId: Int
) {
    YOUNG_ADULT_18_30(R.string.age_group_young_adult, R.string.age_motivation_young_adult),
    ADULT_31_50(R.string.age_group_adult, R.string.age_motivation_adult),
    MIDDLE_AGED_51_60(R.string.age_group_middle_aged, R.string.age_motivation_middle_aged),
    SENIOR_60_PLUS(R.string.age_group_senior, R.string.age_motivation_senior);

    fun getDisplayName(): String {
        return when (this) {
            YOUNG_ADULT_18_30 -> "18-30 years"
            ADULT_31_50 -> "31-50 years"
            MIDDLE_AGED_51_60 -> "51-60 years"
            SENIOR_60_PLUS -> "60+ years"
        }
    }

    // Age-appropriate messaging
    fun getMotivationalMessage(): String {
        return when (this) {
            YOUNG_ADULT_18_30 -> "Your body is building its foundation - keep it hydrated!"
            ADULT_31_50 -> "Hydration is your energy source - fuel your busy life!"
            MIDDLE_AGED_51_60 -> "Stay refreshed and vibrant with proper hydration!"
            SENIOR_60_PLUS -> "Hydration supports your wellness journey every day!"
        }
    }
}

@Serializable
enum class ActivityLevel(
    @param:StringRes val labelResId: Int,
    @param:StringRes val descriptionResId: Int,
    @param:StringRes val hydrationTipResId: Int
) {
    SEDENTARY(R.string.activity_sedentary, R.string.activity_desc_sedentary, R.string.activity_tip_sedentary),
    LIGHT(R.string.activity_light, R.string.activity_desc_light, R.string.activity_tip_light),
    MODERATE(R.string.activity_moderate, R.string.activity_desc_moderate, R.string.activity_tip_moderate),
    ACTIVE(R.string.activity_active, R.string.activity_desc_active, R.string.activity_tip_active),
    VERY_ACTIVE(R.string.activity_very_active, R.string.activity_desc_very_active, R.string.activity_tip_very_active);

    fun getDisplayName(): String {
        return when (this) {
            SEDENTARY -> "Sedentary"
            LIGHT -> "Light Activity"
            MODERATE -> "Moderate Activity"
            ACTIVE -> "Active"
            VERY_ACTIVE -> "Very Active"
        }
    }

    fun getDescription(): String {
        return when (this) {
            SEDENTARY -> "Little to no exercise"
            LIGHT -> "Light exercise 1-3 days/week"
            MODERATE -> "Moderate exercise 3-5 days/week"
            ACTIVE -> "Heavy exercise 6-7 days/week"
            VERY_ACTIVE -> "Very heavy exercise or physical job"
        }
    }

    // Activity-specific encouragement (resolve hydrationTipResId with stringResource() in UI).
}

// Compassionate reminder styles
@Serializable
enum class ReminderStyle(@param:StringRes val labelResId: Int) {
    GENTLE(R.string.reminder_style_gentle),      // Soft, encouraging reminders
    MOTIVATING(R.string.reminder_style_motivating),  // Energetic, goal-focused reminders
    MINIMAL(R.string.reminder_style_minimal);     // Simple, unobtrusive reminders

    fun getDisplayName(): String {
        return when (this) {
            GENTLE -> "Gentle & Caring"
            MOTIVATING -> "Motivating & Energetic"
            MINIMAL -> "Simple & Clean"
        }
    }
}

@Serializable
enum class HydrationStandard(
    @param:StringRes val labelResId: Int,
    @param:StringRes val descriptionResId: Int
) {
    EFSA(R.string.hydration_standard_efsa, R.string.hydration_standard_efsa_desc),    // European Food Safety Authority (default)
    IOM(R.string.hydration_standard_iom, R.string.hydration_standard_iom_desc);     // Institute of Medicine (US)

    fun getMaleIntake(): Double {
        return when (this) {
            EFSA -> 2500.0  // 2.5L
            IOM -> 3700.0   // 3.7L
        }
    }

    fun getFemaleIntake(): Double {
        return when (this) {
            EFSA -> 2000.0  // 2.0L
            IOM -> 2700.0   // 2.7L
        }
    }
}

@Serializable
enum class DayEndMode(@param:StringRes val labelResId: Int) {
    SLEEP_TIME(R.string.day_end_sleep_time),
    MIDNIGHT(R.string.day_end_midnight);

    fun getDisplayName(): String {
        return when (this) {
            SLEEP_TIME -> "Sleep time"
            MIDNIGHT -> "Midnight"
        }
    }
}

@Serializable
enum class ReminderIntervalMode(@param:StringRes val labelResId: Int) {
    AUTOMATIC(R.string.reminder_interval_mode_automatic),
    CUSTOM(R.string.reminder_interval_mode_custom);

    fun getDisplayName(): String {
        return when (this) {
            AUTOMATIC -> "Automatic"
            CUSTOM -> "Custom"
        }
    }
}