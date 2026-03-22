package com.example.lifegame.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.lifegame.data.entity.BehaviorAttributeModifierEntity
import com.example.lifegame.data.entity.BehaviorEntity
import com.example.lifegame.data.entity.BehaviorWithModifiers
import kotlinx.coroutines.flow.Flow

@Dao
interface BehaviorDao {
    @Transaction
    @Query("SELECT * FROM behaviors ORDER BY sortOrder ASC")
    fun getAllBehaviorsWithModifiers(): Flow<List<BehaviorWithModifiers>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBehavior(behavior: BehaviorEntity): Long

    @Update
    suspend fun updateBehavior(behavior: BehaviorEntity)

    @Update
    suspend fun updateBehaviors(behaviors: List<BehaviorEntity>)

    @Delete
    suspend fun deleteBehavior(behavior: BehaviorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBehaviorAttributeModifiers(modifiers: List<BehaviorAttributeModifierEntity>)

    @Query("DELETE FROM behavior_attribute_modifiers WHERE behaviorId = :behaviorId")
    suspend fun deleteModifiersByBehaviorId(behaviorId: Long)

    @Transaction
    suspend fun updateBehaviorWithModifiers(behavior: BehaviorEntity, modifiers: List<BehaviorAttributeModifierEntity>) {
        updateBehavior(behavior)
        deleteModifiersByBehaviorId(behavior.id)
        insertBehaviorAttributeModifiers(modifiers)
    }

    @Query("SELECT COUNT(*) FROM behavior_attribute_modifiers WHERE attributeId = :attributeId")
    suspend fun countAttributeReferences(attributeId: Long): Int
}