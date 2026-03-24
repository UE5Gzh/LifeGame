package com.example.lifegame.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class StatusWithEffects(
    @Embedded val status: StatusEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "statusId"
    )
    val effects: List<StatusEffectEntity>
)
