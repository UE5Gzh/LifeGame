package com.example.lifegame.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.lifegame.data.entity.QuestWithDetails
import com.example.lifegame.repository.AttributeRepository
import com.example.lifegame.repository.LogRepository
import com.example.lifegame.repository.QuestRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class PeriodicQuestResetWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val questRepository: QuestRepository,
    private val attributeRepository: AttributeRepository,
    private val logRepository: LogRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            checkAndResetQuests()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun checkAndResetQuests() {
        val allQuests = questRepository.getActiveQuestsWithDetails()
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val weekStart = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis > System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, -1)
            }
        }.timeInMillis

        for (q in allQuests) {
            when (q.quest.type) {
                0 -> { // Daily
                    if (q.quest.lastResetTime < todayStart) {
                        resetQuest(q, todayStart)
                    }
                }
                3 -> { // Weekly
                    if (q.quest.lastResetTime < weekStart) {
                        resetQuest(q, weekStart)
                    }
                }
            }
        }
    }

    private suspend fun resetQuest(q: QuestWithDetails, currentTime: Long) {
        if (q.quest.status == 0) {
            // Quest was not completed - apply punishments
            val punishments = applyPunishments(q)
            logRepository.insertLogWithDefaultLock(
                type = "QUEST_ABANDON",
                title = "日常任务超时失败: ${q.quest.name}",
                details = if (punishments.isEmpty()) "触发惩罚: 无" else "触发惩罚: $punishments",
                questType = q.quest.type
            )
        }

        val resetBehaviors = q.behaviorGoals.map { it.copy(currentCount = 0) }
        questRepository.updateQuestWithDetails(
            q.quest.copy(status = 0, lastResetTime = currentTime, isFocused = false),
            q.attributeGoals,
            resetBehaviors,
            q.effects
        )
    }

    private suspend fun applyPunishments(questWithDetails: QuestWithDetails): String {
        val currentAttrs = attributeRepository.allAttributesWithRanks.first()
        val punishmentDetails = StringBuilder()

        for (effect in questWithDetails.effects.filter { it.isPunishment && it.type == 0 }) {
            val attrWithRanks = currentAttrs.find { it.attribute.id == effect.attributeId }
            val attrToUpdate = attrWithRanks?.attribute
            if (attrToUpdate != null && effect.valueChange != null) {
                val newValue = attrToUpdate.currentValue + effect.valueChange
                attributeRepository.updateAttribute(attrToUpdate.copy(currentValue = newValue))
                punishmentDetails.append("${attrToUpdate.name} ${if (effect.valueChange > 0) "+" else ""}${effect.valueChange} ")
            }
        }

        return punishmentDetails.toString()
    }

    companion object {
        private const val WORK_NAME = "periodic_quest_reset_work"

        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<PeriodicQuestResetWorker>(
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
