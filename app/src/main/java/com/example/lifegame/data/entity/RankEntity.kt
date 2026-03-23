package com.example.lifegame.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "ranks",
    foreignKeys = [
        ForeignKey(
            entity = AttributeEntity::class,
            parentColumns = ["id"],
            childColumns = ["attributeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["attributeId"])]
)
data class RankEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val attributeId: Long,
    val name: String,
    val minValue: Float,
    val maxValue: Float
)
