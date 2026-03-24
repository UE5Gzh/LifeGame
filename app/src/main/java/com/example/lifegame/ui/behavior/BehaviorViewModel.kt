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
import com.example.lifegame.data.entity.StatusEntity
import com.example.lifegame.repository.AttributeRepository
import com.example.lifegame.repository.BehaviorRepository
import com.example.lifegame.repository.QuestRepository
import com.example.lifegame.repository.LogRepository
import com.example.lifegame.repository.StatusRepository
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
import kotlin.math.roundToInt

@HiltViewModel
class BehaviorViewModel @Inject constructor(
    private val behaviorRepository: BehaviorRepository,
    private val attributeRepository: AttributeRepository,
    private val questRepository: QuestRepository,
    private val logRepository: LogRepository,
    private val statusRepository: StatusRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("energy_prefs", Context.MODE_PRIVATE)

    private val _currentEnergy = MutableStateFlow(sharedPreferences.getInt("current_energy", 100))
    val currentEnergy: StateFlow<Int> = _currentEnergy.asStateFlow()

    private val _maxEnergy = MutableStateFlow(sharedPreferences.getInt("max_energy", 100))
    val maxEnergy: StateFlow<Int> = _maxEnergy.asStateFlow()

    private val _selectedGroupId = MutableStateFlow<Long?>(sharedPreferences.getLong("selected_group_id", Long.MIN_VALUE).let { if (it == Long.MIN_VALUE) null else it })
    val selectedGroupId: StateFlow<Long?> = _selectedGroupId.asStateFlow()

    fun saveSelectedGroupId(groupId: Long?) {
        _selectedGroupId.value = groupId
        if (groupId == null) {
            sharedPreferences.edit().remove("selected_group_id").apply()
        } else {
            sharedPreferences.edit().putLong("selected_group_id", groupId).apply()
        }
    }

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
                attributeRepository.insertAttribute(AttributeEntity(name = "智商", currentValue = 100f, initialValue = 100f, colorHex = "#2196F3"))
                attributeRepository.insertAttribute(AttributeEntity(name = "情商", currentValue = 100f, initialValue = 100f, colorHex = "#E91E63"))
                attributeRepository.insertAttribute(AttributeEntity(name = "体魄", currentValue = 100f, initialValue = 100f, colorHex = "#4CAF50"))
            }
        }
    }

    fun addBehavior(
        name: String,
        energyType: Int,
        energyValue: Int,
        focusDuration: Int,
        groupId: Long?,
        modifiers: List<Pair<Long, Float>>
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
                    behaviorId = 0,
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
        modifiers: List<Pair<Long, Float>>
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

    private fun formatValue(value: Float): String {
        return if (value == value.roundToInt().toFloat()) {
            value.roundToInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }

    private suspend fun calculateFinalChangeForAttribute(attributeId: Long, baseChange: Float): Float {
        if (baseChange <= 0f) return baseChange
        
        val bonusEffects = statusRepository.getEnabledBonusEffectsForAttribute(attributeId).first()
        val decayEffects = statusRepository.getEnabledDecayEffectsForAttribute(attributeId).first()
        
        val now = System.currentTimeMillis()
        
        var totalBonus = 0f
        for (effect in bonusEffects) {
            val status = statusRepository.getStatusById(effect.statusId)
            if (status != null && !isStatusExpired(status, now)) {
                totalBonus += effect.bonusPercent
            }
        }
        
        var totalDecay = 0f
        for (effect in decayEffects) {
            val status = statusRepository.getStatusById(effect.statusId)
            if (status != null && !isStatusExpired(status, now)) {
                totalDecay += effect.bonusPercent
            }
        }
        
        if (totalDecay > 100f) totalDecay = 100f
        
        val afterBonus = baseChange * (1 + totalBonus / 100f)
        val afterDecay = afterBonus * (1 - totalDecay / 100f)
        
        return afterDecay
    }

    private fun isStatusExpired(status: StatusEntity, now: Long): Boolean {
        if (status.durationValue <= 0 || !status.isEnabled) return false
        
        val durationMillis = when (status.durationUnit) {
            0 -> status.durationValue * 60 * 1000L
            1 -> status.durationValue * 60 * 60 * 1000L
            else -> status.durationValue * 24 * 60 * 60 * 1000L
        }
        
        val elapsed = now - status.startTime
        return elapsed >= durationMillis
    }

    fun executeBehavior(behaviorWithModifiers: BehaviorWithModifiers, isFocus: Boolean = false) {
        viewModelScope.launch {
            val behavior = behaviorWithModifiers.behavior
            val change = if (behavior.energyType == 0) -behavior.energyValue else behavior.energyValue
            var newEnergy = _currentEnergy.value + change
            if (newEnergy > _maxEnergy.value) newEnergy = _maxEnergy.value
            if (newEnergy < 0) newEnergy = 0
            
            _currentEnergy.value = newEnergy
            sharedPreferences.edit().putInt("current_energy", newEnergy).apply()

            val detailsBuilder = StringBuilder()
            val attributeChanges = mutableListOf<com.example.lifegame.util.AttributeChangeItem>()

            val currentAttributes = attributeRepository.allAttributesWithRanks.first()
            for (modifier in behaviorWithModifiers.modifiers) {
                val attrWithRanks = currentAttributes.find { it.attribute.id == modifier.attributeId }
                val attributeToUpdate = attrWithRanks?.attribute
                if (attributeToUpdate != null) {
                    var actualChange = modifier.valueChange
                    
                    if (modifier.valueChange > 0) {
                        actualChange = calculateFinalChangeForAttribute(modifier.attributeId, modifier.valueChange)
                    }
                    
                    val oldValue = attributeToUpdate.currentValue
                    val newValue = oldValue + actualChange
                    attributeRepository.updateAttribute(attributeToUpdate.copy(currentValue = newValue))
                    if (detailsBuilder.isNotEmpty()) detailsBuilder.append(", ")
                    detailsBuilder.append("${attributeToUpdate.name} ${if(actualChange >= 0) "+" else ""}${formatValue(actualChange)}")
                    
                    attributeChanges.add(com.example.lifegame.util.AttributeChangeItem(attributeToUpdate.name, actualChange, attributeToUpdate.colorHex))
                    
                    if (attrWithRanks.ranks.isNotEmpty()) {
                        checkRankUp(attributeToUpdate.name, oldValue, newValue, attrWithRanks.ranks)
                    }
                }
            }

            val completedQuests = questRepository.incrementBehaviorGoalCountAndCheckCompletion(behavior.id, currentAttributes)
            for (quest in completedQuests) {
                com.example.lifegame.util.CelebrationBus.postQuestComplete(
                    questName = quest.quest.name,
                    questType = quest.quest.type
                )
            }
            
            val title = if (isFocus) "专注完成: ${behavior.name}" else "执行行动: ${behavior.name}"
            val details = if (detailsBuilder.isNotEmpty()) "属性变动: ${detailsBuilder.toString()}" else "无属性变动"
            logRepository.insertLog(
                type = "BEHAVIOR_EXECUTION",
                title = title,
                details = details
            )
            
            com.example.lifegame.util.AttributeChangeBus.postChanges(attributeChanges)
        }
    }
    
    private fun checkRankUp(attributeName: String, oldValue: Float, newValue: Float, ranks: List<com.example.lifegame.data.entity.RankEntity>) {
        if (ranks.isEmpty()) return
        if (newValue <= oldValue) return
        
        val sortedRanks = ranks.sortedBy { it.minValue }
        
        fun findRankIndex(value: Float): Int {
            for (i in sortedRanks.indices) {
                val rank = sortedRanks[i]
                if (value >= rank.minValue && value < rank.maxValue) {
                    return i
                }
            }
            return if (value >= sortedRanks.last().maxValue) sortedRanks.lastIndex else -1
        }
        
        val oldRankIndex = findRankIndex(oldValue)
        val newRankIndex = findRankIndex(newValue)
        
        if (newRankIndex > oldRankIndex) {
            val oldRankName = if (oldRankIndex >= 0) sortedRanks[oldRankIndex].name else "无"
            val newRankName = sortedRanks[newRankIndex].name
            com.example.lifegame.util.CelebrationBus.postRankUp(
                attributeName = attributeName,
                oldRank = oldRankName,
                newRank = newRankName
            )
        }
    }

    fun resetEnergy() {
        _currentEnergy.value = _maxEnergy.value
        sharedPreferences.edit().putInt("current_energy", _currentEnergy.value).apply()
    }

    fun setMaxEnergy(newMax: Int) {
        _maxEnergy.value = newMax
        sharedPreferences.edit().putInt("max_energy", newMax).apply()
        
        if (_currentEnergy.value > newMax) {
            _currentEnergy.value = newMax
            sharedPreferences.edit().putInt("current_energy", newMax).apply()
        }
    }

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
