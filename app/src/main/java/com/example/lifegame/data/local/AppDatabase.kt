package com.example.lifegame.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.lifegame.data.dao.AttributeDao
import com.example.lifegame.data.dao.BehaviorDao
import com.example.lifegame.data.dao.RankDao
import com.example.lifegame.data.entity.AttributeEntity
import com.example.lifegame.data.entity.BehaviorAttributeModifierEntity
import com.example.lifegame.data.entity.BehaviorEntity
import com.example.lifegame.data.entity.RankEntity

import com.example.lifegame.data.dao.BehaviorGroupDao
import com.example.lifegame.data.entity.BehaviorGroupEntity

@Database(
    entities = [
        AttributeEntity::class, 
        RankEntity::class,
        BehaviorEntity::class,
        BehaviorAttributeModifierEntity::class,
        BehaviorGroupEntity::class
    ], 
    version = 5, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attributeDao(): AttributeDao
    abstract fun rankDao(): RankDao
    abstract fun behaviorDao(): BehaviorDao
    abstract fun behaviorGroupDao(): BehaviorGroupDao
}
