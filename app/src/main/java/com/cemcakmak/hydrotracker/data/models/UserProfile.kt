package com.cemcakmak.hydrotracker.data.models

/**
 * User profile data model
 */
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
    val preferredThemeColor: String? = null, // For custom color themes
    val useSystemTheme: Boolean = true, // Material 3 dynamic color
    val reminderStyle: ReminderStyle = ReminderStyle.GENTLE,
    val hydrationStandard: HydrationStandard = HydrationStandard.EFSA, // Default to EFSA
    val healthConnectSyncEnabled: Boolean = false, // Health Connect data sync setting
    val dayEndMode: DayEndMode = DayEndMode.SLEEP_TIME,
    val reminderIntervalMode: ReminderIntervalMode = ReminderIntervalMode.AUTOMATIC,
    val customReminderInterval: Int = 60
)

enum class Gender {
    MALE,
    FEMALE,
    OTHER;

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

enum class AgeGroup {
    YOUNG_ADULT_18_30,
    ADULT_31_50,
    MIDDLE_AGED_51_60,
    SENIOR_60_PLUS;

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

enum class ActivityLevel {
    SEDENTARY,      // Little to no exercise
    LIGHT,          // Light exercise 1-3 days/week
    MODERATE,       // Moderate exercise 3-5 days/week
    ACTIVE,         // Heavy exercise 6-7 days/week
    VERY_ACTIVE;    // Very heavy exercise, physical job

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

    // Activity-specific encouragement
    fun getHydrationTip(): String {
        return when (this) {
            SEDENTARY -> "Even at rest, your body needs consistent hydration!"
            LIGHT -> "Light activity still means you need extra hydration support!"
            MODERATE -> "Regular exercise requires regular hydration - you're doing great!"
            ACTIVE -> "Your active lifestyle deserves premium hydration care!"
            VERY_ACTIVE -> "High performance demands high hydration - you're a champion!"
        }
    }
}

// Compassionate reminder styles
enum class ReminderStyle {
    GENTLE,      // Soft, encouraging reminders
    MOTIVATING,  // Energetic, goal-focused reminders
    MINIMAL;     // Simple, unobtrusive reminders

    fun getDisplayName(): String {
        return when (this) {
            GENTLE -> "Gentle & Caring"
            MOTIVATING -> "Motivating & Energetic"
            MINIMAL -> "Simple & Clean"
        }
    }
}

enum class HydrationStandard {
    EFSA,    // European Food Safety Authority (default)
    IOM;     // Institute of Medicine (US)

    fun getDisplayName(): String {
        return when (this) {
            EFSA -> "EFSA"
            IOM -> "IOM"
        }
    }

    fun getDescription(): String {
        return when (this) {
            EFSA -> "European Food Safety Authority standards"
            IOM -> "Institute of Medicine standards"
        }
    }

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

enum class DayEndMode {
    SLEEP_TIME,
    MIDNIGHT;

    fun getDisplayName(): String {
        return when (this) {
            SLEEP_TIME -> "Sleep time"
            MIDNIGHT -> "Midnight"
        }
    }
}

enum class ReminderIntervalMode {
    AUTOMATIC,
    CUSTOM;

    fun getDisplayName(): String {
        return when (this) {
            AUTOMATIC -> "Automatic"
            CUSTOM -> "Custom"
        }
    }
}