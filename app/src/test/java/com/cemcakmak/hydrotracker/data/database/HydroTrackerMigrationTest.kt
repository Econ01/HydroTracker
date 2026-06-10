package com.cemcakmak.hydrotracker.data.database

import android.app.Application
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * Validates the full Room migration chain on the JVM via Robolectric — no emulator needed.
 *
 * Uses the driver-based [MigrationTestHelper] with [AndroidSQLiteDriver] (Robolectric shadows
 * Android's SQLite), which opens the database file directly — avoiding both the support-driver
 * name/path mismatch and the bundled-native-driver link error that surface under Robolectric.
 * Exported schemas come from `app/schemas`, exposed to the debug variant's merged assets in
 * `build.gradle.kts`. Forcing the vanilla [Application] keeps the test isolated from app startup.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class HydroTrackerMigrationTest {

    private val dbFile: File =
        File(ApplicationProvider.getApplicationContext<Application>().cacheDir, "migration-test.db")

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        dbFile,
        AndroidSQLiteDriver(),
        HydroTrackerDatabase::class
    )

    @Test
    fun migratesFromV1ToLatest() {
        // Create the database at the very first shipped version
        helper.createDatabase(1).close()

        // Run every shipped migration and validate the result matches the latest schema
        helper.runMigrationsAndValidate(8, DatabaseInitializer.ALL_MIGRATIONS.toList())
    }
}
