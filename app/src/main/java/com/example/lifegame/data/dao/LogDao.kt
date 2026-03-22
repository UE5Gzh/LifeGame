package com.example.lifegame.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.lifegame.data.entity.LogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert
    suspend fun insertLog(log: LogEntity)

    @Update
    suspend fun updateLog(log: LogEntity)

    @Delete
    suspend fun deleteLog(log: LogEntity)

    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntity>>

    @Query("SELECT COUNT(*) FROM logs")
    suspend fun getLogCount(): Int

    @Query("DELETE FROM logs WHERE isLocked = 0 AND id IN (SELECT id FROM logs WHERE isLocked = 0 ORDER BY timestamp ASC LIMIT :count)")
    suspend fun deleteOldestUnlockedLogs(count: Int)

    @Query("DELETE FROM logs WHERE isLocked = 0")
    suspend fun clearLogs()
}