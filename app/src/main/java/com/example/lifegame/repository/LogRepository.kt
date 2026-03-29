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

    fun getLogsPaged(limit: Int, offset: Int): Flow<List<LogEntity>> {
        return logDao.getLogsPaged(limit, offset)
    }

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

    suspend fun insertLogWithDefaultLock(type: String, title: String, details: String, questType: Int) {
        val isLocked = getDefaultLockForQuestType(questType)
        insertLog(type, title, details, isLocked)
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
        return sharedPreferences.getInt("max_log_limit", 50000)
    }

    fun setMaxLogLimit(limit: Int) {
        sharedPreferences.edit().putInt("max_log_limit", limit).apply()
    }

    fun getDefaultLockForQuestType(questType: Int): Boolean {
        val key = when (questType) {
            0 -> "default_lock_daily_quest"
            1 -> "default_lock_main_quest"
            2 -> "default_lock_side_quest"
            3 -> "default_lock_weekly_quest"
            else -> return false
        }
        return sharedPreferences.getBoolean(key, questType == 1 || questType == 2)
    }

    fun setDefaultLockForQuestType(questType: Int, isLocked: Boolean) {
        val key = when (questType) {
            0 -> "default_lock_daily_quest"
            1 -> "default_lock_main_quest"
            2 -> "default_lock_side_quest"
            3 -> "default_lock_weekly_quest"
            else -> return
        }
        sharedPreferences.edit().putBoolean(key, isLocked).apply()
    }

    fun getDefaultLockSettings(): Map<String, Boolean> {
        return mapOf(
            "daily" to getDefaultLockForQuestType(0),
            "main" to getDefaultLockForQuestType(1),
            "side" to getDefaultLockForQuestType(2),
            "weekly" to getDefaultLockForQuestType(3)
        )
    }

    fun isStatusTriggerLogEnabled(): Boolean {
        return sharedPreferences.getBoolean("status_trigger_log_enabled", true)
    }

    fun setStatusTriggerLogEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("status_trigger_log_enabled", enabled).apply()
    }
}
