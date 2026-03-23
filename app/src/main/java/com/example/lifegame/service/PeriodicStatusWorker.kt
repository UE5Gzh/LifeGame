package com.example.lifegame.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.lifegame.data.entity.StatusEntity
import com.example.lifegame.repository.AttributeRepository
import com.example.lifegame.repository.LogRepository
import com.example.lifegame.repository.StatusRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@HiltWorker
class PeriodicStatusWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val statusRepository: StatusRepository,
    private val attributeRepository: AttributeRepository,
    private val logRepository: LogRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val now = System.currentTimeMillis()
            val enabledStatuses = statusRepository.enabledPeriodicStatuses.first()
            
            for (status in enabledStatuses) {
                processPeriodicStatus(status, now)
            }
            
            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }

    private suspend fun processPeriodicStatus(status: StatusEntity, now: Long) {
        val periodMillis = if (status.periodUnit == 0) {
            status.periodValue * 60 * 60 * 1000L
        } else {
            status.periodValue * 24 * 60 * 60 * 1000L
        }
        
        val lastTrigger = if (status.lastTriggerTime == 0L) status.startTime else status.lastTriggerTime
        var nextTrigger = lastTrigger + periodMillis
        
        while (nextTrigger <= now) {
            val attr = attributeRepository.getAttributeById(status.targetAttributeId)
            if (attr != null) {
                val newValue = attr.currentValue + status.changeValue
                attributeRepository.updateAttribute(attr.copy(currentValue = newValue))
                
                logRepository.insertLog(
                    type = "STATUS_TRIGGERED",
                    title = "状态触发: ${status.name}",
                    details = "${attr.name} ${if (status.changeValue >= 0) "+" else ""}${formatValue(status.changeValue)}"
                )
            }
            
            nextTrigger += periodMillis
        }
        
        if (nextTrigger > lastTrigger + periodMillis) {
            statusRepository.updateStatus(status.copy(lastTriggerTime = nextTrigger - periodMillis))
        }
    }

    private fun formatValue(value: Float): String {
        return if (value == value.roundToInt().toFloat()) {
            value.roundToInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }

    companion object {
        private const val WORK_NAME = "periodic_status_work"
        
        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<PeriodicStatusWorker>(
                15, TimeUnit.MINUTES
            ).build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
