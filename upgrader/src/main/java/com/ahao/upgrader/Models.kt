package com.ahao.upgrader

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateCheckRequest(
    @SerialName("current_version")
    val currentVersion: String,
    @SerialName("client_info")
    val clientInfo: String,
    @SerialName("project_name")
    val project: String,
    @SerialName("package_name")
    val packageName: String,
)

@Serializable
data class UpdateCheckResponse(
    @SerialName("has_update")
    var hasUpdate: Boolean = false,
    @SerialName("current_version")
    val currentVersion: String? = null,
    @SerialName("latest_version")
    val latestVersion: String? = null,
    @SerialName("download_url")
    val downloadUrl: String? = null,
    @SerialName("changelog")
    var changelog: String = "",
    @SerialName("file_size")
    val fileSize: Long? = null,
    @SerialName("file_hash")
    val fileHash: String? = null,
)

data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val progress: Int
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val response: UpdateCheckResponse) : UpdateState()
    object NoUpdateAvailable : UpdateState()
    data class Downloading(val progress: DownloadProgress) : UpdateState()
    data class DownloadCompleted(val filePath: String) : UpdateState()
    data class Error(val message: String, val throwable: Throwable? = null) : UpdateState()
}