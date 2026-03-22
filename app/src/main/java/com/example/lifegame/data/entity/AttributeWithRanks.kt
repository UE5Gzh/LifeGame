package com.example.lifegame.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class AttributeWithRanks(
    @Embedded val attribute: AttributeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "attributeId"
    )
    val ranks: List<RankEntity>
)
