package com.example.lifegame.service

import android.content.Context
import android.content.SharedPreferences
import com.example.lifegame.data.entity.AttributeWithRanks
import com.example.lifegame.data.entity.QuestWithDetails
import com.example.lifegame.repository.AttributeRepository
import com.example.lifegame.repository.LogRepository
import com.example.lifegame.repository.QuestRepository
import com.example.lifegame.util.AttributeChangeBus
import com.example.lifegame.util.AttributeChangeItem
import com.example.lifegame.util.CelebrationBus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestCompletionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val questRepository: QuestRepository,
    private val attributeRepository: AttributeRepository,
    private val logRepository: LogRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitorJob: Job? = null
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("quest_celebration_prefs", Context.MODE_PRIVATE)
    private val celebratedQuestIds = mutableSetOf<Long>()
    private val processingQuestIds = mutableSetOf<Long>()

    fun start() {
        if (monitorJob?.isActive == true) return
        
        loadCelebratedQuestIds()
        clearExpiredData()
        
        monitorJob = scope.launch {
            while (isActive) {
                try {
                    checkAllQuestCompletions()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(10_000L)
            }
        }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
    }

    private fun loadCelebratedQuestIds() {
        val savedIds = sharedPreferences.getStringSet("celebrated_quest_ids", emptySet()) ?: emptySet()
        celebratedQuestIds.clear()
        celebratedQuestIds.addAll(savedIds.mapNotNull { it.toLongOrNull() })
    }

    private fun saveCelebratedQuestIds() {
        sharedPreferences.edit()
            .putStringSet("celebrated_quest_ids", celebratedQuestIds.map { it.toString() }.toSet())
            .apply()
    }

    private fun clearExpiredData() {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val lastClearTime = sharedPreferences.getLong("last_clear_time", 0)
        
        if (lastClearTime < todayStart) {
            celebratedQuestIds.clear()
            sharedPreferences.edit()
                .putLong("last_clear_time", System.currentTimeMillis())
                .remove("celebrated_quest_ids")
                .apply()
        }
    }

    private suspend fun checkAllQuestCompletions() {
        val allQuests = questRepository.getActiveQuestsWithDetails()
        val currentAttributes = attributeRepository.allAttributesWithRanks.first()
        
        for (quest in allQuests) {
            processQuestCompletion(quest, currentAttributes)
        }
    }

    suspend fun checkImmediateCompletion(questId: Long) {
        if (questId in celebratedQuestIds || questId in processingQuestIds) return
        
        val allQuests = questRepository.getActiveQuestsWithDetails()
        val quest = allQuests.find { it.quest.id == questId } ?: return
        val currentAttributes = attributeRepository.allAttributesWithRanks.first()
        
        processQuestCompletion(quest, currentAttributes)
    }

    private suspend fun processQuestCompletion(quest: QuestWithDetails, currentAttributes: List<AttributeWithRanks>) {
        val questId = quest.quest.id
        
        if (quest.quest.status != 0) return
        if (questId in celebratedQuestIds) return
        if (questId in processingQuestIds) return
        
        processingQuestIds.add(questId)
        
        try {
            val progress = calculateProgress(quest, currentAttributes)
            if (progress >= 1f) {
                celebratedQuestIds.add(questId)
                saveCelebratedQuestIds()

                // 日常/周常任务自动领取奖励（跳过待领取状态）
                if (quest.quest.type == 0 || quest.quest.type == 3) {
                    applyRewardAndClaim(quest, currentAttributes)
                } else {
                    questRepository.updateQuest(quest.quest.copy(status = 1))
                    CelebrationBus.postQuestComplete(quest.quest.name, quest.quest.type)
                }
            }
        } finally {
            processingQuestIds.remove(questId)
        }
    }

    private suspend fun applyRewardAndClaim(quest: QuestWithDetails, currentAttributes: List<AttributeWithRanks>) {
        val rewardDetails = StringBuilder()
        val attributeChanges = mutableListOf<com.example.lifegame.util.AttributeChangeItem>()

        for (effect in quest.effects.filter { !it.isPunishment && it.type == 0 }) {
            val attrWithRanks = currentAttributes.find { it.attribute.id == effect.attributeId }
            val attrToUpdate = attrWithRanks?.attribute
            if (attrToUpdate != null && effect.valueChange != null) {
                val newValue = attrToUpdate.currentValue + effect.valueChange
                attributeRepository.updateAttribute(attrToUpdate.copy(currentValue = newValue))
                rewardDetails.append("${attrToUpdate.name} ${if(effect.valueChange > 0) "+" else ""}${effect.valueChange} ")
                attributeChanges.add(com.example.lifegame.util.AttributeChangeItem(attrToUpdate.name, effect.valueChange, attrToUpdate.colorHex))
            }
        }

        questRepository.updateQuest(quest.quest.copy(status = 2, isFocused = false))

        val questTypeStr = when (quest.quest.type) {
            0 -> "日常"
            3 -> "周常"
            else -> "支线"
        }
        logRepository.insertLogWithDefaultLock(
            type = "QUEST_COMPLETION",
            title = "完成${questTypeStr}任务: ${quest.quest.name}",
            details = if (rewardDetails.isEmpty()) "获得奖励: 无" else "获得奖励: $rewardDetails",
            questType = quest.quest.type
        )

        com.example.lifegame.util.AttributeChangeBus.postChanges(attributeChanges)
    }

    fun calculateProgress(quest: QuestWithDetails, currentAttributes: List<AttributeWithRanks>): Float {
        val totalGoals = quest.attributeGoals.size + quest.behaviorGoals.size
        if (totalGoals == 0) return 0f

        var totalProgress = 0f

        for (ag in quest.attributeGoals) {
            val attr = currentAttributes.find { it.attribute.id == ag.attributeId }?.attribute
            val currentVal = attr?.currentValue ?: 0f
            val targetVal = ag.targetValue
            if (targetVal <= 0f) {
                totalProgress += 1f
            } else {
                val p = (currentVal / targetVal).coerceIn(0f, 1f)
                totalProgress += p
            }
        }

        for (bg in quest.behaviorGoals) {
            if (bg.targetCount <= 0) {
                totalProgress += 1f
            } else {
                val p = (bg.currentCount.toFloat() / bg.targetCount.toFloat()).coerceIn(0f, 1f)
                totalProgress += p
            }
        }

        return totalProgress / totalGoals
    }

    fun isCelebrated(questId: Long): Boolean {
        return questId in celebratedQuestIds
    }

    fun markCelebrated(questId: Long) {
        celebratedQuestIds.add(questId)
        saveCelebratedQuestIds()
    }

    fun destroy() {
        stop()
        scope.cancel()
    }
}
