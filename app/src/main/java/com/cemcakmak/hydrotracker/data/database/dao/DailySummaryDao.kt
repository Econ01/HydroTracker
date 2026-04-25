// DailySummaryDao.kt
// Location: app/src/main/java/com/cemcakmak/hydrotracker/data/database/dao/DailySummaryDao.kt

package com.cemcakmak.hydrotracker.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.cemcakmak.hydrotracker.data.database.entities.DailySummary

@Dao
interface DailySummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: DailySummary)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummaries(summaries: List<DailySummary>)

    @Query("SELECT * FROM daily_summaries WHERE date = :date")
    fun getSummaryForDate(date: String): Flow<DailySummary?>

    @Query("SELECT * FROM daily_summaries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getSummariesForRange(startDate: String, endDate: String): Flow<List<DailySummary>>

    @Query("SELECT * FROM daily_summaries WHERE date >= date('now', '-30 days') ORDER BY date DESC")
    fun getLast30DaysSummaries(): Flow<List<DailySummary>>

    @Query("SELECT * FROM daily_summaries ORDER BY date DESC")
    fun getAllSummaries(): Flow<List<DailySummary>>

    @Update
    suspend fun updateSummary(summary: DailySummary)

    @Query("DELETE FROM daily_summaries WHERE date = :date")
    suspend fun deleteSummaryForDate(date: String)

    @Query("DELETE FROM daily_summaries")
    suspend fun deleteAllSummaries()
}