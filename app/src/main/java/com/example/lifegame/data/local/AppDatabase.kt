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

@Database(
    entities = [
        AttributeEntity::class, 
        RankEntity::class,
        BehaviorEntity::class,
        BehaviorAttributeModifierEntity::class,
        BehaviorGroupEntity::class,
        QuestEntity::class,
        QuestAttributeGoalEntity::class,
        QuestBehaviorGoalEntity::class,
        QuestEffectEntity::class
    ], 
    version = 6, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attributeDao(): AttributeDao
    abstract fun rankDao(): RankDao
    abstract fun behaviorDao(): BehaviorDao
    abstract fun behaviorGroupDao(): BehaviorGroupDao
    abstract fun questDao(): QuestDao
}
