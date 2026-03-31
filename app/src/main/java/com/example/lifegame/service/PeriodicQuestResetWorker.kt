package com.example.lifegame.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
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
        val now = System.currentTimeMillis()
        
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
                        resetQuest(q)
                    }
                }
                3 -> { // Weekly
                    if (q.quest.lastResetTime < weekStart) {
                        resetQuest(q)
                    }
                }
                1, 2 -> { // Main/Side quests - check deadline
                    val deadline = q.quest.deadline
                    if (deadline != null && q.quest.status == 0) {
                        val deadlineEnd = Calendar.getInstance().apply {
                            timeInMillis = deadline
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis
                        
                        if (now > deadlineEnd) {
                            failExpiredQuest(q)
                        }
                    }
                }
            }
        }
    }

    private suspend fun resetQuest(q: QuestWithDetails) {
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
            q.quest.copy(status = 0, lastResetTime = System.currentTimeMillis(), isFocused = false),
            q.attributeGoals,
            resetBehaviors,
            q.effects
        )
    }

    private suspend fun failExpiredQuest(q: QuestWithDetails) {
        val punishments = applyPunishments(q)
        questRepository.updateQuest(q.quest.copy(status = 3, isFocused = false))
        
        val typeStr = when (q.quest.type) {
            1 -> "主线"
            2 -> "支线"
            else -> ""
        }
        
        logRepository.insertLogWithDefaultLock(
            type = "QUEST_EXPIRED",
            title = "${typeStr}任务过期失败: ${q.quest.name}",
            details = if (punishments.isEmpty()) "触发惩罚: 无" else "触发惩罚: $punishments",
            questType = q.quest.type
        )
    }

    private suspend fun applyPunishments(questWithDetails: QuestWithDetails): String {
        val currentAttrs = attributeRepository.allAttributesWithRanks.first()
        val punishmentDetails = StringBuilder()

        for (effect in questWithDetails.effects.filter { it.isPunishment && it.type == 0 && it.attributeId != null }) {
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
        private const val IMMEDIATE_WORK_NAME = "immediate_quest_reset_work"

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

        fun scheduleImmediate(context: Context) {
            val oneTimeRequest = OneTimeWorkRequestBuilder<PeriodicQuestResetWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
        }
    }
}
