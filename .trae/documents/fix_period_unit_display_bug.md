# 修复周期性变动时间单位显示错误的严重BUG

## 问题分析

### BUG根本原因
在 `StatusAdapter.kt` 第126行的 `buildEffectInfoText` 方法中，时间单位显示逻辑完全错误：

```kotlin
// 错误代码
val unitStr = if (effect.periodUnit == 0) "小时" else "天"
```

### 时间单位定义（来自StatusEffectAdapter.kt第236行）
```kotlin
val units = arrayOf("分钟", "小时", "天")
// periodUnit: 0 = 分钟, 1 = 小时, 2 = 天
```

### 错误映射关系
| periodUnit值 | 正确显示 | 当前错误显示 |
|-------------|---------|------------|
| 0 | 分钟 | 小时 |
| 1 | 小时 | 天 |
| 2 | 天 | 天 |

这就是用户反馈的问题：
- 选择"分钟" → 保存后显示为"小时"
- 选择"小时" → 保存后显示为"天"
- 选择"天" → 正常显示为"天"

## 修复方案

### 修改文件
`f:\android_workplace\LifeGame\app\src\main\java\com\example\lifegame\ui\attribute\StatusAdapter.kt`

### 修改内容
将第126行的错误代码：
```kotlin
val unitStr = if (effect.periodUnit == 0) "小时" else "天"
```

修改为正确的映射：
```kotlin
val unitStr = when (effect.periodUnit) {
    0 -> "分钟"
    1 -> "小时"
    else -> "天"
}
```

## 验证步骤
1. 构建项目验证编译通过
2. 提交git

## 影响范围
- 仅影响状态卡片上周期性变动效果的显示文本
- 不影响数据库存储和实际功能执行
- 修复后所有已创建的状态将正确显示时间单位
