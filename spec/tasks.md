# 任务到期惩罚触发修复 - 任务列表

## 任务概览

| 序号 | 任务描述 | 优先级 | 状态 |
|-----|---------|-------|------|
| 1 | 修改 PeriodicQuestResetWorker 添加主线/支线任务到期检查 | 高 | 待完成 |
| 2 | 添加 failExpiredQuest 方法处理过期任务 | 高 | 待完成 |
| 3 | 测试验证修复效果 | 中 | 待完成 |

## 详细任务

### 任务1: 修改 checkAndResetQuests 方法

**文件**: `PeriodicQuestResetWorker.kt`

**修改内容**:
- 在 when 语句中添加 case 1, 2 处理主线和支线任务
- 检查任务是否有截止日期
- 检查任务是否未完成 (status == 0)
- 比较当前时间与截止日期

### 任务2: 添加 failExpiredQuest 方法

**文件**: `PeriodicQuestResetWorker.kt`

**功能**:
- 调用 applyPunishments 触发惩罚
- 更新任务状态为失败 (status = 3)
- 记录过期失败日志

### 任务3: 测试验证

**测试场景**:
1. 创建一个主线任务，设置截止日期为今天
2. 等待任务过期（或手动触发 Worker）
3. 验证任务状态变为失败
4. 验证惩罚效果已应用
5. 验证日志记录正确
