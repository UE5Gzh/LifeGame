package com.example.lifegame.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.lifegame.data.entity.LogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert
    suspend fun insertLog(log: LogEntity)

    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntity>>

    @Query("DELETE FROM logs")
    suspend fun clearLogs()
}