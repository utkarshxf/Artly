package com.orion.templete.util

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

abstract class SafeApiRequest {
    suspend fun <T> safeApiRequest(
        apiRequest: suspend () -> Response<T>
    ): T {
        try {
            return apiRequest().body()!!
        } catch (e: Exception) {
            throw handleApiError(e)
        }
    }

    private fun handleApiError(e: Exception): Exception {
        Log.e("SafeApiRequest", "handleApiError: ${e.message}")
        return when (e) {
            is HttpException -> {
                when (e.code()) {
                    401 -> Exception("Unauthorized")
                    403 -> Exception("Forbidden")
                    404 -> Exception("Not Found")
                    else -> Exception("Something went wrong")
                }
            }
            is IOException -> Exception("Please check your network connection")
            else -> Exception("An unknown error occurred")
        }
    }
}