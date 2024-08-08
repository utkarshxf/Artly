package com.orion.templete.data.network


import androidx.compose.ui.geometry.Offset
import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Param
import com.orion.templete.data.model.ArtworkDTO
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
    suspend fun getAllArtworks(): retrofit2.Response<ArtworkDTO>

    @POST("login")
    suspend fun loginUser(@Body user: User): retrofit2.Response<LoginResponseDTO>

    @POST("check")
    suspend fun verifyUser(@Body token: TokenRequest): retrofit2.Response<Boolean>

    @GET("pagination")
    suspend fun paginationArtwork(@Query("offset") offset: Int ,@Query("pageSize") pageSize: Int):retrofit2.Response<ArtworkDTO>

    companion object {
        var baseurl = "http://10.0.2.2:8080/"
    }
}