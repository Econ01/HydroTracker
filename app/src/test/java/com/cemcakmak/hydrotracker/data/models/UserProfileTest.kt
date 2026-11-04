package com.cemcakmak.hydrotracker.data.models

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileTest {

    @Test
    fun `new profiles start with reminders disabled`() {
        assertFalse(testProfile().notificationsEnabled)
    }

    @Test
    fun `existing reminder opt in is preserved in the profile`() {
        val profile = testProfile().copy(notificationsEnabled = true)

        assertTrue(profile.notificationsEnabled)
    }

    private fun testProfile() = UserProfile(
        name = "Test",
        gender = Gender.MALE,
        ageGroup = AgeGroup.ADULT_31_50,
        activityLevel = ActivityLevel.MODERATE,
        wakeUpTime = "07:00",
        sleepTime = "23:00",
        dailyWaterGoal = 2700.0,
        reminderInterval = 120
    )
}
