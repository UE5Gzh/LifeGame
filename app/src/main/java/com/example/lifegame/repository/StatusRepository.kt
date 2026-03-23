package com.example.lifegame.repository

import com.example.lifegame.data.dao.StatusDao
import com.example.lifegame.data.entity.StatusEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatusRepository @Inject constructor(
    private val statusDao: StatusDao
) {
    val allStatuses: Flow<List<StatusEntity>> = statusDao.getAllStatuses()
    
    val enabledStatuses: Flow<List<StatusEntity>> = statusDao.getEnabledStatuses()
    
    val enabledPeriodicStatuses: Flow<List<StatusEntity>> = statusDao.getEnabledPeriodicStatuses()

    fun getEnabledBonusStatusesForAttribute(attributeId: Long): Flow<List<StatusEntity>> {
        return statusDao.getEnabledBonusStatusesForAttribute(attributeId)
    }

    fun getEnabledDecayStatusesForAttribute(attributeId: Long): Flow<List<StatusEntity>> {
        return statusDao.getEnabledDecayStatusesForAttribute(attributeId)
    }

    suspend fun getStatusById(id: Long): StatusEntity? {
        return statusDao.getStatusById(id)
    }

    suspend fun insertStatus(status: StatusEntity): Long {
        return statusDao.insertStatus(status)
    }

    suspend fun updateStatus(status: StatusEntity) {
        statusDao.updateStatus(status)
    }

    suspend fun deleteStatus(status: StatusEntity) {
        statusDao.deleteStatus(status)
    }

    suspend fun updateStatuses(statuses: List<StatusEntity>) {
        statusDao.updateStatuses(statuses)
    }
}
