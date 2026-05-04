package com.cemcakmak.hydrotracker.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.DarkModePreference
import com.cemcakmak.hydrotracker.data.models.ColorSource
import com.cemcakmak.hydrotracker.data.models.WeekStartDay
import com.cemcakmak.hydrotracker.data.models.AppFont
import com.cemcakmak.hydrotracker.data.repository.UserRepository

/**
 * Material 3 Expressive Theme ViewModel
 * Manages theme preferences and dynamic color settings with persistence
 */
class ThemeViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _themePreferences = MutableStateFlow(
        // Load persisted preferences on initialization
        userRepository.loadThemePreferences()
    )
    val themePreferences: StateFlow<ThemePreferences> = _themePreferences.asStateFlow()

    /**
     * Update application font
     */
    fun updateAppFont(font: AppFont) {
        viewModelScope.launch {
            val newPreferences = _themePreferences.value.copy(appFont = font)
            _themePreferences.value = newPreferences
            userRepository.updateThemePreferences(newPreferences)
        }
    }

    /**
     * Toggle dynamic color on/off
     * MD3 Expressive: Smooth transition between color schemes
     */
    fun toggleDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            val newPreferences = _themePreferences.value.copy(
                useDynamicColor = enabled,
                colorSource = if (enabled) ColorSource.DYNAMIC_COLOR else ColorSource.HYDRO_THEME
            )
            _themePreferences.value = newPreferences
            userRepository.updateThemePreferences(newPreferences)
        }
    }

    /**
     * Update dark mode preference
     */
    fun updateDarkModePreference(preference: DarkModePreference) {
        viewModelScope.launch {
            val newPreferences = _themePreferences.value.copy(
                darkMode = preference
            )
            _themePreferences.value = newPreferences
            userRepository.updateThemePreferences(newPreferences)
        }
    }

    /**
     * Set color source
     */
    fun setColorSource(source: ColorSource) {
        viewModelScope.launch {
            val newPreferences = _themePreferences.value.copy(
                colorSource = source,
                useDynamicColor = source == ColorSource.DYNAMIC_COLOR
            )
            _themePreferences.value = newPreferences
            userRepository.updateThemePreferences(newPreferences)
        }
    }

    /**
     * Update week start day preference
     */
    fun updateWeekStartDay(weekStartDay: WeekStartDay) {
        viewModelScope.launch {
            val newPreferences = _themePreferences.value.copy(
                weekStartDay = weekStartDay
            )
            _themePreferences.value = newPreferences
            userRepository.updateThemePreferences(newPreferences)
        }
    }

    /**
     * Update pure black preference
     */
    fun updatePureBlackPreference(usePureBlack: Boolean) {
        viewModelScope.launch {
            val newPreferences = _themePreferences.value.copy(
                usePureBlack = usePureBlack
            )
            _themePreferences.value = newPreferences
            userRepository.updateThemePreferences(newPreferences)
        }
    }

    /**
     * Check if dynamic color is available on this device
     */
    fun isDynamicColorAvailable(): Boolean {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    }

    /**
     * Get user-friendly status message for dynamic color
     */
    fun getDynamicColorStatus(): String {
        return when {
            !isDynamicColorAvailable() -> "Requires Android 12+"
            _themePreferences.value.useDynamicColor -> "Using wallpaper colors"
            else -> "Using HydroTracker theme"
        }
    }
}

/**
 * Factory for creating ThemeViewModel with UserRepository dependency
 */
class ThemeViewModelFactory(private val userRepository: UserRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
            return ThemeViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}