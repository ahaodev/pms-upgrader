package com.ahao.upgrader

import java.util.concurrent.TimeUnit

object Constants {
    
    const val ACCESS_TOKEN = "PKMS-e6arPcELWAXVQqVb"
    
    const val DEFAULT_BASE_URL = "https://shyy-port.cn:65080"
    
    const val CHECK_UPDATE_ENDPOINT = "/client-access/check"
    
    val TRUSTED_HOSTNAMES = listOf("shyyy-port.cn","47.92.144.106", "192.168.8.15","192.168.99.107")
    
    const val CONNECTION_TIMEOUT = 300L
    const val READ_TIMEOUT = 300L
    const val WRITE_TIMEOUT = 300L
    val TIMEOUT_UNIT = TimeUnit.SECONDS
    
    const val BUFFER_SIZE = 8192
    
    const val DEFAULT_CLIENT_INFO = "MSC Android App"
    
    const val DEFAULT_APK_FILENAME = "update.apk"
    
    const val FILEPROVIDER_AUTHORITY_SUFFIX = ".provider"
    
    const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    
    const val DOWNLOADS_DIR_NAME = "downloads"
    
    const val APK_FILE_EXTENSION = ".apk"
    
    const val MD5_ALGORITHM = "MD5"
    
    const val HEX_FORMAT = "%02x"
}