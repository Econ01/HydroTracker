package com.cemcakmak.hydrotracker.health

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Volume
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import java.time.Instant
import java.time.ZoneOffset

/**
 * Manager class for Health Connect integration
 * Handles reading/writing hydration data and permission management
 */
object HealthConnectManager {
    private const val TAG = "HealthConnectManager"

    // Permissions needed for hydration data
    val HYDRATION_PERMISSIONS = setOf(
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getWritePermission(HydrationRecord::class)
    )

    // Debug method to print permission details
    fun debugPermissions() {
        Log.d(TAG, "Read permission: ${HealthPermission.getReadPermission(HydrationRecord::class)}")
        Log.d(TAG, "Write permission: ${HealthPermission.getWritePermission(HydrationRecord::class)}")
        Log.d(TAG, "All permissions: $HYDRATION_PERMISSIONS")
    }

    /**
     * Check if Health Connect is supported on this Android version
     * Health Connect requires Android 9 (API level 28) or higher
     */
    fun isVersionSupported(): Boolean {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P
    }

    /**
     * Check if Health Connect is available on this device
     */
    fun isAvailable(context: Context): Boolean {
        return try {
            // First check if the Android version supports Health Connect
            if (!isVersionSupported()) {
                Log.w(TAG, "Health Connect requires Android 9 (API 28) or higher")
                return false
            }

            when (HealthConnectClient.getSdkStatus(context)) {
                HealthConnectClient.SDK_AVAILABLE -> {
                    Log.d(TAG, "Health Connect SDK is available")
                    true
                }
                HealthConnectClient.SDK_UNAVAILABLE -> {
                    Log.w(TAG, "Health Connect SDK is unavailable")
                    false
                }
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                    Log.w(TAG, "Health Connect provider update required")
                    false
                }
                else -> {
                    Log.w(TAG, "Unknown Health Connect SDK status")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Health Connect availability", e)
            false
        }
    }

    /**
     * Check if we have the required permissions
     */
    suspend fun hasPermissions(context: Context): Boolean {
        return try {
            if (!isAvailable(context)) return false

            val healthConnectClient = HealthConnectClient.getOrCreate(context)
            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()
            val hasAllPermissions = HYDRATION_PERMISSIONS.all { it in grantedPermissions }

            Log.d(TAG, "Has all hydration permissions: $hasAllPermissions")
            return hasAllPermissions
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions", e)
            false
        }
    }

    /**
     * Create permission request launcher for Health Connect
     */
    fun createPermissionRequestLauncher(activity: ComponentActivity, onResult: (Set<String>) -> Unit): ActivityResultLauncher<Set<String>> {
        val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()

        return activity.registerForActivityResult(requestPermissionActivityContract) { granted ->
            Log.d(TAG, "Permission request result - Granted: $granted")
            Log.d(TAG, "Required permissions: $HYDRATION_PERMISSIONS")
            onResult(granted)
        }
    }

    /**
     * Check permissions and run the provided action if all are granted
     * Otherwise, launch the permission request
     */
    suspend fun checkPermissionsAndRun(
        context: Context,
        permissionLauncher: ActivityResultLauncher<Set<String>>,
        onPermissionsGranted: () -> Unit
    ) {
        val healthConnectClient = HealthConnectClient.getOrCreate(context)
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        Log.d(TAG, "Currently granted permissions: $granted")

        if (granted.containsAll(HYDRATION_PERMISSIONS)) {
            Log.d(TAG, "All permissions already granted")
            onPermissionsGranted()
        } else {
            Log.d(TAG, "Requesting missing permissions: ${HYDRATION_PERMISSIONS - granted}")
            permissionLauncher.launch(HYDRATION_PERMISSIONS)
        }
    }

    /**
     * Write a hydration record to Health Connect
     */
    suspend fun writeHydrationRecord(context: Context, entry: WaterIntakeEntry): Result<String> {
        return try {
            Log.d(TAG, "Starting Health Connect write for entry: ${entry.amount}ml at ${entry.timestamp}")

            if (!hasPermissions(context)) {
                Log.w(TAG, "Cannot write to Health Connect: Missing permissions")
                return Result.failure(SecurityException("Missing Health Connect permissions"))
            }

            Log.d(TAG, "Permissions verified, creating HydrationRecord...")

            // Convert timestamp to Instant
            val startTime = Instant.ofEpochMilli(entry.timestamp)
            // For instant consumption, end time is 1 second after start time
            val endTime = startTime.plusSeconds(1)
            Log.d(TAG, "Converted timestamp ${entry.timestamp} to Instant: startTime=$startTime, endTime=$endTime")

            // Create volume using effective hydration amount (considers beverage type multiplier)
            val effectiveAmount = entry.getEffectiveHydrationAmount()
            val volumeInMilliliters = effectiveAmount
            Log.d(TAG, "Using effective volume: ${volumeInMilliliters}ml (raw: ${entry.amount}ml, beverage: ${entry.getBeverageType().displayName}, multiplier: ${entry.getBeverageType().hydrationMultiplier})")
            Log.d(TAG, "Type check - effectiveAmount: $effectiveAmount (${effectiveAmount::class.simpleName}), volumeInMilliliters: $volumeInMilliliters (${volumeInMilliliters::class.simpleName})")

            // Create unique ID for tracking this record
            val uniqueId = "hydrotracker_${entry.id}_${System.currentTimeMillis()}"

            // Create HydrationRecord
            val hydrationRecord = HydrationRecord(
                startTime = startTime,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime),
                endTime = endTime,
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(endTime),
                volume = Volume.milliliters(volumeInMilliliters),
                metadata = Metadata.manualEntry(
                    device = Device(type = Device.TYPE_PHONE),
                    clientRecordId = uniqueId
                )
            )

            Log.d(TAG, "Created HydrationRecord: startTime=$startTime, endTime=$endTime, volume=${volumeInMilliliters}ml")
            Log.d(TAG, "Volume object created: ${Volume.milliliters(volumeInMilliliters)} (${Volume.milliliters(volumeInMilliliters).inMilliliters}ml)")

            // Write to Health Connect
            Log.d(TAG, "Writing HydrationRecord to Health Connect...")
            val healthConnectClient = HealthConnectClient.getOrCreate(context)
            healthConnectClient.insertRecords(listOf(hydrationRecord))

            // Return the custom ID we set in metadata for tracking
            Log.d(TAG, "Health Connect record ID: $uniqueId")

            Log.i(TAG, "✅ Successfully wrote hydration record to Health Connect: ${volumeInMilliliters}ml effective (${entry.amount}ml ${entry.getBeverageType().displayName})")
            Result.success(uniqueId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error writing hydration record to Health Connect", e)
            Log.e(TAG, "Entry details: amount=${entry.amount}ml, timestamp=${entry.timestamp}, date=${entry.date}")
            Result.failure(e)
        }
    }

    /**
     * Delete a hydration record from Health Connect using the record ID
     */
    suspend fun deleteHydrationRecord(context: Context, recordId: String): Result<Unit> {
        return try {
            Log.d(TAG, "🗑️ Starting Health Connect deletion for record ID: $recordId")

            if (!hasPermissions(context)) {
                Log.w(TAG, "Cannot delete from Health Connect: Missing permissions")
                return Result.failure(SecurityException("Missing Health Connect permissions"))
            }

            Log.d(TAG, "✅ Permissions verified, deleting HydrationRecord...")

            val healthConnectClient = HealthConnectClient.getOrCreate(context)

            // Determine if this is a client record ID (our app) or Health Connect record ID (external app)
            if (recordId.startsWith("hydrotracker_")) {
                // This is our own record, use client record ID
                Log.d(TAG, "🏠 Deleting our own record using client record ID: $recordId")
                healthConnectClient.deleteRecords(
                    recordType = HydrationRecord::class,
                    recordIdsList = emptyList(),
                    clientRecordIdsList = listOf(recordId)
                )
            } else {
                // This is an external record, use Health Connect record ID
                Log.d(TAG, "🌐 Deleting external record using Health Connect record ID: $recordId")
                healthConnectClient.deleteRecords(
                    recordType = HydrationRecord::class,
                    recordIdsList = listOf(recordId),
                    clientRecordIdsList = emptyList()
                )
            }

            Log.i(TAG, "✅ Successfully deleted hydration record from Health Connect: $recordId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting hydration record from Health Connect: $recordId", e)
            Log.e(TAG, "❌ Error details: ${e.message}")
            Log.e(TAG, "❌ Record type: ${if (recordId.startsWith("hydrotracker_")) "Our record" else "External record"}")
            Result.failure(e)
        }
    }

    /**
     * Read hydration records from Health Connect since a specific time
     * Handles pagination to retrieve all records across multiple pages
     */
    suspend fun readHydrationRecords(context: Context, since: Instant): Result<List<HydrationRecord>> {
        return try {
            Log.d(TAG, "Starting Health Connect read since: $since")

            if (!hasPermissions(context)) {
                Log.w(TAG, "Cannot read from Health Connect: Missing permissions")
                return Result.failure(SecurityException("Missing Health Connect permissions"))
            }

            Log.d(TAG, "Permissions verified, creating read request...")

            val healthConnectClient = HealthConnectClient.getOrCreate(context)
            val allRecords = mutableListOf<HydrationRecord>()
            var pageToken: String? = null
            var pageCount = 0

            do {
                val request = ReadRecordsRequest(
                    recordType = HydrationRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(since),
                    pageToken = pageToken
                )

                Log.d(TAG, "Executing Health Connect read request (page ${pageCount + 1})...")
                val response = healthConnectClient.readRecords(request)
                allRecords.addAll(response.records)
                pageToken = response.pageToken
                pageCount++

                Log.d(TAG, "📄 Page $pageCount: read ${response.records.size} records, hasMorePages=${pageToken != null}")
            } while (pageToken != null)

            Log.i(TAG, "✅ Successfully read ${allRecords.size} hydration records across $pageCount page(s) since $since")

            // Log details of each record for debugging
            allRecords.forEach { record ->
                Log.d(TAG, "📥 Record: ${record.volume.inMilliliters}ml at ${record.startTime} (source: ${record.metadata.dataOrigin})")
            }

            Result.success(allRecords)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reading hydration records from Health Connect", e)
            Result.failure(e)
        }
    }

    /**
     * Read only external hydration records (not from HydroTracker)
     */
    suspend fun readExternalHydrationRecords(context: Context, since: Instant): Result<List<HydrationRecord>> {
        return try {
            val allRecords = readHydrationRecords(context, since)
            if (allRecords.isFailure) {
                return allRecords
            }

            val totalRecords = allRecords.getOrNull()?.size ?: 0
            Log.d(TAG, "📥 Found $totalRecords total hydration records since $since")

            // Filter out records that originated from HydroTracker using multiple criteria
            val externalRecords = allRecords.getOrNull()?.filter { record ->
                val recordId = record.metadata.id
                val clientRecordId = record.metadata.clientRecordId
                val packageName = record.metadata.dataOrigin.packageName

                // Check if this is from HydroTracker by client record ID
                val isFromHydroTrackerByClientId = clientRecordId?.startsWith("hydrotracker_") == true

                // Check if this is from HydroTracker by metadata ID
                val isFromHydroTrackerById = recordId.startsWith("hydrotracker_")

                // Check if this is from HydroTracker by package name
                val isFromHydroTrackerByPackage = packageName == "com.cemcakmak.hydrotracker"

                val isFromHydroTracker = isFromHydroTrackerByClientId || isFromHydroTrackerById || isFromHydroTrackerByPackage

                if (isFromHydroTracker) {
                    Log.d(TAG, "🚫 Excluding HydroTracker record: clientID=$clientRecordId, ID=$recordId, package=$packageName")
                } else {
                    Log.d(TAG, "✅ Including external record: clientID=$clientRecordId, ID=$recordId, package=$packageName, amount=${record.volume.inLiters * 1000}ml")
                }

                !isFromHydroTracker
            } ?: emptyList()

            Log.i(TAG, "📥 Found ${externalRecords.size} external hydration records (excluding ${totalRecords - externalRecords.size} HydroTracker records)")
            Result.success(externalRecords)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error filtering external records", e)
            Result.failure(e)
        }
    }

    /**
     * Convert HydrationRecord to WaterIntakeEntry format
     */
    fun hydrationRecordToWaterIntakeEntry(
        record: HydrationRecord,
        sourceName: String?,
        wakeUpTime: String = "07:00",
        healthConnectRecordId: String? = null
    ): WaterIntakeEntry {
        val volumeInML = record.volume.inMilliliters
        val timestamp = record.startTime.toEpochMilli()
        val date = com.cemcakmak.hydrotracker.utils.UserDayCalculator.getUserDayStringForTimestamp(
            timestamp,
            wakeUpTime
        )

        // Extract a user-friendly source name from the dataOrigin
        val friendlySourceName = extractFriendlySourceName(sourceName)

        // Use provided ID or fallback to Health Connect's UUID
        val recordId = healthConnectRecordId ?: record.metadata.id

        Log.d(TAG, "📱 Converting Health Connect record from: $sourceName → $friendlySourceName")
        Log.d(TAG, "🆔 Health Connect Record ID: ${record.metadata.id}")
        Log.d(TAG, "🏷️ Client Record ID: ${record.metadata.clientRecordId}")
        Log.d(TAG, "📦 Package: ${record.metadata.dataOrigin.packageName}")
        Log.d(TAG, "💾 Using record ID for local storage: $recordId")

        return WaterIntakeEntry(
            amount = volumeInML,
            timestamp = timestamp,
            date = date,
            containerType = friendlySourceName, // Use the actual source app name
            containerVolume = volumeInML, // Use same as amount for external data
            note = "Imported from $friendlySourceName",
            healthConnectRecordId = recordId // Store the Health Connect record ID for deletion
        )
    }

    /**
     * Extract a user-friendly app name from Health Connect dataOrigin
     */
    private fun extractFriendlySourceName(dataOrigin: String?): String {
        if (dataOrigin.isNullOrBlank()) {
            return "Health Connect"
        }

        Log.d(TAG, "🔍 Processing dataOrigin: $dataOrigin")

        // The dataOrigin format is typically: DataOrigin(packageName="com.example.app")
        // Extract the package name from this format
        val packageName = run {
            val s = dataOrigin.trim()

            // 1) Canonical: DataOrigin(packageName="com.example.app")
            val m1 = Regex("""DataOrigin\(\s*packageName\s*=\s*["']([^"']+)["']\s*\)?""").find(s)

            // 2) Fallback: packageName=... or package=..., with/without quotes
            val m2 = m1 ?: Regex("""\b(packageName|package)\s*=\s*["']?([A-Za-z_]\w*(?:\.[A-Za-z_]\w*)+)["']?\)?""").find(s)

            // 3) Last resort: first package-like token anywhere
            val candidate = when {
                m1 != null -> m1.groupValues[1]
                m2 != null -> m2.groupValues[2]
                else -> Regex("""[A-Za-z_]\w*(?:\.[A-Za-z_]\w*)+""").find(s)?.value ?: s
            }

            // Clean common trailing/leading noise (quotes, ) etc.)
            candidate.trim().trim('"', '\'').trimEnd(')')
        }

        Log.d(TAG, "📦 Extracted package name: $packageName")

        // Map known package names to user-friendly names
        val cleanName = when {
            // Samsung Health
            packageName.equals("com.sec.android.app.shealth", ignoreCase = true) -> "Samsung Health"

            // Google Fit
            packageName.equals("com.google.android.apps.fitness", ignoreCase = true) -> "Google Fit"

            // Fitbit
            packageName.equals("com.fitbit.FitbitMobile", ignoreCase = true) -> "Fitbit"

            // MyFitnessPal
            packageName.equals("com.myfitnesspal.android", ignoreCase = true) -> "MyFitnessPal"

            // Garmin Connect
            packageName.startsWith("com.garmin", ignoreCase = true) -> "Garmin Connect"

            // Strava
            packageName.startsWith("com.strava", ignoreCase = true) -> "Strava"

            // Xiaomi Mi Health
            packageName.equals("com.mi.health", ignoreCase = true) -> "Mi Health"

            // Huawei Health
            packageName.equals("com.huawei.health", ignoreCase = true) -> "Huawei Health"

            // Our own app (should not happen in external imports)
            packageName.equals("com.cemcakmak.hydrotracker", ignoreCase = true) -> "HydroTracker"

            // Generic extraction from package name
            else -> {
                val parts = packageName.split(".")
                when {
                    parts.size >= 3 -> {
                        // Try to get the app name from the last part
                        val appName = parts.last()
                        appName.replaceFirstChar { it.uppercase() }
                    }
                    parts.size == 2 -> {
                        // Format: com.appname
                        parts.last().replaceFirstChar { it.uppercase() }
                    }
                    else -> {
                        // Unknown format, use the whole string
                        packageName.replaceFirstChar { it.uppercase() }
                    }
                }
            }
        }

        Log.d(TAG, "🏷️ Mapped '$packageName' → '$cleanName'")
        return cleanName
    }

    /**
     * Read only HydroTracker hydration records from Health Connect
     * Used to restore local data after reinstall
     */
    suspend fun readHydroTrackerRecords(context: Context, since: Instant): Result<List<HydrationRecord>> {
        return try {
            val allRecords = readHydrationRecords(context, since)
            if (allRecords.isFailure) {
                return allRecords
            }

            val totalRecords = allRecords.getOrNull()?.size ?: 0
            Log.d(TAG, "📥 Found $totalRecords total hydration records since $since")

            // Keep only records that originated from HydroTracker
            val hydroTrackerRecords = allRecords.getOrNull()?.filter { record ->
                val recordId = record.metadata.id
                val clientRecordId = record.metadata.clientRecordId
                val packageName = record.metadata.dataOrigin.packageName

                val isFromHydroTrackerByClientId = clientRecordId?.startsWith("hydrotracker_") == true
                val isFromHydroTrackerById = recordId.startsWith("hydrotracker_")
                val isFromHydroTrackerByPackage = packageName == "com.cemcakmak.hydrotracker"

                val isFromHydroTracker = isFromHydroTrackerByClientId || isFromHydroTrackerById || isFromHydroTrackerByPackage

                if (isFromHydroTracker) {
                    Log.d(TAG, "✅ Including HydroTracker record: clientID=$clientRecordId, ID=$recordId, package=$packageName, amount=${record.volume.inMilliliters}ml")
                }

                isFromHydroTracker
            } ?: emptyList()

            Log.i(TAG, "📥 Found ${hydroTrackerRecords.size} HydroTracker records (filtered from $totalRecords total)")
            Result.success(hydroTrackerRecords)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error filtering HydroTracker records", e)
            Result.failure(e)
        }
    }

    /**
     * Get a user-friendly status message
     */
    suspend fun getStatusMessage(context: Context): String {
        return when {
            !isAvailable(context) -> {
                when (HealthConnectClient.getSdkStatus(context)) {
                    HealthConnectClient.SDK_UNAVAILABLE -> "Health Connect is not available on this device"
                    HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> "Health Connect app needs to be updated"
                    else -> "Health Connect is not available"
                }
            }
            !hasPermissions(context) -> "Permissions not granted for Health Connect"
            else -> "Health Connect is ready"
        }
    }
}