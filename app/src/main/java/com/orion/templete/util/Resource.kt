package com.orion.templete.util

sealed  class Resource<T>(val data:T?=null,val message:String?=null , val loading:Boolean?=null){

    class  Success<T>(data:T?): Resource<T>(data = data)

    class Loading<T>(loading: Boolean?) : Resource<T>(loading = loading)

    class Error<T>(message:String?) : Resource<T>(message= message)

}