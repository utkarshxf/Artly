package com.orion.templete.util

sealed class AuthResultState<out T> {
    data class Success<out R>(val data:R) : AuthResultState<R>()
    data class Failure(val msg:Throwable) : AuthResultState<Nothing>()
    object Loading : AuthResultState<Nothing>()
}