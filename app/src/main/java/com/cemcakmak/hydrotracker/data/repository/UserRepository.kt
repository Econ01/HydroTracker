package com.cemcakmak.hydrotracker.data.repository

import android.content.Context
import com.cemcakmak.hydrotracker.data.models.BeveragePreferences
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.models.WidgetPreferences
import com.cemcakmak.hydrotracker.data.preferences.AppPreferences
import com.cemcakmak.hydrotracker.data.preferences.appPreferencesStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for user preferences, backed by a typed DataStore (see [appPreferencesStore]).
 *
 * Reads are [Flow]s; writes are `suspend`. Existing users' legacy SharedPreferences data is imported
 * once, automatically, by the DataStore migration the first time the store is read — so callers see
 * their data with no extra work.
 */
class UserRepository(context: Context) {

    // Always go through the application-scoped singleton DataStore (one instance per process/file).
    private val dataStore = context.applicationContext.appPreferencesStore

    /** The full preferences snapshot. `null` from a collector's initial value means "not loaded yet". */
    val appPreferences: Flow<AppPreferences> = dataStore.data
    val userProfile: Flow<UserProfile?> = dataStore.data.map { it.profile }
    val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { it.onboardingCompleted }
    val themePreferences: Flow<ThemePreferences> = dataStore.data.map { it.theme }
    val beveragePreferences: Flow<BeveragePreferences> = dataStore.data.map { it.beverages }
    val widgetPreferences: Flow<WidgetPreferences> = dataStore.data.map { it.widget }

    suspend fun saveUserProfile(profile: UserProfile) {
        dataStore.updateData { current ->
            current.copy(
                profile = profile,
                onboardingCompleted = profile.isOnboardingCompleted,
            )
        }
    }

    suspend fun updateThemePreferences(themePreferences: ThemePreferences) {
        dataStore.updateData { it.copy(theme = themePreferences) }
    }

    suspend fun saveBeveragePreferences(beveragePreferences: BeveragePreferences) {
        dataStore.updateData { it.copy(beverages = beveragePreferences) }
    }

    suspend fun updateHapticsEnabled(enabled: Boolean) {
        dataStore.updateData { it.copy(hapticsEnabled = enabled) }
    }

    suspend fun updateLastHealthConnectImportTime(timeMillis: Long?) {
        dataStore.updateData { it.copy(lastHealthConnectImportTime = timeMillis) }
    }

    suspend fun updateWidgetPreviewRevision(revision: Int) {
        dataStore.updateData { it.copy(widgetPreviewRevision = revision) }
    }

    suspend fun updateWidgetPreferences(widgetPreferences: WidgetPreferences) {
        dataStore.updateData { it.copy(widget = widgetPreferences) }
    }

    suspend fun updateDateBoundaryMigratedVersion(version: Int) {
        dataStore.updateData { it.copy(dateBoundaryMigratedVersion = version) }
    }

    /** Resets all preferences (profile, theme, beverages) back to defaults. */
    suspend fun clearUserProfile() {
        dataStore.updateData { AppPreferences() }
    }

    /** Sends the user back to onboarding without discarding theme/beverage settings. */
    suspend fun resetOnboarding() {
        dataStore.updateData { it.copy(onboardingCompleted = false, profile = null) }
    }
}
