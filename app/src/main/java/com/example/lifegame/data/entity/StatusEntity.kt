package com.example.lifegame.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "statuses")
data class StatusEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val colorHex: String = "",
    val isEnabled: Boolean = false,
    val startTime: Long = 0L,
    val sortOrder: Int = 0,
    val durationValue: Int = 0,
    val durationUnit: Int = 0
)
