package com.example.lifegame.repository

import com.example.lifegame.data.dao.BehaviorDao
import com.example.lifegame.data.entity.BehaviorAttributeModifierEntity
import com.example.lifegame.data.entity.BehaviorEntity
import com.example.lifegame.data.entity.BehaviorWithModifiers
import com.example.lifegame.data.dao.BehaviorGroupDao
import com.example.lifegame.data.entity.BehaviorGroupEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BehaviorRepository @Inject constructor(
    private val behaviorDao: BehaviorDao,
    private val behaviorGroupDao: BehaviorGroupDao
) {
    val allBehaviorsWithModifiers: Flow<List<BehaviorWithModifiers>> = behaviorDao.getAllBehaviorsWithModifiers()
    val allBehaviorGroups: Flow<List<BehaviorGroupEntity>> = behaviorGroupDao.getAllGroups()

    suspend fun insertBehaviorWithModifiers(behavior: BehaviorEntity, modifiers: List<BehaviorAttributeModifierEntity>) {
        val behaviorId = behaviorDao.insertBehavior(behavior)
        val modifiersWithBehaviorId = modifiers.map { it.copy(behaviorId = behaviorId) }
        behaviorDao.insertBehaviorAttributeModifiers(modifiersWithBehaviorId)
    }

    suspend fun updateBehaviorWithModifiers(behavior: BehaviorEntity, modifiers: List<BehaviorAttributeModifierEntity>) {
        behaviorDao.updateBehaviorWithModifiers(behavior, modifiers)
    }

    suspend fun updateBehaviors(behaviors: List<BehaviorEntity>) {
        behaviorDao.updateBehaviors(behaviors)
    }

    suspend fun deleteBehavior(behavior: BehaviorEntity) {
        behaviorDao.deleteBehavior(behavior)
    }

    suspend fun isAttributeReferenced(attributeId: Long): Boolean {
        return behaviorDao.countAttributeReferences(attributeId) > 0
    }

    suspend fun insertGroup(group: BehaviorGroupEntity): Long {
        return behaviorGroupDao.insertGroup(group)
    }

    suspend fun updateGroup(group: BehaviorGroupEntity) {
        behaviorGroupDao.updateGroup(group)
    }

    suspend fun updateGroups(groups: List<BehaviorGroupEntity>) {
        behaviorGroupDao.updateGroups(groups)
    }

    suspend fun deleteGroup(group: BehaviorGroupEntity) {
        behaviorGroupDao.deleteGroup(group)
    }
}