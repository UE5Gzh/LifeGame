package com.example.lifegame.repository

import com.example.lifegame.data.dao.BehaviorDao
import com.example.lifegame.data.entity.BehaviorAttributeModifierEntity
import com.example.lifegame.data.entity.BehaviorEntity
import com.example.lifegame.data.entity.BehaviorWithModifiers
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BehaviorRepository @Inject constructor(
    private val behaviorDao: BehaviorDao
) {
    val allBehaviorsWithModifiers: Flow<List<BehaviorWithModifiers>> = behaviorDao.getAllBehaviorsWithModifiers()

    suspend fun insertBehaviorWithModifiers(behavior: BehaviorEntity, modifiers: List<BehaviorAttributeModifierEntity>) {
        val behaviorId = behaviorDao.insertBehavior(behavior)
        val modifiersWithBehaviorId = modifiers.map { it.copy(behaviorId = behaviorId) }
        behaviorDao.insertBehaviorAttributeModifiers(modifiersWithBehaviorId)
    }

    suspend fun updateBehaviorWithModifiers(behavior: BehaviorEntity, modifiers: List<BehaviorAttributeModifierEntity>) {
        behaviorDao.updateBehaviorWithModifiers(behavior, modifiers)
    }

    suspend fun deleteBehavior(behavior: BehaviorEntity) {
        behaviorDao.deleteBehavior(behavior)
    }

    suspend fun isAttributeReferenced(attributeId: Long): Boolean {
        return behaviorDao.countAttributeReferences(attributeId) > 0
    }
}