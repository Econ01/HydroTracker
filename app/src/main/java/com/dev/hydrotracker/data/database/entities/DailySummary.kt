// DailySummary.kt
// Location: app/src/main/java/com/cemcakmak/hydrotracker/data/database/entities/DailySummary.kt

package com.dev.hydrotracker.data.database.entities

import androidx.room.*

@Entity(
    tableName = "daily_summaries",
    indices = [Index(value = ["date"], unique = true)]
)
data class DailySummary(
    @PrimaryKey
    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "total_intake")
    val totalIntake: Double,

    @ColumnInfo(name = "daily_goal")
    val dailyGoal: Double,

    @ColumnInfo(name = "goal_achieved")
    val goalAchieved: Boolean,

    @ColumnInfo(name = "goal_percentage")
    val goalPercentage: Float,

    @ColumnInfo(name = "entry_count")
    val entryCount: Int,

    @ColumnInfo(name = "first_intake_time")
    val firstIntakeTime: Long?,

    @ColumnInfo(name = "last_intake_time")
    val lastIntakeTime: Long?,

    @ColumnInfo(name = "largest_intake")
    val largestIntake: Double,

    @ColumnInfo(name = "average_intake")
    val averageIntake: Double,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getCompletionText(): String {
        val percentage = (goalPercentage * 100).toInt()
        return "$percentage%"
    }

    fun getFormattedTotal(): String {
        return when {
            totalIntake >= 1000 -> "${String.format("%.1f", totalIntake / 1000)} L"
            else -> "${totalIntake.toInt()} ml"
        }
    }

    fun isToday(): Boolean {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        return date == today
    }
}