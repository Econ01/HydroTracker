package com.cemcakmak.hydrotracker.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import com.cemcakmak.hydrotracker.data.models.ActivityLevel
import com.cemcakmak.hydrotracker.data.models.AgeGroup
import com.cemcakmak.hydrotracker.data.models.AppFont
import com.cemcakmak.hydrotracker.data.models.BeveragePreferences
import com.cemcakmak.hydrotracker.data.models.ColorSource
import com.cemcakmak.hydrotracker.data.models.DarkModePreference
import com.cemcakmak.hydrotracker.data.models.DayEndMode
import com.cemcakmak.hydrotracker.data.models.Gender
import com.cemcakmak.hydrotracker.data.models.HydrationStandard
import com.cemcakmak.hydrotracker.data.models.NavBarLabelMode
import com.cemcakmak.hydrotracker.data.models.ReminderIntervalMode
import com.cemcakmak.hydrotracker.data.models.ReminderStyle
import com.cemcakmak.hydrotracker.data.models.ThemePreferences
import com.cemcakmak.hydrotracker.data.models.UserProfile
import com.cemcakmak.hydrotracker.data.models.WeekStartDay
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

private val appPrefsJson = Json {
    ignoreUnknownKeys = true   // a field removed in a later version won't break old documents
    coerceInputValues = true   // an unknown/renamed enum value falls back to the property default
    encodeDefaults = true
}

/** Serializes [AppPreferences] as JSON; parse failures surface as corruption (handled below). */
// DataStore invokes readFrom/writeTo on Dispatchers.IO, so the blocking stream calls are safe here.
@Suppress("BlockingMethodInNonBlockingContext")
object AppPreferencesSerializer : Serializer<AppPreferences> {
    override val defaultValue = AppPreferences()

    override suspend fun readFrom(input: InputStream): AppPreferences =
        try {
            appPrefsJson.decodeFromString(
                AppPreferences.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            throw CorruptionException("Could not parse app preferences", e)
        }

    override suspend fun writeTo(t: AppPreferences, output: OutputStream) {
        output.write(
            appPrefsJson.encodeToString(AppPreferences.serializer(), t).encodeToByteArray()
        )
    }
}

/**
 * Process-wide singleton DataStore for app preferences. Always access it through this Context
 * extension — opening a second DataStore for the same file crashes at runtime.
 */
val Context.appPreferencesStore: DataStore<AppPreferences> by dataStore(
    fileName = "app_preferences.json",
    serializer = AppPreferencesSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler { AppPreferences() },
    produceMigrations = { context -> listOf(LegacySharedPrefsMigration(context)) }
)

private const val LEGACY_PREFS_NAME = "hydrotracker_prefs"

/**
 * One-time import of the legacy SharedPreferences profile/theme/beverage data into DataStore.
 * Runs once, before the first read; DataStore records completion so it never re-runs. The old keys
 * are intentionally left in place as a one-release backup (and UpdateRepository still uses that same
 * SharedPreferences file for unrelated update-checker keys).
 */
class LegacySharedPrefsMigration(private val context: Context) : DataMigration<AppPreferences> {

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun shouldMigrate(currentData: AppPreferences): Boolean =
        currentData == AppPreferences() && prefs.contains("onboarding_completed")

    override suspend fun migrate(currentData: AppPreferences): AppPreferences =
        mapLegacyPrefs(prefs, currentData)

    override suspend fun cleanUp() {
        // Keep legacy keys as a one-release backup; nothing to clean up.
    }
}

/**
 * Pure mapping from the legacy SharedPreferences layout to [AppPreferences]. Extracted and
 * `internal` so it can be unit-tested directly. Uses defaulting + safe enum parsing.
 */
internal fun mapLegacyPrefs(prefs: SharedPreferences, current: AppPreferences): AppPreferences {
    val theme = ThemePreferences(
        useDynamicColor = prefs.getBoolean("use_dynamic_color", true),
        darkMode = prefs.getString("dark_mode", null).toEnumOrDefault(DarkModePreference.SYSTEM),
        colorSource = prefs.getString("color_source", null).toEnumOrDefault(ColorSource.DYNAMIC_COLOR),
        weekStartDay = prefs.getString("week_start_day", null).toEnumOrDefault(WeekStartDay.SYSTEM),
        usePureBlack = prefs.getBoolean("use_pure_black", false),
        appFont = prefs.getString("app_font", null).toEnumOrDefault(AppFont.GOOGLE_SANS_FLEX),
        autoHideNavBar = prefs.getBoolean("auto_hide_nav_bar", false),
        navBarLabelMode = prefs.getString("nav_bar_label_mode", null).toEnumOrDefault(NavBarLabelMode.ALWAYS),
    )

    val beverages = run {
        val orderStr = prefs.getString("beverage_order", null)
        val hiddenStr = prefs.getString("beverage_hidden", null)
        if (orderStr == null && hiddenStr == null) {
            BeveragePreferences.default()
        } else {
            BeveragePreferences(
                orderedVisible = orderStr?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
                hidden = hiddenStr?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet(),
            )
        }
    }

    val profile = legacyProfile(prefs)

    return current.copy(
        onboardingCompleted = profile != null,
        profile = profile,
        theme = theme,
        beverages = beverages,
    )
}

/** Builds a [UserProfile] from legacy keys, or null if onboarding wasn't completed / data is incomplete. */
private fun legacyProfile(prefs: SharedPreferences): UserProfile? {
    if (!prefs.getBoolean("onboarding_completed", false)) return null
    val gender = prefs.getString("gender", null).toEnumOrNull<Gender>() ?: return null
    val ageGroup = prefs.getString("age_group", null).toEnumOrNull<AgeGroup>() ?: return null
    val activityLevel = prefs.getString("activity_level", null).toEnumOrNull<ActivityLevel>() ?: return null
    return UserProfile(
        name = prefs.getString("name", "User") ?: "User",
        profileImagePath = prefs.getString("profile_image_path", null),
        gender = gender,
        ageGroup = ageGroup,
        activityLevel = activityLevel,
        wakeUpTime = prefs.getString("wake_up_time", "07:00") ?: "07:00",
        sleepTime = prefs.getString("sleep_time", "23:00") ?: "23:00",
        dailyWaterGoal = prefs.getFloat("daily_water_goal", 2700f).toDouble(),
        reminderInterval = prefs.getInt("reminder_interval", 120),
        isOnboardingCompleted = true,
        weight = prefs.getFloat("weight", 0f).let { if (it > 0) it.toDouble() else null },
        preferredThemeColor = prefs.getString("preferred_theme_color", null),
        useSystemTheme = prefs.getBoolean("use_system_theme", true),
        reminderStyle = prefs.getString("reminder_style", null).toEnumOrDefault(ReminderStyle.GENTLE),
        hydrationStandard = prefs.getString("hydration_standard", null).toEnumOrDefault(HydrationStandard.EFSA),
        healthConnectSyncEnabled = prefs.getBoolean("health_connect_sync_enabled", false),
        dayEndMode = prefs.getString("day_end_mode", null).toEnumOrDefault(DayEndMode.SLEEP_TIME),
        reminderIntervalMode = prefs.getString("reminder_interval_mode", null).toEnumOrDefault(ReminderIntervalMode.AUTOMATIC),
        customReminderInterval = prefs.getInt("custom_reminder_interval", 60),
    )
}

private inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? =
    this?.let { name -> enumValues<T>().firstOrNull { it.name == name } }

private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
    toEnumOrNull<T>() ?: default
