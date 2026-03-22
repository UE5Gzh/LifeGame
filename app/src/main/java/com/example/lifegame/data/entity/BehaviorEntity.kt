package com.example.lifegame.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "behaviors")
data class BehaviorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val energyType: Int, // 0: consume, 1: restore
    val energyValue: Int,
    val focusDuration: Int // in minutes, 0 means instant
)