package com.cemcakmak.hydrotracker.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry

@Dao
interface WaterIntakeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: WaterIntakeEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<WaterIntakeEntry>)

    @Query("SELECT * FROM water_intake_entries WHERE date = :date AND is_hidden = 0 ORDER BY timestamp DESC")
    fun getEntriesForDate(date: String): Flow<List<WaterIntakeEntry>>

    @Query("SELECT * FROM water_intake_entries WHERE date = :date AND is_hidden = 0 ORDER BY timestamp DESC")
    suspend fun getEntriesForDateSync(date: String): List<WaterIntakeEntry>

    @Query("SELECT * FROM water_intake_entries WHERE date = :date ORDER BY timestamp DESC")
    suspend fun getAllEntriesForDateSync(date: String): List<WaterIntakeEntry>

    @Query("SELECT * FROM water_intake_entries WHERE date BETWEEN :startDate AND :endDate AND is_hidden = 0 ORDER BY timestamp DESC")
    fun getEntriesForDateRange(startDate: String, endDate: String): Flow<List<WaterIntakeEntry>>

    @Query("SELECT * FROM water_intake_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    suspend fun getAllEntriesForDateRangeSync(startDate: String, endDate: String): List<WaterIntakeEntry>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM water_intake_entries WHERE date = :date AND is_hidden = 0")
    fun getTotalIntakeForDate(date: String): Flow<Double>

    @Query("SELECT COUNT(*) FROM water_intake_entries WHERE date = :date AND is_hidden = 0")
    suspend fun getEntryCountForDate(date: String): Int

    @Query("SELECT COUNT(*) FROM water_intake_entries")
    suspend fun getEntryCount(): Int

    @Query("SELECT * FROM water_intake_entries WHERE date >= date('now', '-30 days') AND is_hidden = 0 ORDER BY timestamp DESC")
    fun getLast30DaysEntries(): Flow<List<WaterIntakeEntry>>

    @Query("SELECT * FROM water_intake_entries WHERE is_hidden = 0 ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<WaterIntakeEntry>>

    @Query("SELECT * FROM water_intake_entries WHERE is_hidden = 0 ORDER BY timestamp DESC")
    suspend fun getAllEntriesForExportSync(): List<WaterIntakeEntry>

    @Query("SELECT * FROM water_intake_entries ORDER BY timestamp DESC")
    suspend fun getAllEntriesSync(): List<WaterIntakeEntry>

    @Update
    suspend fun updateEntry(entry: WaterIntakeEntry)

    @Update
    suspend fun updateEntries(entries: List<WaterIntakeEntry>)

    @Delete
    suspend fun deleteEntry(entry: WaterIntakeEntry)

    @Query("DELETE FROM water_intake_entries WHERE id = :entryId")
    suspend fun deleteEntryById(entryId: Long)

    @Query("DELETE FROM water_intake_entries")
    suspend fun deleteAllEntries()

    @Query("DELETE FROM water_intake_entries WHERE timestamp <= :timestampMillis")
    suspend fun deleteEntriesBefore(timestampMillis: Long)

    /** Count entries matching the [deleteEntriesBefore] predicate (inclusive cutoff). */
    @Query("SELECT COUNT(*) FROM water_intake_entries WHERE timestamp <= :timestampMillis")
    suspend fun countEntriesBefore(timestampMillis: Long): Int

    @Query("UPDATE water_intake_entries SET is_hidden = 1 WHERE id = :entryId")
    suspend fun hideEntry(entryId: Long)

    @Query("UPDATE water_intake_entries SET is_hidden = 0 WHERE id = :entryId")
    suspend fun unhideEntry(entryId: Long)

    @Query("SELECT * FROM water_intake_entries WHERE is_hidden = 1 ORDER BY timestamp DESC")
    fun getHiddenEntries(): Flow<List<WaterIntakeEntry>>

@Query("""
    SELECT date, SUM(amount) as totalAmount, COUNT(*) as entryCount
    FROM water_intake_entries
    WHERE date BETWEEN :startDate AND :endDate AND is_hidden = 0
    GROUP BY date
    ORDER BY date ASC
""")
suspend fun getDailyTotals(startDate: String, endDate: String): List<DailyTotal>

@Query("""
    SELECT container_type AS name, container_volume AS volume, COUNT(*) AS count
    FROM water_intake_entries
    WHERE is_hidden = 0
    GROUP BY container_type, container_volume
    ORDER BY count DESC
    LIMIT 1
""")
suspend fun getMostUsedContainer(): MostUsedContainer?

@Query("""
    SELECT container_type AS name, container_volume AS volume, COUNT(*) AS count
    FROM water_intake_entries
    WHERE is_hidden = 0
    GROUP BY container_type, container_volume
    ORDER BY count DESC
    LIMIT :limit
""")
suspend fun getMostUsedContainers(limit: Int): List<MostUsedContainer>

@Query("""
    SELECT container_type AS containerName, container_volume AS volume,
           beverage_type AS beverage, COUNT(*) AS count
    FROM water_intake_entries
    WHERE is_hidden = 0
    GROUP BY container_type, container_volume, beverage_type
    ORDER BY count DESC
    LIMIT :limit
""")
suspend fun getMostUsedQuickAddCombos(limit: Int): List<MostUsedQuickAddCombo>

@Query("SELECT * FROM water_intake_entries WHERE is_hidden = 0 ORDER BY timestamp DESC LIMIT 1")
suspend fun getMostRecentEntry(): WaterIntakeEntry?
}

data class DailyTotal(
    val date: String,
    val totalAmount: Double,
    val entryCount: Int
)

data class MostUsedContainer(
    val name: String,
    val volume: Double,
    val count: Int
)

/** A frequently logged (container, volume, beverage) combination, ranked by entry count. */
data class MostUsedQuickAddCombo(
    val containerName: String,
    val volume: Double,
    val beverage: String,
    val count: Int
)

