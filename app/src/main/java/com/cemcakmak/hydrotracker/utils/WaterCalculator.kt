package com.cemcakmak.hydrotracker.utils

import android.util.Log
import com.cemcakmak.hydrotracker.data.models.ActivityLevel
import com.cemcakmak.hydrotracker.data.models.Gender
import com.cemcakmak.hydrotracker.data.models.HydrationStandard
import com.cemcakmak.hydrotracker.data.models.ReminderIntervalMode
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Utility class for calculating daily water intake goals based on scientific research
 *
 * Based on:
 * - European Food Safety Authority (EFSA) guidelines (default)
 * - Institute of Medicine (IOM) recommendations (optional)
 * - Activity level adjustments from sports medicine research
 * - Evidence-based discrete activity additions
 */
object WaterCalculator {

    // Weight-based calculation factor (0.67 * weight in pounds converted to kg)
    private const val WEIGHT_FACTOR_ML_PER_KG = 30.0 // approximately 0.67 * 2.205 * 20

    /**
     * Calculates daily water intake goal based on user profile
     *
     * @param gender User's gender
     * @param activityLevel User's activity level
     * @param weight User's weight in kg (optional for more precise calculation)
     * @param hydrationStandard EFSA (default) or IOM standards
     * @return Daily water intake goal in milliliters
     */
    fun calculateDailyWaterGoal(
        gender: Gender,
        activityLevel: ActivityLevel,
        weight: Double? = null,
        hydrationStandard: HydrationStandard = HydrationStandard.EFSA
    ): Double {

        // Step 1: Get base intake based on gender and chosen standard
        val baseIntake = when (gender) {
            Gender.MALE -> hydrationStandard.getMaleIntake()
            Gender.FEMALE -> hydrationStandard.getFemaleIntake()
            Gender.OTHER -> (hydrationStandard.getMaleIntake() + hydrationStandard.getFemaleIntake()) / 2 // Average
        }

        // Step 2: NO age adjustments - research shows older adults need same intake
        // All adults get the same base amount regardless of age

        // Step 3: Apply discrete activity level additions (evidence-based)
        val activityAdjustedIntake = baseIntake + getActivityAddition(activityLevel)

        // Step 4: If weight is provided, use weight-based validation
        val finalIntake = weight?.let { weightKg ->
            val weightBasedIntake = weightKg * WEIGHT_FACTOR_ML_PER_KG
            // Use the higher of the two calculations for safety
            max(activityAdjustedIntake, weightBasedIntake)
        } ?: activityAdjustedIntake

        // Step 5: Ensure reasonable bounds (1.5L minimum, 5L maximum for safety)
        return finalIntake.coerceIn(1500.0, 5000.0)
    }

    /**
     * Gets discrete activity level additions based on research
     * Evidence shows fixed additions are more accurate than percentage multipliers
     */
    private fun getActivityAddition(activityLevel: ActivityLevel): Double {
        return when (activityLevel) {
            ActivityLevel.SEDENTARY -> 0.0        // No addition for sedentary
            ActivityLevel.LIGHT -> 400.0          // +0.4L for light activity
            ActivityLevel.MODERATE -> 500.0       // +0.5L for moderate activity
            ActivityLevel.ACTIVE -> 600.0         // +0.6L for active lifestyle
            ActivityLevel.VERY_ACTIVE -> 800.0    // +0.8L for very active
        }
    }

    /**
     * Calculates optimal reminder interval based on awake hours and daily goal
     *
     * @param wakeUpTime Wake-up time in HH:mm format
     * @param sleepTime Sleep time in HH:mm format
     * @param dailyGoal Daily water goal in milliliters
     * @param reminderIntervalMode Auto or Custom mode
     * @param customReminderInterval User-defined interval in minutes (used only in Custom mode)
     * @return Optimal reminder interval in minutes
     */
    fun calculateReminderInterval(
        wakeUpTime: String,
        sleepTime: String,
        dailyGoal: Double,
        reminderIntervalMode: ReminderIntervalMode = ReminderIntervalMode.AUTOMATIC,
        customReminderInterval: Int = 60
    ): Int {
        if (reminderIntervalMode == ReminderIntervalMode.CUSTOM) {
            return customReminderInterval.coerceAtLeast(1)
        }

        val awakeHours = calculateAwakeHours(wakeUpTime, sleepTime)

        Log.d("Awake Hours", "Awake hours: $awakeHours")

        // Glass-based reminder count: one reminder per ~300ml glass
        val targetReminders = (dailyGoal / 300.0).roundToInt().coerceAtLeast(1)

        val intervalMinutes = ((awakeHours * 60) / targetReminders).toInt()

        return intervalMinutes.coerceAtLeast(1)
    }

    /**
     * Calculates awake hours from wake up and sleep times
     */
    fun calculateAwakeHours(wakeUpTime: String, sleepTime: String): Double {
        return try {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val wakeUp = LocalTime.parse(wakeUpTime, formatter)
            val sleep = LocalTime.parse(sleepTime, formatter)

            val awakeMinutes = if (sleep.isAfter(wakeUp)) {
                // Same day (e.g., wake at 07:00, sleep at 23:00)
                sleep.toSecondOfDay() - wakeUp.toSecondOfDay()
            } else {
                // Next day (e.g., wake at 07:00, sleep at 01:00 next day)
                (24 * 3600) - wakeUp.toSecondOfDay() + sleep.toSecondOfDay()
            } / 60

            awakeMinutes / 60.0

        } catch (_: Exception) {
            // Fallback to 16 hours if time parsing fails
            16.0
        }
    }

    /**
     * Formats water amount for display
     *
     * @param amountMl Amount in milliliters
     * @return Formatted string (e.g., "2.5 L" or "750 ml")
     */
    fun formatWaterAmount(amountMl: Double): String {
        return when {
            amountMl >= 1000 -> "${(amountMl / 1000).format(1)} L"
            else -> "${amountMl.toInt()} ml"
        }
    }


    /**
     * Extension function to format Double with specified decimal places
     */
    private fun Double.format(decimals: Int): String {
        return "%.${decimals}f".format(this)
    }
}