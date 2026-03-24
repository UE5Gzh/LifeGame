# 优化输入验证提示

## 需求分析

### 需求一：新增状态效果验证提示
当前代码在 `effects.isEmpty()` 时直接返回 `false`，没有给用户任何提示。需要添加弹窗提示。

### 需求二：新增行动名称验证
**问题发现**：`showAddBehaviorDialog` 方法（第737-739行）中，当名称为空时只是 `return@setOnClickListener`，没有显示任何错误提示！

```kotlin
// 当前代码（有问题）
if (name.isNullOrEmpty()) {
    return@setOnClickListener  // 没有任何提示！
}
```

需要添加错误提示：
```kotlin
// 修复后
if (name.isNullOrEmpty()) {
    dialogBinding.tilName.error = "请输入行动名称"
    return@setOnClickListener
}
dialogBinding.tilName.error = null
```

### 需求三：新增任务验证提示
当前代码在以下情况直接返回，没有提示：
1. 主线/支线任务没有选择日期
2. 没有选择至少一个目标

## 实现方案

### 一、新增状态效果验证提示

**修改文件**：`AttributeFragment.kt`

**修改位置**：`validateInput` 方法

```kotlin
val effects = effectsAdapter.getEffects()
if (effects.isEmpty()) {
    showValidationErrorDialog("请至少添加一个效果")
    return false
}
```

### 二、新增行动名称验证

**修改文件**：`BehaviorFragment.kt`

**修改位置**：`showAddBehaviorDialog` 方法（第737行附近）

```kotlin
if (name.isNullOrEmpty()) {
    dialogBinding.tilName.error = "请输入行动名称"
    return@setOnClickListener
}
dialogBinding.tilName.error = null
```

### 三、新增任务验证提示

**修改文件**：`QuestFragment.kt`

**修改位置**：`showCreateQuestDialog` 方法中的 `btnConfirm.setOnClickListener`

1. 主线/支线没有选择日期：
```kotlin
if (type != 0 && type != 3 && selectedDeadline == null) {
    showValidationErrorDialog("主线和支线任务必须选择截止日期")
    return@setOnClickListener
}
```

2. 没有选择目标：
```kotlin
if (attrGoals.isEmpty() && behGoals.isEmpty()) {
    showValidationErrorDialog("请至少添加一个完成目标")
    return@setOnClickListener
}
```

### 四、创建通用验证错误弹窗方法

**新增方法**：在 `AttributeFragment.kt` 和 `QuestFragment.kt` 中添加

```kotlin
private fun showValidationErrorDialog(message: String) {
    val dialogBinding = DialogConfirmBinding.inflate(layoutInflater)
    dialogBinding.tvTitle.text = "提示"
    dialogBinding.tvMessage.text = message
    
    val dialog = MaterialAlertDialogBuilder(requireContext(), ...)
        .setView(dialogBinding.root)
        .create()
    
    dialogBinding.btnCancel.visibility = View.GONE
    dialogBinding.btnConfirm.text = "知道了"
    dialogBinding.btnConfirm.setOnClickListener { dialog.dismiss() }
    
    dialog.show()
}
```

## 文件修改清单

1. **修改文件**：
   - `AttributeFragment.kt` - 添加效果为空时的弹窗提示
   - `BehaviorFragment.kt` - 修复 `showAddBehaviorDialog` 方法中缺少名称验证提示的问题
   - `QuestFragment.kt` - 添加日期和目标验证弹窗提示

## 验证标准

1. 新增状态时，如果没有添加任何效果，显示弹窗提示
2. 新增行动时，如果没有输入名称，显示输入框下方错误提示
3. 新增任务时，如果是主线/支线且没有选择日期，显示弹窗提示
4. 新增任务时，如果没有添加任何目标，显示弹窗提示
