package com.ahao.upgrader

import kotlinx.serialization.Serializable

@Serializable
class BaseResponse<T> {
    var code: Int = 0
    var msg: String = ""
    var data: T? = null
    fun success(): Boolean {
        return 0 == code
    }

    override fun toString(): String {
        return "BaseResponse(code=$code, msg='$msg', data=$data)"
    }
}