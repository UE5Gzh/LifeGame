package com.example.lifegame.ui.attribute

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifegame.data.entity.AttributeEntity
import com.example.lifegame.data.entity.AttributeWithRanks
import com.example.lifegame.repository.AttributeRepository
import com.example.lifegame.repository.BehaviorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttributeViewModel @Inject constructor(
    private val repository: AttributeRepository,
    private val behaviorRepository: BehaviorRepository
) : ViewModel() {

    val attributesWithRanks: StateFlow<List<AttributeWithRanks>> = repository.allAttributesWithRanks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addAttribute(name: String, initialValue: Int, colorHex: String) {
        viewModelScope.launch {
            val newAttribute = AttributeEntity(
                name = name,
                currentValue = initialValue,
                initialValue = initialValue,
                colorHex = colorHex
            )
            repository.insertAttribute(newAttribute)
        }
    }

    fun updateAttribute(attribute: AttributeEntity) {
        viewModelScope.launch {
            repository.updateAttribute(attribute)
        }
    }

    fun updateAttributeValue(attribute: AttributeEntity, newValue: Int) {
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
