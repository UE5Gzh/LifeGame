package com.example.lifegame.data.dao

import androidx.room.*
import com.example.lifegame.data.entity.StatusEntity
import com.example.lifegame.data.entity.StatusEffectEntity
import com.example.lifegame.data.entity.StatusWithEffects
import kotlinx.coroutines.flow.Flow

@Dao
interface StatusDao {
    @Query("SELECT * FROM statuses ORDER BY sortOrder ASC")
    fun getAllStatuses(): Flow<List<StatusEntity>>

    @Query("SELECT * FROM statuses WHERE id = :id")
    suspend fun getStatusById(id: Long): StatusEntity?

    @Query("SELECT * FROM statuses WHERE isEnabled = 1")
    fun getEnabledStatuses(): Flow<List<StatusEntity>>

    @Transaction
    @Query("SELECT * FROM statuses ORDER BY sortOrder ASC")
    fun getAllStatusesWithEffects(): Flow<List<StatusWithEffects>>

    @Transaction
    @Query("SELECT * FROM statuses WHERE id = :id")
    suspend fun getStatusWithEffectsById(id: Long): StatusWithEffects?

    @Transaction
    @Query("SELECT * FROM statuses WHERE isEnabled = 1")
    fun getEnabledStatusesWithEffects(): Flow<List<StatusWithEffects>>

    @Query("SELECT * FROM status_effects WHERE statusId = :statusId ORDER BY sortOrder ASC")
    fun getEffectsForStatus(statusId: Long): Flow<List<StatusEffectEntity>>

    @Query("SELECT * FROM status_effects WHERE statusId = :statusId ORDER BY sortOrder ASC")
    suspend fun getEffectsForStatusSync(statusId: Long): List<StatusEffectEntity>

    @Query("""
        SELECT se.* FROM status_effects se
        INNER JOIN statuses s ON se.statusId = s.id
        WHERE s.isEnabled = 1 AND se.effectType = 0
        ORDER BY se.sortOrder ASC
    """)
    fun getEnabledPeriodicEffects(): Flow<List<StatusEffectEntity>>

    @Query("""
        SELECT se.* FROM status_effects se
        INNER JOIN statuses s ON se.statusId = s.id
        WHERE s.isEnabled = 1 AND se.effectType = 1 AND se.targetAttributeId = :attributeId
        ORDER BY se.sortOrder ASC
    """)
    fun getEnabledBonusEffectsForAttribute(attributeId: Long): Flow<List<StatusEffectEntity>>

    @Query("""
        SELECT se.* FROM status_effects se
        INNER JOIN statuses s ON se.statusId = s.id
        WHERE s.isEnabled = 1 AND se.effectType = 2 AND se.targetAttributeId = :attributeId
        ORDER BY se.sortOrder ASC
    """)
    fun getEnabledDecayEffectsForAttribute(attributeId: Long): Flow<List<StatusEffectEntity>>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEffect(effect: StatusEffectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEffects(effects: List<StatusEffectEntity>)

    @Update
    suspend fun updateEffect(effect: StatusEffectEntity)

    @Delete
    suspend fun deleteEffect(effect: StatusEffectEntity)

    @Query("DELETE FROM status_effects WHERE id = :id")
    suspend fun deleteEffectById(id: Long)

    @Query("DELETE FROM status_effects WHERE statusId = :statusId")
    suspend fun deleteEffectsForStatus(statusId: Long)

    @Transaction
    suspend fun updateEffects(effects: List<StatusEffectEntity>) {
        effects.forEach { updateEffect(it) }
    }

    @Transaction
    suspend fun insertStatusWithEffects(status: StatusEntity, effects: List<StatusEffectEntity>): Long {
        val statusId = insertStatus(status)
        val effectsWithStatusId = effects.map { it.copy(statusId = statusId) }
        insertEffects(effectsWithStatusId)
        return statusId
    }

    @Transaction
    suspend fun updateStatusWithEffects(status: StatusEntity, effects: List<StatusEffectEntity>) {
        updateStatus(status)
        deleteEffectsForStatus(status.id)
        val effectsWithStatusId = effects.map { it.copy(statusId = status.id) }
        insertEffects(effectsWithStatusId)
    }

    @Transaction
    suspend fun deleteStatusWithEffects(status: StatusEntity) {
        deleteEffectsForStatus(status.id)
        deleteStatus(status)
    }
}
