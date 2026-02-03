package com.cemcakmak.hydrotracker.data.database.repository

import com.cemcakmak.hydrotracker.data.database.dao.ContainerPresetDao
import com.cemcakmak.hydrotracker.data.database.entities.ContainerPresetEntity
import com.cemcakmak.hydrotracker.data.models.ContainerPreset
import com.cemcakmak.hydrotracker.utils.ContainerIconMapper
import com.cemcakmak.hydrotracker.utils.IconType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContainerPresetRepository(
    private val containerPresetDao: ContainerPresetDao
) {
    /**
     * Get all container presets as a Flow, mapped to the UI model
     */
    fun getAllPresets(): Flow<List<ContainerPreset>> {
        return containerPresetDao.getAllPresets().map { entities ->
            entities.map { it.toContainerPreset() }
        }
    }

    /**
     * Get all container presets synchronously
     */
    suspend fun getAllPresetsSync(): List<ContainerPreset> {
        return containerPresetDao.getAllPresetsSync().map { it.toContainerPreset() }
    }

    /**
     * Add a new preset with auto-assigned icon based on volume
     */
    suspend fun addPreset(name: String, volume: Double): Long {
        val icon = ContainerIconMapper.getIconForVolume(volume)
        val maxOrder = containerPresetDao.getMaxDisplayOrder()

        val entity = ContainerPresetEntity(
            name = name,
            volume = volume,
            iconType = icon.type.name,
            iconName = icon.name,
            isDefault = false,
            displayOrder = maxOrder + 1
        )

        return containerPresetDao.insertPreset(entity)
    }

    /**
     * Update an existing preset, re-assigning icon if volume changed
     */
    suspend fun updatePreset(id: Long, name: String, volume: Double) {
        val existing = containerPresetDao.getPresetById(id) ?: return
        val icon = ContainerIconMapper.getIconForVolume(volume)

        val updated = existing.copy(
            name = name,
            volume = volume,
            iconType = icon.type.name,
            iconName = icon.name
        )

        containerPresetDao.updatePreset(updated)
    }

    /**
     * Delete a preset by ID
     */
    suspend fun deletePreset(id: Long) {
        containerPresetDao.deletePresetById(id)
    }

    /**
     * Reset to default presets - clears all custom presets and re-seeds defaults
     */
    suspend fun resetToDefaults() {
        containerPresetDao.deleteAllPresets()
        seedDefaults()
    }

    /**
     * Seed default presets if the table is empty
     * Called on app startup
     */
    suspend fun seedDefaultsIfNeeded() {
        val count = containerPresetDao.getPresetCount()
        if (count == 0) {
            seedDefaults()
        }
    }

    /**
     * Internal method to seed the default presets
     */
    private suspend fun seedDefaults() {
        val defaults = getDefaultPresetEntities()
        containerPresetDao.insertPresets(defaults)
    }

    /**
     * Get the current preset count
     */
    suspend fun getPresetCount(): Int {
        return containerPresetDao.getPresetCount()
    }

    companion object {
        /**
         * Default preset configurations matching the original ContainerPreset.getDefaultPresets()
         */
        fun getDefaultPresetEntities(): List<ContainerPresetEntity> {
            return listOf(
                ContainerPresetEntity(
                    name = "Coffee Cup",
                    volume = 100.0,
                    iconType = IconType.VECTOR.name,
                    iconName = "LocalCafe",
                    isDefault = true,
                    displayOrder = 0
                ),
                ContainerPresetEntity(
                    name = "Tea Cup",
                    volume = 150.0,
                    iconType = IconType.DRAWABLE.name,
                    iconName = "glass_cup",
                    isDefault = true,
                    displayOrder = 1
                ),
                ContainerPresetEntity(
                    name = "Small Cup",
                    volume = 175.0,
                    iconType = IconType.DRAWABLE.name,
                    iconName = "water_loss",
                    isDefault = true,
                    displayOrder = 2
                ),
                ContainerPresetEntity(
                    name = "Medium Glass",
                    volume = 200.0,
                    iconType = IconType.DRAWABLE.name,
                    iconName = "water_medium",
                    isDefault = true,
                    displayOrder = 3
                ),
                ContainerPresetEntity(
                    name = "Large Glass",
                    volume = 300.0,
                    iconType = IconType.DRAWABLE.name,
                    iconName = "water_full",
                    isDefault = true,
                    displayOrder = 4
                ),
                ContainerPresetEntity(
                    name = "Water Bottle",
                    volume = 500.0,
                    iconType = IconType.DRAWABLE.name,
                    iconName = "water_bottle",
                    isDefault = true,
                    displayOrder = 5
                ),
                ContainerPresetEntity(
                    name = "Large Bottle",
                    volume = 1000.0,
                    iconType = IconType.DRAWABLE.name,
                    iconName = "water_bottle_large",
                    isDefault = true,
                    displayOrder = 6
                )
            )
        }
    }
}

/**
 * Extension function to convert entity to UI model
 */
fun ContainerPresetEntity.toContainerPreset(): ContainerPreset {
    val vectorIcon = if (iconType == IconType.VECTOR.name) {
        ContainerIconMapper.getVectorIcon(iconName)
    } else null

    val drawableRes = if (iconType == IconType.DRAWABLE.name) {
        ContainerIconMapper.getDrawableResId(iconName)
    } else null

    return ContainerPreset(
        id = id,
        name = name,
        volume = volume,
        isDefault = isDefault,
        icon = vectorIcon,
        iconRes = drawableRes,
        isCustom = !isDefault
    )
}
