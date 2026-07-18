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
import com.cemcakmak.hydrotracker.data.models.DateFormatPattern
import com.cemcakmak.hydrotracker.data.models.TimeFormat
import com.cemcakmak.hydrotracker.data.models.WeekStartDay
import com.cemcakmak.hydrotracker.data.models.AppFont
import com.cemcakmak.hydrotracker.data.models.NavBarLabelMode
import com.cemcakmak.hydrotracker.data.models.EdgeEffect
import com.cemcakmak.hydrotracker.data.repository.UserRepository

class ThemeViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _themePreferences = MutableStateFlow(ThemePreferences())
    val themePreferences: StateFlow<ThemePreferences> = _themePreferences.asStateFlow()

    init {
        // Mirror the persisted theme into the in-memory state. Collecting also triggers the one-time
        // SharedPreferences -> DataStore import on first read.
        viewModelScope.launch {
            userRepository.themePreferences.collect { _themePreferences.value = it }
        }
    }

    fun updateDarkModePreference(preference: DarkModePreference) {
        viewModelScope.launch {
            val newPreferences = _themePreferences.value.copy(
                darkMode = preference
            )
            _themePreferences.value = newPreferences
            userRepository.updateThemePreferences(newPreferences)
        }
    }

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

    fun updateWeekStartDay(weekStartDay: WeekStartDay) {
        viewModelScope.launch {
            val newPreferences = _themePreferences.value.copy(
                weekStartDay = weekStartDay
            )
            _themePreferences.value = newPreferences
            userRepository.updateThemePreferences(newPreferences)
        }
    }

    fun updateTimeFormat(timeFormat: TimeFormat) {
        viewModelScope.launch {
            val newPreferences = _themePreferences.value.copy(
                timeFormat = timeFormat
            )
            _themePreferences.value = newPreferences
            userRepository.updateThemePreferences(newPreferences)
        }
    }

    fun updateDateFormat(dateFormat: DateFormatPattern) {
        viewModelScope.launch {
            val newPreferences = _themePreferences.value.copy(
                dateFormat = dateFormat
            )
            _themePreferences.value = newPreferences
            userRepository.updateThemePreferences(newPreferences)
        }
    }

    fun updatePureBlackPreference(usePureBlack: Boolean) {
        viewModelScope.launch {
            val newPreferences = _themePreferences.value.copy(
                usePureBlack = usePureBlack
            )
            _themePreferences.value = newPreferences
            userRepository.updateThemePreferences(newPreferences)
        }
    }

    fun updateAmoledBordersPreference(showBorders: Boolean) {
        viewModelScope.launch {
            val newPreferences = _themePreferences.value.copy(
                showAmoledBorders = showBorders
            )
            _themePreferences.value = newPreferences
            userRepository.updateThemePreferences(newPreferences)
        }
    }

    fun setAppFont(font: AppFont) {
        viewModelScope.launch {
            val newPreferences = _themePreferences.value.copy(
                appFont = font
            )
            _themePreferences.value = newPreferences
            userRepository.updateThemePreferences(newPreferences)
        }
    }

    fun setAutoHideNavBar(enabled: Boolean) {
        viewModelScope.launch {
            val newPreferences = _themePreferences.value.copy(
                autoHideNavBar = enabled
            )
            _themePreferences.value = newPreferences
            userRepository.updateThemePreferences(newPreferences)
        }
    }

    fun setNavBarLabelMode(mode: NavBarLabelMode) {
        viewModelScope.launch {
            val newPreferences = _themePreferences.value.copy(
                navBarLabelMode = mode
            )
            _themePreferences.value = newPreferences
            userRepository.updateThemePreferences(newPreferences)
        }
    }

    fun setEdgeEffect(style: EdgeEffect) {
        viewModelScope.launch {
            val newPreferences = _themePreferences.value.copy(
                edgeEffect = style
            )
            _themePreferences.value = newPreferences
            userRepository.updateThemePreferences(newPreferences)
        }
    }

    fun setUseBeverageColors(enabled: Boolean) {
        viewModelScope.launch {
            val newPreferences = _themePreferences.value.copy(
                useBeverageColors = enabled
            )
            _themePreferences.value = newPreferences
            userRepository.updateThemePreferences(newPreferences)
        }
    }

    fun isDynamicColorAvailable(): Boolean {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    }

    /** Whether the "Blurred" edge effect can render (its AGSL RuntimeShader requires API 33+). */
    fun isBlurSupported(): Boolean {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
    }
}

/**
 * Factory for creating ThemeViewModel with UserRepository dependency
 */
class ThemeViewModelFactory(private val userRepository: UserRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
            return ThemeViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}