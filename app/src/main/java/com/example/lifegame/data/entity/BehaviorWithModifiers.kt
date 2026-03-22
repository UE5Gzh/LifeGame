package com.example.lifegame.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class BehaviorWithModifiers(
    @Embedded val behavior: BehaviorEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "behaviorId"
    )
    val modifiers: List<BehaviorAttributeModifierEntity>
)