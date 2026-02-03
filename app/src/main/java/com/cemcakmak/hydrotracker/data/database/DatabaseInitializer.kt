package com.cemcakmak.hydrotracker.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.database.repository.ContainerPresetRepository
import com.cemcakmak.hydrotracker.data.repository.UserRepository

object DatabaseInitializer {

    @Volatile
    internal var database: HydroTrackerDatabase? = null

    // Migration from version 1 to version 2
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Version 2 was used during development but never deployed
            // This migration should not be needed in production
        }
    }

    // Migration from version 1 to version 3 (adding health_connect_record_id)
    private val MIGRATION_1_3 = object : Migration(1, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add the new health_connect_record_id column to water_intake_entries table
            db.execSQL(
                "ALTER TABLE water_intake_entries ADD COLUMN health_connect_record_id TEXT"
            )
        }
    }

    // Migration from version 2 to version 3 (adding health_connect_record_id)
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add the new health_connect_record_id column to water_intake_entries table
            db.execSQL(
                "ALTER TABLE water_intake_entries ADD COLUMN health_connect_record_id TEXT"
            )
        }
    }

    // Migration from version 3 to version 4 (adding is_hidden)
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                println("DatabaseInitializer: Starting migration from version 3 to 4")

                // Check if the column already exists (defensive programming)
                val cursor = db.query("PRAGMA table_info(water_intake_entries)")
                var hasIsHiddenColumn = false

                while (cursor.moveToNext()) {
                    val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    if (columnName == "is_hidden") {
                        hasIsHiddenColumn = true
                        break
                    }
                }
                cursor.close()

                if (!hasIsHiddenColumn) {
                    // Add the new is_hidden column to water_intake_entries table
                    db.execSQL(
                        "ALTER TABLE water_intake_entries ADD COLUMN is_hidden INTEGER NOT NULL DEFAULT 0"
                    )
                    println("DatabaseInitializer: Successfully added is_hidden column")
                } else {
                    println("DatabaseInitializer: is_hidden column already exists, skipping")
                }

                // Verify the migration was successful
                val verificationCursor = db.query("PRAGMA table_info(water_intake_entries)")
                var columnCount = 0
                while (verificationCursor.moveToNext()) {
                    columnCount++
                }
                verificationCursor.close()

                println("DatabaseInitializer: Migration 3→4 completed. Column count: $columnCount")

            } catch (e: Exception) {
                println("DatabaseInitializer: Error during migration 3→4: ${e.message}")
                throw e
            }
        }
    }

    // Migration from version 4 to version 5 (adding beverage_type)
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                println("DatabaseInitializer: Starting migration from version 4 to 5")

                // Check if the column already exists (defensive programming)
                val cursor = db.query("PRAGMA table_info(water_intake_entries)")
                var hasBeverageTypeColumn = false

                while (cursor.moveToNext()) {
                    val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    if (columnName == "beverage_type") {
                        hasBeverageTypeColumn = true
                        break
                    }
                }
                cursor.close()

                if (!hasBeverageTypeColumn) {
                    // Add the new beverage_type column to water_intake_entries table
                    // Default to WATER for all existing entries (backwards compatibility)
                    db.execSQL(
                        "ALTER TABLE water_intake_entries ADD COLUMN beverage_type TEXT NOT NULL DEFAULT 'WATER'"
                    )
                    println("DatabaseInitializer: Successfully added beverage_type column")
                } else {
                    println("DatabaseInitializer: beverage_type column already exists, skipping")
                }

                // Verify the migration was successful
                val verificationCursor = db.query("PRAGMA table_info(water_intake_entries)")
                var columnCount = 0
                while (verificationCursor.moveToNext()) {
                    columnCount++
                }
                verificationCursor.close()

                println("DatabaseInitializer: Migration 4→5 completed. Column count: $columnCount")

            } catch (e: Exception) {
                println("DatabaseInitializer: Error during migration 4→5: ${e.message}")
                throw e
            }
        }
    }

    // Migration from version 5 to version 6 (adding container_presets table)
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                println("DatabaseInitializer: Starting migration from version 5 to 6")

                // Create the container_presets table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS container_presets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        volume REAL NOT NULL,
                        icon_type TEXT NOT NULL,
                        icon_name TEXT NOT NULL,
                        is_default INTEGER NOT NULL DEFAULT 0,
                        display_order INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // Create index on display_order for efficient sorting
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_container_presets_display_order
                    ON container_presets (display_order)
                """)

                println("DatabaseInitializer: Migration 5→6 completed. Created container_presets table.")

            } catch (e: Exception) {
                println("DatabaseInitializer: Error during migration 5→6: ${e.message}")
                throw e
            }
        }
    }

    fun getDatabase(context: Context): HydroTrackerDatabase {
        return database ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                HydroTrackerDatabase::class.java,
                HydroTrackerDatabase.DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_1_3, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                // Add fallback strategy for Room 2.8.1 compatibility issues
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()
            database = instance
            instance
        }
    }

    fun getWaterIntakeRepository(context: Context, userRepository: UserRepository): WaterIntakeRepository {
        // Create a new repository instance each time to avoid memory leaks
        // Use applicationContext to prevent Activity context leaks
        val db = getDatabase(context)
        return WaterIntakeRepository(
            waterIntakeDao = db.waterIntakeDao(),
            dailySummaryDao = db.dailySummaryDao(),
            userRepository = userRepository,
            context = context.applicationContext // Use application context to prevent leaks
        )
    }

    fun getContainerPresetRepository(context: Context): ContainerPresetRepository {
        val db = getDatabase(context)
        return ContainerPresetRepository(
            containerPresetDao = db.containerPresetDao()
        )
    }

    /**
     * Validate database connection (simplified version for synchronous use)
     * Call this method if you suspect database corruption or migration issues
     */
    fun validateDatabase(context: Context): Boolean {
        return try {
            println("DatabaseInitializer: Validating database...")
            val db = getDatabase(context)

            // Try to open database connection - this will trigger migrations if needed
            db.openHelper.readableDatabase.version
            println("DatabaseInitializer: Database validation successful")
            true
        } catch (e: Exception) {
            println("DatabaseInitializer: Database validation failed: ${e.message}")
            false
        }
    }

    /**
     * Repair corrupted database by recreating it
     */
    fun repairDatabase(context: Context): Boolean {
        return try {
            println("DatabaseInitializer: Attempting database repair...")

            // Close existing database connection
            database?.close()
            database = null

            // Clear database file and recreate
            val dbFile = context.getDatabasePath(HydroTrackerDatabase.DATABASE_NAME)
            if (dbFile.exists()) {
                dbFile.delete()
                println("DatabaseInitializer: Deleted corrupted database file")
            }

            // Force recreation
            val newDb = getDatabase(context)
            println("DatabaseInitializer: Database recreated successfully")
            true
        } catch (repairError: Exception) {
            println("DatabaseInitializer: Database repair failed: ${repairError.message}")
            false
        }
    }
}