package com.example.lifegame.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "behaviors",
    foreignKeys = [
        ForeignKey(
            entity = BehaviorGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["groupId"])]
)
data class BehaviorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val energyType: Int, // 0: consume, 1: restore
    val energyValue: Int,
    val focusDuration: Int, // in minutes, 0 means instant
    val sortOrder: Int = 0,
    val groupId: Long? = null
)