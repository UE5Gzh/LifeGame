package com.example.lifegame.service

import android.content.Context
import android.content.SharedPreferences
import com.example.lifegame.data.entity.AttributeWithRanks
import com.example.lifegame.data.entity.QuestWithDetails
import com.example.lifegame.repository.AttributeRepository
import com.example.lifegame.repository.QuestRepository
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
    private val attributeRepository: AttributeRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitorJob: Job? = null
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("quest_celebration_prefs", Context.MODE_PRIVATE)
    private val celebratedQuestIds = mutableSetOf<Long>()

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
                delay(60_000L)
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
            if (quest.quest.status == 0 && quest.quest.id !in celebratedQuestIds) {
                val progress = calculateProgress(quest, currentAttributes)
                if (progress >= 1f) {
                    celebratedQuestIds.add(quest.quest.id)
                    saveCelebratedQuestIds()
                    
                    questRepository.updateQuest(quest.quest.copy(status = 1))
                    
                    CelebrationBus.postQuestComplete(quest.quest.name, quest.quest.type)
                }
            }
        }
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
