package com.ahao.upgrader

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ServiceApi {
    @POST(Constants.CHECK_UPDATE_ENDPOINT)
    fun checkUpdate(@Body request: UpdateCheckRequest): Call<BaseResponse<UpdateCheckResponse>>
}