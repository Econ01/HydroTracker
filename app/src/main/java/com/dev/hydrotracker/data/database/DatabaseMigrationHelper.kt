package com.dev.hydrotracker.data.database

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper class for handling database migrations and validation
 * Specifically designed to handle Room 2.8.1 compatibility issues
 */
object DatabaseMigrationHelper {

    private const val TAG = "DatabaseMigrationHelper"

    /**
     * Comprehensive database health check and migration assistance
     */
    suspend fun performStartupDatabaseCheck(context: Context): DatabaseHealthResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting comprehensive database health check...")

                // Step 1: Check if database file exists
                val dbFile = context.getDatabasePath(HydroTrackerDatabase.DATABASE_NAME)
                val databaseExists = dbFile.exists()
                Log.d(TAG, "Database file exists: $databaseExists")

                if (!databaseExists) {
                    Log.d(TAG, "Fresh installation - no database migration needed")
                    return@withContext DatabaseHealthResult.FreshInstall
                }

                // Step 2: Try to initialize database and check version
                try {
                    val db = DatabaseInitializer.getDatabase(context)
                    val dao = db.waterIntakeDao()
                    val entryCount = dao.getEntryCount()

                    Log.d(TAG, "Database initialized successfully. Entry count: $entryCount")
                    return@withContext DatabaseHealthResult.Healthy(entryCount)

                } catch (migrationError: Exception) {
                    Log.w(TAG, "Database migration/initialization failed: ${migrationError.message}")

                    // Step 3: Attempt recovery
                    return@withContext attemptDatabaseRecovery(context, migrationError)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Critical database check failure", e)
                return@withContext DatabaseHealthResult.CriticalFailure(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun attemptDatabaseRecovery(
        context: Context,
        originalError: Exception
    ): DatabaseHealthResult {
        return try {
            Log.d(TAG, "Attempting database recovery...")

            // Close any existing connections
            DatabaseInitializer.database?.close()
            DatabaseInitializer.database = null

            // Backup the problematic database
            val dbFile = context.getDatabasePath(HydroTrackerDatabase.DATABASE_NAME)
            val backupFile = context.getDatabasePath("${HydroTrackerDatabase.DATABASE_NAME}.backup")

            if (dbFile.exists()) {
                dbFile.copyTo(backupFile, overwrite = true)
                Log.d(TAG, "Created database backup")
            }

            // Delete the problematic database
            dbFile.delete()
            Log.d(TAG, "Deleted corrupted database")

            // Try to initialize fresh database
            val newDb = DatabaseInitializer.getDatabase(context)
            Log.d(TAG, "Created fresh database successfully")

            DatabaseHealthResult.RecoveredWithDataLoss(
                originalError.message ?: "Migration failed",
                hasBackup = backupFile.exists()
            )

        } catch (recoveryError: Exception) {
            Log.e(TAG, "Database recovery failed", recoveryError)
            DatabaseHealthResult.CriticalFailure(
                "Recovery failed: ${recoveryError.message}"
            )
        }
    }

    /**
     * Get user-friendly message for database health status
     */
    fun getHealthMessage(result: DatabaseHealthResult): String {
        return when (result) {
            is DatabaseHealthResult.FreshInstall ->
                "Welcome! Setting up your hydration tracker..."

            is DatabaseHealthResult.Healthy ->
                "Database ready. ${result.entryCount} water entries found."

            is DatabaseHealthResult.RecoveredWithDataLoss ->
                "Database recovered after migration issue. Previous data backed up."

            is DatabaseHealthResult.CriticalFailure ->
                "Critical database error: ${result.error}"
        }
    }

    /**
     * Check if the result requires user notification
     */
    fun shouldNotifyUser(result: DatabaseHealthResult): Boolean {
        return result is DatabaseHealthResult.RecoveredWithDataLoss ||
               result is DatabaseHealthResult.CriticalFailure
    }
}

/**
 * Represents the health status of the database after startup checks
 */
sealed class DatabaseHealthResult {
    object FreshInstall : DatabaseHealthResult()
    data class Healthy(val entryCount: Int) : DatabaseHealthResult()
    data class RecoveredWithDataLoss(
        val originalError: String,
        val hasBackup: Boolean
    ) : DatabaseHealthResult()
    data class CriticalFailure(val error: String) : DatabaseHealthResult()
}