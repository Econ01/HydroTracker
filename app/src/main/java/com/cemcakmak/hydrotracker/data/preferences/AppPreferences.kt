package com.cemcakmak.hydrotracker.data.preferences

import com.cemcakmak.hydrotracker.data.models.BeveragePreferences
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.UserProfile
import kotlinx.serialization.Serializable

/**
 * Single versioned document backing the app-preferences DataStore.
 *
 * Every field has a default and the JSON reader is lenient (ignoreUnknownKeys + coerceInputValues),
 * so adding a field, removing a field, or hitting an unknown enum value never breaks a user's
 * stored data. [schemaVersion] is reserved for the rare change
 * that needs an explicit transform (e.g. repurposing a field); bump it and branch in the serializer
 * if that ever happens.
 */
@Serializable
data class AppPreferences(
    val schemaVersion: Int = 1,
    val onboardingCompleted: Boolean = false,
    val profile: UserProfile? = null,
    val theme: ThemePreferences = ThemePreferences(),
    val beverages: BeveragePreferences = BeveragePreferences.default(),
    val hapticsEnabled: Boolean = true,
    val lastHealthConnectImportTime: Long? = null,
    val widgetPreviewRevision: Int = 0,
)
