package com.example.lifegame.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class AttributeChangeEvent(
    val changes: List<AttributeChangeItem>
)

data class AttributeChangeItem(
    val attributeName: String,
    val changeValue: Float,
    val colorHex: String?
)

object AttributeChangeBus {
    private val _events = MutableSharedFlow<AttributeChangeEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<AttributeChangeEvent> = _events.asSharedFlow()
    
    fun postEvent(event: AttributeChangeEvent) {
        _events.tryEmit(event)
    }
    
    fun postChanges(changes: List<AttributeChangeItem>) {
        if (changes.isNotEmpty()) {
            _events.tryEmit(AttributeChangeEvent(changes))
        }
    }
    
    fun postChangesNamed(changes: List<Pair<String, Float>>, colorMap: Map<String, String?> = emptyMap()) {
        val items = changes.map { (name, value) ->
            AttributeChangeItem(name, value, colorMap[name])
        }
        postChanges(items)
    }
}
