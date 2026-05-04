package com.cemcakmak.hydrotracker.data.models

import java.time.DayOfWeek

/**
 * Material 3 Expressive theme preferences
 * Manages dynamic color settings and theme customization
 */
data class ThemePreferences(
    val useDynamicColor: Boolean = true, // Default to dynamic colors
    val darkMode: DarkModePreference = DarkModePreference.SYSTEM,
    val colorSource: ColorSource = ColorSource.DYNAMIC_COLOR,
    val weekStartDay: WeekStartDay = WeekStartDay.MONDAY,
    val usePureBlack: Boolean = false, // Pure black backgrounds in dark mode
    val appFont: AppFont = AppFont.ROBOTO // Default to Roboto
)

enum class AppFont(val displayName: String) {
    ROBOTO("Roboto Flex"),
    DMSANS("DM Sans"),
    JETBRAINS_MONO("JetBrains Mono"),
    GOOGLE_SANS("Google Sans"),
    OUTFIT("Outfit")
}

enum class DarkModePreference {
    SYSTEM,     // Follow system setting
    LIGHT,      // Always light mode
    DARK;       // Always dark mode

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

enum class ColorSource {
    HYDRO_THEME,    // Our default water-themed colors
    DYNAMIC_COLOR,  // Material You dynamic colors from wallpaper
    CUSTOM;         // Future: Custom color picker

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

enum class WeekStartDay(val displayName: String, val dayOfWeek: DayOfWeek) {
    SUNDAY("Sunday", DayOfWeek.SUNDAY),
    MONDAY("Monday", DayOfWeek.MONDAY);
    
    fun getDescription(): String {
        return when (this) {
            SUNDAY -> "Week starts on Sunday"
            MONDAY -> "Week starts on Monday"
        }
    }
}