package com.dev.hydrotracker.data.database.entities

import androidx.room.*
import com.dev.hydrotracker.data.models.BeverageType
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
    val beverageType: String = BeverageType.WATER.name
) {
    /**
     * Returns a formatted time string according to system locale and timezone preferences
     */
    fun getFormattedTime(): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val localDateTime = instant.atZone(ZoneId.systemDefault())

        // Use system locale and preferences for 12/24 hour format
        val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())

        return formatter.format(localDateTime)
    }

    /**
     * Returns a formatted date and time string according to system locale and timezone preferences
     */
    fun getFormattedDateTime(): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val localDateTime = instant.atZone(ZoneId.systemDefault())

        // Use system locale and preferences for date and time format
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)
            .withLocale(Locale.getDefault())

        return formatter.format(localDateTime)
    }

    fun getFormattedAmount(): String {
        return if (amount >= 1000) {
            val liters = amount / 1000
            val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
            formatter.maximumFractionDigits = 1
            "${formatter.format(liters)} L"
        } else {
            "${amount.toInt()} ml"
        }
    }

    /**
     * Check if this entry was imported from an external Health Connect app
     */
    fun isExternalEntry(): Boolean {
        return note?.startsWith("Imported from ") == true
    }

    /**
     * Get the beverage type enum for this entry
     */
    fun getBeverageType(): BeverageType {
        return BeverageType.fromStringOrDefault(beverageType)
    }

    /**
     * Get the effective hydration amount considering beverage type multiplier
     */
    fun getEffectiveHydrationAmount(): Double {
        return amount * getBeverageType().hydrationMultiplier
    }

    /**
     * Get formatted effective hydration amount
     */
    fun getFormattedEffectiveAmount(): String {
        val effectiveAmount = getEffectiveHydrationAmount()
        return if (effectiveAmount >= 1000) {
            val liters = effectiveAmount / 1000
            val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
            formatter.maximumFractionDigits = 1
            "${formatter.format(liters)} L"
        } else {
            "${effectiveAmount.toInt()} ml"
        }
    }

    companion object {
        fun create(
            amount: Double,
            containerType: String,
            containerVolume: Double,
            beverageType: BeverageType = BeverageType.WATER,
            note: String? = null
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
                beverageType = beverageType.name
            )
        }
    }
}

