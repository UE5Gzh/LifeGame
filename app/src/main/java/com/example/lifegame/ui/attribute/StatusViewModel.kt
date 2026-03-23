package com.example.lifegame.ui.attribute

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifegame.data.entity.AttributeEntity
import com.example.lifegame.data.entity.AttributeWithRanks
import com.example.lifegame.data.entity.StatusEntity
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

    val attributes: StateFlow<List<AttributeWithRanks>> = attributeRepository.allAttributesWithRanks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val enabledPeriodicStatuses: StateFlow<List<StatusEntity>> = statusRepository.enabledPeriodicStatuses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addStatus(
        name: String,
        description: String,
        colorHex: String,
        effectType: Int,
        targetAttributeId: Long,
        periodValue: Int,
        periodUnit: Int,
        changeValue: Float,
        bonusPercent: Float
    ) {
        viewModelScope.launch {
            val status = StatusEntity(
                name = name,
                description = description,
                colorHex = colorHex,
                isEnabled = false,
                effectType = effectType,
                targetAttributeId = targetAttributeId,
                periodValue = periodValue,
                periodUnit = periodUnit,
                changeValue = changeValue,
                bonusPercent = bonusPercent
            )
            statusRepository.insertStatus(status)
            
            logRepository.insertLog(
                type = "STATUS_CREATED",
                title = "创建状态: $name",
                details = if (effectType == 0) "周期性变动" else "属性加成"
            )
        }
    }

    fun updateStatus(status: StatusEntity) {
        viewModelScope.launch {
            statusRepository.updateStatus(status)
        }
    }

    fun toggleStatus(status: StatusEntity, enabled: Boolean) {
        viewModelScope.launch {
            val updatedStatus = if (enabled) {
                status.copy(
                    isEnabled = true,
                    startTime = System.currentTimeMillis(),
                    lastTriggerTime = 0L
                )
            } else {
                status.copy(isEnabled = false)
            }
            statusRepository.updateStatus(updatedStatus)
            
            logRepository.insertLog(
                type = "STATUS_TOGGLED",
                title = if (enabled) "开启状态: ${status.name}" else "关闭状态: ${status.name}",
                details = ""
            )
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

    fun processPeriodicStatuses() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val enabledStatuses = statusRepository.enabledPeriodicStatuses.stateIn(viewModelScope).value
            
            for (status in enabledStatuses) {
                val periodMillis = if (status.periodUnit == 0) {
                    status.periodValue * 60 * 60 * 1000L
                } else {
                    status.periodValue * 24 * 60 * 60 * 1000L
                }
                
                val lastTrigger = if (status.lastTriggerTime == 0L) status.startTime else status.lastTriggerTime
                var nextTrigger = lastTrigger + periodMillis
                
                while (nextTrigger <= now) {
                    val attr = attributeRepository.getAttributeById(status.targetAttributeId)
                    if (attr != null) {
                        val newValue = attr.currentValue + status.changeValue
                        attributeRepository.updateAttribute(attr.copy(currentValue = newValue))
                        
                        logRepository.insertLog(
                            type = "STATUS_TRIGGERED",
                            title = "状态触发: ${status.name}",
                            details = "${attr.name} ${if (status.changeValue >= 0) "+" else ""}${formatValue(status.changeValue)}"
                        )
                    }
                    
                    nextTrigger += periodMillis
                }
                
                if (nextTrigger > lastTrigger + periodMillis) {
                    statusRepository.updateStatus(status.copy(lastTriggerTime = nextTrigger - periodMillis))
                }
            }
        }
    }

    suspend fun calculateFinalChangeForAttribute(attributeId: Long, baseChange: Float): Float {
        if (baseChange <= 0f) return baseChange
        
        val bonusStatuses = statusRepository.getEnabledBonusStatusesForAttribute(attributeId)
            .stateIn(viewModelScope).value
        val decayStatuses = statusRepository.getEnabledDecayStatusesForAttribute(attributeId)
            .stateIn(viewModelScope).value
        
        var totalBonus = 0f
        for (status in bonusStatuses) {
            totalBonus += status.bonusPercent
        }
        
        var totalDecay = 0f
        for (status in decayStatuses) {
            totalDecay += status.bonusPercent
        }
        
        if (totalDecay > 100f) totalDecay = 100f
        
        val afterBonus = baseChange * (1 + totalBonus / 100f)
        val afterDecay = afterBonus * (1 - totalDecay / 100f)
        
        return afterDecay
    }

    private fun formatValue(value: Float): String {
        return if (value == value.roundToInt().toFloat()) {
            value.roundToInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }
}
