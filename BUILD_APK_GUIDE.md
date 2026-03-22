# LifeGame APK 打包指南

本文档介绍了如何为 LifeGame 项目生成签名版的 Release APK，以便在真实设备上安装测试。

## 1. 签名密钥配置

项目已经生成了用于测试的签名密钥文件，位于项目根目录：
- 文件名: `lifegame.jks`
- Key Store 密码: `lifegame123`
- Key Alias: `lifegame_key`
- Key 密码: `lifegame123`

*(注意：如果是正式发布到应用商店，请务必生成一个新的密钥并妥善保管密码，不要将包含密码的配置提交到公开的代码仓库。)*

## 2. Gradle 签名配置

已经在 `app/build.gradle.kts` 中配置了 signingConfigs 和 buildTypes：

```kotlin
android {
    ...
    signingConfigs {
        create("release") {
            storeFile = file("../lifegame.jks")
            storePassword = "lifegame123"
            keyAlias = "lifegame_key"
            keyPassword = "lifegame123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

## 3. 版本管理

如果您需要更新版本号，请修改 `app/build.gradle.kts` 中的以下字段：
- `versionCode = 1` (每次发布新版本必须递增)
- `versionName = "1.0"` (展示给用户看的版本号字符串)

## 4. 完整打包命令

在终端 (Terminal) 中，确保当前目录是项目的根目录 (`LifeGame/`)，然后依次执行以下命令：

### 1) 生成签名密钥（仅首次需要，目前已生成）
```bash
keytool -genkey -v -keystore lifegame.jks -keyalg RSA -keysize 2048 -validity 10000 -alias lifegame_key -dname "CN=LifeGame, OU=Development, O=LifeGameApp, L=City, ST=State, C=CN" -storepass lifegame123 -keypass lifegame123
```

### 2) 清理并生成 Release APK
*(Windows 环境下使用 `.\gradlew.bat`，Mac/Linux 环境下使用 `./gradlew`)*

```bash
.\gradlew.bat clean assembleRelease
```

### 3) APK 生成位置
构建成功后，生成的 APK 文件位于以下路径：
`app/build/outputs/apk/release/app-release.apk`

### 4) 验证 APK 签名 (可选)
如果需要确认 APK 是否正确签名，可以使用 apksigner 工具：
```bash
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
```

## 5. 安装与测试注意事项

1. 将 `app-release.apk` 传输到您的 Android 手机（通过 USB、微信、钉钉等）。
2. 在手机上点击安装该 APK。
3. **重要**：因为这不是从应用商店下载的，您需要在手机设置中开启 **“允许安装未知来源应用”** 的权限。部分手机可能会弹出安全警告（因为是自签名），选择“继续安装”即可。