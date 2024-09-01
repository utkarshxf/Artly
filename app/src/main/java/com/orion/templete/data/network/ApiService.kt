package com.orion.templete.data.network


import com.orion.templete.data.model.RecommendedArtworkDTO
import com.orion.templete.data.model.LoginResponseDTO
import com.orion.templete.data.model.TokenRequest
import com.orion.templete.data.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("artwork")
    suspend fun getAllArtworks(): retrofit2.Response<RecommendedArtworkDTO>

    @POST("login")
    suspend fun loginUser(@Body user: User): retrofit2.Response<LoginResponseDTO>

    @POST("signup")
    suspend fun signup(@Body user: User): retrofit2.Response<User>

    @POST("check")
    suspend fun verifyUser(@Body token: TokenRequest): retrofit2.Response<Boolean>

    @GET("artwork/recommend")
    suspend fun paginationArtwork(@Query("userId") userId: String ,@Query("skip") skip: Int , @Query("limit") limit:Int):retrofit2.Response<List<RecommendedArtworkDTO>>

    @PUT("artwork/user/like/{artworkId}/{userId}")
    suspend fun likeArtwork(
        @Path("artworkId") artworkId: String,
        @Path("userId") userId: String
    ): Response<Void>

    companion object {
        var baseurl = "http://10.0.2.2:7040/"
    }
}