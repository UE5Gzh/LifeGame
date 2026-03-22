package com.example.lifegame.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.lifegame.data.entity.AttributeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttributeDao {
    @Query("SELECT * FROM attributes")
    fun getAllAttributes(): Flow<List<AttributeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttribute(attribute: AttributeEntity)

    @Update
    suspend fun updateAttribute(attribute: AttributeEntity)

    @Delete
    suspend fun deleteAttribute(attribute: AttributeEntity)

    @Query("UPDATE attributes SET currentValue = initialValue")
    suspend fun resetAllAttributes()
}
