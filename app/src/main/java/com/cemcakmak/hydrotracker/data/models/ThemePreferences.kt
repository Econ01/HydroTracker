package com.cemcakmak.hydrotracker.data.models

import androidx.annotation.StringRes
import com.cemcakmak.hydrotracker.R
import java.time.DayOfWeek
import java.util.Calendar
import java.util.Locale
import kotlinx.serialization.Serializable

/**
 * Material 3 Expressive theme preferences
 * Manages dynamic colour settings and theme customization
 */
@Serializable
data class ThemePreferences(
    val useDynamicColor: Boolean = true, // Default to dynamic colours
    val darkMode: DarkModePreference = DarkModePreference.SYSTEM,
    val colorSource: ColorSource = ColorSource.DYNAMIC_COLOR,
    val weekStartDay: WeekStartDay = WeekStartDay.SYSTEM,
    val timeFormat: TimeFormat = TimeFormat.SYSTEM, // 12/24-hour display preference
    val dateFormat: DateFormatPattern = DateFormatPattern.SYSTEM, // Date display pattern preference
    val usePureBlack: Boolean = false, // Pure black backgrounds in dark mode
    val showAmoledBorders: Boolean = true, // Subtle hairline borders around cards in AMOLED mode
    val useBeverageColors: Boolean = false, // Tint beverage-related surfaces with dedicated beverage colours
    val appFont: AppFont = AppFont.GOOGLE_SANS_FLEX, // App-wide typeface
    val autoHideNavBar: Boolean = false, // Hide the bottom nav bar when scrolling down
    val navBarLabelMode: NavBarLabelMode = NavBarLabelMode.ALWAYS, // Bottom nav label visibility
    val edgeEffect: EdgeEffect = EdgeEffect.BLURRED // Home screen top edge treatment
)

@Serializable
enum class DarkModePreference(
    @param:StringRes val labelResId: Int,
) {
    SYSTEM(R.string.dark_mode_system),     // Follow system setting
    LIGHT(R.string.dark_mode_light),      // Always light mode
    DARK(R.string.dark_mode_dark)        // Always dark mode
}

@Serializable
enum class ColorSource(
    @param:StringRes val labelResId: Int,
) {
    HYDRO_THEME(R.string.color_source_hydro),    // Our default water-themed colours
    DYNAMIC_COLOR(R.string.color_source_dynamic),  // Material You dynamic colours from wallpaper
}

@Serializable
enum class WeekStartDay(
    val dayOfWeek: DayOfWeek?,
    @param:StringRes val labelResId: Int,
) {
    SYSTEM(null, R.string.week_start_system),
    SUNDAY(DayOfWeek.SUNDAY, R.string.weekday_sunday),
    MONDAY(DayOfWeek.MONDAY, R.string.weekday_monday),
    TUESDAY(DayOfWeek.TUESDAY, R.string.weekday_tuesday),
    WEDNESDAY(DayOfWeek.WEDNESDAY, R.string.weekday_wednesday),
    THURSDAY(DayOfWeek.THURSDAY, R.string.weekday_thursday),
    FRIDAY(DayOfWeek.FRIDAY, R.string.weekday_friday),
    SATURDAY(DayOfWeek.SATURDAY, R.string.weekday_saturday);

    /**
     * Returns the concrete first day of the week.
     * For [SYSTEM], this is derived from the provided [locale] using the device's calendar data.
     */
    fun resolve(locale: Locale = Locale.getDefault()): DayOfWeek {
        return dayOfWeek ?: Calendar.getInstance(locale).firstDayOfWeek.toDayOfWeek()
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
enum class TimeFormat(
    @param:StringRes val labelResId: Int,
) {
    SYSTEM(R.string.time_format_system),
    HOUR_12(R.string.time_format_12_hour),
    HOUR_24(R.string.time_format_24_hour)
}

@Serializable
enum class DateFormatPattern(
    @param:StringRes val labelResId: Int,
) {
    SYSTEM(R.string.date_format_system),
    DAY_MONTH_YEAR(R.string.date_format_day_month_year),
    MONTH_DAY_YEAR(R.string.date_format_month_day_year),
    YEAR_MONTH_DAY(R.string.date_format_year_month_day)
}

@Serializable
enum class AppFont(@param:StringRes val labelResId: Int) {
    GOOGLE_SANS_FLEX(R.string.font_google_sans_flex),
    SYSTEM(R.string.font_system),
    OUTFIT(R.string.font_outfit),
    DM_SANS(R.string.font_dm_sans),
    JETBRAINS_MONO(R.string.font_jetbrains_mono)
}

@Serializable
enum class NavBarLabelMode(@param:StringRes val labelResId: Int) {
    ALWAYS(R.string.navbar_label_always),    // Show every tab's label
    SELECTED(R.string.navbar_label_selected),  // Show only the selected tab's label
    NONE(R.string.navbar_label_none)      // No labels
}

@Serializable
enum class EdgeEffect(@param:StringRes val labelResId: Int) {
    TRANSPARENT(R.string.edge_effect_transparent), // No top treatment; content runs edge-to-edge
    BLURRED(R.string.edge_effect_blurred),         // Variable-radius backdrop blur (API 33+)
    SCRIM(R.string.edge_effect_scrim)              // Surface -> transparent gradient fade
}