package com.example.lifegame.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.lifegame.data.entity.QuestAttributeGoalEntity
import com.example.lifegame.data.entity.QuestBehaviorGoalEntity
import com.example.lifegame.data.entity.QuestEffectEntity
import com.example.lifegame.data.entity.QuestEntity
import com.example.lifegame.data.entity.QuestWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    @Transaction
    @Query("SELECT * FROM quests ORDER BY sortOrder ASC, createdAt DESC")
    fun getAllQuestsWithDetails(): Flow<List<QuestWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: QuestEntity): Long

    @Update
    suspend fun updateQuest(quest: QuestEntity)

    @Update
    suspend fun updateQuests(quests: List<QuestEntity>)

    @Delete
    suspend fun deleteQuest(quest: QuestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttributeGoals(goals: List<QuestAttributeGoalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBehaviorGoals(goals: List<QuestBehaviorGoalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEffects(effects: List<QuestEffectEntity>)

    @Query("DELETE FROM quest_attribute_goals WHERE questId = :questId")
    suspend fun deleteAttributeGoalsByQuestId(questId: Long)

    @Query("DELETE FROM quest_behavior_goals WHERE questId = :questId")
    suspend fun deleteBehaviorGoalsByQuestId(questId: Long)

    @Query("DELETE FROM quest_effects WHERE questId = :questId")
    suspend fun deleteEffectsByQuestId(questId: Long)

    @Transaction
    suspend fun updateQuestWithDetails(
        quest: QuestEntity,
        attributeGoals: List<QuestAttributeGoalEntity>,
        behaviorGoals: List<QuestBehaviorGoalEntity>,
        effects: List<QuestEffectEntity>
    ) {
        updateQuest(quest)
        
        deleteAttributeGoalsByQuestId(quest.id)
        deleteBehaviorGoalsByQuestId(quest.id)
        deleteEffectsByQuestId(quest.id)
        
        insertAttributeGoals(attributeGoals)
        insertBehaviorGoals(behaviorGoals)
        insertEffects(effects)
    }

    @Query("UPDATE quest_behavior_goals SET currentCount = currentCount + 1 WHERE behaviorId = :behaviorId AND questId IN (SELECT id FROM quests WHERE status = 0)")
    suspend fun incrementBehaviorGoalCount(behaviorId: Long)

    @Transaction
    @Query("SELECT * FROM quests WHERE status = 0")
    suspend fun getActiveQuestsWithDetails(): List<QuestWithDetails>

    @Transaction
    @Query("SELECT * FROM quests WHERE isFocused = 1 LIMIT 1")
    fun getFocusedQuest(): Flow<QuestWithDetails?>
}