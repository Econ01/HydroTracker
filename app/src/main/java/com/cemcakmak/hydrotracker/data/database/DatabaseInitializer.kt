package com.cemcakmak.hydrotracker.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.database.repository.ContainerPresetRepository
import com.cemcakmak.hydrotracker.data.database.repository.CustomBeverageRepository
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

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                println("DatabaseInitializer: Starting migration from version 6 to 7")

                // Create the custom_beverages table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS custom_beverages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        hydration_multiplier REAL NOT NULL,
                        icon_key TEXT NOT NULL
                    )
                """)

                println("DatabaseInitializer: Migration 6→7 completed. Created custom_beverages table.")

            } catch (e: Exception) {
                println("DatabaseInitializer: Error during migration 6→7: ${e.message}")
                throw e
            }
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                println("DatabaseInitializer: Starting migration from version 7 to 8")

                // Add nullable beverage_multiplier to water_intake_entries (custom beverage effectiveness)
                db.execSQL("ALTER TABLE water_intake_entries ADD COLUMN beverage_multiplier REAL")

                println("DatabaseInitializer: Migration 7→8 completed. Added beverage_multiplier column.")

            } catch (e: Exception) {
                println("DatabaseInitializer: Error during migration 7→8: ${e.message}")
                throw e
            }
        }
    }

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                println("DatabaseInitializer: Starting migration from version 9 to 10")

                db.execSQL(
                    "ALTER TABLE water_intake_entries ADD COLUMN source TEXT NOT NULL DEFAULT 'LOCAL'"
                )

                // Existing entries that were imported from other apps carry a Health Connect
                // record id that is not one of our client record ids. Tag them accordingly so
                // they are not re-written to Health Connect.
                db.execSQL(
                    """
                    UPDATE water_intake_entries
                    SET source = 'HEALTH_CONNECT_EXTERNAL'
                    WHERE health_connect_record_id IS NOT NULL
                      AND health_connect_record_id NOT LIKE 'hydrotracker_%'
                    """.trimIndent()
                )

                println("DatabaseInitializer: Migration 9→10 completed. Added source column.")
            } catch (e: Exception) {
                println("DatabaseInitializer: Error during migration 9→10: ${e.message}")
                throw e
            }
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                println("DatabaseInitializer: Starting migration from version 8 to 9")

                // Add icon columns to water_intake_entries so each entry keeps its container icon.
                db.execSQL("ALTER TABLE water_intake_entries ADD COLUMN icon_type TEXT NOT NULL DEFAULT 'DRAWABLE'")
                db.execSQL("ALTER TABLE water_intake_entries ADD COLUMN icon_name TEXT NOT NULL DEFAULT 'water_filled'")

                // Backfill existing rows with an icon derived from their stored container volume.
                // Thresholds mirror ContainerIconMapper.getIconForVolume().
                db.execSQL(
                    """
                    UPDATE water_intake_entries
                    SET icon_name = CASE
                        WHEN container_volume <= 125 THEN 'local_cafe'
                        WHEN container_volume <= 162 THEN 'glass_cup'
                        WHEN container_volume <= 187 THEN 'water_loss'
                        WHEN container_volume <= 250 THEN 'water_medium'
                        WHEN container_volume <= 400 THEN 'water_full'
                        WHEN container_volume <= 750 THEN 'water_bottle'
                        ELSE 'water_bottle_large'
                    END
                    """.trimIndent()
                )

                println("DatabaseInitializer: Migration 8→9 completed. Added icon columns and backfilled existing rows.")

            } catch (e: Exception) {
                println("DatabaseInitializer: Error during migration 8→9: ${e.message}")
                throw e
            }
        }
    }

    /** Every shipped schema migration, in order. Exposed so migration tests exercise the real chain. */
    val ALL_MIGRATIONS: Array<Migration> = arrayOf(
        MIGRATION_1_2, MIGRATION_1_3, MIGRATION_2_3, MIGRATION_3_4,
        MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8,
        MIGRATION_8_9, MIGRATION_9_10
    )

    fun getDatabase(context: Context): HydroTrackerDatabase {
        return database ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                HydroTrackerDatabase::class.java,
                HydroTrackerDatabase.DATABASE_NAME
            )
                .addMigrations(*ALL_MIGRATIONS)
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

    fun getCustomBeverageRepository(context: Context): CustomBeverageRepository {
        val db = getDatabase(context)
        return CustomBeverageRepository(
            customBeverageDao = db.customBeverageDao()
        )
    }
}