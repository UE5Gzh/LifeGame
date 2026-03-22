package com.example.lifegame.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.lifegame.data.entity.RankEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RankDao {
    @Query("SELECT * FROM ranks WHERE attributeId = :attributeId ORDER BY minValue ASC")
    fun getRanksByAttributeId(attributeId: Long): Flow<List<RankEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRank(rank: RankEntity)

    @Update
    suspend fun updateRank(rank: RankEntity)

    @Delete
    suspend fun deleteRank(rank: RankEntity)

    @Query("DELETE FROM ranks WHERE attributeId = :attributeId")
    suspend fun deleteRanksByAttributeId(attributeId: Long)
}
