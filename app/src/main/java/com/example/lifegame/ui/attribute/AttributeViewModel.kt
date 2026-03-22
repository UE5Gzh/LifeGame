package com.example.lifegame.ui.attribute

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifegame.data.entity.AttributeEntity
import com.example.lifegame.repository.AttributeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttributeViewModel @Inject constructor(
    private val repository: AttributeRepository
) : ViewModel() {

    val attributes: StateFlow<List<AttributeEntity>> = repository.allAttributes
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

    fun updateAttributeValue(attribute: AttributeEntity, newValue: Int) {
        viewModelScope.launch {
            repository.updateAttribute(attribute.copy(currentValue = newValue))
        }
    }

    fun deleteAttribute(attribute: AttributeEntity) {
        viewModelScope.launch {
            repository.deleteAttribute(attribute)
        }
    }

    fun resetAllAttributes() {
        viewModelScope.launch {
            repository.resetAllAttributes()
        }
    }
}
