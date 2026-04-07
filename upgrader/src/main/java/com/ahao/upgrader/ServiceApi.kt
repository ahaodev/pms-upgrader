package com.ahao.upgrader

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ServiceApi {
    @POST(Constants.CHECK_UPDATE_ENDPOINT)
    @retrofit2.http.Headers("x-access-token: ${Constants.ACCESS_TOKEN}")
    fun checkUpdate(@Body request: UpdateCheckRequest): Call<BaseResponse<UpdateCheckResponse>>

}