package com.cemcakmak.hydrotracker.data.database.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.cemcakmak.hydrotracker.data.database.dao.WaterIntakeDao
import com.cemcakmak.hydrotracker.data.database.dao.DailySummaryDao
import com.cemcakmak.hydrotracker.data.database.dao.MostUsedQuickAddCombo
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.data.database.entities.DailySummary
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.models.DayEndMode
import com.cemcakmak.hydrotracker.data.models.UserProfile
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.*
import com.cemcakmak.hydrotracker.widgets.HydroWidgetUpdater
import com.cemcakmak.hydrotracker.utils.UserDayCalculator
import com.cemcakmak.hydrotracker.utils.ContainerIconMapper
import android.content.Context
import androidx.core.content.edit
import androidx.health.connect.client.records.HydrationRecord
import android.content.SharedPreferences
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import com.cemcakmak.hydrotracker.health.HealthConnectManager
import com.cemcakmak.hydrotracker.health.HealthConnectSyncManager
import kotlinx.coroutines.flow.first
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

    // Get today's user day string based on the configured day boundary.
    private suspend fun getTodayUserDayString(): String {
        val userProfile = userRepository.userProfile.first()
        val dayEndTime = getDayEndTime(userProfile)
        val dayEndMode = userProfile?.dayEndMode ?: DayEndMode.SLEEP_TIME
        return UserDayCalculator.getCurrentUserDayString(dayEndTime, dayEndMode)
    }

    /**
     * Returns the boundary time for the current user-day settings.
     * For [DayEndMode.SLEEP_TIME] this is the user's sleep time; for [DayEndMode.MIDNIGHT]
     * the value is unused, so a placeholder is returned.
     */
    private fun getDayEndTime(userProfile: UserProfile?): String {
        return when (userProfile?.dayEndMode) {
            DayEndMode.MIDNIGHT -> "00:00"
            else -> userProfile?.sleepTime ?: "23:00"
        }
    }

    /**
     * Check if a new user day has started and handle the transition
     * Should be called when the app starts or becomes foreground
     */
    suspend fun checkAndHandleNewUserDay() = withContext(Dispatchers.IO) {
        val userProfile = userRepository.userProfile.first() ?: return@withContext
        val dayEndTime = getDayEndTime(userProfile)
        val currentTime = System.currentTimeMillis()
        val lastCheckTime = prefs.getLong("last_day_check_time", 0L)

        if (lastCheckTime == 0L) {
            // First time running, just store current time
            prefs.edit { putLong("last_day_check_time", currentTime) }
            return@withContext
        }

        val dayEndMode = userProfile.dayEndMode
        if (UserDayCalculator.hasNewUserDayStarted(lastCheckTime, dayEndTime, dayEndMode)) {
            // New user day has started, update widgets to reflect reset
            HydroWidgetUpdater.updateAll(context)

            // Store the new check time
            prefs.edit { putLong("last_day_check_time", currentTime) }
        }
    }

    /**
     * One-time repair for the SLEEP_TIME boundary fix.
     *
     * Old entries were dated using wake-up time as the boundary, while the UI claimed the boundary
     * was sleep time. This routine rewrites every stored [WaterIntakeEntry.date] using the current
     * day-end settings, rebuilds all daily summaries, and records the migration so it never runs
     * again.
     */
    suspend fun repairUserDayBoundariesIfNeeded() = withContext(Dispatchers.IO) {
        try {
            val appPreferences = userRepository.appPreferences.first()
            if (appPreferences.dateBoundaryMigratedVersion >= 1) {
                Log.d(TAG, "User-day boundary migration already completed")
                return@withContext
            }

            val userProfile = userRepository.userProfile.first()
            if (userProfile == null || !userProfile.isOnboardingCompleted) {
                Log.d(TAG, "Skipping boundary migration: onboarding not complete")
                return@withContext
            }

            Log.i(TAG, "🛠️ Starting user-day boundary migration...")
            val dayEndTime = getDayEndTime(userProfile)
            val dayEndMode = userProfile.dayEndMode

            // Recompute the user-day string for every entry, including hidden ones, because
            // hidden entries still participate in duplicate detection.
            val allEntries = waterIntakeDao.getAllEntriesSync()
            if (allEntries.isNotEmpty()) {
                val recomputedEntries = allEntries.map { entry ->
                    val newDate = UserDayCalculator.getUserDayStringForTimestamp(
                        entry.timestamp,
                        dayEndTime,
                        dayEndMode
                    )
                    entry.copy(date = newDate)
                }

                // Bulk-update entries with their recomputed dates.
                waterIntakeDao.updateEntries(recomputedEntries)
                Log.i(TAG, "✅ Migrated ${recomputedEntries.size} entries to the corrected user-day boundary")
            } else {
                Log.d(TAG, "No entries to migrate")
            }

            // Rebuild summaries from scratch so they match the recomputed dates.
            dailySummaryDao.deleteAllSummaries()
            val distinctDates = waterIntakeDao.getAllEntriesSync()
                .filter { !it.isHidden }
                .map { it.date }
                .distinct()
            distinctDates.forEach { date ->
                updateDailySummaryForDate(date)
            }
            Log.i(TAG, "✅ Rebuilt ${distinctDates.size} daily summaries")

            // Refresh widgets and record completion.
            HydroWidgetUpdater.updateAll(context)
            userRepository.updateDateBoundaryMigratedVersion(1)
            Log.i(TAG, "🎉 User-day boundary migration complete")
        } catch (e: Exception) {
            Log.e(TAG, "❌ User-day boundary migration failed", e)
            // Do not mark as completed so the next launch retries.
        }
    }

    // ===== WATER INTAKE OPERATIONS =====

    suspend fun addWaterIntake(
        amount: Double,
        containerPreset: ContainerPreset,
        beverageKey: String = BeverageType.WATER.name,
        beverageMultiplier: Double? = null,
        note: String? = null,
        date: String? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📝 Adding water intake: ${amount}ml (${containerPreset.name})")

            val userDayString = date ?: getTodayUserDayString()
            val entry = WaterIntakeEntry(
                amount = amount,
                timestamp = System.currentTimeMillis(),
                date = userDayString,
                containerType = containerPreset.name,
                containerVolume = containerPreset.volume,
                beverageType = beverageKey,
                beverageMultiplier = beverageMultiplier,
                note = note,
                createdAt = System.currentTimeMillis(),
                iconType = containerPreset.iconType,
                iconName = containerPreset.iconName
            )

            Log.d(TAG, "💾 Saving water intake to local database...")
            val entryId = waterIntakeDao.insertEntry(entry)
            updateDailySummaryForDate(userDayString)

            // Update widgets after successful water intake
            Log.d(TAG, "🔄 Updating widgets...")
            HydroWidgetUpdater.updateAll(context)

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
            if (!entry.isSyncableToHealthConnect()) {
                Log.d(TAG, "👁️ Hiding external entry: ${entry.amount}ml (ID: ${entry.id}) from ${entry.containerType}")

                // External imports are read-only snapshots; hide them locally
                waterIntakeDao.hideEntry(entry.id)
                updateDailySummaryForDate(entry.date)

                // Update widgets after hiding
                Log.d(TAG, "🔄 Updating widgets...")
                HydroWidgetUpdater.updateAll(context)

                Log.i(TAG, "👁️ External entry hidden successfully: ${entry.amount}ml")
                Result.success(Unit)
            } else {
                Log.d(TAG, "🗑️ Deleting water intake: ${entry.amount}ml (ID: ${entry.id})")

                // Delete our own entries normally
                waterIntakeDao.deleteEntry(entry)
                updateDailySummaryForDate(entry.date)

                // Update widgets after successful deletion
                Log.d(TAG, "🔄 Updating widgets...")
                HydroWidgetUpdater.updateAll(context)

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
            HydroWidgetUpdater.updateAll(context)

            Log.i(TAG, "👁️ Water intake unhidden successfully: ${entry.amount}ml")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error unhiding water intake", e)
            Result.failure(e)
        }
    }

    fun getHiddenEntries(): Flow<List<WaterIntakeEntry>> = waterIntakeDao.getHiddenEntries()

    /**
     * Returns all non-hidden HydroTracker entries for export.
     */
    suspend fun getAllEntriesForExport(): List<WaterIntakeEntry> = withContext(Dispatchers.IO) {
        waterIntakeDao.getAllEntriesForExportSync()
    }

    /**
     * Check whether an entry already exists locally using the same Health Connect record ID or
     * timestamp/amount window used during Health Connect import.
     */
    suspend fun isDuplicateEntry(entry: WaterIntakeEntry): Boolean = withContext(Dispatchers.IO) {
        val existingEntries = waterIntakeDao.getAllEntriesForDateSync(entry.date)

        if (entry.healthConnectRecordId != null) {
            if (existingEntries.any { it.healthConnectRecordId == entry.healthConnectRecordId }) {
                return@withContext true
            }
        }

        val timeWindow = 5 * 60 * 1000L
        val amountTolerance = 10.0

        existingEntries.any { existing ->
            val timeDiff = kotlin.math.abs(existing.timestamp - entry.timestamp)
            val amountDiff = kotlin.math.abs(existing.getEffectiveHydrationAmount() - entry.getEffectiveHydrationAmount())
            timeDiff <= timeWindow && amountDiff <= amountTolerance
        }
    }

    suspend fun updateWaterIntake(oldEntry: WaterIntakeEntry, newEntry: WaterIntakeEntry): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "✏️ Updating water intake: ${newEntry.amount}ml (ID: ${newEntry.id})")
            waterIntakeDao.updateEntry(newEntry)
            updateDailySummaryForDate(newEntry.date)

            // Update widgets after successful update
            Log.d(TAG, "🔄 Updating widgets...")
            HydroWidgetUpdater.updateAll(context)

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

    /**
     * Returns the primary and secondary quick-add presets for notifications.
     *
     * The primary preset is the most-used container by entry count. The secondary preset
     * is the most recently logged container, unless its volume matches the primary volume,
     * in which case the next most-used container with a different volume is used instead.
     */
    suspend fun getQuickAddPresets(): QuickAddPresets = withContext(Dispatchers.IO) {
        val topContainers = waterIntakeDao.getMostUsedContainers(10)
        val mostRecent = waterIntakeDao.getMostRecentEntry()

        val primary = topContainers.firstOrNull()?.let {
            ContainerPreset(
                name = it.name,
                volume = it.volume,
                isDefault = false
            )
        }

        val secondary = when {
            primary == null || mostRecent == null -> null
            mostRecent.containerVolume != primary.volume -> ContainerPreset(
                name = mostRecent.containerType,
                volume = mostRecent.containerVolume,
                isDefault = false
            )
            else -> topContainers
                .drop(1)
                .firstOrNull { it.volume != primary.volume }
                ?.let {
                    ContainerPreset(
                        name = it.name,
                        volume = it.volume,
                        isDefault = false
                    )
                }
        }

        Log.d(
            TAG,
            "📝 Quick-add presets: primary=${primary?.let { "${it.name}/${it.volume}" } ?: "none"}, " +
                "secondary=${secondary?.let { "${it.name}/${it.volume}" } ?: "none"}"
        )

        QuickAddPresets(primary, secondary)
    }

    /**
     * Returns the [limit] most frequently logged (container, volume, beverage) combinations,
     * ranked by entry count. Drives the home-screen widget's quick-add cards.
     */
    suspend fun getTopQuickAddCombos(limit: Int = 3): List<MostUsedQuickAddCombo> =
        withContext(Dispatchers.IO) {
            waterIntakeDao.getMostUsedQuickAddCombos(limit)
        }

    suspend fun getAllEntriesForDate(date: String): List<WaterIntakeEntry> = withContext(Dispatchers.IO) {
        waterIntakeDao.getAllEntriesForDateSync(date)
    }

    /**
     * Observe all non-hidden water intake entries for a specific calendar date.
     */
    fun getEntriesForDate(date: String): Flow<List<WaterIntakeEntry>> {
        return waterIntakeDao.getEntriesForDate(date)
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
        HydroWidgetUpdater.updateAll(context)

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
                firstIntakeTime = entries.minByOrNull { it.timestamp }?.timestamp,
                lastIntakeTime = entries.maxByOrNull { it.timestamp }?.timestamp,
                isGoalAchieved = progress.isGoalAchieved,
                remainingAmount = progress.remainingAmount
            )
        }
    }

    fun getSummariesForRange(startDate: String, endDate: String): Flow<List<DailySummary>> {
        return dailySummaryDao.getSummariesForRange(startDate, endDate)
    }

    fun getAllSummaries(): Flow<List<DailySummary>> {
        return dailySummaryDao.getAllSummaries()
    }

    private suspend fun updateDailySummaryForDate(date: String) = withContext(Dispatchers.IO) {
        try {
            val userProfile = userRepository.userProfile.first()
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
            HydroWidgetUpdater.updateAll(context)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete all local water intake entries recorded on or before the supplied calendar date.
     * The cutoff is inclusive and uses the end of the selected day in the local time zone.
     */
    suspend fun deleteEntriesBefore(date: LocalDate): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val endOfDay = date.atTime(LocalTime.MAX)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            // Capture affected dates before deletion so summaries can be recomputed.
            val affectedDates = waterIntakeDao.getAllEntriesForDateRangeSync(
                startDate = "1970-01-01",
                endDate = date.toString()
            ).map { it.date }.distinct()

            val countBefore = waterIntakeDao.getEntryCount()
            waterIntakeDao.deleteEntriesBefore(endOfDay)
            val countAfter = waterIntakeDao.getEntryCount()
            val deleted = countBefore - countAfter

            affectedDates.forEach { updateDailySummaryForDate(it) }

            HydroWidgetUpdater.updateAll(context)
            Result.success(deleted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Count local water intake entries recorded on or before the supplied calendar date.
     * Uses the same inclusive end-of-day cutoff as [deleteEntriesBefore].
     */
    suspend fun countEntriesBefore(date: LocalDate): Int = withContext(Dispatchers.IO) {
        val endOfDay = date.atTime(LocalTime.MAX)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        waterIntakeDao.countEntriesBefore(endOfDay)
    }

    /**
     * Delete HydroTracker records from Health Connect that fall on or before the given calendar date.
     * Returns the number of records deleted.
     */
    suspend fun deleteHealthConnectEntriesBefore(
        context: Context,
        date: LocalDate
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (!HealthConnectManager.isAvailable(context) || !HealthConnectManager.hasPermissions(context)) {
                return@withContext Result.success(0)
            }

            val endOfDay = date.atTime(LocalTime.MAX)
                .atZone(ZoneId.systemDefault())
                .toInstant()

            val result = HealthConnectManager.readHydrationRecords(context, Instant.EPOCH, endOfDay)
            if (result.isFailure) {
                return@withContext Result.failure(
                    result.exceptionOrNull() ?: Exception("Failed to read Health Connect records")
                )
            }

            val hydroTrackerRecords = result.getOrNull()?.filter { record ->
                record.metadata.dataOrigin.packageName == "com.cemcakmak.hydrotracker" ||
                    record.metadata.clientRecordId?.startsWith("hydrotracker_") == true
            } ?: emptyList()

            var deletedCount = 0
            hydroTrackerRecords.forEach { record ->
                val recordId = record.metadata.clientRecordId ?: record.metadata.id
                val deleteResult = HealthConnectManager.deleteHydrationRecord(context, recordId)
                if (deleteResult.isSuccess) {
                    deletedCount++
                }
            }

            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete all HydroTracker records from Health Connect.
     */
    suspend fun deleteAllHealthConnectData(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        deleteHealthConnectRecords(context) { true }
    }

    private suspend fun deleteHealthConnectRecords(
        context: Context,
        predicate: (HydrationRecord) -> Boolean
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (!HealthConnectManager.isAvailable(context) || !HealthConnectManager.hasPermissions(context)) {
                return@withContext Result.success(0)
            }

            val result = HealthConnectManager.readHydrationRecords(context, Instant.EPOCH, Instant.now())
            if (result.isFailure) {
                return@withContext Result.failure(
                    result.exceptionOrNull() ?: Exception("Failed to read Health Connect records")
                )
            }

            val hydroTrackerRecords = result.getOrNull()?.filter { record ->
                (record.metadata.dataOrigin.packageName == "com.cemcakmak.hydrotracker" ||
                    record.metadata.clientRecordId?.startsWith("hydrotracker_") == true) &&
                    predicate(record)
            } ?: emptyList()

            var deletedCount = 0
            hydroTrackerRecords.forEach { record ->
                val recordId = record.metadata.clientRecordId ?: record.metadata.id
                val deleteResult = HealthConnectManager.deleteHydrationRecord(context, recordId)
                if (deleteResult.isSuccess) {
                    deletedCount++
                }
            }

            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Debug function to inject realistic water intake data for the past [days] days
     * This helps test the History & Statistics screen with meaningful data
     * Also creates DailySummary records so the multi-day views are populated
     */
    suspend fun injectDebugData(days: Int = 30): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val random = Random

            val containerTypes = listOf(
                "Coffee Cup" to 100.0,
                "Tea Cup" to 150.0,
                "Small Cup" to 175.0,
                "Medium Glass" to 200.0,
                "Large Glass" to 300.0,
                "Water Bottle" to 500.0,
                "Large Bottle" to 1000.0
            )

            val entries = mutableListOf<WaterIntakeEntry>()
            val summaries = mutableListOf<DailySummary>()

            // Generate data for the past [days] days
            for (dayOffset in 0 until days) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -dayOffset)

                val dateString = dateFormat.format(calendar.time)
                val baseDate = calendar.timeInMillis

                // Simulate realistic daily patterns
                val dailyGoal = userRepository.userProfile.first()?.dailyWaterGoal ?: 2700.0
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

                    val containerIcon = ContainerIconMapper.getIconForVolume(baseAmount)
                    val entry = WaterIntakeEntry(
                        amount = finalAmount,
                        timestamp = entryTimestamp,
                        date = dateString,
                        containerType = containerName,
                        containerVolume = baseAmount,
                        note = if (random.nextFloat() > 0.8f) {
                            listOf("After workout", "With meal", "Morning hydration", "Feeling thirsty", "Reminder").random()
                        } else null,
                        createdAt = entryTimestamp,
                        iconType = containerIcon.type.name,
                        iconName = containerIcon.name
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
)

data class TodayStatistics(
    val totalIntake: Double,
    val goalProgress: Float,
    val entryCount: Int,
    val averageIntake: Double,
    val largestIntake: Double,
    val firstIntakeTime: Long?,
    val lastIntakeTime: Long?,
    val isGoalAchieved: Boolean,
    val remainingAmount: Double
)

data class QuickAddPresets(
    val primary: ContainerPreset?,
    val secondary: ContainerPreset?
)