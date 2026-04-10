# pms-upgrader

PMS 平台配套的 Android 应用内更新库，提供检查更新、下载 APK、MD5 校验、自动安装等完整更新流程，基于 Kotlin Coroutines + StateFlow 实现响应式状态管理。

---

## 功能特性

- **检查更新**：向 PMS 服务端 POST `/access/check` 查询最新版本
- **下载 APK**：带实时进度回调，支持断点感知
- **MD5 校验**：下载完成后自动验证文件完整性
- **安装 APK**：适配 Android N+（FileProvider）及旧版本
- **响应式状态**：通过 `StateFlow` 暴露 `UpdateState`，方便 Compose / LiveData 集成

---

## 接入方式

### 1. 添加 JitPack 仓库

**`settings.gradle.kts`**
```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. 添加依赖

```kotlin
implementation("com.github.ahaodev:pmsupgrader:版本号")
```

最新版本请查看 [Releases](https://github.com/ahaodev/pmsupgrader/releases)。

### 3. 配置 FileProvider

在应用的 `AndroidManifest.xml` 中声明 FileProvider（安装 APK 所需）：

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.provider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_provider_paths" />
</provider>
```

在 `res/xml/file_provider_paths.xml` 中添加下载目录：

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-files-path name="downloads" path="downloads/" />
    <files-path name="downloads" path="downloads/" />
    <cache-path name="downloads" path="downloads/" />
</paths>
```

---

## 使用示例

### 初始化

```kotlin
val upgrader = Upgrader.Builder(context)
    .accessToken("YOUR_ACCESS_TOKEN")   // PMS 平台访问令牌
    .baseUrl("https://your-pms-server.com")
    .project("your-project-name")
    .packageName("com.your.app")
    .build()
```

四个参数均为必填，缺少任意一项 `build()` 将抛出 `IllegalArgumentException`。

### 检查更新

```kotlin
lifecycleScope.launch {
    upgrader.checkUpdate(currentVersion = "1.0.0")
}

// 收集状态
upgrader.updateState.collect { state ->
    when (state) {
        is UpdateState.Idle            -> { /* 初始状态 */ }
        is UpdateState.Checking        -> { /* 正在检查 */ }
        is UpdateState.UpdateAvailable -> {
            val info = state.response
            // info.latestVersion, info.changelog, info.downloadUrl ...
        }
        is UpdateState.NoUpdateAvailable -> { /* 已是最新 */ }
        is UpdateState.Downloading     -> {
            val pct = state.progress.progress // 0-100，-1 表示未知
        }
        is UpdateState.DownloadCompleted -> {
            upgrader.installApk(state.filePath)
        }
        is UpdateState.Error           -> {
            // state.message, state.throwable
        }
    }
}
```

### 一键完整更新流程

```kotlin
lifecycleScope.launch {
    upgrader.performUpdate(
        currentVersion = "1.0.0",
        autoInstall = true        // 下载完成后自动触发安装
    )
}
```

### 手动分步执行

```kotlin
// 1. 检查
upgrader.checkUpdate("1.0.0")

// 2. 下载
val filePath = upgrader.downloadApk(downloadUrl, "update.apk")

// 3. 校验（可选）
val ok = upgrader.verifyApkFile(filePath!!, expectedMd5)

// 4. 安装
if (ok) upgrader.installApk(filePath)
```

---

## UpdateState 状态说明

| 状态 | 说明 |
|------|------|
| `Idle` | 初始/重置状态 |
| `Checking` | 正在向服务端查询 |
| `UpdateAvailable(response)` | 有新版本，包含下载地址、changelog 等 |
| `NoUpdateAvailable` | 当前已是最新版本 |
| `Downloading(progress)` | 下载中，携带字节数和百分比 |
| `DownloadCompleted(filePath)` | 下载完成，携带本地文件路径 |
| `Error(message, throwable)` | 任意阶段出错 |

---

## 最低要求

| 项目 | 要求 |
|------|------|
| minSdk | 24（Android 7.0） |
| 编译语言 | Kotlin |
| 序列化 | kotlinx.serialization |

---

## 发布新版本

APK 的发布（上传至 PMS 平台）使用 [pms-releaser](https://github.com/ahaodev/pms-releaser)。

在 GitHub Actions 中推送 `v*` tag 即可触发自动构建并发布 AAR 到 JitPack：

```bash
git tag v1.0.0
git push origin v1.0.0
```

发布 APK 到 PMS 平台示例：

```yaml
- uses: ahaodev/pms-releaser@main
  with:
    file_path: './app-release.apk'
    version: ${{ github.ref_name }}
    project_name: ${{ vars.PROJECT_NAME }}
    package_name: ${{ vars.PACKAGE_NAME }}
    access_token: ${{ secrets.ACCESS_TOKEN }}
    release_url: ${{ secrets.RELEASE_URL }}
```

---

## License

[MIT](LICENSE)
