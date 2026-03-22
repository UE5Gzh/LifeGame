package com.example.lifegame.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.lifegame.data.entity.BehaviorGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BehaviorGroupDao {
    @Query("SELECT * FROM behavior_groups ORDER BY sortOrder ASC")
    fun getAllGroups(): Flow<List<BehaviorGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: BehaviorGroupEntity): Long

    @Update
    suspend fun updateGroup(group: BehaviorGroupEntity)

    @Update
    suspend fun updateGroups(groups: List<BehaviorGroupEntity>)

    @Delete
    suspend fun deleteGroup(group: BehaviorGroupEntity)
}