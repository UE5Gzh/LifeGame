package com.example.lifegame.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "behavior_groups")
data class BehaviorGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String = "",
    val sortOrder: Int = 0
)