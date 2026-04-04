package com.example.lifegame.ui.attribute

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifegame.data.entity.AttributeEntity
import com.example.lifegame.data.entity.AttributeWithRanks
import com.example.lifegame.data.entity.QuestWithDetails
import com.example.lifegame.data.entity.RankEntity
import com.example.lifegame.repository.AttributeRepository
import com.example.lifegame.repository.BehaviorRepository
import com.example.lifegame.repository.QuestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttributeViewModel @Inject constructor(
    private val repository: AttributeRepository,
    private val behaviorRepository: BehaviorRepository,
    private val questRepository: QuestRepository
) : ViewModel() {

    val attributesWithRanks: StateFlow<List<AttributeWithRanks>> = repository.allAttributesWithRanks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val attributes: StateFlow<List<AttributeWithRanks>> = attributesWithRanks

    val focusedQuest: StateFlow<QuestWithDetails?> = questRepository.getFocusedQuest()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun addAttribute(name: String, initialValue: Float, colorHex: String) {
        viewModelScope.launch {
            val newAttribute = AttributeEntity(
                name = name,
                currentValue = initialValue,
                initialValue = initialValue,
                colorHex = colorHex
            )
            val attributeId = repository.insertAttributeAndGetId(newAttribute)
            
            val defaultRanks = listOf(
                RankEntity(attributeId = attributeId, name = "D", minValue = 0f, maxValue = 100f),
                RankEntity(attributeId = attributeId, name = "C", minValue = 101f, maxValue = 300f),
                RankEntity(attributeId = attributeId, name = "B", minValue = 301f, maxValue = 600f),
                RankEntity(attributeId = attributeId, name = "A", minValue = 601f, maxValue = 1000f),
                RankEntity(attributeId = attributeId, name = "S", minValue = 1001f, maxValue = 1500f),
                RankEntity(attributeId = attributeId, name = "SS", minValue = 1501f, maxValue = 2500f),
                RankEntity(attributeId = attributeId, name = "SSS", minValue = 2501f, maxValue = 99999f)
            )
            
            for (rank in defaultRanks) {
                repository.insertRank(rank)
            }
        }
    }

    fun updateAttribute(attribute: AttributeEntity) {
        viewModelScope.launch {
            repository.updateAttribute(attribute)
        }
    }

    fun updateAttributeValue(attribute: AttributeEntity, newValue: Float) {
        viewModelScope.launch {
            repository.updateAttribute(attribute.copy(currentValue = newValue))
        }
    }

    fun checkAndDeleteAttribute(attribute: AttributeEntity, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val isReferenced = behaviorRepository.isAttributeReferenced(attribute.id)
            if (isReferenced) {
                onResult(false, "无法删除：「${attribute.name}」已被某些行动引用")
            } else {
                repository.deleteAttribute(attribute)
                onResult(true, "删除成功")
            }
        }
    }

    fun updateAttributeSortOrders(attributes: List<AttributeEntity>) {
        viewModelScope.launch {
            repository.updateAttributes(attributes)
        }
    }
}
