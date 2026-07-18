package com.cemcakmak.hydrotracker.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.cemcakmak.hydrotracker.data.database.entities.ContainerPresetEntity

@Dao
interface ContainerPresetDao {

    @Query("SELECT * FROM container_presets ORDER BY display_order ASC")
    fun getAllPresets(): Flow<List<ContainerPresetEntity>>

    @Query("SELECT * FROM container_presets ORDER BY display_order ASC")
    suspend fun getAllPresetsSync(): List<ContainerPresetEntity>

    @Query("SELECT * FROM container_presets WHERE id = :id")
    suspend fun getPresetById(id: Long): ContainerPresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: ContainerPresetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<ContainerPresetEntity>)

    @Update
    suspend fun updatePreset(preset: ContainerPresetEntity)

    @Query("DELETE FROM container_presets WHERE id = :id")
    suspend fun deletePresetById(id: Long)

    @Query("DELETE FROM container_presets")
    suspend fun deleteAllPresets()

    @Query("SELECT COUNT(*) FROM container_presets")
    suspend fun getPresetCount(): Int

    @Query("SELECT COALESCE(MAX(display_order), 0) FROM container_presets")
    suspend fun getMaxDisplayOrder(): Int

    @Query("UPDATE container_presets SET display_order = :displayOrder WHERE id = :id")
    suspend fun updateDisplayOrder(id: Long, displayOrder: Int)

    /** Persist a new ordering by writing each preset's index as its display_order. */
    @Transaction
    suspend fun reorderPresets(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            updateDisplayOrder(id, index)
        }
    }
}
