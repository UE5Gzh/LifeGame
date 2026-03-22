package com.example.lifegame.ui.quest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifegame.data.entity.AttributeWithRanks
import com.example.lifegame.data.entity.QuestAttributeGoalEntity
import com.example.lifegame.data.entity.QuestBehaviorGoalEntity
import com.example.lifegame.data.entity.QuestEffectEntity
import com.example.lifegame.data.entity.QuestEntity
import com.example.lifegame.data.entity.QuestWithDetails
import com.example.lifegame.data.entity.BehaviorWithModifiers
import com.example.lifegame.repository.BehaviorRepository
import com.example.lifegame.repository.AttributeRepository
import com.example.lifegame.repository.QuestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class QuestViewModel @Inject constructor(
    private val questRepository: QuestRepository,
    private val attributeRepository: AttributeRepository,
    private val behaviorRepository: BehaviorRepository
) : ViewModel() {

    val attributes: StateFlow<List<AttributeWithRanks>> = attributeRepository.allAttributesWithRanks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val behaviors: StateFlow<List<BehaviorWithModifiers>> = behaviorRepository.allBehaviorsWithModifiers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val quests: StateFlow<List<QuestWithDetails>> = questRepository.allQuestsWithDetails
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        checkDailyResets()
    }

    private fun checkDailyResets() {
        viewModelScope.launch {
            val allQuests = questRepository.getActiveQuestsWithDetails()
            val now = Calendar.getInstance()
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            for (q in allQuests) {
                if (q.quest.type == 0 && q.quest.lastResetTime < todayStart) {
                    // It's a daily quest and hasn't been reset today
                    if (q.quest.status == 0) {
                        // Failed
                        questRepository.updateQuest(q.quest.copy(status = 3, lastResetTime = System.currentTimeMillis()))
                        applyPunishments(q)
                    } else if (q.quest.status == 2) {
                        // Claimed -> Reset to In Progress
                        questRepository.updateQuest(q.quest.copy(status = 0, lastResetTime = System.currentTimeMillis()))
                        // Also reset behavior counts
                        val resetBehaviors = q.behaviorGoals.map { it.copy(currentCount = 0) }
                        questRepository.updateQuestWithDetails(q.quest.copy(status = 0, lastResetTime = System.currentTimeMillis()), q.attributeGoals, resetBehaviors, q.effects)
                    }
                }
            }
        }
    }

    fun checkQuestCompletions(currentAttributes: List<AttributeWithRanks>) {
        viewModelScope.launch {
            val allQuests = quests.value
            for (q in allQuests) {
                if (q.quest.status == 0) {
                    val progress = calculateProgress(q, currentAttributes)
                    if (progress >= 1f) {
                        questRepository.updateQuest(q.quest.copy(status = 1))
                    }
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
            val currentVal = attr?.currentValue ?: 0
            val targetVal = ag.targetValue
            if (targetVal <= 0) {
                totalProgress += 1f
            } else {
                val p = (currentVal.toFloat() / targetVal.toFloat()).coerceIn(0f, 1f)
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

    fun claimReward(questWithDetails: QuestWithDetails) {
        viewModelScope.launch {
            // Apply rewards
            val currentAttrs = attributes.value
            for (effect in questWithDetails.effects.filter { !it.isPunishment && it.type == 0 }) {
                val attrToUpdate = currentAttrs.find { it.attribute.id == effect.attributeId }?.attribute
                if (attrToUpdate != null && effect.valueChange != null) {
                    attributeRepository.updateAttribute(attrToUpdate.copy(currentValue = attrToUpdate.currentValue + effect.valueChange))
                }
            }
            questRepository.updateQuest(questWithDetails.quest.copy(status = 2))
        }
    }

    private suspend fun applyPunishments(questWithDetails: QuestWithDetails) {
        val currentAttrs = attributes.value
        for (effect in questWithDetails.effects.filter { it.isPunishment && it.type == 0 }) {
            val attrToUpdate = currentAttrs.find { it.attribute.id == effect.attributeId }?.attribute
            if (attrToUpdate != null && effect.valueChange != null) {
                attributeRepository.updateAttribute(attrToUpdate.copy(currentValue = attrToUpdate.currentValue + effect.valueChange))
            }
        }
    }

    fun createQuest(
        name: String,
        type: Int,
        deadline: Long?,
        attributeGoals: List<QuestAttributeGoalEntity>,
        behaviorGoals: List<QuestBehaviorGoalEntity>,
        effects: List<QuestEffectEntity>
    ) {
        viewModelScope.launch {
            val q = QuestEntity(
                name = name,
                type = type,
                deadline = deadline,
                status = 0
            )
            questRepository.insertQuestWithDetails(q, attributeGoals, behaviorGoals, effects)
        }
    }
}