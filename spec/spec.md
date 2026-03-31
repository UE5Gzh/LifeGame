# 任务到期惩罚触发修复规范

## 问题描述

**当前Bug**: 主线任务和支线任务到期时，如果没有完成，无法触发任务惩罚。

## 问题分析

### 当前代码逻辑

1. **PeriodicQuestResetWorker.kt** - 只处理日常(type=0)和周常(type=3)任务的重置：
   - 日常任务：每天重置，未完成时触发惩罚
   - 周常任务：每周重置，未完成时触发惩罚

2. **主线任务(type=1)和支线任务(type=2)** - 有截止日期(deadline)，但缺少：
   - 到期检查机制
   - 未完成时的惩罚触发
   - 任务状态更新为失败

### 缺失的功能

主线/支线任务到期时应该：
1. 检查任务是否已完成 (status != 0)
2. 如果未完成，触发惩罚效果
3. 将任务状态设为失败 (status = 3)
4. 记录日志

## 解决方案

### 修改文件

**PeriodicQuestResetWorker.kt**

在 `checkAndResetQuests()` 方法中添加主线/支线任务的到期检查逻辑：

```kotlin
private suspend fun checkAndResetQuests() {
    val allQuests = questRepository.getActiveQuestsWithDetails()
    val now = System.currentTimeMillis()
    
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
        when (q.quest.type) {
            0 -> { // Daily
                if (q.quest.lastResetTime < todayStart) {
                    resetQuest(q)
                }
            }
            3 -> { // Weekly
                if (q.quest.lastResetTime < weekStart) {
                    resetQuest(q)
                }
            }
            1, 2 -> { // Main/Side quests - check deadline
                if (q.quest.deadline != null && q.quest.status == 0) {
                    val deadlineEnd = Calendar.getInstance().apply {
                        timeInMillis = q.quest.deadline!!
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    
                    if (now > deadlineEnd) {
                        failExpiredQuest(q)
                    }
                }
            }
        }
    }
}
```

添加新方法 `failExpiredQuest()`:

```kotlin
private suspend fun failExpiredQuest(q: QuestWithDetails) {
    val punishments = applyPunishments(q)
    questRepository.updateQuest(q.quest.copy(status = 3, isFocused = false))
    
    val typeStr = when (q.quest.type) {
        1 -> "主线"
        2 -> "支线"
        else -> ""
    }
    
    logRepository.insertLogWithDefaultLock(
        type = "QUEST_EXPIRED",
        title = "${typeStr}任务过期失败: ${q.quest.name}",
        details = if (punishments.isEmpty()) "触发惩罚: 无" else "触发惩罚: $punishments",
        questType = q.quest.type
    )
}
```

## 技术细节

### 截止日期判断逻辑

- 截止日期当天的 23:59:59.999 为最后期限
- 超过此时间点即为过期

### 任务状态说明

| 状态值 | 含义 |
|-------|------|
| 0 | 进行中 |
| 1 | 已完成待领取 |
| 2 | 已领取奖励 |
| 3 | 失败/放弃 |

### 日志类型

- `QUEST_EXPIRED` - 任务过期失败（新增）
- `QUEST_ABANDON` - 任务放弃/超时失败

## 影响范围

- 主线任务(type=1)
- 支线任务(type=2)
- 不影响日常任务和周常任务的现有逻辑
