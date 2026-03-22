package com.example.lifegame.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quest_behavior_goals",
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["questId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BehaviorEntity::class,
            parentColumns = ["id"],
            childColumns = ["behaviorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["questId"]),
        Index(value = ["behaviorId"])
    ]
)
data class QuestBehaviorGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val questId: Long,
    val behaviorId: Long,
    val targetCount: Int,
    val currentCount: Int = 0
)