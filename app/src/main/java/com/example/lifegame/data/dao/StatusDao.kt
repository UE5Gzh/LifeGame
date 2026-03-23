package com.example.lifegame.data.dao

import androidx.room.*
import com.example.lifegame.data.entity.StatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatusDao {
    @Query("SELECT * FROM statuses ORDER BY sortOrder ASC")
    fun getAllStatuses(): Flow<List<StatusEntity>>

    @Query("SELECT * FROM statuses WHERE id = :id")
    suspend fun getStatusById(id: Long): StatusEntity?

    @Query("SELECT * FROM statuses WHERE isEnabled = 1")
    fun getEnabledStatuses(): Flow<List<StatusEntity>>

    @Query("SELECT * FROM statuses WHERE isEnabled = 1 AND effectType = 0")
    fun getEnabledPeriodicStatuses(): Flow<List<StatusEntity>>

    @Query("SELECT * FROM statuses WHERE isEnabled = 1 AND effectType = 1 AND targetAttributeId = :attributeId")
    fun getEnabledBonusStatusesForAttribute(attributeId: Long): Flow<List<StatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: StatusEntity): Long

    @Update
    suspend fun updateStatus(status: StatusEntity)

    @Delete
    suspend fun deleteStatus(status: StatusEntity)

    @Query("DELETE FROM statuses WHERE id = :id")
    suspend fun deleteStatusById(id: Long)

    @Transaction
    suspend fun updateStatuses(statuses: List<StatusEntity>) {
        statuses.forEach { updateStatus(it) }
    }
}
