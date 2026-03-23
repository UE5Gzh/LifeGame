package com.example.lifegame.repository

import com.example.lifegame.data.dao.QuestDao
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

    suspend fun incrementBehaviorGoalCount(behaviorId: Long) {
        questDao.incrementBehaviorGoalCount(behaviorId)
    }

    suspend fun getActiveQuestsWithDetails(): List<QuestWithDetails> {
        return questDao.getActiveQuestsWithDetails()
    }
}