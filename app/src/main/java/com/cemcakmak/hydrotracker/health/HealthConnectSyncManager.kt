package com.cemcakmak.hydrotracker.health

import android.content.Context
import android.util.Log
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.data.repository.UserRepository
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Health Connect synchronization operations
 */
object HealthConnectSyncManager {
    private const val TAG = "HealthConnectSync"

    // Sync state tracking
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)

    /**
     * Sync a water intake entry to Health Connect if conditions are met
     */
    fun syncWaterIntakeToHealthConnect(context: Context, userRepository: UserRepository, waterIntakeRepository: WaterIntakeRepository, entry: WaterIntakeEntry) {
        Log.d(TAG, "🔄 Sync request received for entry: ${entry.amount}ml at ${entry.timestamp}")

        CoroutineScope(Dispatchers.IO).launch {
            _isSyncing.value = true
            Log.d(TAG, "🔄 Setting isSyncing = true for regular sync")
            try {
                // Check if user has sync enabled
                val userProfile = userRepository.userProfile.value
                if (userProfile?.healthConnectSyncEnabled != true) {
                    Log.d(TAG, "⏭️ Sync skipped: Health Connect sync disabled in user settings")
                    _isSyncing.value = false
                    return@launch
                }
                Log.d(TAG, "✅ User has Health Connect sync enabled")

                // Check if Health Connect is available and has permissions
                if (!HealthConnectManager.isAvailable(context)) {
                    Log.w(TAG, "⚠️ Sync skipped: Health Connect not available on device")
                    _isSyncing.value = false
                    return@launch
                }
                Log.d(TAG, "✅ Health Connect is available")

                if (!HealthConnectManager.hasPermissions(context)) {
                    Log.w(TAG, "⚠️ Sync skipped: Missing Health Connect permissions")
                    _isSyncing.value = false
                    return@launch
                }
                Log.d(TAG, "✅ Health Connect permissions granted")

                // Perform the sync
                Log.i(TAG, "🚀 Starting Health Connect sync for ${entry.amount}ml")
                val result = HealthConnectManager.writeHydrationRecord(context,entry)

                if (result.isSuccess) {
                    val recordId = result.getOrNull()
                    Log.i(TAG, "✅ Health Connect sync completed successfully: $recordId")
                    _lastSyncTime.value = System.currentTimeMillis()

                    // Update the database entry with the Health Connect record ID
                    if (recordId != null) {
                        val entryWithRecordId = entry.copy(healthConnectRecordId = recordId)
                        waterIntakeRepository.updateWaterIntakeEntry(entryWithRecordId)
                        Log.d(TAG, "📝 Updated database entry with Health Connect record ID: $recordId")
                    }
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "❌ Health Connect sync failed", error)
                    Log.e(TAG, "Failed entry: amount=${entry.amount}ml, timestamp=${entry.timestamp}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Unexpected error during Health Connect sync", e)
                Log.e(TAG, "Entry details: amount=${entry.amount}ml, date=${entry.date}")
            } finally {
                // Add a small delay to ensure the sync state is visible to users
                kotlinx.coroutines.delay(800)
                _isSyncing.value = false
                Log.d(TAG, "🔄 Setting isSyncing = false for regular sync")
            }
        }
    }

    /**
     * Import external hydration data from Health Connect
     * Only imports data not created by HydroTracker to avoid duplicates
     */
    fun importExternalHydrationData(context: Context, userRepository: UserRepository, waterIntakeRepository: WaterIntakeRepository, since: java.time.Instant, onImportComplete: (imported: Int, errors: Int) -> Unit = { _, _ -> }) {
        Log.d(TAG, "🔄 Import request received for external data since: $since")

        CoroutineScope(Dispatchers.IO).launch {
            _isSyncing.value = true
            Log.d(TAG, "🔄 Setting isSyncing = true for import sync")
            try {
                // Check if user has sync enabled
                val userProfile = userRepository.userProfile.value
                if (userProfile?.healthConnectSyncEnabled != true) {
                    Log.d(TAG, "⏭️ Import skipped: Health Connect sync disabled")
                    _isSyncing.value = false
                    onImportComplete(0, 0)
                    return@launch
                }

                if (!HealthConnectManager.isAvailable(context) || !HealthConnectManager.hasPermissions(context)) {
                    Log.w(TAG, "⚠️ Import skipped: Health Connect not ready")
                    _isSyncing.value = false
                    onImportComplete(0, 1)
                    return@launch
                }

                Log.i(TAG, "🚀 Starting Health Connect import for external data")

                // Read external records only (exclude HydroTracker's own records)
                val result = HealthConnectManager.readExternalHydrationRecords(context,since)

                if (result.isFailure) {
                    Log.e(TAG, "❌ Failed to read external records: ${result.exceptionOrNull()?.message}")
                    onImportComplete(0, 1)
                    return@launch
                }

                val externalRecords = result.getOrNull() ?: emptyList()
                Log.i(TAG, "📥 Found ${externalRecords.size} external records to potentially import")

                if (externalRecords.isEmpty()) {
                    Log.d(TAG, "✅ No new external records to import")
                    _isSyncing.value = false
                    onImportComplete(0, 0)
                    return@launch
                }

                var importedCount = 0
                var errorCount = 0

                // Process each external record with conflict resolution
                val wakeUpTime = userRepository.userProfile.value?.wakeUpTime ?: "07:00"
                externalRecords.forEach { record ->
                    try {
                        val waterIntakeEntry = HealthConnectManager.hydrationRecordToWaterIntakeEntry(
                            record,
                            record.metadata.dataOrigin.toString(),
                            wakeUpTime
                        )

                        // Check for potential duplicates using timestamp and amount
                        val isDuplicate = checkForDuplicate(waterIntakeRepository, waterIntakeEntry)

                        if (isDuplicate) {
                            Log.d(TAG, "⚠️ Duplicate detected, skipping: ${waterIntakeEntry.amount}ml at ${waterIntakeEntry.timestamp}")
                        } else {
                            // Add to database
                            val result = addImportedWaterEntry(waterIntakeRepository, waterIntakeEntry)
                            if (result.isSuccess) {
                                importedCount++
                                Log.d(TAG, "📥 Imported: ${waterIntakeEntry.amount}ml from ${waterIntakeEntry.note}")
                            } else {
                                errorCount++
                                Log.e(TAG, "❌ Failed to import: ${result.exceptionOrNull()?.message}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error processing record: ${e.message}")
                        errorCount++
                    }
                }

                Log.i(TAG, "📊 Import completed: $importedCount entries imported, $errorCount errors")
                onImportComplete(importedCount, errorCount)

                // Update last sync time if we imported anything
                if (importedCount > 0) {
                    _lastSyncTime.value = System.currentTimeMillis()
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Unexpected error during Health Connect import", e)
                onImportComplete(0, 1)
            } finally {
                // Add a small delay to ensure the sync state is visible to users
                kotlinx.coroutines.delay(800)
                _isSyncing.value = false
                Log.d(TAG, "🔄 Setting isSyncing = false for import sync")
            }
        }
    }

    /**
     * Perform app launch sync to import any missed external data
     * This should be called when the app starts to catch up on external hydration data
     */
    fun performAppLaunchSync(context: Context, userRepository: UserRepository, waterIntakeRepository: WaterIntakeRepository) {
        Log.d(TAG, "🚀 Starting app launch sync...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if user has sync enabled
                val userProfile = userRepository.userProfile.value
                if (userProfile?.healthConnectSyncEnabled != true) {
                    Log.d(TAG, "⏭️ App launch sync skipped: Health Connect sync disabled")
                    return@launch
                }

                // Check if Health Connect is available and has permissions
                if (!HealthConnectManager.isAvailable(context) || !HealthConnectManager.hasPermissions(context)) {
                    Log.d(TAG, "⏭️ App launch sync skipped: Health Connect not ready")
                    return@launch
                }

                Log.i(TAG, "🔄 Performing app launch sync for external hydration data...")

                // Import external data from the last 7 days to catch any missed entries
                val since = java.time.Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS)

                importExternalHydrationData(context, userRepository, waterIntakeRepository, since) { imported, errors ->
                    if (imported > 0) {
                        Log.i(TAG, "✅ App launch sync completed: $imported entries imported, $errors errors")
                    } else {
                        Log.d(TAG, "📝 App launch sync completed: No new external data found")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error during app launch sync", e)
            }
        }
    }

    /**
     * Restore HydroTracker history from Health Connect
     * Imports HydroTracker-tagged records back into the local database
     * Used after reinstall when local data is empty but Health Connect still holds records
     */
    fun restoreHydroTrackerHistory(
        context: Context,
        userRepository: UserRepository,
        waterIntakeRepository: WaterIntakeRepository,
        since: java.time.Instant,
        onComplete: (imported: Int, skipped: Int) -> Unit = { _, _ -> }
    ) {
        Log.d(TAG, "🔄 Restore request received for HydroTracker history since: $since")

        CoroutineScope(Dispatchers.IO).launch {
            _isSyncing.value = true
            Log.d(TAG, "🔄 Setting isSyncing = true for restore sync")
            try {
                // Check if user has sync enabled
                val userProfile = userRepository.userProfile.value
                if (userProfile?.healthConnectSyncEnabled != true) {
                    Log.d(TAG, "⏭️ Restore skipped: Health Connect sync disabled")
                    _isSyncing.value = false
                    onComplete(0, 0)
                    return@launch
                }

                if (!HealthConnectManager.isAvailable(context) || !HealthConnectManager.hasPermissions(context)) {
                    Log.w(TAG, "⚠️ Restore skipped: Health Connect not ready")
                    _isSyncing.value = false
                    onComplete(0, 0)
                    return@launch
                }

                Log.i(TAG, "🚀 Starting HydroTracker history restore from Health Connect")

                // Read HydroTracker records only
                val result = HealthConnectManager.readHydroTrackerRecords(context, since)

                if (result.isFailure) {
                    Log.e(TAG, "❌ Failed to read HydroTracker records: ${result.exceptionOrNull()?.message}")
                    _isSyncing.value = false
                    onComplete(0, 0)
                    return@launch
                }

                val hydroTrackerRecords = result.getOrNull() ?: emptyList()
                Log.i(TAG, "📥 Found ${hydroTrackerRecords.size} HydroTracker records to restore")

                if (hydroTrackerRecords.isEmpty()) {
                    Log.d(TAG, "✅ No HydroTracker records found to restore")
                    _isSyncing.value = false
                    onComplete(0, 0)
                    return@launch
                }

                var importedCount = 0
                var skippedCount = 0

                val wakeUpTime = userRepository.userProfile.value?.wakeUpTime ?: "07:00"
                hydroTrackerRecords.forEach { record ->
                    try {
                        // For HydroTracker records, prefer our clientRecordId so it matches
                        // what we store locally after sync (hydrotracker_${entry.id}_${timestamp})
                        val recordIdForLocalStorage = record.metadata.clientRecordId ?: record.metadata.id

                        val waterIntakeEntry = HealthConnectManager.hydrationRecordToWaterIntakeEntry(
                            record,
                            record.metadata.dataOrigin.toString(),
                            wakeUpTime,
                            healthConnectRecordId = recordIdForLocalStorage
                        )

                        // Check for duplicates using existing logic
                        val isDuplicate = checkForDuplicate(waterIntakeRepository, waterIntakeEntry)

                        if (isDuplicate) {
                            Log.d(TAG, "⚠️ Duplicate detected, skipping: ${waterIntakeEntry.amount}ml at ${waterIntakeEntry.timestamp}")
                            skippedCount++
                        } else {
                            val addResult = addImportedWaterEntry(waterIntakeRepository, waterIntakeEntry)
                            if (addResult.isSuccess) {
                                importedCount++
                                Log.d(TAG, "📥 Restored: ${waterIntakeEntry.amount}ml at ${waterIntakeEntry.timestamp}")
                            } else {
                                Log.e(TAG, "❌ Failed to restore entry: ${addResult.exceptionOrNull()?.message}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error processing HydroTracker record: ${e.message}")
                    }
                }

                Log.i(TAG, "📊 Restore completed: $importedCount entries imported, $skippedCount duplicates skipped")
                onComplete(importedCount, skippedCount)

                if (importedCount > 0) {
                    _lastSyncTime.value = System.currentTimeMillis()
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Unexpected error during HydroTracker history restore", e)
                onComplete(0, 0)
            } finally {
                kotlinx.coroutines.delay(800)
                _isSyncing.value = false
                Log.d(TAG, "🔄 Setting isSyncing = false for restore sync")
            }
        }
    }

    /**
     * Handle update of a water intake entry using delete + add pattern
     * This properly handles Health Connect updates by deleting the old record and adding a new one
     */
    fun syncUpdatedWaterIntakeToHealthConnect(context: Context, userRepository: UserRepository, waterIntakeRepository: WaterIntakeRepository, oldEntry: WaterIntakeEntry, updatedEntry: WaterIntakeEntry) {
        Log.d(TAG, "🔄 Update sync request received for entry: ${updatedEntry.amount}ml (ID: ${updatedEntry.id})")

        CoroutineScope(Dispatchers.IO).launch {
            _isSyncing.value = true
            Log.d(TAG, "🔄 Setting isSyncing = true for update sync")
            try {
                // Check if user has sync enabled
                val userProfile = userRepository.userProfile.value
                if (userProfile?.healthConnectSyncEnabled != true) {
                    Log.d(TAG, "⏭️ Update sync skipped: Health Connect sync disabled")
                    return@launch
                }

                // Check if Health Connect is available and has permissions
                if (!HealthConnectManager.isAvailable(context) || !HealthConnectManager.hasPermissions(context)) {
                    Log.w(TAG, "⚠️ Update sync skipped: Health Connect not ready")
                    return@launch
                }

                Log.i(TAG, "🔄 Updating entry in Health Connect using delete + add pattern")

                // Step 1: Delete the old record if it has a Health Connect record ID
                val oldHealthConnectRecordId = oldEntry.healthConnectRecordId
                if (oldHealthConnectRecordId != null) {
                    Log.d(TAG, "🗑️ Deleting old record from Health Connect: $oldHealthConnectRecordId")
                    val deleteResult = HealthConnectManager.deleteHydrationRecord(context,oldHealthConnectRecordId)

                    if (deleteResult.isFailure) {
                        Log.w(TAG, "⚠️ Failed to delete old record, but continuing with add: ${deleteResult.exceptionOrNull()?.message}")
                    } else {
                        Log.d(TAG, "✅ Old record deleted successfully")
                    }
                }

                // Step 2: Add the new record
                Log.d(TAG, "➕ Adding updated record to Health Connect")
                val addResult = HealthConnectManager.writeHydrationRecord(context,updatedEntry)

                if (addResult.isSuccess) {
                    val newRecordId = addResult.getOrNull()
                    Log.i(TAG, "✅ Updated entry synced successfully to Health Connect: $newRecordId")

                    // Update the database entry with the new Health Connect record ID
                    if (newRecordId != null) {
                        val entryWithNewId = updatedEntry.copy(healthConnectRecordId = newRecordId)
                        waterIntakeRepository.updateWaterIntakeEntry(entryWithNewId)
                        Log.d(TAG, "📝 Updated database entry with new Health Connect record ID")
                    }

                    _lastSyncTime.value = System.currentTimeMillis()
                } else {
                    Log.e(TAG, "❌ Failed to sync updated entry: ${addResult.exceptionOrNull()?.message}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Unexpected error during update sync", e)
            } finally {
                // Add a small delay to ensure the sync state is visible to users
                kotlinx.coroutines.delay(800)
                _isSyncing.value = false
                Log.d(TAG, "🔄 Setting isSyncing = false for update sync")
            }
        }
    }

    /**
     * Handle deletion of a water intake entry from Health Connect
     * Uses the proper Health Connect deletion API
     */
    fun handleWaterIntakeDelete(context: Context, userRepository: UserRepository, deletedEntry: WaterIntakeEntry) {
        Log.d(TAG, "🗑️ Delete notification received for entry: ${deletedEntry.amount}ml (ID: ${deletedEntry.id})")
        Log.d(TAG, "🔍 Entry details: note='${deletedEntry.note}', containerType='${deletedEntry.containerType}'")
        Log.d(TAG, "🆔 Health Connect Record ID: '${deletedEntry.healthConnectRecordId}'")

        CoroutineScope(Dispatchers.IO).launch {
            _isSyncing.value = true
            try {
                // Check if user has sync enabled
                val userProfile = userRepository.userProfile.value
                if (userProfile?.healthConnectSyncEnabled != true) {
                    Log.d(TAG, "⏭️ Delete handling skipped: Health Connect sync disabled")
                    return@launch
                }

                // Check if this entry has a Health Connect record ID
                var healthConnectRecordId = deletedEntry.healthConnectRecordId
                if (healthConnectRecordId == null) {
                    Log.w(TAG, "⚠️ No Health Connect record ID found for entry")
                    Log.d(TAG, "🔍 Attempting to find matching record in Health Connect...")

                    // Try to find the record by searching Health Connect
                    val foundRecordId = findHealthConnectRecordId(context, deletedEntry)
                    if (foundRecordId != null) {
                        healthConnectRecordId = foundRecordId
                        Log.i(TAG, "✅ Found matching Health Connect record: $foundRecordId")
                    } else {
                        Log.w(TAG, "❌ Could not find matching record in Health Connect, skipping deletion")
                        return@launch
                    }
                }

                // Check if Health Connect is available and has permissions
                if (!HealthConnectManager.isAvailable(context) || !HealthConnectManager.hasPermissions(context)) {
                    Log.w(TAG, "⚠️ Delete handling skipped: Health Connect not ready")
                    return@launch
                }

                Log.i(TAG, "🗑️ Deleting entry from Health Connect: $healthConnectRecordId")
                Log.d(TAG, "🔍 Entry details: ${deletedEntry.amount}ml from ${deletedEntry.note}")
                Log.d(TAG, "🏷️ Record ID type: ${if (healthConnectRecordId.startsWith("hydrotracker_")) "Our record" else "External record"}")
                val result = HealthConnectManager.deleteHydrationRecord(context,healthConnectRecordId)

                if (result.isSuccess) {
                    Log.i(TAG, "✅ Successfully deleted entry from Health Connect")
                    _lastSyncTime.value = System.currentTimeMillis()
                } else {
                    Log.e(TAG, "❌ Failed to delete entry from Health Connect: ${result.exceptionOrNull()?.message}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Error during Health Connect delete", e)
            } finally {
                kotlinx.coroutines.delay(800)
                _isSyncing.value = false
            }
        }
    }

    enum class SyncStatus {
        READY,
        DISABLED,
        UNAVAILABLE,
        NO_PERMISSIONS,
        ERROR
    }

    /**
     * Check if an entry is a potential duplicate based on timestamp and amount
     * Uses a tolerance window to account for slight time differences
     *
     * ARCHITECTURAL NOTE: This function uses the stored [date] field (user-day) as the
     * single source of truth for date-based queries. It does NOT recompute the date from
     * the timestamp to avoid inconsistencies with UserDayCalculator logic. Time display
     * formatting is a UI concern and must remain separate from deduplication logic.
     */
    private suspend fun checkForDuplicate(repository: WaterIntakeRepository, newEntry: WaterIntakeEntry): Boolean {
        return try {
            // Use the entry's pre-computed date field (user-day) as the single source of truth.
            // Recomputing from timestamp via toLocalDate() would produce a different result
            // for entries before wake-up time, causing us to query the wrong date.
            val entryDate = newEntry.date

            // Get all entries for the same user-day (including hidden ones to prevent re-import)
            val existingEntries = repository.getAllEntriesForDate(entryDate)

            Log.d(TAG, "🔍 Duplicate check for ${newEntry.amount}ml at ${newEntry.timestamp} (date=$entryDate): found ${existingEntries.size} existing entries on same user-day")

            // Fast-path: exact healthConnectRecordId match
            if (newEntry.healthConnectRecordId != null) {
                val idMatch = existingEntries.find { it.healthConnectRecordId == newEntry.healthConnectRecordId }
                if (idMatch != null) {
                    Log.i(TAG, "⚠️ Exact duplicate by Health Connect record ID: ${newEntry.healthConnectRecordId}")
                    return true
                }
            }

            // Check for duplicates within a 5-minute window with similar effective hydration amounts
            val timeWindow = 5 * 60 * 1000L // 5 minutes in milliseconds
            val amountTolerance = 10.0 // ±10ml

            val isDuplicate = existingEntries.any { existing ->
                // Compare effective hydration amounts, not raw amounts.
                // When writing to Health Connect we store the effective amount (raw * multiplier).
                // When restoring, newEntry.amount IS the effective amount from Health Connect.
                // existing.amount is the raw amount stored locally, so we must convert it.
                val existingEffectiveAmount = existing.getEffectiveHydrationAmount()
                val timeDiff = kotlin.math.abs(existing.timestamp - newEntry.timestamp)
                val amountDiff = kotlin.math.abs(existingEffectiveAmount - newEntry.amount)

                val isTimeMatch = timeDiff <= timeWindow
                val isAmountMatch = amountDiff <= amountTolerance

                Log.d(TAG, "   vs existing ${existing.amount}ml (effective=${existingEffectiveAmount}) at ${existing.timestamp}: timeDiff=${timeDiff}ms, amountDiff=${amountDiff}ml, timeMatch=$isTimeMatch, amountMatch=$isAmountMatch")

                if (isTimeMatch && isAmountMatch) {
                    Log.d(TAG, "🔍 Potential duplicate found: existing ${existing.amount}ml (effective=${existingEffectiveAmount}) at ${existing.timestamp}, new ${newEntry.amount}ml at ${newEntry.timestamp}")
                    true
                } else {
                    false
                }
            }

            if (isDuplicate) {
                Log.i(TAG, "⚠️ Duplicate entry detected and skipped: ${newEntry.amount}ml at ${newEntry.timestamp} (date=$entryDate)")
            } else {
                Log.d(TAG, "✅ No duplicate found for ${newEntry.amount}ml at ${newEntry.timestamp} (date=$entryDate)")
            }

            isDuplicate
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking for duplicates", e)
            false // If we can't check, don't block the import
        }
    }

    /**
     * Add an imported water entry to the database
     * This bypasses Health Connect sync to avoid circular syncing
     */
    private suspend fun addImportedWaterEntry(repository: WaterIntakeRepository, entry: WaterIntakeEntry): Result<Long> {
        return try {
            Log.d(TAG, "💾 Adding imported entry to database: ${entry.amount}ml from ${entry.note}")

            // Use the special import method that doesn't trigger Health Connect sync
            val entryId = repository.addImportedWaterEntry(entry)

            Log.i(TAG, "✅ Successfully imported entry with ID: $entryId")
            Result.success(entryId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to import entry to database", e)
            Result.failure(e)
        }
    }

    /**
     * Find Health Connect record ID by searching for a matching record
     * Used for entries imported before we started storing record IDs
     */
    private suspend fun findHealthConnectRecordId(context: Context, entry: WaterIntakeEntry): String? {
        return try {
            Log.d(TAG, "🔍 Searching for Health Connect record matching: ${entry.amount}ml at ${entry.timestamp}")

            // Search in a 5-minute window around the entry timestamp
            val entryTime = java.time.Instant.ofEpochMilli(entry.timestamp)
            val startTime = entryTime.minusSeconds(150) // 2.5 minutes before
            val endTime = entryTime.plusSeconds(150)     // 2.5 minutes after

            Log.d(TAG, "⏰ Search window: $startTime to $endTime")

            val result = HealthConnectManager.readHydrationRecords(context, startTime)
            if (result.isFailure) {
                Log.e(TAG, "❌ Failed to search Health Connect: ${result.exceptionOrNull()?.message}")
                return null
            }

            val records = result.getOrNull() ?: emptyList()
            Log.d(TAG, "📋 Found ${records.size} records in time window")

            // Look for a record that matches volume and is within our time window
            val matchingRecord = records.find { record ->
                val recordTime = record.startTime
                val recordVolume = record.volume.inMilliliters
                val timeDiff = kotlin.math.abs(recordTime.toEpochMilli() - entry.timestamp)

                // Match criteria: same volume (±1ml) and within 5 minutes
                val volumeMatch = kotlin.math.abs(recordVolume - entry.amount) <= 1.0
                val timeMatch = timeDiff <= 300_000 // 5 minutes in milliseconds

                Log.d(TAG, "🔍 Record check: ${recordVolume}ml at $recordTime, volume match: $volumeMatch, time match: $timeMatch")

                volumeMatch && timeMatch
            }

            if (matchingRecord != null) {
                Log.i(TAG, "🎯 Found matching record: ${matchingRecord.metadata.id}")
                matchingRecord.metadata.id
            } else {
                Log.w(TAG, "🚫 No matching record found")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error searching for Health Connect record", e)
            null
        }
    }
}