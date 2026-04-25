package com.cemcakmak.hydrotracker.data.database.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.cemcakmak.hydrotracker.data.database.dao.WaterIntakeDao
import com.cemcakmak.hydrotracker.data.database.dao.DailySummaryDao
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.data.database.entities.DailySummary
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.models.BeverageType
import java.text.SimpleDateFormat
import java.util.*
import com.cemcakmak.hydrotracker.widgets.WidgetUpdateHelper
import com.cemcakmak.hydrotracker.utils.UserDayCalculator
import android.content.Context
import androidx.core.content.edit
import android.content.SharedPreferences
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import com.cemcakmak.hydrotracker.health.HealthConnectSyncManager
import kotlinx.coroutines.flow.flow
import kotlin.random.Random
import android.util.Log

class WaterIntakeRepository(
    private val waterIntakeDao: WaterIntakeDao,
    private val dailySummaryDao: DailySummaryDao,
    private val userRepository: UserRepository,
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "water_intake_prefs", Context.MODE_PRIVATE
    )

    // Helper property to access the sync manager
    private val healthConnectSyncManager get() = HealthConnectSyncManager

    // Public getter for UI components to access sync state
    fun getSyncManager(): HealthConnectSyncManager = healthConnectSyncManager

    // Public getter for accessing user repository
    fun getUserRepository(): UserRepository = userRepository

    companion object {
        private const val TAG = "WaterIntakeRepository"
    }

    // Get today's user day string based on wake-up time
    private fun getTodayUserDayString(): String {
        val userProfile = userRepository.userProfile.value
        val wakeUpTime = userProfile?.wakeUpTime ?: "07:00"
        return UserDayCalculator.getCurrentUserDayString(wakeUpTime)
    }

    /**
     * Check if a new user day has started and handle the transition
     * Should be called when the app starts or becomes foreground
     */
    suspend fun checkAndHandleNewUserDay() = withContext(Dispatchers.IO) {
        val userProfile = userRepository.userProfile.value ?: return@withContext
        val wakeUpTime = userProfile.wakeUpTime
        val currentTime = System.currentTimeMillis()
        val lastCheckTime = prefs.getLong("last_day_check_time", 0L)

        if (lastCheckTime == 0L) {
            // First time running, just store current time
            prefs.edit { putLong("last_day_check_time", currentTime) }
            return@withContext
        }

        if (UserDayCalculator.hasNewUserDayStarted(lastCheckTime, wakeUpTime)) {
            // New user day has started, update widgets to reflect reset
            WidgetUpdateHelper.updateAllWidgets(context)
            
            // Store the new check time
            prefs.edit { putLong("last_day_check_time", currentTime) }
        }
    }

    // ===== WATER INTAKE OPERATIONS =====

    suspend fun addWaterIntake(
        amount: Double,
        containerPreset: ContainerPreset,
        beverageType: BeverageType = BeverageType.WATER,
        note: String? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📝 Adding water intake: ${amount}ml (${containerPreset.name})")

            val userDayString = getTodayUserDayString()
            val entry = WaterIntakeEntry(
                amount = amount,
                timestamp = System.currentTimeMillis(),
                date = userDayString,
                containerType = containerPreset.name,
                containerVolume = containerPreset.volume,
                beverageType = beverageType.name,
                note = note,
                createdAt = System.currentTimeMillis()
            )

            Log.d(TAG, "💾 Saving water intake to local database...")
            val entryId = waterIntakeDao.insertEntry(entry)
            updateDailySummaryForDate(userDayString)

            // Update widgets after successful water intake
            Log.d(TAG, "🔄 Updating widgets...")
            WidgetUpdateHelper.updateAllWidgets(context)

            // Sync to Health Connect if enabled with UI feedback
            Log.d(TAG, "🏥 Initiating Health Connect sync...")
            val entryWithId = entry.copy(id = entryId)
            healthConnectSyncManager.syncWaterIntakeToHealthConnect(context, userRepository, this@WaterIntakeRepository, entryWithId)

            Log.i(TAG, "✅ Water intake added successfully: ${amount}ml (ID: $entryId)")
            Result.success(entryId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding water intake", e)
            Result.failure(e)
        }
    }

    suspend fun deleteWaterIntake(entry: WaterIntakeEntry): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (entry.isExternalEntry()) {
                Log.d(TAG, "👁️ Hiding external entry: ${entry.amount}ml (ID: ${entry.id}) from ${entry.containerType}")

                // Hide external entries instead of deleting them
                waterIntakeDao.hideEntry(entry.id)
                updateDailySummaryForDate(entry.date)

                // Update widgets after hiding
                Log.d(TAG, "🔄 Updating widgets...")
                WidgetUpdateHelper.updateAllWidgets(context)

                Log.i(TAG, "👁️ External entry hidden successfully: ${entry.amount}ml")
                Result.success(Unit)
            } else {
                Log.d(TAG, "🗑️ Deleting water intake: ${entry.amount}ml (ID: ${entry.id})")

                // Delete our own entries normally
                waterIntakeDao.deleteEntry(entry)
                updateDailySummaryForDate(entry.date)

                // Update widgets after successful deletion
                Log.d(TAG, "🔄 Updating widgets...")
                WidgetUpdateHelper.updateAllWidgets(context)

                // Handle Health Connect delete using proper API
                Log.d(TAG, "🏥 Deleting entry from Health Connect...")
                healthConnectSyncManager.handleWaterIntakeDelete(context, userRepository, entry)

                Log.i(TAG, "✅ Water intake deleted successfully: ${entry.amount}ml")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting/hiding water intake", e)
            Result.failure(e)
        }
    }

    suspend fun unhideWaterIntake(entry: WaterIntakeEntry): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "👁️ Unhiding water intake: ${entry.amount}ml (ID: ${entry.id})")

            waterIntakeDao.unhideEntry(entry.id)
            updateDailySummaryForDate(entry.date)

            // Update widgets after unhiding
            Log.d(TAG, "🔄 Updating widgets...")
            WidgetUpdateHelper.updateAllWidgets(context)

            Log.i(TAG, "👁️ Water intake unhidden successfully: ${entry.amount}ml")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error unhiding water intake", e)
            Result.failure(e)
        }
    }

    fun getHiddenEntries(): Flow<List<WaterIntakeEntry>> = waterIntakeDao.getHiddenEntries()

    suspend fun updateWaterIntake(oldEntry: WaterIntakeEntry, newEntry: WaterIntakeEntry): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "✏️ Updating water intake: ${newEntry.amount}ml (ID: ${newEntry.id})")
            waterIntakeDao.updateEntry(newEntry)
            updateDailySummaryForDate(newEntry.date)

            // Update widgets after successful update
            Log.d(TAG, "🔄 Updating widgets...")
            WidgetUpdateHelper.updateAllWidgets(context)

            // Re-sync updated entry to Health Connect using delete + add pattern
            Log.d(TAG, "🏥 Re-syncing updated entry to Health Connect...")
            healthConnectSyncManager.syncUpdatedWaterIntakeToHealthConnect(context, userRepository, this@WaterIntakeRepository, oldEntry, newEntry)

            Log.i(TAG, "✅ Water intake updated successfully: ${newEntry.amount}ml")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating water intake", e)
            Result.failure(e)
        }
    }

    /**
     * Update a water intake entry without triggering Health Connect sync
     * Used internally for updating Health Connect record IDs
     */
    suspend fun updateWaterIntakeEntry(entry: WaterIntakeEntry): Unit = withContext(Dispatchers.IO) {
        waterIntakeDao.updateEntry(entry)
        updateDailySummaryForDate(entry.date)
    }

    // ===== QUERY OPERATIONS =====

    fun getTodayEntries(): Flow<List<WaterIntakeEntry>> {
        return flow {
            val userDayString = getTodayUserDayString()
            waterIntakeDao.getEntriesForDate(userDayString).collect { emit(it) }
        }
    }

    fun getTodayTotalIntake(): Flow<Double> {
        return flow {
            val userDayString = getTodayUserDayString()
            waterIntakeDao.getEntriesForDate(userDayString).collect { entries ->
                // Calculate effective hydration considering beverage type multipliers
                val effectiveTotal = entries.sumOf { entry ->
                    entry.getEffectiveHydrationAmount()
                }
                emit(effectiveTotal)
            }
        }
    }

    fun getLast30DaysEntries(): Flow<List<WaterIntakeEntry>> {
        return waterIntakeDao.getLast30DaysEntries()
    }

    fun getAllEntries(): Flow<List<WaterIntakeEntry>> {
        return waterIntakeDao.getAllEntries()
    }

    suspend fun getAllEntriesForDate(date: String): List<WaterIntakeEntry> = withContext(Dispatchers.IO) {
        waterIntakeDao.getAllEntriesForDateSync(date)
    }

    /**
     * Add an imported water entry without triggering Health Connect sync
     * Used for importing external data to avoid circular syncing
     */
    suspend fun addImportedWaterEntry(entry: WaterIntakeEntry): Long = withContext(Dispatchers.IO) {
        Log.d(TAG, "📥 Adding imported water entry: ${entry.amount}ml from ${entry.note}")

        val entryId = waterIntakeDao.insertEntry(entry)
        updateDailySummaryForDate(entry.date)

        // Update widgets after importing
        Log.d(TAG, "🔄 Updating widgets after import...")
        WidgetUpdateHelper.updateAllWidgets(context)

        Log.i(TAG, "✅ Imported entry saved with ID: $entryId")
        entryId
    }

    // ===== PROGRESS & STATISTICS =====

    fun getTodayProgress(): Flow<WaterProgress> {
        return combine(
            getTodayTotalIntake(),
            userRepository.userProfile
        ) { totalIntake, userProfile ->
            val goal = userProfile?.dailyWaterGoal ?: 2700.0
            WaterProgress(
                currentIntake = totalIntake,
                dailyGoal = goal,
                progress = (totalIntake / goal).toFloat().coerceIn(0f, 1f),
                isGoalAchieved = totalIntake >= goal,
                remainingAmount = maxOf(0.0, goal - totalIntake)
            )
        }
    }

    fun getTodayStatistics(): Flow<TodayStatistics> {
        return combine(
            getTodayEntries(),
            getTodayProgress()
        ) { entries, progress ->
            TodayStatistics(
                totalIntake = progress.currentIntake,
                goalProgress = progress.progress,
                entryCount = entries.size,
                averageIntake = if (entries.isNotEmpty()) progress.currentIntake / entries.size else 0.0,
                largestIntake = entries.maxOfOrNull { it.getEffectiveHydrationAmount() } ?: 0.0,
                firstIntakeTime = entries.minByOrNull { it.timestamp }?.getFormattedTime(),
                lastIntakeTime = entries.maxByOrNull { it.timestamp }?.getFormattedTime(),
                isGoalAchieved = progress.isGoalAchieved,
                remainingAmount = progress.remainingAmount
            )
        }
    }

    // ===== DAILY SUMMARY OPERATIONS =====

    fun getLast30DaysSummaries(): Flow<List<DailySummary>> {
        return dailySummaryDao.getLast30DaysSummaries()
    }

    fun getAllSummaries(): Flow<List<DailySummary>> {
        return dailySummaryDao.getAllSummaries()
    }

    private suspend fun updateDailySummaryForDate(date: String) = withContext(Dispatchers.IO) {
        try {
            val userProfile = userRepository.userProfile.value
            val dailyGoal = userProfile?.dailyWaterGoal ?: 2700.0

            // Get all entries for this date to calculate effective hydration
            val entries = waterIntakeDao.getAllEntriesForDateSync(date).filter { !it.isHidden }

            if (entries.isNotEmpty()) {
                // Calculate effective hydration considering beverage type multipliers
                val totalIntake = entries.sumOf { it.getEffectiveHydrationAmount() }
                val entryCount = entries.size
                val goalPercentage = (totalIntake / dailyGoal).toFloat()
                val goalAchieved = totalIntake >= dailyGoal

                // Since this is a Flow, we need to collect it once. For now, we'll use basic stats
                val averageIntake = if (entryCount > 0) totalIntake / entryCount else 0.0

                val summary = DailySummary(
                    date = date,
                    totalIntake = totalIntake,
                    dailyGoal = dailyGoal,
                    goalAchieved = goalAchieved,
                    goalPercentage = goalPercentage,
                    entryCount = entryCount,
                    firstIntakeTime = null, // Could be calculated from entries if needed
                    lastIntakeTime = null,  // Could be calculated from entries if needed
                    largestIntake = 0.0,    // Could be calculated from entries if needed
                    averageIntake = averageIntake
                )

                dailySummaryDao.insertSummary(summary)
            }
        } catch (e: Exception) {
            // Handle error - in production you might want to log this
            println("Error updating daily summary for $date: ${e.message}")
        }
    }

    // ===== BULK OPERATIONS =====

    suspend fun clearAllData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            waterIntakeDao.deleteAllEntries()
            dailySummaryDao.deleteAllSummaries()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Debug function to inject realistic water intake data for the past 30 days
     * This helps test the History & Statistics screen with meaningful data
     * FIXED: Now also creates DailySummary records for 30-day view
     */
    suspend fun injectDebugData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val random = Random

            val containerTypes = listOf(
                "Small Glass" to 200.0,
                "Large Glass" to 300.0,
                "Water Bottle" to 500.0,
                "Large Bottle" to 750.0,
                "Coffee Mug" to 250.0,
                "Sports Bottle" to 1000.0
            )

            val entries = mutableListOf<WaterIntakeEntry>()
            val summaries = mutableListOf<DailySummary>()

            // Generate data for the past 30 days
            for (dayOffset in 0..29) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -dayOffset)

                val dateString = dateFormat.format(calendar.time)
                val baseDate = calendar.timeInMillis

                // Simulate realistic daily patterns
                val dailyGoal = userRepository.userProfile.value?.dailyWaterGoal ?: 2700.0
                val achievementRate = random.nextFloat()

                // Create different daily patterns
                val dailyIntakeTarget = when {
                    achievementRate > 0.9f -> dailyGoal * (1.0 + random.nextFloat() * 0.3) // Exceed goal
                    achievementRate > 0.7f -> dailyGoal * (0.8 + random.nextFloat() * 0.3) // Meet or close to goal
                    achievementRate > 0.4f -> dailyGoal * (0.5 + random.nextFloat() * 0.3) // Moderate intake
                    else -> dailyGoal * (0.2 + random.nextFloat() * 0.3) // Low intake days
                }

                // Generate 3-8 entries per day with realistic timing
                val entriesPerDay = random.nextInt(3, 9)
                var totalDailyIntake = 0.0
                var largestIntake = 0.0
                var firstIntakeTime: Long? = null
                var lastIntakeTime: Long? = null

                // Create entries distributed throughout the day
                val wakeUpHour = 6 + random.nextInt(0, 4) // 6-9 AM
                val sleepHour = 21 + random.nextInt(0, 4) // 9 PM - 1 AM
                val activeHours = sleepHour - wakeUpHour

                for (entryIndex in 0 until entriesPerDay) {
                    // Distribute entries throughout active hours
                    val hourOffset = (activeHours.toFloat() / entriesPerDay) * entryIndex + random.nextFloat() * 2
                    val entryHour = (wakeUpHour + hourOffset).toInt().coerceIn(wakeUpHour, sleepHour)
                    val entryMinute = random.nextInt(0, 60)

                    // Set time for this entry
                    calendar.time = Date(baseDate)
                    calendar.set(Calendar.HOUR_OF_DAY, entryHour)
                    calendar.set(Calendar.MINUTE, entryMinute)
                    calendar.set(Calendar.SECOND, random.nextInt(0, 60))

                    val entryTimestamp = calendar.timeInMillis

                    // Track first and last intake times
                    if (firstIntakeTime == null || entryTimestamp < firstIntakeTime) {
                        firstIntakeTime = entryTimestamp
                    }
                    if (lastIntakeTime == null || entryTimestamp > lastIntakeTime) {
                        lastIntakeTime = entryTimestamp
                    }

                    // Choose container type and amount
                    val (containerName, baseAmount) = containerTypes.random()
                    val amount = baseAmount + random.nextFloat() * 50 - 25 // Add some variation

                    // Ensure we don't exceed the daily target too much
                    val remainingTarget = dailyIntakeTarget - totalDailyIntake
                    val finalAmount = if (entryIndex == entriesPerDay - 1) {
                        // Last entry of the day - try to hit target
                        maxOf(100.0, minOf(amount, remainingTarget))
                    } else {
                        minOf(amount, remainingTarget / (entriesPerDay - entryIndex))
                    }.coerceAtLeast(50.0) // Minimum 50ml per entry

                    totalDailyIntake += finalAmount
                    if (finalAmount > largestIntake) {
                        largestIntake = finalAmount
                    }

                    val entry = WaterIntakeEntry(
                        amount = finalAmount,
                        timestamp = entryTimestamp,
                        date = dateString,
                        containerType = containerName,
                        containerVolume = baseAmount,
                        note = if (random.nextFloat() > 0.8f) {
                            listOf("After workout", "With meal", "Morning hydration", "Feeling thirsty", "Reminder").random()
                        } else null,
                        createdAt = entryTimestamp
                    )

                    entries.add(entry)
                }

                // Create corresponding DailySummary
                val goalPercentage = (totalDailyIntake / dailyGoal).toFloat()
                val goalAchieved = totalDailyIntake >= dailyGoal
                val averageIntake = if (entriesPerDay > 0) totalDailyIntake / entriesPerDay else 0.0

                val summary = DailySummary(
                    date = dateString,
                    totalIntake = totalDailyIntake,
                    dailyGoal = dailyGoal,
                    goalAchieved = goalAchieved,
                    goalPercentage = goalPercentage,
                    entryCount = entriesPerDay,
                    firstIntakeTime = firstIntakeTime,
                    lastIntakeTime = lastIntakeTime,
                    largestIntake = largestIntake,
                    averageIntake = averageIntake
                )

                summaries.add(summary)
            }

            // Insert all entries and summaries
            waterIntakeDao.insertEntries(entries)
            dailySummaryDao.insertSummaries(summaries)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ===== DATA CLASSES =====

data class WaterProgress(
    val currentIntake: Double,
    val dailyGoal: Double,
    val progress: Float,
    val isGoalAchieved: Boolean,
    val remainingAmount: Double
) {
    fun getFormattedCurrent(): String {
        return when {
            currentIntake >= 1000 -> "${String.format(Locale.US, "%.1f", currentIntake / 1000)} L"
            else -> "${currentIntake.toInt()} ml"
        }
    }

    fun getFormattedGoal(): String {
        return when {
            dailyGoal >= 1000 -> "${String.format(Locale.US, "%.1f", dailyGoal / 1000)} L"
            else -> "${dailyGoal.toInt()} ml"
        }
    }

}

data class TodayStatistics(
    val totalIntake: Double,
    val goalProgress: Float,
    val entryCount: Int,
    val averageIntake: Double,
    val largestIntake: Double,
    val firstIntakeTime: String?,
    val lastIntakeTime: String?,
    val isGoalAchieved: Boolean,
    val remainingAmount: Double
)