package com.ahao.upgrader

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ServerCreator {

    /**
     * 初始化OkHttpClient
     * @param client
     */
    fun okHttpClient(
        connectTimeout: Long = 15,
        readTimeout: Long = 60,
        writeTimeout: Long = 60,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY


        val trustedHostnames =
            listOf("localhost","47.92.144.106")

        return OkHttpClient.Builder()
            .hostnameVerifier { hostname, session -> trustedHostnames.containsAll(trustedHostnames) }
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .writeTimeout(writeTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /**
     * 创建服务实例
     * @param url 服务地址
     * @param service 服务接口
     * @param <T>
     * @return 返回服务实例
    </T> */
    fun <T> create(url: String?, service: Class<T>?): T {
        val factory = json.asConverterFactory("application/json".toMediaType())
        val rt = Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient())
            .addConverterFactory(factory)
            .build()
        return rt.create(service)
    }
}