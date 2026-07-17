package com.cemcakmak.hydrotracker.presentation.common

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface NavigationRoutes : NavKey {
    @Serializable data object Onboarding : NavigationRoutes
    @Serializable data object Home : NavigationRoutes
    @Serializable data object History : NavigationRoutes
    @Serializable data object Statistics : NavigationRoutes
    @Serializable data object Settings : NavigationRoutes

    @Serializable data object SettingsAppearance : NavigationRoutes
    @Serializable data object SettingsWidget : NavigationRoutes
    @Serializable data object SettingsWidgetQuickAdd : NavigationRoutes
    @Serializable data object SettingsDisplay : NavigationRoutes
    @Serializable data object SettingsHydration : NavigationRoutes
    @Serializable data object SettingsContainers : NavigationRoutes
    @Serializable data object SettingsContainerPresets : NavigationRoutes
    @Serializable data object SettingsBeverageTypes : NavigationRoutes
    @Serializable data object SettingsNotifications : NavigationRoutes
    @Serializable data object SettingsReminderInterval : NavigationRoutes
    @Serializable data object SettingsSupport : NavigationRoutes
    @Serializable data object SettingsAbout : NavigationRoutes
    @Serializable data object SettingsUpdates : NavigationRoutes
    @Serializable data object SettingsLicenses : NavigationRoutes
    @Serializable data object SettingsDeveloper : NavigationRoutes
    @Serializable data object SettingsDeveloperHaptics : NavigationRoutes
    @Serializable data object SettingsDeveloperHapticsLab : NavigationRoutes
    @Serializable data object SettingsProfile : NavigationRoutes
    @Serializable data object SettingsDataManagement : NavigationRoutes

    @Serializable
    enum class CropCaller {
        PROFILE_SETTINGS,
        ONBOARDING
    }

    @Serializable
    data class CropProfileImage(
        val sourceUri: String,
        val caller: CropCaller
    ) : NavigationRoutes
}
