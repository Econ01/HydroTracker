package com.cemcakmak.hydrotracker.data.database.entities

import androidx.room.*

@Entity(
    tableName = "container_presets",
    indices = [
        Index(value = ["display_order"])
    ]
)
data class ContainerPresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "volume")
    val volume: Double,

    @ColumnInfo(name = "icon_type")
    val iconType: String,  // "VECTOR" or "DRAWABLE"

    @ColumnInfo(name = "icon_name")
    val iconName: String,  // e.g., "LocalCafe" or "water_bottle"

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,

    @ColumnInfo(name = "display_order")
    val displayOrder: Int = 0
)
