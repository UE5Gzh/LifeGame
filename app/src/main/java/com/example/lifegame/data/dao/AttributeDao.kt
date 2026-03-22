package com.example.lifegame.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import com.example.lifegame.data.entity.AttributeEntity
import com.example.lifegame.data.entity.AttributeWithRanks
import kotlinx.coroutines.flow.Flow

@Dao
interface AttributeDao {
    @Transaction
    @Query("SELECT * FROM attributes ORDER BY sortOrder ASC")
    fun getAllAttributesWithRanks(): Flow<List<AttributeWithRanks>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttribute(attribute: AttributeEntity)

    @Update
    suspend fun updateAttribute(attribute: AttributeEntity)

    @Update
    suspend fun updateAttributes(attributes: List<AttributeEntity>)

    @Delete
    suspend fun deleteAttribute(attribute: AttributeEntity)

    @Transaction
    @Query("SELECT * FROM attributes ORDER BY sortOrder ASC")
    suspend fun getAllAttributesWithRanksSync(): List<AttributeWithRanks>
}
