package com.cemcakmak.hydrotracker.data.database.repository

import com.cemcakmak.hydrotracker.data.database.dao.CustomBeverageDao
import com.cemcakmak.hydrotracker.data.database.entities.CustomBeverageEntity
import kotlinx.coroutines.flow.Flow

class CustomBeverageRepository(
    private val customBeverageDao: CustomBeverageDao
) {
    fun getAll(): Flow<List<CustomBeverageEntity>> = customBeverageDao.getAll()

    /** Insert a new custom beverage and return its generated id. */
    suspend fun addBeverage(name: String, hydrationMultiplier: Double, iconKey: String): Long {
        return customBeverageDao.insert(
            CustomBeverageEntity(
                name = name,
                hydrationMultiplier = hydrationMultiplier,
                iconKey = iconKey
            )
        )
    }

    suspend fun updateBeverage(id: Long, name: String, hydrationMultiplier: Double, iconKey: String) {
        customBeverageDao.update(
            CustomBeverageEntity(
                id = id,
                name = name,
                hydrationMultiplier = hydrationMultiplier,
                iconKey = iconKey
            )
        )
    }

    suspend fun deleteBeverage(id: Long) {
        customBeverageDao.deleteById(id)
    }

    /** Remove all custom beverages (used by the beverage page "Reset to Defaults"). */
    suspend fun deleteAll() {
        customBeverageDao.deleteAll()
    }
}
