package com.example.lifegame.ui.attribute.rank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifegame.data.entity.RankEntity
import com.example.lifegame.repository.AttributeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RankViewModel @Inject constructor(
    private val repository: AttributeRepository
) : ViewModel() {

    private val _attributeId = MutableStateFlow<Long>(-1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val ranks: StateFlow<List<RankEntity>> = _attributeId
        .flatMapLatest { id -> repository.getRanksForAttribute(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setAttributeId(id: Long) {
        _attributeId.value = id
    }

    fun addRank(name: String, minValue: Float, maxValue: Float) {
        val currentAttributeId = _attributeId.value
        if (currentAttributeId == -1L) return

        viewModelScope.launch {
            val newRank = RankEntity(
                attributeId = currentAttributeId,
                name = name,
                minValue = minValue,
                maxValue = maxValue
            )
            repository.insertRank(newRank)
        }
    }

    fun deleteRank(rank: RankEntity) {
        viewModelScope.launch {
            repository.deleteRank(rank)
        }
    }
}
