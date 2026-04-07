package com.ahao.upgrader

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest

object Utils {
    val TAG = Utils::class.java.simpleName
    /**
     * 安装APK文件
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            Log.d(TAG,"准备安装APK: ${apkFile.absolutePath}")
            Log.d(TAG,"APK文件存在: ${apkFile.exists()}")
            Log.d(TAG,"APK文件大小: ${apkFile.length()} bytes")

            if (!apkFile.exists()) {
                Log.e(TAG,"APK文件不存在: ${apkFile.absolutePath}")
                return
            }

            if (apkFile.length() == 0L) {
                Log.e(TAG,"APK文件为空: ${apkFile.absolutePath}")
                return
            }

            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val authority = "${context.packageName}${Constants.FILEPROVIDER_AUTHORITY_SUFFIX}"
                Log.d(TAG,"FileProvider authority: $authority")

                val fileUri = FileProvider.getUriForFile(context, authority, apkFile)
                Log.d(TAG,"生成的FileProvider URI: $fileUri")
                fileUri
            } else {
                val fileUri = Uri.fromFile(apkFile)
                Log.d(TAG,"生成的File URI: $fileUri")
                fileUri
            }

            intent.setDataAndType(uri, Constants.APK_MIME_TYPE)
            Log.d(TAG,"启动安装Intent")
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG,"安装APK时发生异常", e)
            e.printStackTrace()
        }
    }

    /**
     * 计算文件MD5值
     */
    fun calculateMD5(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance(Constants.MD5_ALGORITHM)
            file.inputStream().use { inputStream ->
                val buffer = ByteArray(Constants.BUFFER_SIZE)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { Constants.HEX_FORMAT.format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取应用下载目录 - 根据Android版本选择合适的存储策略
     */
    fun getDownloadDir(context: Context): File {
        return when {
            // Android 10+ (API 29+): 使用分区存储，优先使用应用私有外部目录
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // 使用应用的外部缓存目录，系统安装器可以访问
                val cacheDir = context.externalCacheDir ?: context.cacheDir
                File(cacheDir, Constants.DOWNLOADS_DIR_NAME).apply {
                    if (!exists()) mkdirs()
                    Log.d(TAG,"Android 10+ 使用缓存目录: $absolutePath")
                }
            }

            // Android 7-9 (API 24-28): 尝试使用公共Downloads目录
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> {
                try {
                    val publicDownloadsDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val downloadDir = File(publicDownloadsDir, "MSC")

                    if (publicDownloadsDir != null && publicDownloadsDir.exists()) {
                        downloadDir.apply {
                            if (!exists()) mkdirs()
                            Log.d(TAG,"Android 7-9 使用公共Downloads目录: $absolutePath")
                        }
                    } else {
                        // 回退到应用私有目录
                        File(
                            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                            Constants.DOWNLOADS_DIR_NAME
                        ).apply {
                            if (!exists()) mkdirs()
                            Log.d(TAG,"回退到应用私有目录: $absolutePath")
                        }
                    }
                } catch (e: Exception) {
                    // 如果出错，使用应用私有目录
                    File(
                        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                        Constants.DOWNLOADS_DIR_NAME
                    ).apply {
                        if (!exists()) mkdirs()
                        Log.d(TAG,"异常回退到应用私有目录: $absolutePath")
                    }
                }
            }

            // Android 6及以下: 使用传统外部存储
            else -> {
                val downloadDir = File(Environment.getExternalStorageDirectory(), "Download/MSC")
                downloadDir.apply {
                    if (!exists()) mkdirs()
                    Log.d(TAG,"Android 6及以下使用传统外部存储: $absolutePath")
                }
            }
        }
    }

    /**
     * 删除旧的APK文件
     */
    fun cleanOldApkFiles(context: Context) {
        try {
            val downloadDir = getDownloadDir(context)
            downloadDir.listFiles { _, name -> name.endsWith(Constants.APK_FILE_EXTENSION) }
                ?.forEach { file ->
                    file.delete()
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 格式化文件大小
     */
    fun formatFileSize(sizeInBytes: Long): String {
        val kb = sizeInBytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> "$sizeInBytes B"
        }
    }
}