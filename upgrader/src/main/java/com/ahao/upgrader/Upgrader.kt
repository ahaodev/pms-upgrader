package com.ahao.upgrader

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.awaitResponse
import java.io.File
import java.io.FileOutputStream

class Upgrader private constructor(
    private val context: Context,
    private val accessToken: String,
    private val baseUrl: String,
    private val project: String,
    private val packageName: String,
) {
    private val TAG = Upgrader::class.java.simpleName
    private val updateApi = Service.createUpdateApi(baseUrl, accessToken)
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("x-access-token", accessToken)
                .build()
            chain.proceed(request)
        }
        .build()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    /**
     * 检查更新
     */
    suspend fun checkUpdate(
        currentVersionCode: String,
        clientInfo: String = Constants.DEFAULT_CLIENT_INFO,
    ) {
        _updateState.value = UpdateState.Checking

        try {
            val request = UpdateCheckRequest(
                currentVersion = currentVersionCode,
                clientInfo = clientInfo,
                project = project,
                packageName = packageName,
            )

            val response = updateApi.checkUpdate(request).awaitResponse()
            if (response.isSuccessful) {
                val body = response.body()
                val updateResponse = body?.data
                if (updateResponse != null) {
                    if (updateResponse.hasUpdate && !updateResponse.downloadUrl.isNullOrEmpty()) {
                        _updateState.value = UpdateState.UpdateAvailable(updateResponse)
                    } else {
                        _updateState.value = UpdateState.NoUpdateAvailable
                    }
                } else {
                    _updateState.value = UpdateState.Error("更新检查返回空数据")
                }
            } else {
                _updateState.value = UpdateState.Error("更新检查失败: ${response.code()}")
            }
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("更新检查异常: ${e.message}", e)
        }
    }

    /**
     * 下载APK文件
     */
    suspend fun downloadApk(downloadUrl: String, fileName: String = Constants.DEFAULT_APK_FILENAME): String? {
        Log.d(TAG, downloadUrl)
        return withContext(Dispatchers.IO) {
            try {
                Utils.cleanOldApkFiles(context)

                val downloadDir = Utils.getDownloadDir(context)
                val apkFile = File(downloadDir, fileName)

                val fullDownloadUrl = baseUrl + downloadUrl
                Log.d(TAG, "完整下载URL: $fullDownloadUrl")

                val request = Request.Builder()
                    .url(fullDownloadUrl)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    _updateState.value = UpdateState.Error("下载失败: ${response.code}")
                    return@withContext null
                }

                val responseBody = response.body
                if (responseBody == null) {
                    _updateState.value = UpdateState.Error("下载内容为空")
                    return@withContext null
                }

                val totalBytes = responseBody.contentLength()
                var downloadedBytes = 0L

                responseBody.byteStream().use { inputStream ->
                    FileOutputStream(apkFile).use { outputStream ->
                        val buffer = ByteArray(Constants.BUFFER_SIZE)
                        var bytesRead: Int

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val progress = if (totalBytes > 0) {
                                ((downloadedBytes * 100) / totalBytes).toInt()
                            } else {
                                -1
                            }

                            _updateState.value = UpdateState.Downloading(
                                DownloadProgress(downloadedBytes, totalBytes, progress)
                            )
                        }
                    }
                }

                Log.d(TAG, "APK下载完成: ${apkFile.absolutePath}")
                _updateState.value = UpdateState.DownloadCompleted(apkFile.absolutePath)
                apkFile.absolutePath
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error("下载异常: ${e.message}", e)
                null
            }
        }
    }

    /**
     * 验证下载的APK文件
     */
    fun verifyApkFile(filePath: String, expectedMd5: String?): Boolean {
        if (expectedMd5.isNullOrEmpty()) return true

        val file = File(filePath)
        if (!file.exists()) return false

        val actualMd5 = Utils.calculateMD5(file)
        return actualMd5?.equals(expectedMd5, ignoreCase = true) == true
    }

    /**
     * 安装APK
     */
    fun installApk(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            Utils.installApk(context, file)
        } else {
            _updateState.value = UpdateState.Error("APK文件不存在")
        }
    }

    /**
     * 执行完整的更新流程
     */
    suspend fun performUpdate(
        currentVersion: String,
        clientInfo: String = Constants.DEFAULT_CLIENT_INFO,
        autoInstall: Boolean = true,
    ) {
        checkUpdate(currentVersion, clientInfo)

        val currentState = _updateState.value
        if (currentState !is UpdateState.UpdateAvailable) return

        val updateResponse = currentState.response
        val downloadUrl = updateResponse.downloadUrl

        if (downloadUrl.isNullOrEmpty()) {
            _updateState.value = UpdateState.Error("下载链接为空")
            return
        }

        val fileName = "${packageName}_${updateResponse.latestVersion}_update.apk"
        val filePath = downloadApk(downloadUrl, fileName) ?: return

        if (!verifyApkFile(filePath, updateResponse.fileHash)) {
            _updateState.value = UpdateState.Error("APK文件校验失败")
            return
        }

        if (autoInstall) {
            installApk(filePath)
        }
    }

    /**
     * 重置更新状态
     */
    fun resetState() {
        _updateState.value = UpdateState.Idle
    }

    class Builder(private val context: Context) {
        private var accessToken: String = ""
        private var baseUrl: String = ""
        private var project: String = ""
        private var packageName: String = ""

        fun accessToken(token: String) = apply { this.accessToken = token }
        fun baseUrl(url: String) = apply { this.baseUrl = url }
        fun project(project: String) = apply { this.project = project }
        fun packageName(pkg: String) = apply { this.packageName = pkg }

        fun build(): Upgrader {
            require(accessToken.isNotEmpty()) { "accessToken must be provided" }
            require(baseUrl.isNotEmpty()) { "baseUrl must be provided" }
            require(project.isNotEmpty()) { "project must be provided" }
            require(packageName.isNotEmpty()) { "packageName must be provided" }
            return Upgrader(context, accessToken, baseUrl, project, packageName)
        }
    }
}