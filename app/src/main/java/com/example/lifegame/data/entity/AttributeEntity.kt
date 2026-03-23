package com.example.lifegame.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attributes")
data class AttributeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val currentValue: Float,
    val initialValue: Float,
    val colorHex: String,
    val sortOrder: Int = 0
)
