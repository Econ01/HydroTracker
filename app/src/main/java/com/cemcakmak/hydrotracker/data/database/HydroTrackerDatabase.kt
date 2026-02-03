package com.cemcakmak.hydrotracker.data.database

import androidx.room.*
import com.cemcakmak.hydrotracker.data.database.dao.WaterIntakeDao
import com.cemcakmak.hydrotracker.data.database.dao.DailySummaryDao
import com.cemcakmak.hydrotracker.data.database.dao.ContainerPresetDao
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.data.database.entities.DailySummary
import com.cemcakmak.hydrotracker.data.database.entities.ContainerPresetEntity

@Database(
    entities = [
        WaterIntakeEntry::class,
        DailySummary::class,
        ContainerPresetEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class HydroTrackerDatabase : RoomDatabase() {

    abstract fun waterIntakeDao(): WaterIntakeDao
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun containerPresetDao(): ContainerPresetDao

    companion object {
        const val DATABASE_NAME = "hydrotracker_database"
    }
}

