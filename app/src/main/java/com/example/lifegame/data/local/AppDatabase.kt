package com.example.lifegame.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.lifegame.data.dao.AttributeDao
import com.example.lifegame.data.entity.AttributeEntity

@Database(entities = [AttributeEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attributeDao(): AttributeDao
}
