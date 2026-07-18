package com.cemcakmak.hydrotracker.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.cemcakmak.hydrotracker.data.database.entities.CustomBeverageEntity

@Dao
interface CustomBeverageDao {

    @Query("SELECT * FROM custom_beverages ORDER BY id ASC")
    fun getAll(): Flow<List<CustomBeverageEntity>>

    @Insert
    suspend fun insert(beverage: CustomBeverageEntity): Long

    @Update
    suspend fun update(beverage: CustomBeverageEntity)

    @Query("DELETE FROM custom_beverages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM custom_beverages")
    suspend fun deleteAll()
}
