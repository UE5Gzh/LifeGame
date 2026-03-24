package com.example.lifegame.ui.quest

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifegame.data.entity.AttributeWithRanks
import com.example.lifegame.data.entity.QuestAttributeGoalEntity
import com.example.lifegame.data.entity.QuestBehaviorGoalEntity
import com.example.lifegame.data.entity.QuestEffectEntity
import com.example.lifegame.data.entity.QuestEntity
import com.example.lifegame.data.entity.QuestWithDetails
import com.example.lifegame.data.entity.BehaviorWithModifiers
import com.example.lifegame.data.entity.StatusEntity
import com.example.lifegame.repository.BehaviorRepository
import com.example.lifegame.repository.AttributeRepository
import com.example.lifegame.repository.QuestRepository
import com.example.lifegame.repository.LogRepository
import com.example.lifegame.repository.StatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class QuestViewModel @Inject constructor(
    private val questRepository: QuestRepository,
    private val attributeRepository: AttributeRepository,
    private val behaviorRepository: BehaviorRepository,
    private val logRepository: LogRepository,
    private val statusRepository: StatusRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("quest_prefs", Context.MODE_PRIVATE)
    
    private val celebratedQuestIds = mutableSetOf<Long>()

    private val _selectedTabType = MutableStateFlow(sharedPreferences.getInt("selected_tab_type", 0))
    val selectedTabType: StateFlow<Int> = _selectedTabType.asStateFlow()

    fun saveSelectedTabType(tabType: Int) {
        _selectedTabType.value = tabType
        sharedPreferences.edit().putInt("selected_tab_type", tabType).apply()
    }

    val attributes: StateFlow<List<AttributeWithRanks>> = attributeRepository.allAttributesWithRanks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val behaviors: StateFlow<List<BehaviorWithModifiers>> = behaviorRepository.allBehaviorsWithModifiers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val quests: StateFlow<List<QuestWithDetails>> = questRepository.allQuestsWithDetails
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        checkDailyResets()
    }

    private fun checkDailyResets() {
        viewModelScope.launch {
            val allQuests = questRepository.getActiveQuestsWithDetails()
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val weekStart = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis > System.currentTimeMillis()) {
                    add(Calendar.WEEK_OF_YEAR, -1)
                }
            }.timeInMillis

            for (q in allQuests) {
                if (q.quest.type == 0 && q.quest.lastResetTime < todayStart) {
                    if (q.quest.status == 0) {
                        val failedQuest = q.quest.copy(status = 3, lastResetTime = System.currentTimeMillis(), isFocused = false)
                        questRepository.updateQuest(failedQuest)
                        val punishments = applyPunishments(q)
                        logRepository.insertLogWithDefaultLock(
                            type = "QUEST_ABANDON",
                            title = "日常任务超时失败: ${q.quest.name}",
                            details = if (punishments.isEmpty()) "触发惩罚: 无" else "触发惩罚: $punishments",
                            questType = 0
                        )
                    } else if (q.quest.status == 2) {
                        questRepository.updateQuest(q.quest.copy(status = 0, lastResetTime = System.currentTimeMillis()))
                        val resetBehaviors = q.behaviorGoals.map { it.copy(currentCount = 0) }
                        questRepository.updateQuestWithDetails(q.quest.copy(status = 0, lastResetTime = System.currentTimeMillis()), q.attributeGoals, resetBehaviors, q.effects)
                    }
                }
                
                if (q.quest.type == 3 && q.quest.lastResetTime < weekStart) {
                    if (q.quest.status == 0) {
                        val failedQuest = q.quest.copy(status = 3, lastResetTime = System.currentTimeMillis(), isFocused = false)
                        questRepository.updateQuest(failedQuest)
                        val punishments = applyPunishments(q)
                        logRepository.insertLogWithDefaultLock(
                            type = "QUEST_ABANDON",
                            title = "周常任务超时失败: ${q.quest.name}",
                            details = if (punishments.isEmpty()) "触发惩罚: 无" else "触发惩罚: $punishments",
                            questType = 3
                        )
                    } else if (q.quest.status == 2) {
                        questRepository.updateQuest(q.quest.copy(status = 0, lastResetTime = System.currentTimeMillis()))
                        val resetBehaviors = q.behaviorGoals.map { it.copy(currentCount = 0) }
                        questRepository.updateQuestWithDetails(q.quest.copy(status = 0, lastResetTime = System.currentTimeMillis()), q.attributeGoals, resetBehaviors, q.effects)
                    }
                }
            }
        }
    }

    fun checkQuestCompletions(currentAttributes: List<AttributeWithRanks>) {
        viewModelScope.launch {
            val allQuests = quests.value
            for (q in allQuests) {
                if (q.quest.status == 0 && q.quest.id !in celebratedQuestIds) {
                    val progress = calculateProgress(q, currentAttributes)
                    if (progress >= 1f) {
                        celebratedQuestIds.add(q.quest.id)
                        questRepository.updateQuest(q.quest.copy(status = 1))
                        com.example.lifegame.util.CelebrationBus.postQuestComplete(q.quest.name, q.quest.type)
                    }
                }
            }
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

    fun calculateProgress(quest: QuestWithDetails, currentAttributes: List<AttributeWithRanks>): Float {
        val totalGoals = quest.attributeGoals.size + quest.behaviorGoals.size
        if (totalGoals == 0) return 0f

        var totalProgress = 0f

        for (ag in quest.attributeGoals) {
            val attr = currentAttributes.find { it.attribute.id == ag.attributeId }?.attribute
            val currentVal = attr?.currentValue ?: 0f
            val targetVal = ag.targetValue
            if (targetVal <= 0f) {
                totalProgress += 1f
            } else {
                val p = (currentVal / targetVal).coerceIn(0f, 1f)
                totalProgress += p
            }
        }

        for (bg in quest.behaviorGoals) {
            if (bg.targetCount <= 0) {
                totalProgress += 1f
            } else {
                val p = (bg.currentCount.toFloat() / bg.targetCount.toFloat()).coerceIn(0f, 1f)
                totalProgress += p
            }
        }

        return totalProgress / totalGoals
    }

    fun claimReward(questWithDetails: QuestWithDetails) {
        viewModelScope.launch {
            val currentAttrs = attributes.value
            val rewardDetails = StringBuilder()
            val attributeChanges = mutableListOf<com.example.lifegame.util.AttributeChangeItem>()
            
            for (effect in questWithDetails.effects.filter { !it.isPunishment && it.type == 0 }) {
                val attrWithRanks = currentAttrs.find { it.attribute.id == effect.attributeId }
                val attrToUpdate = attrWithRanks?.attribute
                if (attrToUpdate != null && effect.valueChange != null && effect.attributeId != null) {
                    var actualChange = effect.valueChange
                    if (effect.valueChange > 0) {
                        actualChange = calculateFinalChangeForAttribute(effect.attributeId!!, effect.valueChange)
                    }
                    val oldValue = attrToUpdate.currentValue
                    val newValue = oldValue + actualChange
                    attributeRepository.updateAttribute(attrToUpdate.copy(currentValue = newValue))
                    rewardDetails.append("${attrToUpdate.name} ${if(actualChange > 0) "+" else ""}${formatValue(actualChange)} ")
                    attributeChanges.add(com.example.lifegame.util.AttributeChangeItem(attrToUpdate.name, actualChange, attrToUpdate.colorHex))
                    
                    if (attrWithRanks.ranks.isNotEmpty()) {
                        checkRankUp(attrToUpdate.name, oldValue, newValue, attrWithRanks.ranks)
                    }
                }
            }
            
            val questType = questWithDetails.quest.type
            if (questType == 1 || questType == 2) {
                questRepository.deleteQuest(questWithDetails.quest)
            } else {
                questRepository.updateQuest(questWithDetails.quest.copy(status = 2, isFocused = false))
            }
            
            val questTypeStr = getTypeStr(questType)
            logRepository.insertLogWithDefaultLock(
                type = "QUEST_COMPLETION",
                title = "完成${questTypeStr}任务: ${questWithDetails.quest.name}",
                details = if (rewardDetails.isEmpty()) "获得奖励: 无" else "获得奖励: $rewardDetails",
                questType = questType
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
            for (i in (oldRankIndex + 1)..newRankIndex) {
                val prevRank = if (i > 0) sortedRanks[i - 1].name else "无"
                val currentRank = sortedRanks[i].name
                com.example.lifegame.util.CelebrationBus.postRankUp(
                    attributeName = attributeName,
                    oldRank = prevRank,
                    newRank = currentRank
                )
            }
        }
    }

    private suspend fun applyPunishments(questWithDetails: QuestWithDetails): String {
        val currentAttrs = attributes.value
        val punishmentDetails = StringBuilder()
        val attributeChanges = mutableListOf<com.example.lifegame.util.AttributeChangeItem>()
        
        for (effect in questWithDetails.effects.filter { it.isPunishment && it.type == 0 }) {
            val attrWithRanks = currentAttrs.find { it.attribute.id == effect.attributeId }
            val attrToUpdate = attrWithRanks?.attribute
            if (attrToUpdate != null && effect.valueChange != null) {
                val oldValue = attrToUpdate.currentValue
                val newValue = oldValue + effect.valueChange
                attributeRepository.updateAttribute(attrToUpdate.copy(currentValue = newValue))
                punishmentDetails.append("${attrToUpdate.name} ${if(effect.valueChange > 0) "+" else ""}${formatValue(effect.valueChange)} ")
                attributeChanges.add(com.example.lifegame.util.AttributeChangeItem(attrToUpdate.name, effect.valueChange, attrToUpdate.colorHex))
                
                if (attrWithRanks.ranks.isNotEmpty()) {
                    checkRankUp(attrToUpdate.name, oldValue, newValue, attrWithRanks.ranks)
                }
            }
        }
        
        com.example.lifegame.util.AttributeChangeBus.postChanges(attributeChanges)
        
        return punishmentDetails.toString()
    }

    private fun getTypeStr(type: Int): String {
        return when(type) {
            0 -> "日常"
            1 -> "主线"
            3 -> "周常"
            else -> "支线"
        }
    }

    fun createQuest(
        name: String,
        type: Int,
        deadline: Long?,
        attributeGoals: List<QuestAttributeGoalEntity>,
        behaviorGoals: List<QuestBehaviorGoalEntity>,
        effects: List<QuestEffectEntity>
    ) {
        viewModelScope.launch {
            val q = QuestEntity(
                name = name,
                type = type,
                deadline = deadline,
                status = 0
            )
            questRepository.insertQuestWithDetails(q, attributeGoals, behaviorGoals, effects)
            
            logRepository.insertLogWithDefaultLock(
                type = "QUEST_CREATION",
                title = "创建${getTypeStr(type)}任务: $name",
                details = "",
                questType = type
            )
        }
    }

    fun deleteQuest(quest: QuestEntity) {
        viewModelScope.launch {
            questRepository.deleteQuest(quest)
        }
    }

    fun giveUpQuest(questWithDetails: QuestWithDetails) {
        viewModelScope.launch {
            questRepository.updateQuest(questWithDetails.quest.copy(status = 3, isFocused = false))
            val punishments = applyPunishments(questWithDetails)
            
            logRepository.insertLogWithDefaultLock(
                type = "QUEST_ABANDON",
                title = "放弃${getTypeStr(questWithDetails.quest.type)}任务: ${questWithDetails.quest.name}",
                details = if (punishments.isEmpty()) "触发惩罚: 无" else "触发惩罚: $punishments",
                questType = questWithDetails.quest.type
            )
        }
    }
    
    fun toggleQuestFocus(quest: QuestEntity) {
        viewModelScope.launch {
            if (!quest.isFocused) {
                val currentFocused = quests.value.find { it.quest.isFocused }?.quest
                if (currentFocused != null) {
                    questRepository.updateQuest(currentFocused.copy(isFocused = false))
                }
                questRepository.updateQuest(quest.copy(isFocused = true))
            } else {
                questRepository.updateQuest(quest.copy(isFocused = false))
            }
        }
    }

    fun updateQuestSortOrders(quests: List<QuestEntity>) {
        viewModelScope.launch {
            questRepository.updateQuests(quests)
        }
    }

    fun updateQuestWithDetails(
        quest: QuestEntity,
        attributeGoals: List<QuestAttributeGoalEntity>,
        behaviorGoals: List<QuestBehaviorGoalEntity>,
        effects: List<QuestEffectEntity>
    ) {
        viewModelScope.launch {
            questRepository.updateQuestWithDetails(quest, attributeGoals, behaviorGoals, effects)
        }
    }

    fun instantCompleteQuest(questWithDetails: QuestWithDetails) {
        viewModelScope.launch {
            val currentAttrs = attributes.value
            val rewardDetails = StringBuilder()
            
            for (effect in questWithDetails.effects.filter { !it.isPunishment && it.type == 0 }) {
                val attrToUpdate = currentAttrs.find { it.attribute.id == effect.attributeId }?.attribute
                if (attrToUpdate != null && effect.valueChange != null && effect.attributeId != null) {
                    var actualChange = effect.valueChange
                    if (effect.valueChange > 0) {
                        actualChange = calculateFinalChangeForAttribute(effect.attributeId!!, effect.valueChange)
                    }
                    attributeRepository.updateAttribute(attrToUpdate.copy(currentValue = attrToUpdate.currentValue + actualChange))
                    rewardDetails.append("${attrToUpdate.name} ${if(actualChange > 0) "+" else ""}${formatValue(actualChange)} ")
                }
            }
            
            questRepository.updateQuest(questWithDetails.quest.copy(status = 1, isFocused = false))
            
            logRepository.insertLogWithDefaultLock(
                type = "QUEST_INSTANT_COMPLETE",
                title = "通过立即完成功能完成${getTypeStr(questWithDetails.quest.type)}任务: ${questWithDetails.quest.name}",
                details = if (rewardDetails.isEmpty()) "获得奖励: 无" else "获得奖励: $rewardDetails",
                questType = questWithDetails.quest.type
            )
        }
    }
}
