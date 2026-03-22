package com.example.lifegame.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.lifegame.data.dao.LogDao
import com.example.lifegame.data.entity.LogEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepository @Inject constructor(
    private val logDao: LogDao,
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("log_prefs", Context.MODE_PRIVATE)

    val allLogs: Flow<List<LogEntity>> = logDao.getAllLogs()

    suspend fun insertLog(type: String, title: String, details: String, isLocked: Boolean = false) {
        val log = LogEntity(
            type = type,
            title = title,
            details = details,
            isLocked = isLocked
        )
        logDao.insertLog(log)
        checkAndEnforceLimit()
    }

    suspend fun updateLog(log: LogEntity) {
        logDao.updateLog(log)
    }

    suspend fun deleteLog(log: LogEntity) {
        logDao.deleteLog(log)
    }

    suspend fun clearLogs() {
        logDao.clearLogs()
    }

    suspend fun checkAndEnforceLimit() {
        val maxLimit = getMaxLogLimit()
        val currentCount = logDao.getLogCount()
        if (currentCount > maxLimit) {
            val exceedCount = currentCount - maxLimit
            logDao.deleteOldestUnlockedLogs(exceedCount)
        }
    }

    fun getMaxLogLimit(): Int {
        return sharedPreferences.getInt("max_log_limit", 2000)
    }

    fun setMaxLogLimit(limit: Int) {
        sharedPreferences.edit().putInt("max_log_limit", limit).apply()
    }
}