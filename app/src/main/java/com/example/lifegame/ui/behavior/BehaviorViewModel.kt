package com.example.lifegame.ui.behavior

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifegame.data.entity.AttributeEntity
import com.example.lifegame.data.entity.AttributeWithRanks
import com.example.lifegame.data.entity.BehaviorAttributeModifierEntity
import com.example.lifegame.data.entity.BehaviorEntity
import com.example.lifegame.data.entity.BehaviorGroupEntity
import com.example.lifegame.data.entity.BehaviorWithModifiers
import com.example.lifegame.repository.AttributeRepository
import com.example.lifegame.repository.BehaviorRepository
import com.example.lifegame.repository.QuestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BehaviorViewModel @Inject constructor(
    private val behaviorRepository: BehaviorRepository,
    private val attributeRepository: AttributeRepository,
    private val questRepository: QuestRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("energy_prefs", Context.MODE_PRIVATE)

    private val _currentEnergy = MutableStateFlow(sharedPreferences.getInt("current_energy", 100))
    val currentEnergy: StateFlow<Int> = _currentEnergy.asStateFlow()

    private val _maxEnergy = MutableStateFlow(sharedPreferences.getInt("max_energy", 100))
    val maxEnergy: StateFlow<Int> = _maxEnergy.asStateFlow()

    val behaviors: StateFlow<List<BehaviorWithModifiers>> = behaviorRepository.allBehaviorsWithModifiers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val behaviorGroups: StateFlow<List<BehaviorGroupEntity>> = behaviorRepository.allBehaviorGroups
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val attributes: StateFlow<List<AttributeWithRanks>> = attributeRepository.allAttributesWithRanks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        checkAndInitDefaultAttributes()
    }

    private fun checkAndInitDefaultAttributes() {
        viewModelScope.launch {
            val currentAttributes = attributeRepository.allAttributesWithRanks.first()
            if (currentAttributes.isEmpty()) {
                attributeRepository.insertAttribute(AttributeEntity(name = "智商", currentValue = 100, initialValue = 100, colorHex = "#2196F3"))
                attributeRepository.insertAttribute(AttributeEntity(name = "情商", currentValue = 100, initialValue = 100, colorHex = "#E91E63"))
                attributeRepository.insertAttribute(AttributeEntity(name = "体魄", currentValue = 100, initialValue = 100, colorHex = "#4CAF50"))
            }
        }
    }

    fun addBehavior(
        name: String,
        energyType: Int,
        energyValue: Int,
        focusDuration: Int,
        groupId: Long?,
        modifiers: List<Pair<Long, Int>>
    ) {
        viewModelScope.launch {
            val behavior = BehaviorEntity(
                name = name,
                energyType = energyType,
                energyValue = energyValue,
                focusDuration = focusDuration,
                groupId = groupId
            )
            val modifierEntities = modifiers.map { (attributeId, valueChange) ->
                BehaviorAttributeModifierEntity(
                    behaviorId = 0, // Will be set in repository
                    attributeId = attributeId,
                    valueChange = valueChange
                )
            }
            behaviorRepository.insertBehaviorWithModifiers(behavior, modifierEntities)
        }
    }

    fun updateBehavior(
        behaviorId: Long,
        name: String,
        energyType: Int,
        energyValue: Int,
        focusDuration: Int,
        groupId: Long?,
        modifiers: List<Pair<Long, Int>>
    ) {
        viewModelScope.launch {
            val behavior = BehaviorEntity(
                id = behaviorId,
                name = name,
                energyType = energyType,
                energyValue = energyValue,
                focusDuration = focusDuration,
                groupId = groupId
            )
            val modifierEntities = modifiers.map { (attributeId, valueChange) ->
                BehaviorAttributeModifierEntity(
                    behaviorId = behaviorId,
                    attributeId = attributeId,
                    valueChange = valueChange
                )
            }
            behaviorRepository.updateBehaviorWithModifiers(behavior, modifierEntities)
        }
    }

    fun deleteBehavior(behavior: BehaviorEntity) {
        viewModelScope.launch {
            behaviorRepository.deleteBehavior(behavior)
        }
    }

    fun updateBehaviorSortOrders(behaviors: List<BehaviorEntity>) {
        viewModelScope.launch {
            behaviorRepository.updateBehaviors(behaviors)
        }
    }

    fun executeBehavior(behaviorWithModifiers: BehaviorWithModifiers) {
        viewModelScope.launch {
            // Update Energy
            val behavior = behaviorWithModifiers.behavior
            val change = if (behavior.energyType == 0) -behavior.energyValue else behavior.energyValue
            var newEnergy = _currentEnergy.value + change
            if (newEnergy > _maxEnergy.value) newEnergy = _maxEnergy.value
            if (newEnergy < 0) newEnergy = 0
            
            _currentEnergy.value = newEnergy
            sharedPreferences.edit().putInt("current_energy", newEnergy).apply()

            // Apply attribute modifiers
            val currentAttributes = attributeRepository.allAttributesWithRanks.first()
            for (modifier in behaviorWithModifiers.modifiers) {
                val attributeToUpdate = currentAttributes.find { it.attribute.id == modifier.attributeId }?.attribute
                if (attributeToUpdate != null) {
                    val newValue = attributeToUpdate.currentValue + modifier.valueChange
                    attributeRepository.updateAttribute(attributeToUpdate.copy(currentValue = newValue))
                }
            }

            // Update quest behavior goals
            questRepository.incrementBehaviorGoalCount(behavior.id)
        }
    }

    fun resetEnergy() {
        _currentEnergy.value = _maxEnergy.value
        sharedPreferences.edit().putInt("current_energy", _currentEnergy.value).apply()
    }

    fun setMaxEnergy(newMax: Int) {
        _maxEnergy.value = newMax
        sharedPreferences.edit().putInt("max_energy", newMax).apply()
        
        // Adjust current energy if it exceeds the new max
        if (_currentEnergy.value > newMax) {
            _currentEnergy.value = newMax
            sharedPreferences.edit().putInt("current_energy", newMax).apply()
        }
    }

    // --- Group Management ---
    fun addGroup(name: String, colorHex: String = "") {
        viewModelScope.launch {
            behaviorRepository.insertGroup(BehaviorGroupEntity(name = name, colorHex = colorHex))
        }
    }

    fun updateGroup(group: BehaviorGroupEntity) {
        viewModelScope.launch {
            behaviorRepository.updateGroup(group)
        }
    }

    fun updateGroupSortOrders(groups: List<BehaviorGroupEntity>) {
        viewModelScope.launch {
            behaviorRepository.updateGroups(groups)
        }
    }

    fun deleteGroup(group: BehaviorGroupEntity) {
        viewModelScope.launch {
            behaviorRepository.deleteGroup(group)
        }
    }
}