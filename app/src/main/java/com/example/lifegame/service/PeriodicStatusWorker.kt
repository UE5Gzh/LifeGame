package com.example.lifegame.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
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
            val enabledStatusesWithEffects = statusRepository.enabledStatusesWithEffects.first()
            
            for (statusWithEffects in enabledStatusesWithEffects) {
                val status = statusWithEffects.status
                
                if (isStatusExpired(status, now)) {
                    autoDisableStatus(status)
                    continue
                }
                
                for (effect in statusWithEffects.effects) {
                    if (effect.effectType == 0) {
                        processPeriodicEffect(status, effect, now)
                    }
                }
            }
            
            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }

    private suspend fun processPeriodicEffect(
        status: com.example.lifegame.data.entity.StatusEntity,
        effect: com.example.lifegame.data.entity.StatusEffectEntity,
        now: Long
    ) {
        val periodMillis = when (effect.periodUnit) {
            0 -> effect.periodValue * 60 * 1000L
            1 -> effect.periodValue * 60 * 60 * 1000L
            else -> effect.periodValue * 24 * 60 * 60 * 1000L
        }
        
        val lastTrigger = if (effect.lastTriggerTime == 0L) status.startTime else effect.lastTriggerTime
        var nextTrigger = lastTrigger + periodMillis
        
        while (nextTrigger <= now) {
            val attr = attributeRepository.getAttributeById(effect.targetAttributeId)
            if (attr != null) {
                val newValue = attr.currentValue + effect.changeValue
                attributeRepository.updateAttribute(attr.copy(currentValue = newValue))
                
                logRepository.insertLog(
                    type = "STATUS_TRIGGERED",
                    title = "状态触发: ${status.name}",
                    details = "${attr.name} ${if (effect.changeValue >= 0) "+" else ""}${formatValue(effect.changeValue)}"
                )
            }
            
            nextTrigger += periodMillis
        }
        
        if (nextTrigger > lastTrigger + periodMillis) {
            statusRepository.updateEffect(effect.copy(lastTriggerTime = nextTrigger - periodMillis))
        }
    }

    private fun isStatusExpired(status: com.example.lifegame.data.entity.StatusEntity, now: Long): Boolean {
        if (status.durationValue <= 0 || !status.isEnabled) return false
        
        val durationMillis = when (status.durationUnit) {
            0 -> status.durationValue * 60 * 1000L
            1 -> status.durationValue * 60 * 60 * 1000L
            else -> status.durationValue * 24 * 60 * 60 * 1000L
        }
        
        val elapsed = now - status.startTime
        return elapsed >= durationMillis
    }

    private suspend fun autoDisableStatus(status: com.example.lifegame.data.entity.StatusEntity) {
        val updatedStatus = status.copy(isEnabled = false)
        statusRepository.updateStatus(updatedStatus)
        
        logRepository.insertLog(
            type = "STATUS_EXPIRED",
            title = "状态到期: ${status.name}",
            details = "持续时间已结束，自动关闭"
        )
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
                1, TimeUnit.MINUTES
            ).build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
