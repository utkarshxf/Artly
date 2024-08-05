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
    //    after ? everything represent query
    @GET("artwork")
    suspend fun getAllArtworks(
        @Header("Authorization") authHeader: String = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzdHJpbmciLCJpYXQiOjE3MjI3ODI4NzQsImV4cCI6MTcyMjc4NTg3NH0.1I2EObf2N_V10F1QRV29TNhyQDJdFgg7j06LQZxMnuM"
    ): retrofit2.Response<ArtWorkDTO>

    @POST("login")
    suspend fun loginUser(@Body user: User): retrofit2.Response<LoginResponseDTO>

    @POST("check")
    suspend fun verifyUser(@Body token: TokenRequest =TokenRequest( "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzdHJpbmciLCJpYXQiOjE3MjI4MTk4MTgsImV4cCI6MTcyMjgyMjgxOH0.NAr1R-mp72ejSfKA4sUTs6yyLLcOOsmGvCRs1hi2ZVU" )): retrofit2.Response<Boolean>

    companion object {
        var baseurl = "http://10.0.2.2:8080/"
    }
}