package com.example.lifegame.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val title: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)