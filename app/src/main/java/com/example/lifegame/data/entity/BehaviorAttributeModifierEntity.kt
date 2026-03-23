package com.example.lifegame.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "behavior_attribute_modifiers",
    foreignKeys = [
        ForeignKey(
            entity = BehaviorEntity::class,
            parentColumns = ["id"],
            childColumns = ["behaviorId"],
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
        Index(value = ["behaviorId"]),
        Index(value = ["attributeId"])
    ]
)
data class BehaviorAttributeModifierEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val behaviorId: Long,
    val attributeId: Long,
    val valueChange: Float
)
