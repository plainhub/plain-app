package com.ismartcoding.plain.db

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Update
import com.ismartcoding.plain.lib.generateId

@Entity(tableName = "pomodoro_items")
data class DPomodoroItem(
    @PrimaryKey override var id: String = generateId(),
) : IData, DEntityBase() {
    var date: String = "" // YYYY-MM-DD format

    @ColumnInfo(name = "completed_count")
    var completedCount: Int = 0

    @ColumnInfo(name = "total_work_seconds")
    var totalWorkSeconds: Int = 0

    @ColumnInfo(name = "total_break_seconds")
    var totalBreakSeconds: Int = 0
}

@Dao
interface PomodoroItemDao {
    @Query("SELECT * FROM pomodoro_items ORDER BY date DESC")
    suspend fun getAll(): List<DPomodoroItem>

    @Query("SELECT * FROM pomodoro_items WHERE date = :date")
    suspend fun getByDate(date: String): DPomodoroItem?

    @Query("SELECT * FROM pomodoro_items WHERE date >= :startDate ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentRecords(startDate: String, limit: Int): List<DPomodoroItem>

    @Query("SELECT SUM(completed_count) FROM pomodoro_items")
    suspend fun getTotalPomodoros(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg item: DPomodoroItem)

    @Update
    suspend fun update(vararg item: DPomodoroItem)

    @Query("DELETE FROM pomodoro_items WHERE id = :id")
    suspend fun deleteById(id: String)
}
