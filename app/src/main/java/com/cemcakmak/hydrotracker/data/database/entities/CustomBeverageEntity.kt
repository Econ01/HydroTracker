package com.cemcakmak.hydrotracker.data.database.entities

import androidx.room.*

/**
 * A user-defined custom beverage. Presets remain the [com.cemcakmak.hydrotracker.data.models.BeverageType]
 * enum; these are the additional beverages a user creates.
 *
 * Display order and visibility are tracked in BeveragePreferences (as "custom:<id>" tokens);
 * this table only holds the definition. Custom beverages are deleted, never hidden.
 */
@Entity(tableName = "custom_beverages")
data class CustomBeverageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "hydration_multiplier")
    val hydrationMultiplier: Double,

    @ColumnInfo(name = "icon_key")
    val iconKey: String
)
