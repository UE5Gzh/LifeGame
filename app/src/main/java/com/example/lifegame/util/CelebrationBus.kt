package com.example.lifegame.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class CelebrationEvent(
    val type: CelebrationType,
    val priority: Int,
    val attributeName: String = "",
    val oldRank: String = "",
    val newRank: String = "",
    val questName: String = "",
    val questType: String = ""
)

enum class CelebrationType(val priority: Int) {
    RANK_UP(0),
    MAIN_QUEST(1),
    SIDE_QUEST(2),
    WEEKLY_QUEST(3),
    DAILY_QUEST(4)
}

object CelebrationBus {
    private val _events = MutableSharedFlow<CelebrationEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<CelebrationEvent> = _events.asSharedFlow()
    
    fun postRankUp(attributeName: String, oldRank: String, newRank: String) {
        _events.tryEmit(CelebrationEvent(
            type = CelebrationType.RANK_UP,
            priority = CelebrationType.RANK_UP.priority,
            attributeName = attributeName,
            oldRank = oldRank,
            newRank = newRank
        ))
    }
    
    fun postQuestComplete(questName: String, questType: Int) {
        val (type, typeName) = when (questType) {
            1 -> CelebrationType.MAIN_QUEST to "主线"
            2 -> CelebrationType.SIDE_QUEST to "支线"
            3 -> CelebrationType.WEEKLY_QUEST to "周常"
            else -> CelebrationType.DAILY_QUEST to "日常"
        }
        _events.tryEmit(CelebrationEvent(
            type = type,
            priority = type.priority,
            questName = questName,
            questType = typeName
        ))
    }
}
