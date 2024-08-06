package com.orion.templete.data.network


import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Param
import com.orion.templete.data.model.ArtWorkDTO
import com.orion.templete.data.model.LoginResponseDTO
import com.orion.templete.data.model.TokenRequest
import com.orion.templete.data.model.User
import dagger.Provides
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @GET("artwork")
    suspend fun getAllArtworks(): retrofit2.Response<ArtWorkDTO>

    @POST("login")
    suspend fun loginUser(@Body user: User): retrofit2.Response<LoginResponseDTO>

    @POST("check")
    suspend fun verifyUser(@Body token: TokenRequest): retrofit2.Response<Boolean>

    companion object {
        var baseurl = "http://10.0.2.2:8080/"
    }
}