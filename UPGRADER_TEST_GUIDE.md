# 升级器测试指南

## 功能概述

已在 `app` 模块中集成了 `upgrader` 库的测试界面，提供了完整的应用更新检查和管理功能。

## 项目结构修改

### 1. 模块依赖 (`app/build.gradle.kts`)
已添加了对 `upgrader` 模块的依赖：
```kotlin
implementation(project(":upgrader"))
```

### 2. 权限配置 (`app/src/main/AndroidManifest.xml` & `upgrader/src/main/AndroidManifest.xml`)
添加了以下权限：
- `android.permission.INTERNET` - 网络通信
- `android.permission.READ_EXTERNAL_STORAGE` - 读取外部存储
- `android.permission.WRITE_EXTERNAL_STORAGE` - 写入外部存储
- `android.permission.QUERY_ALL_PACKAGES` - API 30+ 包管理
- `android.permission.REQUEST_INSTALL_PACKAGES` - 应用安装

## 测试界面功能

### 输入字段

| 字段 | 默认值 | 说明 |
|------|--------|------|
| 服务器地址 | https://shyy-port.cn:65080 | 升级检查服务的基础URL |
| 访问Token | PKMS-e6arPcELWAXVQqVb | API访问令牌 |
| 项目名称 | msc | 项目或应用名称 |
| 应用包名 | com.ahao.upgrader | 应用的包名 |
| 当前版本 | 1.0 | 当前应用版本号 |

### 功能按钮

- **检查更新** - 根据输入的参数检查是否有新版本可用

### 结果显示

#### 检查结果区域
- 显示检查操作的详细结果信息
- 包含版本号、下载链接、文件大小等信息
- 出错时显示错误详情

#### 状态实时显示
根据更新状态显示不同的界面：

1. **正在检查** - 显示加载进度条
2. **发现新版本** - 显示新版本信息卡片，包括：
   - 最新版本号
   - 文件大小
   - 强制更新标志
3. **已是最新版本** - 显示提示信息
4. **下载中** - 显示下载进度条和百分比
5. **下载完成** - 显示APK文件路径
6. **错误** - 显示错误消息

## 使用示例

### 基本流程

1. **启动应用** - MainActivity 会自动显示升级测试界面
2. **配置参数** - 根据需要修改服务器地址、Token等参数
3. **检查更新** - 点击"检查更新"按钮
4. **查看结果** - 等待响应并在界面上查看结果

### 典型场景

#### 场景1：检查是否有新版本
```
1. 保持默认配置
2. 点击"检查更新"
3. 查看是否有新版本提示
```

#### 场景2：测试自定义服务器
```
1. 修改"服务器地址"为测试服务器
2. 修改"访问Token"为对应的token
3. 修改"项目名称"为对应的项目
4. 点击"检查更新"
```

#### 场景3：测试不同版本
```
1. 修改"当前版本"为不同的版本号
2. 点击"检查更新"
3. 观察服务器是否返回相应的更新
```

## 技术细节

### 核心类

- **MainActivity** - 应用入口，初始化UI
- **UpgraderTestScreen** - Compose UI组件，显示测试界面
- **Upgrader** - 升级器核心类，处理更新检查和下载

### 异步处理

- 使用 `lifecycleScope.launch` 在后台执行更新检查
- 支持取消加载操作（加载中禁用按钮）
- 实时更新UI状态

### 错误处理

- 网络异常捕获和显示
- 无效参数提示
- 详细的错误堆栈跟踪

## API 集成

### 更新检查请求

```kotlin
val request = UpdateCheckRequest(
    currentVersion = "1.0",
    clientInfo = "msc"
)
```

### 响应字段

```kotlin
data class UpdateCheckResponse(
    val hasUpdate: Boolean,
    val latestVersion: String?,
    val downloadUrl: String?,
    val changelog: String,
    val forceUpdate: Boolean,
    val fileSize: Long?,
    val fileMd5: String?
)
```

## 常见问题

### Q: 如何修改默认的服务器地址？
A: 在 Constants.kt 中修改 `DEFAULT_BASE_URL` 的值

### Q: 如何处理HTTPS证书问题？
A: 检查 ServerCreator.kt 中的信任列表配置

### Q: 如何自定义Token？
A: 在输入字段中直接修改"访问Token"值

## 注意事项

1. **网络权限** - 确保设备已授予网络权限
2. **文件权限** - Android 6+ 需要动态请求文件访问权限
3. **服务器连接** - 确保能连接到指定的服务器
4. **版本号格式** - 版本号应为字符串格式（如"1.0"、"1.0.0"等）

## 后续功能

- [ ] 添加下载功能测试
- [ ] 添加安装功能测试
- [ ] 添加MD5验证功能测试
- [ ] 添加进度跟踪
- [ ] 集成单元测试

