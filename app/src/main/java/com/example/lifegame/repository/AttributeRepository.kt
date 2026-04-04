package com.example.lifegame.repository

import com.example.lifegame.data.dao.AttributeDao
import com.example.lifegame.data.dao.RankDao
import com.example.lifegame.data.entity.AttributeEntity
import com.example.lifegame.data.entity.AttributeWithRanks
import com.example.lifegame.data.entity.RankEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AttributeRepository @Inject constructor(
    private val attributeDao: AttributeDao,
    private val rankDao: RankDao
) {
    val allAttributesWithRanks: Flow<List<AttributeWithRanks>> = attributeDao.getAllAttributesWithRanks()

    suspend fun getAttributeById(id: Long): AttributeEntity? {
        return attributeDao.getAttributeById(id)
    }

    suspend fun insertAttribute(attribute: AttributeEntity) {
        attributeDao.insertAttribute(attribute)
    }

    suspend fun insertAttributeAndGetId(attribute: AttributeEntity): Long {
        return attributeDao.insertAttribute(attribute)
    }

    suspend fun updateAttribute(attribute: AttributeEntity) {
        attributeDao.updateAttribute(attribute)
    }

    suspend fun updateAttributes(attributes: List<AttributeEntity>) {
        attributeDao.updateAttributes(attributes)
    }

    suspend fun deleteAttribute(attribute: AttributeEntity) {
        attributeDao.deleteAttribute(attribute)
    }


    fun getRanksForAttribute(attributeId: Long): Flow<List<RankEntity>> {
        return rankDao.getRanksByAttributeId(attributeId)
    }

    suspend fun insertRank(rank: RankEntity) {
        rankDao.insertRank(rank)
    }

    suspend fun deleteRank(rank: RankEntity) {
        rankDao.deleteRank(rank)
    }

    suspend fun deleteRanksByAttributeId(attributeId: Long) {
        rankDao.deleteRanksByAttributeId(attributeId)
    }
}
