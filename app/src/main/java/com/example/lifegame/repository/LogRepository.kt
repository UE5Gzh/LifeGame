package com.example.lifegame.repository

import com.example.lifegame.data.dao.LogDao
import com.example.lifegame.data.entity.LogEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepository @Inject constructor(
    private val logDao: LogDao
) {
    val allLogs: Flow<List<LogEntity>> = logDao.getAllLogs()

    suspend fun insertLog(type: String, title: String, details: String) {
        val log = LogEntity(
            type = type,
            title = title,
            details = details
        )
        logDao.insertLog(log)
    }

    suspend fun clearLogs() {
        logDao.clearLogs()
    }
}