package com.example.lifegame.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class QuestWithDetails(
    @Embedded val quest: QuestEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "questId"
    )
    val attributeGoals: List<QuestAttributeGoalEntity>,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "questId"
    )
    val behaviorGoals: List<QuestBehaviorGoalEntity>,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "questId"
    )
    val effects: List<QuestEffectEntity>
)