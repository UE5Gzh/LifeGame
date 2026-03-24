# 美化任务卡片与记录段位提升日志

## 需求分析

### 需求一：美化任务卡片
当前任务卡片已有基础设计，需要进一步优化：
- 视觉层次：增加卡片阴影、优化间距
- 信息展示：优化任务类型标识显示
- 状态指示：已有基础状态区分，需增强视觉效果
- 交互反馈：替换简陋对话框为现代化设计

### 需求二：段位提升日志记录
当前段位提升只有庆祝动画，需要添加日志记录：
- 触发时机：任何属性段位提升时
- 日志内容：属性名称、旧段位、新段位
- 默认锁定：防止被自动清理

## 实现方案

### 一、美化任务卡片

#### 1.1 修改 item_quest.xml
- 添加卡片阴影效果（cardElevation）
- 优化进度条样式
- 添加任务类型图标/标签
- 增强状态视觉效果

#### 1.2 修改 QuestAdapter.kt
- 添加任务类型图标显示
- 优化状态颜色和样式
- 增强关注任务的视觉效果

#### 1.3 创建现代化交互对话框
- 创建 dialog_quest_options.xml - 任务操作选项
- 创建 dialog_quest_confirm.xml - 确认操作

### 二、段位提升日志记录

#### 2.1 修改 BehaviorViewModel.kt
在 checkRankUp 方法中添加日志记录：
```kotlin
if (newRankIndex > oldRankIndex) {
    // 现有庆祝动画代码...
    
    // 新增日志记录
    logRepository.insertLogWithDefaultLock(
        type = "RANK_UP",
        title = "段位提升: $attributeName",
        details = "$oldRankName → $newRankName",
        isLocked = true
    )
}
```

#### 2.2 修改 QuestViewModel.kt
同样在 checkRankUp 方法中添加日志记录

## 文件修改清单

### 新增文件
1. `dialog_quest_options.xml` - 任务操作选项对话框
2. `dialog_quest_confirm.xml` - 确认操作对话框

### 修改文件
1. `item_quest.xml` - 任务卡片布局美化
2. `QuestAdapter.kt` - 适配器逻辑优化
3. `QuestFragment.kt` - 使用新对话框
4. `BehaviorViewModel.kt` - 添加段位提升日志
5. `QuestViewModel.kt` - 添加段位提升日志

## 验证标准

### 任务卡片美化
1. 卡片有明显的阴影和层次感
2. 不同状态有清晰的视觉区分
3. 任务类型有明确的图标/标签标识
4. 交互使用现代化对话框

### 段位提升日志
1. 段位提升时自动记录日志
2. 日志内容包含属性名、旧段位、新段位
3. 日志默认锁定，不会被自动清理
