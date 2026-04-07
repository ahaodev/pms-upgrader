package com.ahao.upgrader

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object Service {

    private fun createOkHttpClient(accessToken: String): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("x-access-token", accessToken)
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(Constants.CONNECTION_TIMEOUT, Constants.TIMEOUT_UNIT)
            .readTimeout(Constants.READ_TIMEOUT, Constants.TIMEOUT_UNIT)
            .writeTimeout(Constants.WRITE_TIMEOUT, Constants.TIMEOUT_UNIT)
            .build()
    }

    fun createUpdateApi(baseUrl: String, accessToken: String): ServiceApi {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createOkHttpClient(accessToken))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ServiceApi::class.java)
    }
}