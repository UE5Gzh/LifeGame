package com.example.lifegame.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: Int, // 0: Daily, 1: Main, 2: Side
    val deadline: Long? = null,
    val status: Int = 0, // 0: In Progress, 1: Completed Unclaimed, 2: Claimed, 3: Failed
    val createdAt: Long = System.currentTimeMillis(),
    val lastResetTime: Long = System.currentTimeMillis()
)