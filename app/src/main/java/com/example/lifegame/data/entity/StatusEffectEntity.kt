package com.example.lifegame.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "status_effects",
    foreignKeys = [
        ForeignKey(
            entity = StatusEntity::class,
            parentColumns = ["id"],
            childColumns = ["statusId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("statusId")]
)
data class StatusEffectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val statusId: Long,
    val effectType: Int = 0,
    val targetAttributeId: Long,
    val periodValue: Int = 0,
    val periodUnit: Int = 0,
    val changeValue: Float = 0f,
    val bonusPercent: Float = 0f,
    val lastTriggerTime: Long = 0L,
    val sortOrder: Int = 0
)
