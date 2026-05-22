package com.ismartcoding.plain.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.helpers.generateId

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
    fun getAll(): List<DPomodoroItem>

    @Query("SELECT * FROM pomodoro_items WHERE date = :date")
    fun getByDate(date: String): DPomodoroItem?

    @Query("SELECT * FROM pomodoro_items WHERE date >= :startDate ORDER BY date DESC LIMIT :limit")
    fun getRecentRecords(startDate: String, limit: Int): List<DPomodoroItem>

    @Query("SELECT SUM(completed_count) FROM pomodoro_items")
    fun getTotalPomodoros(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg item: DPomodoroItem)

    @Update
    fun update(vararg item: DPomodoroItem)

    @Query("DELETE FROM pomodoro_items WHERE id = :id")
    fun deleteById(id: String)
}
