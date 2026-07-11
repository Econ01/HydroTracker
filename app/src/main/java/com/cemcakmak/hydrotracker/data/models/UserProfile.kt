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
    val customReminderInterval: Int = 60,
    val volumeUnit: VolumeUnit = VolumeUnit.MILLILITRES // Display unit for hydration amounts
)

@Serializable
enum class Gender(
    @param:StringRes val labelResId: Int,
    @param:StringRes val greetingResId: Int
) {
    MALE(R.string.gender_male, R.string.gender_greeting_male),
    FEMALE(R.string.gender_female, R.string.gender_greeting_female),
    OTHER(R.string.gender_other, R.string.gender_greeting_other);

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

}

@Serializable
enum class VolumeUnit(
    @param:StringRes val labelResId: Int,
    @param:StringRes val shortLabelResId: Int,
    val toMillilitresFactor: Double
) {
    MILLILITRES(R.string.unit_millilitres, R.string.unit_ml_short, 1.0),
    US_FLUID_OUNCE(R.string.unit_us_fl_oz, R.string.unit_us_fl_oz_short, 29.5735),
    IMPERIAL_FLUID_OUNCE(R.string.unit_imperial_fl_oz, R.string.unit_imperial_fl_oz_short, 28.4131)
}