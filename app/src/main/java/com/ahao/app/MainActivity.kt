package com.ahao.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.ahao.app.ui.theme.PmsupgraderTheme
import com.ahao.upgrader.Upgrader
import com.ahao.upgrader.UpdateState
import kotlinx.coroutines.launch

const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            PmsupgraderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    UpgraderTestScreen(
                        context = this@MainActivity,
                        modifier = Modifier.padding(innerPadding),
                        lifecycleScope = lifecycleScope
                    )
                }
            }
        }
    }
}

@Composable
fun UpgraderTestScreen(
    context: android.content.Context,
    modifier: Modifier = Modifier,
    lifecycleScope: androidx.lifecycle.LifecycleCoroutineScope
) {
    val appVersion = remember {
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    info.longVersionCode
                } else {
                    @Suppress("DEPRECATION") info.versionCode.toLong()
                }
            "v${info.versionName} ($versionCode)"
        } catch (e: Exception) {
            "unknown"
        }
    }

    var baseUrl by remember { mutableStateOf("https://ppr.ahaodev.com") }
    var accessToken by remember { mutableStateOf("kZLdTvr2VVyUpaEy") }
    var projectName by remember { mutableStateOf("Test") }
    var packageName by remember { mutableStateOf("debug") }
    var currentVersion by remember { mutableStateOf("1.0") }

    var upgrader by remember { mutableStateOf<Upgrader?>(null) }
    val updateState by remember(upgrader) {
        upgrader?.updateState ?: kotlinx.coroutines.flow.MutableStateFlow(UpdateState.Idle)
    }.collectAsState()
    var isLoading by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Title
        Text(
            text = "升级测试工具",
            style = MaterialTheme.typography.headlineSmall,
        )

        // Input Fields
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("服务器地址") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )

        OutlinedTextField(
            value = accessToken,
            onValueChange = { accessToken = it },
            label = { Text("访问Token") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            maxLines = 1
        )

        OutlinedTextField(
            value = projectName,
            onValueChange = { projectName = it },
            label = { Text("项目名称") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )

        OutlinedTextField(
            value = packageName,
            onValueChange = { packageName = it },
            label = { Text("应用包名") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )

        OutlinedTextField(
            value = currentVersion,
            onValueChange = { currentVersion = it },
            label = { Text("当前版本") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )

        Divider()

        // Check Update Button
        Button(
            onClick = {
                isLoading = true
                resultMessage = "正在检查更新..."
                lifecycleScope.launch {
                    try {
                        val newUpgrader = Upgrader.Builder(context)
                            .accessToken(accessToken)
                            .baseUrl(baseUrl)
                            .project(projectName)
                            .packageName(packageName)
                            .build()
                        upgrader = newUpgrader
                        newUpgrader.checkUpdate(currentVersion)

                        resultMessage = when (val state = newUpgrader.updateState.value) {
                            is UpdateState.UpdateAvailable -> {
                                val response = state.response
                                buildString {
                                    append("✓ 发现新版本\n")
                                    append("版本: ${response.latestVersion}\n")
                                    if (!response.downloadUrl.isNullOrEmpty()) {
                                        append("下载链接: ${response.downloadUrl}\n")
                                    }
                                    if (response.fileSize != null) {
                                        append("文件大小: ${response.fileSize} bytes\n")
                                    }
                                    append("更新日志: ${response.changelog}")
                                }
                            }

                            is UpdateState.NoUpdateAvailable -> "✓ 已是最新版本"
                            is UpdateState.Error -> "✗ 错误: ${state.message}"
                            else -> "状态: $state"
                        }
                    } catch (e: Exception) {
                        resultMessage = "✗ 异常: ${e.message}\n${e.stackTraceToString()}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("检查更新")
            }
        }

        Divider()

        // Status/Result Display
        if (resultMessage.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = resultMessage,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Update State Display
        when (updateState) {
            is UpdateState.Checking -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("正在检查更新...", color = MaterialTheme.colorScheme.primary)
            }

            is UpdateState.UpdateAvailable -> {
                val response = (updateState as UpdateState.UpdateAvailable).response
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("✓ 有新版本可用", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "版本: ${response.latestVersion}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (response.fileSize != null) {
                            Text(
                                "大小: ${response.fileSize} bytes",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (response.fileHash != null) {
                            Text(
                                "Hash: ${response.fileHash}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (response.changelog.isNotEmpty()) {
                            Text(
                                "更新日志: ${response.changelog}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(
                            onClick = {
                                val currentUpgrader = upgrader ?: return@Button
                                val downloadUrl = response.downloadUrl ?: return@Button
                                isDownloading = true
                                lifecycleScope.launch {
                                    try {
                                        val fileName = "${packageName}_${response.latestVersion}_update.apk"
                                        val filePath = currentUpgrader.downloadApk(downloadUrl, fileName)
                                        if (filePath != null) {
                                            val isValid = currentUpgrader.verifyApkFile(filePath, response.fileHash)
                                            if (isValid) {
                                                currentUpgrader.installApk(filePath)
                                            } else {
                                                resultMessage = "✗ APK文件校验失败"
                                            }
                                        }
                                    } catch (e: Exception) {
                                        resultMessage = "✗ 下载异常: ${e.message}"
                                    } finally {
                                        isDownloading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isDownloading
                        ) {
                            Text("立即更新")
                        }
                    }
                }
            }

            is UpdateState.Downloading -> {
                val progress = (updateState as UpdateState.Downloading).progress
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("正在下载更新...", style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(
                        progress = { if (progress.progress >= 0) progress.progress / 100f else 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        if (progress.progress >= 0) "下载进度: ${progress.progress}%" else "下载中...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            is UpdateState.DownloadCompleted -> {
                val filePath = (updateState as UpdateState.DownloadCompleted).filePath
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "✓ 下载完成，正在安装...",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { upgrader?.installApk(filePath) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("手动安装")
                        }
                    }
                }
            }

            is UpdateState.NoUpdateAvailable -> {
                Text("✓ 已是最新版本", color = MaterialTheme.colorScheme.tertiary)
            }

            is UpdateState.Error -> {
                val error = (updateState as UpdateState.Error)
                Log.d(TAG, "UpgraderTestScreen: 错误: ${error.message}", error.throwable)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "✗ 错误: ${error.message}",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            else -> {}
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PmsupgraderTheme {
        // Preview不支持Context，这里仅显示布局结构
    }
}