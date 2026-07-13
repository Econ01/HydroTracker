package com.cemcakmak.hydrotracker.data.database.entities

import android.content.Context
import androidx.room.*
import com.cemcakmak.hydrotracker.data.models.BeverageType
import com.cemcakmak.hydrotracker.data.models.EntrySource
import com.cemcakmak.hydrotracker.data.models.TimeFormat
import com.cemcakmak.hydrotracker.data.models.VolumeUnit
import com.cemcakmak.hydrotracker.utils.DateTimeFormatters
import com.cemcakmak.hydrotracker.utils.VolumeUnitConverter
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@Entity(
    tableName = "water_intake_entries",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["date"])
    ]
)
data class WaterIntakeEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "container_type")
    val containerType: String,

    @ColumnInfo(name = "container_volume")
    val containerVolume: Double,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "health_connect_record_id")
    val healthConnectRecordId: String? = null,

    @ColumnInfo(name = "is_hidden")
    val isHidden: Boolean = false,

    @ColumnInfo(name = "beverage_type")
    val beverageType: String = BeverageType.WATER.name,

    // Effectiveness captured at log time for custom beverages. Null for preset/legacy rows,
    // which fall back to the BeverageType enum multiplier.
    @ColumnInfo(name = "beverage_multiplier")
    val beverageMultiplier: Double? = null,

    // Container icon captured at log time so recent entries keep the correct icon even if the
    // preset is edited or deleted later.
    @ColumnInfo(name = "icon_type", defaultValue = "DRAWABLE")
    val iconType: String = "DRAWABLE",

    @ColumnInfo(name = "icon_name", defaultValue = "water_filled")
    val iconName: String = "water_filled",

    @ColumnInfo(name = "source", defaultValue = "LOCAL")
    val source: EntrySource = EntrySource.LOCAL
) {
    /**
     * Returns a formatted time string according to the user's [timeFormat] preference.
     * Internal storage continues to use epoch milliseconds; this only affects display.
     */
    fun getFormattedTime(context: Context, timeFormat: TimeFormat): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        return DateTimeFormatters.formatTime(context, localDateTime.toLocalTime(), timeFormat)
    }

    /**
     * Returns the intake amount formatted in the user's preferred [volumeUnit].
     * Internal storage continues to use millilitres.
     */
    fun getFormattedAmount(context: Context, volumeUnit: VolumeUnit): String {
        return VolumeUnitConverter.format(context, amount, volumeUnit)
    }

    /**
     * Check if this entry was imported from an external Health Connect app
     */
    fun isExternalEntry(): Boolean {
        return source == EntrySource.HEALTH_CONNECT_EXTERNAL
    }

    /**
     * Check if this entry can be mirrored to Health Connect on write/update/delete.
     * Local entries and our own restored entries are mirrored; external imports are read-only.
     */
    fun isSyncableToHealthConnect(): Boolean {
        return source == EntrySource.LOCAL || source == EntrySource.HEALTH_CONNECT_RESTORED
    }

    /**
     * Get the beverage type enum for this entry
     */
    fun getBeverageType(): BeverageType {
        return BeverageType.fromStringOrDefault(beverageType)
    }

    /**
     * Get the effective hydration amount considering beverage type multiplier.
     * Custom beverages store their multiplier on the entry; presets/legacy rows use the enum.
     */
    fun getEffectiveHydrationAmount(): Double {
        return amount * (beverageMultiplier ?: getBeverageType().hydrationMultiplier)
    }

    /**
     * Get formatted effective hydration amount in the user's preferred [volumeUnit].
     * Internal storage continues to use millilitres.
     */
    fun getFormattedEffectiveAmount(context: Context, volumeUnit: VolumeUnit): String {
        return VolumeUnitConverter.format(context, getEffectiveHydrationAmount(), volumeUnit)
    }

    companion object {
        fun create(
            amount: Double,
            containerType: String,
            containerVolume: Double,
            beverageType: BeverageType = BeverageType.WATER,
            note: String? = null,
            iconType: String = "DRAWABLE",
            iconName: String = "water_filled",
            source: EntrySource = EntrySource.LOCAL
        ): WaterIntakeEntry {
            val now = System.currentTimeMillis()
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(java.util.Date())

            return WaterIntakeEntry(
                amount = amount,
                timestamp = now,
                date = today,
                containerType = containerType,
                containerVolume = containerVolume,
                note = note,
                beverageType = beverageType.name,
                iconType = iconType,
                iconName = iconName,
                source = source
            )
        }
    }
}

