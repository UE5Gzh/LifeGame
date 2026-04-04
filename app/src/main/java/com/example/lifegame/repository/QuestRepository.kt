package com.example.lifegame.repository

import com.example.lifegame.data.dao.QuestDao
import com.example.lifegame.data.entity.AttributeWithRanks
import com.example.lifegame.data.entity.QuestAttributeGoalEntity
import com.example.lifegame.data.entity.QuestBehaviorGoalEntity
import com.example.lifegame.data.entity.QuestEffectEntity
import com.example.lifegame.data.entity.QuestEntity
import com.example.lifegame.data.entity.QuestWithDetails
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class QuestRepository @Inject constructor(
    private val questDao: QuestDao
) {
    val allQuestsWithDetails: Flow<List<QuestWithDetails>> = questDao.getAllQuestsWithDetails()

    suspend fun insertQuestWithDetails(
        quest: QuestEntity,
        attributeGoals: List<QuestAttributeGoalEntity>,
        behaviorGoals: List<QuestBehaviorGoalEntity>,
        effects: List<QuestEffectEntity>
    ) {
        val questId = questDao.insertQuest(quest)
        
        questDao.insertAttributeGoals(attributeGoals.map { it.copy(questId = questId) })
        questDao.insertBehaviorGoals(behaviorGoals.map { it.copy(questId = questId) })
        questDao.insertEffects(effects.map { it.copy(questId = questId) })
    }

    suspend fun updateQuestWithDetails(
        quest: QuestEntity,
        attributeGoals: List<QuestAttributeGoalEntity>,
        behaviorGoals: List<QuestBehaviorGoalEntity>,
        effects: List<QuestEffectEntity>
    ) {
        questDao.updateQuestWithDetails(quest, attributeGoals, behaviorGoals, effects)
    }

    suspend fun updateQuest(quest: QuestEntity) {
        questDao.updateQuest(quest)
    }

    suspend fun updateQuests(quests: List<QuestEntity>) {
        questDao.updateQuests(quests)
    }

    suspend fun deleteQuest(quest: QuestEntity) {
        questDao.deleteQuest(quest)
    }

    suspend fun incrementBehaviorGoalCountAndCheckCompletion(
        behaviorId: Long,
        currentAttributes: List<AttributeWithRanks>
    ): List<QuestWithDetails> {
        questDao.incrementBehaviorGoalCount(behaviorId)
        
        val activeQuests = questDao.getActiveQuestsWithDetails()
        val completedQuests = mutableListOf<QuestWithDetails>()
        
        for (quest in activeQuests) {
            if (quest.quest.status == 0 && isQuestComplete(quest, currentAttributes)) {
                completedQuests.add(quest)
            }
        }
        
        return completedQuests
    }
    
    private fun isQuestComplete(quest: QuestWithDetails, currentAttributes: List<AttributeWithRanks>): Boolean {
        val totalGoals = quest.attributeGoals.size + quest.behaviorGoals.size
        if (totalGoals == 0) return false

        var completedGoals = 0

        for (ag in quest.attributeGoals) {
            val attr = currentAttributes.find { it.attribute.id == ag.attributeId }?.attribute
            val currentVal = attr?.currentValue ?: 0f
            if (currentVal >= ag.targetValue) {
                completedGoals++
            }
        }

        for (bg in quest.behaviorGoals) {
            if (bg.currentCount >= bg.targetCount) {
                completedGoals++
            }
        }

        return completedGoals == totalGoals
    }

    suspend fun getActiveQuestsWithDetails(): List<QuestWithDetails> {
        return questDao.getActiveQuestsWithDetails()
    }

    fun getFocusedQuest(): Flow<QuestWithDetails?> {
        return questDao.getFocusedQuest()
    }
}