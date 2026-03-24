package com.example.lifegame.ui.attribute

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifegame.data.entity.AttributeEntity
import com.example.lifegame.data.entity.AttributeWithRanks
import com.example.lifegame.data.entity.StatusEntity
import com.example.lifegame.data.entity.StatusEffectEntity
import com.example.lifegame.data.entity.StatusWithEffects
import com.example.lifegame.repository.AttributeRepository
import com.example.lifegame.repository.StatusRepository
import com.example.lifegame.repository.LogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val statusRepository: StatusRepository,
    private val attributeRepository: AttributeRepository,
    private val logRepository: LogRepository
) : ViewModel() {

    val statuses: StateFlow<List<StatusEntity>> = statusRepository.allStatuses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val statusesWithEffects: StateFlow<List<StatusWithEffects>> = statusRepository.allStatusesWithEffects
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

    val enabledPeriodicEffects: StateFlow<List<StatusEffectEntity>> = statusRepository.enabledPeriodicEffects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addStatus(
        name: String,
        description: String,
        colorHex: String,
        effects: List<StatusEffectEntity>,
        durationValue: Int = 0,
        durationUnit: Int = 0
    ) {
        viewModelScope.launch {
            val status = StatusEntity(
                name = name,
                description = description,
                colorHex = colorHex,
                isEnabled = false,
                durationValue = durationValue,
                durationUnit = durationUnit
            )
            statusRepository.insertStatusWithEffects(status, effects)
            
            val effectTypes = effects.map { 
                when (it.effectType) {
                    0 -> "周期性变动"
                    1 -> "属性获取加成"
                    else -> "属性获取减少"
                }
            }.distinct().joinToString(", ")
            
            logRepository.insertLog(
                type = "STATUS_CREATED",
                title = "创建状态: $name",
                details = effectTypes
            )
        }
    }

    fun updateStatus(status: StatusEntity, effects: List<StatusEffectEntity>) {
        viewModelScope.launch {
            statusRepository.updateStatusWithEffects(status, effects)
        }
    }

    fun toggleStatus(status: StatusEntity, enabled: Boolean) {
        viewModelScope.launch {
            val isExpired = isStatusExpired(status)
            
            val updatedStatus = if (enabled && !isExpired) {
                status.copy(
                    isEnabled = true,
                    startTime = System.currentTimeMillis()
                )
            } else {
                status.copy(isEnabled = false)
            }
            statusRepository.updateStatus(updatedStatus)
            
            if (isExpired && status.isEnabled) {
                logRepository.insertLog(
                    type = "STATUS_EXPIRED",
                    title = "状态到期: ${status.name}",
                    details = "持续时间已结束，自动关闭"
                )
            } else {
                logRepository.insertLog(
                    type = "STATUS_TOGGLED",
                    title = if (enabled && !isExpired) "进入状态: ${status.name}" else "退出状态: ${status.name}",
                    details = ""
                )
            }
        }
    }

    fun deleteStatus(status: StatusEntity) {
        viewModelScope.launch {
            statusRepository.deleteStatus(status)
            
            logRepository.insertLog(
                type = "STATUS_DELETED",
                title = "删除状态: ${status.name}",
                details = ""
            )
        }
    }

    fun updateStatusSortOrders(statuses: List<StatusEntity>) {
        viewModelScope.launch {
            statusRepository.updateStatuses(statuses)
        }
    }

    fun processPeriodicEffects() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val enabledStatusesWithEffects = statusRepository.enabledStatusesWithEffects.stateIn(viewModelScope).value
            
            for (statusWithEffects in enabledStatusesWithEffects) {
                val status = statusWithEffects.status
                
                if (isStatusExpired(status)) {
                    autoDisableStatus(status)
                    continue
                }
                
                for (effect in statusWithEffects.effects) {
                    if (effect.effectType != 0) continue
                    
                    val periodMillis = if (effect.periodUnit == 0) {
                        effect.periodValue * 60 * 60 * 1000L
                    } else {
                        effect.periodValue * 24 * 60 * 60 * 1000L
                    }
                    
                    val lastTrigger = if (effect.lastTriggerTime == 0L) status.startTime else effect.lastTriggerTime
                    var nextTrigger = lastTrigger + periodMillis
                    
                    while (nextTrigger <= now) {
                        val attr = attributeRepository.getAttributeById(effect.targetAttributeId)
                        if (attr != null) {
                            val newValue = attr.currentValue + effect.changeValue
                            attributeRepository.updateAttribute(attr.copy(currentValue = newValue))
                            
                            logRepository.insertLog(
                                type = "STATUS_TRIGGERED",
                                title = "状态触发: ${status.name}",
                                details = "${attr.name} ${if (effect.changeValue >= 0) "+" else ""}${formatValue(effect.changeValue)}"
                            )
                        }
                        
                        nextTrigger += periodMillis
                    }
                    
                    if (nextTrigger > lastTrigger + periodMillis) {
                        statusRepository.updateEffect(effect.copy(lastTriggerTime = nextTrigger - periodMillis))
                    }
                }
            }
        }
    }

    fun checkAndDisableExpiredStatuses() {
        viewModelScope.launch {
            val enabledStatuses = statusRepository.enabledStatuses.stateIn(viewModelScope).value
            
            for (status in enabledStatuses) {
                if (isStatusExpired(status)) {
                    autoDisableStatus(status)
                }
            }
        }
    }

    private fun isStatusExpired(status: StatusEntity): Boolean {
        if (status.durationValue <= 0 || !status.isEnabled) return false
        
        val durationMillis = when (status.durationUnit) {
            0 -> status.durationValue * 60 * 1000L
            1 -> status.durationValue * 60 * 60 * 1000L
            else -> status.durationValue * 24 * 60 * 60 * 1000L
        }
        
        val elapsed = System.currentTimeMillis() - status.startTime
        return elapsed >= durationMillis
    }

    private suspend fun autoDisableStatus(status: StatusEntity) {
        val updatedStatus = status.copy(isEnabled = false)
        statusRepository.updateStatus(updatedStatus)
        
        logRepository.insertLog(
            type = "STATUS_EXPIRED",
            title = "状态到期: ${status.name}",
            details = "持续时间已结束，自动关闭"
        )
    }

    suspend fun calculateFinalChangeForAttribute(attributeId: Long, baseChange: Float): Float {
        if (baseChange <= 0f) return baseChange
        
        val bonusEffects = statusRepository.getEnabledBonusEffectsForAttribute(attributeId)
            .stateIn(viewModelScope).value
        val decayEffects = statusRepository.getEnabledDecayEffectsForAttribute(attributeId)
            .stateIn(viewModelScope).value
        
        var totalBonus = 0f
        for (effect in bonusEffects) {
            val status = statusRepository.getStatusById(effect.statusId)
            if (status != null && !isStatusExpired(status)) {
                totalBonus += effect.bonusPercent
            }
        }
        
        var totalDecay = 0f
        for (effect in decayEffects) {
            val status = statusRepository.getStatusById(effect.statusId)
            if (status != null && !isStatusExpired(status)) {
                totalDecay += effect.bonusPercent
            }
        }
        
        if (totalDecay > 100f) totalDecay = 100f
        
        val afterBonus = baseChange * (1 + totalBonus / 100f)
        val afterDecay = afterBonus * (1 - totalDecay / 100f)
        
        return afterDecay
    }

    suspend fun getStatusWithEffects(statusId: Long): StatusWithEffects? {
        return statusRepository.getStatusWithEffectsById(statusId)
    }

    suspend fun getEffectsForStatus(statusId: Long): List<StatusEffectEntity> {
        return statusRepository.getEffectsForStatusSync(statusId)
    }

    private fun formatValue(value: Float): String {
        return if (value == value.roundToInt().toFloat()) {
            value.roundToInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }
}
