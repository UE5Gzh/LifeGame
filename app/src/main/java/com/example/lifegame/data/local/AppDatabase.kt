package com.example.lifegame.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.lifegame.data.dao.AttributeDao
import com.example.lifegame.data.dao.BehaviorDao
import com.example.lifegame.data.dao.BehaviorGroupDao
import com.example.lifegame.data.dao.QuestDao
import com.example.lifegame.data.dao.RankDao
import com.example.lifegame.data.entity.AttributeEntity
import com.example.lifegame.data.entity.BehaviorAttributeModifierEntity
import com.example.lifegame.data.entity.BehaviorEntity
import com.example.lifegame.data.entity.BehaviorGroupEntity
import com.example.lifegame.data.entity.QuestAttributeGoalEntity
import com.example.lifegame.data.entity.QuestBehaviorGoalEntity
import com.example.lifegame.data.entity.QuestEffectEntity
import com.example.lifegame.data.entity.QuestEntity
import com.example.lifegame.data.entity.RankEntity

import com.example.lifegame.data.dao.LogDao
import com.example.lifegame.data.entity.LogEntity

@Database(
    entities = [
        AttributeEntity::class,
        RankEntity::class,
        BehaviorGroupEntity::class,
        BehaviorEntity::class,
        BehaviorAttributeModifierEntity::class,
        QuestEntity::class,
        QuestAttributeGoalEntity::class,
        QuestBehaviorGoalEntity::class,
        QuestEffectEntity::class,
        LogEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attributeDao(): AttributeDao
    abstract fun rankDao(): RankDao
    abstract fun behaviorGroupDao(): BehaviorGroupDao
    abstract fun behaviorDao(): BehaviorDao
    abstract fun questDao(): QuestDao
    abstract fun logDao(): LogDao
}
