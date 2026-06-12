package com.cemcakmak.hydrotracker.data.models

import androidx.annotation.StringRes
import com.cemcakmak.hydrotracker.R
import java.time.DayOfWeek
import java.util.Calendar
import java.util.Locale
import kotlinx.serialization.Serializable

/**
 * Material 3 Expressive theme preferences
 * Manages dynamic color settings and theme customization
 */
@Serializable
data class ThemePreferences(
    val useDynamicColor: Boolean = true, // Default to dynamic colors
    val darkMode: DarkModePreference = DarkModePreference.SYSTEM,
    val colorSource: ColorSource = ColorSource.DYNAMIC_COLOR,
    val weekStartDay: WeekStartDay = WeekStartDay.SYSTEM,
    val usePureBlack: Boolean = false, // Pure black backgrounds in dark mode
    val appFont: AppFont = AppFont.GOOGLE_SANS_FLEX, // App-wide typeface
    val autoHideNavBar: Boolean = false, // Hide the bottom nav bar when scrolling down
    val navBarLabelMode: NavBarLabelMode = NavBarLabelMode.ALWAYS // Bottom nav label visibility
)

@Serializable
enum class DarkModePreference(
    @param:StringRes val labelResId: Int,
    @param:StringRes val descriptionResId: Int
) {
    SYSTEM(R.string.dark_mode_system, R.string.dark_mode_system_desc),     // Follow system setting
    LIGHT(R.string.dark_mode_light, R.string.dark_mode_light_desc),      // Always light mode
    DARK(R.string.dark_mode_dark, R.string.dark_mode_dark_desc);       // Always dark mode

    fun getDisplayName(): String {
        return when (this) {
            SYSTEM -> "System Default"
            LIGHT -> "Light Mode"
            DARK -> "Dark Mode"
        }
    }

    fun getDescription(): String {
        return when (this) {
            SYSTEM -> "Follows your device settings"
            LIGHT -> "Always use light theme"
            DARK -> "Always use dark theme"
        }
    }
}

@Serializable
enum class ColorSource(
    @param:StringRes val labelResId: Int,
    @param:StringRes val descriptionResId: Int
) {
    HYDRO_THEME(R.string.color_source_hydro, R.string.color_source_hydro_desc),    // Our default water-themed colors
    DYNAMIC_COLOR(R.string.color_source_dynamic, R.string.color_source_dynamic_desc),  // Material You dynamic colors from wallpaper
    CUSTOM(R.string.color_source_custom, R.string.color_source_custom_desc);         // Future: Custom color picker

    fun getDisplayName(): String {
        return when (this) {
            HYDRO_THEME -> "HydroTracker Blue"
            DYNAMIC_COLOR -> "Dynamic Colors"
            CUSTOM -> "Custom Colors"
        }
    }

    fun getDescription(): String {
        return when (this) {
            HYDRO_THEME -> "Beautiful water-themed blue palette"
            DYNAMIC_COLOR -> "Colors from your wallpaper"
            CUSTOM -> "Create your own color scheme"
        }
    }

    fun requiresAndroid12(): Boolean {
        return this == DYNAMIC_COLOR
    }
}

@Serializable
enum class WeekStartDay(
    val displayName: String,
    val dayOfWeek: DayOfWeek?,
    @param:StringRes val labelResId: Int,
    @param:StringRes val descriptionResId: Int
) {
    SYSTEM("System", null, R.string.week_start_system, R.string.week_start_system_desc),
    SUNDAY("Sunday", DayOfWeek.SUNDAY, R.string.weekday_sunday, R.string.week_start_sunday_desc),
    MONDAY("Monday", DayOfWeek.MONDAY, R.string.weekday_monday, R.string.week_start_monday_desc),
    TUESDAY("Tuesday", DayOfWeek.TUESDAY, R.string.weekday_tuesday, R.string.week_start_tuesday_desc),
    WEDNESDAY("Wednesday", DayOfWeek.WEDNESDAY, R.string.weekday_wednesday, R.string.week_start_wednesday_desc),
    THURSDAY("Thursday", DayOfWeek.THURSDAY, R.string.weekday_thursday, R.string.week_start_thursday_desc),
    FRIDAY("Friday", DayOfWeek.FRIDAY, R.string.weekday_friday, R.string.week_start_friday_desc),
    SATURDAY("Saturday", DayOfWeek.SATURDAY, R.string.weekday_saturday, R.string.week_start_saturday_desc);

    /**
     * Returns the concrete first day of the week.
     * For [SYSTEM], this is derived from the provided [locale] using the device's calendar data.
     */
    fun resolve(locale: Locale = Locale.getDefault()): DayOfWeek {
        return dayOfWeek ?: Calendar.getInstance(locale).firstDayOfWeek.toDayOfWeek()
    }

    fun getDescription(): String {
        return when (this) {
            SYSTEM -> "Follows your device locale"
            SUNDAY -> "Week starts on Sunday"
            MONDAY -> "Week starts on Monday"
            TUESDAY -> "Week starts on Tuesday"
            WEDNESDAY -> "Week starts on Wednesday"
            THURSDAY -> "Week starts on Thursday"
            FRIDAY -> "Week starts on Friday"
            SATURDAY -> "Week starts on Saturday"
        }
    }

    companion object {
        /** Maps a [Calendar] weekday constant to the Java 8 [DayOfWeek]. */
        private fun Int.toDayOfWeek(): DayOfWeek {
            return when (this) {
                Calendar.SUNDAY -> DayOfWeek.SUNDAY
                Calendar.MONDAY -> DayOfWeek.MONDAY
                Calendar.TUESDAY -> DayOfWeek.TUESDAY
                Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
                Calendar.THURSDAY -> DayOfWeek.THURSDAY
                Calendar.FRIDAY -> DayOfWeek.FRIDAY
                Calendar.SATURDAY -> DayOfWeek.SATURDAY
                else -> DayOfWeek.MONDAY
            }
        }
    }
}

@Serializable
enum class AppFont(@param:StringRes val labelResId: Int) {
    GOOGLE_SANS_FLEX(R.string.font_google_sans_flex),
    SYSTEM(R.string.font_system),
    OUTFIT(R.string.font_outfit),
    DM_SANS(R.string.font_dm_sans),
    JETBRAINS_MONO(R.string.font_jetbrains_mono);

    fun getDisplayName(): String {
        return when (this) {
            GOOGLE_SANS_FLEX -> "Google Sans Flex"
            SYSTEM -> "System Default"
            OUTFIT -> "Outfit"
            DM_SANS -> "DM Sans"
            JETBRAINS_MONO -> "JetBrains Mono"
        }
    }
}

@Serializable
enum class NavBarLabelMode(@param:StringRes val labelResId: Int) {
    ALWAYS(R.string.navbar_label_always),    // Show every tab's label
    SELECTED(R.string.navbar_label_selected),  // Show only the selected tab's label
    NONE(R.string.navbar_label_none);      // No labels

    fun getDisplayName(): String {
        return when (this) {
            ALWAYS -> "Always shown"
            SELECTED -> "Selected only"
            NONE -> "Off"
        }
    }
}