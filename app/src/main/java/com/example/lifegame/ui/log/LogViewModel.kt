package com.example.lifegame.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifegame.data.entity.LogEntity
import com.example.lifegame.repository.LogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor(
    private val logRepository: LogRepository
) : ViewModel() {

    val allLogs: StateFlow<List<LogEntity>> = logRepository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun insertCustomLog(content: String) {
        viewModelScope.launch {
            logRepository.insertLog(
                type = "CUSTOM_NOTE",
                title = "我的笔记",
                details = content,
                isLocked = true
            )
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            logRepository.clearLogs()
        }
    }

    fun deleteLog(log: LogEntity) {
        viewModelScope.launch {
            logRepository.deleteLog(log)
        }
    }

    fun toggleLogLock(log: LogEntity) {
        viewModelScope.launch {
            logRepository.updateLog(log.copy(isLocked = !log.isLocked))
        }
    }

    fun getMaxLogLimit(): Int {
        return logRepository.getMaxLogLimit()
    }

    fun setMaxLogLimit(limit: Int) {
        logRepository.setMaxLogLimit(limit)
        viewModelScope.launch {
            logRepository.checkAndEnforceLimit()
        }
    }
}