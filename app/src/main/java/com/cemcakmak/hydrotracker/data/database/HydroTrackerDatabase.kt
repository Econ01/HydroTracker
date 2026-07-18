package com.cemcakmak.hydrotracker.data.database

import androidx.room.*
import com.cemcakmak.hydrotracker.data.database.dao.WaterIntakeDao
import com.cemcakmak.hydrotracker.data.database.dao.DailySummaryDao
import com.cemcakmak.hydrotracker.data.database.dao.ContainerPresetDao
import com.cemcakmak.hydrotracker.data.database.dao.CustomBeverageDao
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.data.database.entities.DailySummary
import com.cemcakmak.hydrotracker.data.database.entities.ContainerPresetEntity
import com.cemcakmak.hydrotracker.data.database.entities.CustomBeverageEntity

@Database(
    entities = [
        WaterIntakeEntry::class,
        DailySummary::class,
        ContainerPresetEntity::class,
        CustomBeverageEntity::class
    ],
    version = 10,
    exportSchema = true
)
abstract class HydroTrackerDatabase : RoomDatabase() {

    abstract fun waterIntakeDao(): WaterIntakeDao
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun containerPresetDao(): ContainerPresetDao
    abstract fun customBeverageDao(): CustomBeverageDao

    companion object {
        const val DATABASE_NAME = "hydrotracker_database"
    }
}

