package com.example.lifegame.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.lifegame.data.dao.AttributeDao
import com.example.lifegame.data.dao.RankDao
import com.example.lifegame.data.entity.AttributeEntity
import com.example.lifegame.data.entity.RankEntity

@Database(entities = [AttributeEntity::class, RankEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attributeDao(): AttributeDao
    abstract fun rankDao(): RankDao
}
