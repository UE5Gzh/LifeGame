# 统一任务完成庆祝动画触发逻辑

## 问题分析

### 当前实现问题
1. **触发机制不统一**：
   - 日常、周常、支线任务：在 `QuestFragment` 的 `attributes.collect` 中检测完成
   - 主线任务：同样只在任务界面检测

2. **重复触发问题**：
   - `celebratedQuestIds` 是内存中的集合，ViewModel重建时丢失
   - 界面切换后重新订阅 `attributes`，导致已完成的任务再次触发庆祝

3. **非全局监听**：
   - 只有在任务界面时才会调用 `checkQuestCompletions`
   - 其他界面完成任务无法触发庆祝

## 解决方案

### 1. 创建 QuestCompletionManager
- 全局单例，在 Application 中启动
- 持久化存储已触发庆祝的任务ID
- 监听属性变化，全局检测任务完成
- 与现有 CelebrationBus 集成

### 2. 持久化已触发任务ID
- 使用 SharedPreferences 存储已触发庆祝的任务ID集合
- 应用启动时清理过期数据（非当日数据）

### 3. 修改现有代码
- 移除 QuestViewModel 中的 `celebratedQuestIds`
- 移除 QuestFragment 中的 `checkQuestCompletions` 调用
- 由 QuestCompletionManager 统一管理

## 实现步骤

### Step 1: 创建 QuestCompletionManager
```kotlin
@Singleton
class QuestCompletionManager @Inject constructor(
    private val questRepository: QuestRepository,
    private val attributeRepository: AttributeRepository,
    @ApplicationContext private val context: Context
) {
    private val celebratedQuestIds = mutableSetOf<Long>()
    private val sharedPreferences: SharedPreferences
    
    fun start() {
        // 启动监听
    }
    
    fun isCelebrated(questId: Long): Boolean
    fun markCelebrated(questId: Long)
    fun clearExpiredData()
}
```

### Step 2: 在 LifeGameApplication 中启动
```kotlin
@Inject
lateinit var questCompletionManager: QuestCompletionManager

override fun onCreate() {
    questCompletionManager.start()
}
```

### Step 3: 修改 QuestViewModel
- 移除 `celebratedQuestIds` 集合
- 移除 `checkQuestCompletions` 方法中的庆祝触发逻辑
- 保留进度计算方法

### Step 4: 修改 QuestFragment
- 移除 `attributes.collect` 中的 `checkQuestCompletions` 调用

## 文件修改清单

1. **新增文件**：
   - `QuestCompletionManager.kt`

2. **修改文件**：
   - `LifeGameApplication.kt` - 注入并启动管理器
   - `QuestViewModel.kt` - 移除庆祝触发逻辑
   - `QuestFragment.kt` - 移除 checkQuestCompletions 调用

## 验证标准

1. 任何界面完成任务都能触发庆祝动画
2. 同一任务不会重复触发庆祝
3. 应用重启后已触发任务不会再次触发
4. 与现有庆祝动画队列系统正常集成
