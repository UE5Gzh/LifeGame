package com.example.lifegame.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quest_effects",
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["questId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AttributeEntity::class,
            parentColumns = ["id"],
            childColumns = ["attributeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["questId"]),
        Index(value = ["attributeId"])
    ]
)
data class QuestEffectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val questId: Long,
    val isPunishment: Boolean,
    val type: Int, // 0: Attribute, 1: Text
    val attributeId: Long? = null,
    val valueChange: Float? = null,
    val text: String? = null
)
