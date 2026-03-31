# Unreal Engine 5 委托与代理系统详解

## 前言

委托（Delegate）和代理（Multicast Delegate）是 Unreal Engine 中实现事件驱动编程的核心机制。UE5 提供了类型安全的委托系统，支持单播和多播两种模式。本文深入解析 UE5 委托系统的设计原理、使用方式以及底层实现。

---

## 一、委托的核心概念

### 1.1 什么是委托

委托本质上是一个指向成员函数的指针，它封装了对象实例和成员函数地址，使得我们可以在不知道对象具体类型的情况下调用其成员函数。

```cpp
// 声明一个委托类型
DECLARE_DELEGATE(MyDelegate);

// 绑定到 Lambda
MyDelegate MyDel;
MyDel.BindLambda([](){ UE_LOG(LogTemp, Warning, TEXT("Hello")); });

// 调用
MyDel.Execute();
```

### 1.2 单播委托 vs 多播委托

| 特性 | 单播委托 (Delegate) | 多播委托 (Multicast Delegate) |
|------|-------------------|---------------------------|
| 绑定数量 | 只能绑定一个 | 可以绑定多个 |
| 调用方式 | `Execute()` | `Broadcast()` |
| 返回值 | 支持 | 不支持（返回最后一个结果） |
| 执行顺序 | 确定性 | 不保证顺序 |

---

## 二、委托类型详解

### 2.1 按返回值分类

UE5 委托系统提供了多种声明宏：

```cpp
// 无返回值委托
DECLARE_DELEGATE(FDele_NoParam);              // 无参数
DECLARE_DELEGATE_OneParam(FDele_OneParam, int32);  // 单参数
DECLARE_DELEGATE_TwoParams(FDele_TwoParam, int32, float);
DECLARE_DELEGATE_ThreeParams(FDele_ThreeParams, int32, float, FString);
DECLARE_DELEGATE_FourParams(FDele_FourParams, int32, float, FString, bool);

// 有返回值委托
DECLARE_DELEGATE_RetVal_OneParam(RetValType, FDelegateName, Param1Type);
```

### 2.2 委托声明宏解析

以 `DECLARE_DELEGATE_OneParam` 为例，其完整声明如下：

```cpp
// Engine\Delegates\Delegates.h
#define DECLARE_DELEGATE(DelegateName) // ...
#define DECLARE_DELEGATE_OneParam(DelegateName, Param1Type) // ...
```

声明委托的完整流程：

```cpp
// 头文件
class UMyActor : public AActor
{
public:
    // 声明委托类型
    DECLARE_DELEGATE_OneParam(FOnHealthChanged, float);

    // 声明委托成员变量
    FOnHealthChanged OnHealthChanged;

    UFUNCTION()
    void HandleDamage(float Damage);
};
```

```cpp
// 源文件
void UMyActor::Setup()
{
    // 绑定到成员函数
    OnHealthChanged.BindUObject(this, &UMyActor::HandleDamage);

    // 或绑定到 Lambda
    OnHealthChanged.BindLambda([this](float Dmg){
        UE_LOG(LogTemp, Warning, TEXT("Damaged: %f"), Dmg);
    });

    // 或绑定到 UFunction（Blueprint）
    FScriptDelegate ScriptDel;
    ScriptDel.BindUFunction(TargetObject, TEXT("OnDamage"));
    OnHealthChanged.Add(ScriptDel);
}
```

---

## 三、动态委托

动态委托支持通过名称字符串进行绑定，常用于蓝图交互和序列化场景。

### 3.1 声明与实现

```cpp
// 头文件 - 使用 DECLARE_DYNAMIC_DELEGATE 系列宏
UCLASS(Blueprintable)
class UMyWidget : public UUserWidget
{
    GENERATED_BODY()

public:
    // 动态单播委托
    UPROPERTY(EditAnywhere, BlueprintReadWrite)
    FDynamicDelegate OnClick;

    // 动态多播委托
    UPROPERTY(EditAnywhere, BlueprintReadWrite)
    FDynamicMulticastDelegate OnStateChange;

    UFUNCTION()
    void HandleClick();

    UFUNCTION()
    void HandleStateChange(int32 NewState);
};
```

```cpp
// 源文件 - 绑定函数
void UMyWidget::BindDelegates()
{
    // 绑定到 UFunction
    OnClick.AddDynamic(this, &UMyWidget::HandleClick);
    OnStateChange.AddDynamic(this, &UMyWidget::HandleStateChange);

    // 也可以在蓝图中绑定，或通过字符串名称延迟绑定
    FScriptDelegate Del;
    Del.BindUFunction(Object, TEXT("FunctionName"));
}
```

### 3.2 BlueprintCallable 事件

配合 `FScriptDelegate` 可以实现 Blueprint 可调用的事件：

```cpp
// 在 Actor 中声明
UPROPERTY(EditAnywhere, BlueprintAssignable)
FMulticastScriptDelegate OnActorDestroyed;

// 调用时（引擎内部）
void AActor::Destroyed()
{
    OnActorDestroyed.Broadcast();
    // ...
}
```

---

## 四、多播委托的高级用法

### 4.1 安全广播

多播委托广播时，如果某个委托对象已被销毁，直接广播会导致崩溃。UE5 提供了 `BroadcastSafe`：

```cpp
// 普通 Broadcast 可能崩溃
OnHealthChange.Broadcast(CurrentHealth);

// Safe 版本会过滤已失效的委托
OnHealthChange.BroadcastSafe(CurrentHealth);
```

`BroadcastSafe` 内部通过 `TArray<FDelegateHandle>` 追踪所有已订阅的委托，在广播前验证每个委托的有效性。

### 4.2 委托句柄

每个绑定的委托都有一个唯一的 `FDelegateHandle`：

```cpp
FDelegateHandle Handle = MyMulticast.AddLambda([](){
    UE_LOG(LogTemp, Warning, TEXT("Called"));
});

// 通过句柄移除特定委托
MyMulticast.Remove(Handle);

// 检查是否已绑定
bool bHas = MyMulticast.IsBound();
```

### 4.3 委托列表操作

```cpp
// 清空所有委托
MyMulticast.Clear();

// 获取委托数量
int32 Count = MyMulticast.GetNum();

// 复制委托列表（广播中修改的安全做法）
auto Delegates = MyMulticast.GetAllHandles();
for (auto& Handle : Delegates)
{
    // 处理...
}
```

---

## 五、委托与生命周期管理

### 5.1 智能指针绑定

当绑定到 Lambda 时，如果 Lambda 捕获了 `TSharedPtr` 或 `TWeakPtr`，可以避免常见的悬空引用问题：

```cpp
TWeakPtr<FMyClass> WeakThis = MyClassPtr;

MyDelegate.BindLambda([WeakThis](){
    if (auto Strong = WeakThis.Pin())
    {
        Strong->DoSomething();
    }
});
```

### 5.2 UPROPERTY 与委托

如果委托需要被序列化或在蓝图访问，需要用 `UPROPERTY` 标记：

```cpp
UPROPERTY(BlueprintAssignable)
FMulticastScriptDelegate OnValueChanged;
```

---

## 六、性能考量

### 6.1 委托的内存开销

每个委托实例包含：
- 函数指针或 UObject 指针
- 对象指针（若是成员函数绑定）
- 额外的调度开销

对于高频调用场景（如每帧执行），需评估是否直接函数调用更高效。

### 6.2 多播委托的广播成本

```cpp
// O(N) 时间复杂度
MyMulticast.Broadcast(Args...);

// 每次广播都会遍历整个委托列表
// 高频场景考虑：
// 1. 使用快速委托（详见下节）
// 2. 评估是否真正需要多播
```

### 6.3 快速委托（Fast Delegate）

UE5 内部使用 FastDelegate 实现，提供更高效的委托调用：

```cpp
// 引擎源码中的实现
// 使用函数指针而非间接调用
// 性能接近直接函数调用
```

---

## 七、常见问题与最佳实践

### 7.1 常见问题

**Q: 委托绑定后对象被销毁会怎样？**
A: 单播委托中 BindUObject 会检查对象有效性；Lambda 捕获需自行管理生命周期。多播委托建议使用 `BroadcastSafe`。

**Q: 委托能否跨 DLL 边界使用？**
A: 可以，但需使用 `DECLARE_DYNAMIC_DELEGATE` 系列，并确保两边使用相同的 RTTI 系统。

**Q: 如何在蓝图中实现事件？**
A: 使用 `FMulticastScriptDelegate` + `BlueprintAssignable`，或使用 `UFUNCTION(BlueprintImplementableEvent)`。

### 7.2 最佳实践

1. **优先使用强类型委托**：在 C++ 中使用 `DECLARE_DELEGATE` 系列而非动态委托，以获得编译期类型检查。

2. **合理选择单播或多播**：不需要广播时使用单播，性能更好且语义更清晰。

3. **Lambda 捕获需谨慎**：避免捕获裸指针，优先使用 `TWeakPtr`。

4. **委托成员声明在头文件中**：使用 `DECLARE_DELEGATE` 宏在类内部声明，确保编译期生成完整的委托类型。

5. **避免在构造函数中绑定**：此时对象可能尚未完全初始化。

---

## 八、完整示例

```cpp
// MyCharacter.h
#pragma once
#include "CoreMinimal.h"
#include "GameFramework/Character.h"
#include "MyCharacter.generated.h"

DECLARE_DYNAMIC_MULTICAST_DELEGATE_OneParam(FOnHealthChangeDelegate, float, NewHealth);

UCLASS()
class AMyCharacter : public ACharacter
{
    GENERATED_BODY()

public:
    AMyCharacter();

    // 事件委托
    UPROPERTY(BlueprintAssignable, Category = "Health")
    FOnHealthChangeDelegate OnHealthChange;

    UFUNCTION(BlueprintCallable, Category = "Health")
    void TakeDamage(float Damage);

protected:
    virtual void BeginPlay() override;

private:
    float Health = 100.0f;
    float MaxHealth = 100.0f;
};
```

```cpp
// MyCharacter.cpp
#include "MyCharacter.h"
#include "Kismet/GameplayStatics.h"

AMyCharacter::AMyCharacter()
{
    PrimaryActorTick.bCanEverTick = true;
}

void AMyCharacter::BeginPlay()
{
    Super::BeginPlay();
}

void AMyCharacter::TakeDamage(float Damage)
{
    Health = FMath::Clamp(Health - Damage, 0.0f, MaxHealth);

    UE_LOG(LogTemp, Warning, TEXT("Health: %f"), Health);

    // 广播事件
    OnHealthChange.Broadcast(Health);

    if (Health <= 0.0f)
    {
        // 处理死亡逻辑
        UE_LOG(LogTemp, Warning, TEXT("Character died!"));
    }
}
```

---

## 结语

Unreal Engine 5 的委托系统是构建事件驱动架构的基石。掌握单播/多播委托的区别、动态委托的使用场景、以及委托的生命周期管理，能够帮助开发者编写出更加模块化、解耦合的代码。建议在实际项目中根据具体场景选择合适的委托类型，并遵循最佳实践以避免常见的陷阱。

---

*本文基于 Unreal Engine 5.3 编写。*
