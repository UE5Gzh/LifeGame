package com.example.lifegame.repository

import com.example.lifegame.data.dao.StatusDao
import com.example.lifegame.data.entity.StatusEntity
import com.example.lifegame.data.entity.StatusEffectEntity
import com.example.lifegame.data.entity.StatusWithEffects
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatusRepository @Inject constructor(
    private val statusDao: StatusDao
) {
    val allStatuses: Flow<List<StatusEntity>> = statusDao.getAllStatuses()
    
    val enabledStatuses: Flow<List<StatusEntity>> = statusDao.getEnabledStatuses()
    
    val allStatusesWithEffects: Flow<List<StatusWithEffects>> = statusDao.getAllStatusesWithEffects()
    
    val enabledStatusesWithEffects: Flow<List<StatusWithEffects>> = statusDao.getEnabledStatusesWithEffects()
    
    val enabledPeriodicEffects: Flow<List<StatusEffectEntity>> = statusDao.getEnabledPeriodicEffects()

    fun getEnabledBonusEffectsForAttribute(attributeId: Long): Flow<List<StatusEffectEntity>> {
        return statusDao.getEnabledBonusEffectsForAttribute(attributeId)
    }

    fun getEnabledDecayEffectsForAttribute(attributeId: Long): Flow<List<StatusEffectEntity>> {
        return statusDao.getEnabledDecayEffectsForAttribute(attributeId)
    }

    suspend fun getStatusById(id: Long): StatusEntity? {
        return statusDao.getStatusById(id)
    }

    suspend fun getStatusWithEffectsById(id: Long): StatusWithEffects? {
        return statusDao.getStatusWithEffectsById(id)
    }

    suspend fun getEffectsForStatusSync(statusId: Long): List<StatusEffectEntity> {
        return statusDao.getEffectsForStatusSync(statusId)
    }

    suspend fun insertStatus(status: StatusEntity): Long {
        return statusDao.insertStatus(status)
    }

    suspend fun insertStatusWithEffects(status: StatusEntity, effects: List<StatusEffectEntity>): Long {
        return statusDao.insertStatusWithEffects(status, effects)
    }

    suspend fun updateStatus(status: StatusEntity) {
        statusDao.updateStatus(status)
    }

    suspend fun updateStatusWithEffects(status: StatusEntity, effects: List<StatusEffectEntity>) {
        statusDao.updateStatusWithEffects(status, effects)
    }

    suspend fun deleteStatus(status: StatusEntity) {
        statusDao.deleteStatusWithEffects(status)
    }

    suspend fun updateStatuses(statuses: List<StatusEntity>) {
        statusDao.updateStatuses(statuses)
    }

    suspend fun insertEffect(effect: StatusEffectEntity): Long {
        return statusDao.insertEffect(effect)
    }

    suspend fun insertEffects(effects: List<StatusEffectEntity>) {
        statusDao.insertEffects(effects)
    }

    suspend fun updateEffect(effect: StatusEffectEntity) {
        statusDao.updateEffect(effect)
    }

    suspend fun deleteEffect(effect: StatusEffectEntity) {
        statusDao.deleteEffect(effect)
    }

    suspend fun deleteEffectsForStatus(statusId: Long) {
        statusDao.deleteEffectsForStatus(statusId)
    }
}
