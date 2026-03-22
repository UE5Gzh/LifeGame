package com.example.lifegame.repository

import com.example.lifegame.data.dao.AttributeDao
import com.example.lifegame.data.entity.AttributeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AttributeRepository @Inject constructor(
    private val attributeDao: AttributeDao
) {
    val allAttributes: Flow<List<AttributeEntity>> = attributeDao.getAllAttributes()

    suspend fun insertAttribute(attribute: AttributeEntity) {
        attributeDao.insertAttribute(attribute)
    }

    suspend fun updateAttribute(attribute: AttributeEntity) {
        attributeDao.updateAttribute(attribute)
    }

    suspend fun deleteAttribute(attribute: AttributeEntity) {
        attributeDao.deleteAttribute(attribute)
    }

    suspend fun resetAllAttributes() {
        attributeDao.resetAllAttributes()
    }
}
