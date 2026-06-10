package com.cemcakmak.hydrotracker.data.preferences

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Verifies the one-time SharedPreferences -> DataStore import ([mapLegacyPrefs]) is lossless and
 * resilient. Runs on the JVM via Robolectric using a real SharedPreferences instance.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class PrefsMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private fun prefs() = context.getSharedPreferences("hydrotracker_prefs", Context.MODE_PRIVATE)

    @Test
    fun migratesCompleteLegacyProfileLosslessly() {
        prefs().edit {
            putBoolean("onboarding_completed", true)
            putString("name", "Cem")
            putString("gender", "MALE")
            putString("age_group", "ADULT_31_50")
            putString("activity_level", "MODERATE")
            putString("wake_up_time", "06:30")
            putString("sleep_time", "22:30")
            putFloat("daily_water_goal", 3000f)
            putInt("reminder_interval", 90)
            putFloat("weight", 75f)
            putString("hydration_standard", "IOM")
            putBoolean("health_connect_sync_enabled", true)
            putString("day_end_mode", "MIDNIGHT")
            putString("reminder_interval_mode", "CUSTOM")
            putInt("custom_reminder_interval", 45)
            putString("dark_mode", "DARK")
            putString("color_source", "HYDRO_THEME")
            putString("app_font", "OUTFIT")
            putBoolean("auto_hide_nav_bar", true)
            putString("nav_bar_label_mode", "SELECTED")
            putString("beverage_order", "COFFEE,TEA")
            putString("beverage_hidden", "JUICE")
        }

        val result = mapLegacyPrefs(prefs(), AppPreferences())

        assertTrue(result.onboardingCompleted)
        assertNotNull(result.profile)
        val p = result.profile!!
        assertEquals("Cem", p.name)
        assertEquals(Gender.MALE, p.gender)
        assertEquals(AgeGroup.ADULT_31_50, p.ageGroup)
        assertEquals(ActivityLevel.MODERATE, p.activityLevel)
        assertEquals("06:30", p.wakeUpTime)
        assertEquals("22:30", p.sleepTime)
        assertEquals(3000.0, p.dailyWaterGoal, 0.001)
        assertEquals(90, p.reminderInterval)
        assertEquals(75.0, p.weight!!, 0.001)
        assertEquals(HydrationStandard.IOM, p.hydrationStandard)
        assertTrue(p.healthConnectSyncEnabled)
        assertEquals(DayEndMode.MIDNIGHT, p.dayEndMode)
        assertEquals(ReminderIntervalMode.CUSTOM, p.reminderIntervalMode)
        assertEquals(45, p.customReminderInterval)

        assertEquals(DarkModePreference.DARK, result.theme.darkMode)
        assertEquals(ColorSource.HYDRO_THEME, result.theme.colorSource)
        assertEquals(AppFont.OUTFIT, result.theme.appFont)
        assertTrue(result.theme.autoHideNavBar)
        assertEquals(NavBarLabelMode.SELECTED, result.theme.navBarLabelMode)

        assertEquals(listOf("COFFEE", "TEA"), result.beverages.orderedVisible)
        assertEquals(setOf("JUICE"), result.beverages.hidden)
    }

    @Test
    fun unknownEnumValueCoercesToDefaultInsteadOfFailing() {
        prefs().edit {
            putBoolean("onboarding_completed", true)
            putString("gender", "MALE")
            putString("age_group", "ADULT_31_50")
            putString("activity_level", "MODERATE")
            putString("hydration_standard", "NOT_A_REAL_STANDARD") // removed/renamed in a future version
            putString("dark_mode", "BOGUS")
        }

        val result = mapLegacyPrefs(prefs(), AppPreferences())

        // Profile still builds, and the bad values fall back to defaults rather than wiping anything.
        assertNotNull(result.profile)
        assertEquals(HydrationStandard.EFSA, result.profile!!.hydrationStandard)
        assertEquals(DarkModePreference.SYSTEM, result.theme.darkMode)
    }

    @Test
    fun incompleteProfileLeavesProfileNullButStillMigratesTheme() {
        prefs().edit {
            putBoolean("onboarding_completed", true)
            // gender / age_group / activity_level missing -> not a valid profile
            putString("dark_mode", "LIGHT")
        }

        val result = mapLegacyPrefs(prefs(), AppPreferences())

        assertNull(result.profile)
        assertFalse(result.onboardingCompleted)
        assertEquals(DarkModePreference.LIGHT, result.theme.darkMode)
    }

    @Test
    fun freshInstallYieldsDefaults() {
        val result = mapLegacyPrefs(prefs(), AppPreferences())

        assertNull(result.profile)
        assertFalse(result.onboardingCompleted)
        assertEquals(AppPreferences().theme, result.theme)
        assertEquals(BeveragePreferences.default(), result.beverages)
    }
}
