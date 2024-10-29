package com.orion.templete.util

sealed class ResponseStates<out T> {
    object Loading : ResponseStates<Nothing>()
    class Error(val error: String) : ResponseStates<Nothing>()
    class Success<T>(val data: T) : ResponseStates<T>()
}