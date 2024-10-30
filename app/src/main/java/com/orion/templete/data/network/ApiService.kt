package com.orion.templete.data.network


import com.orion.templete.data.model.artist_model.SearchArtistResponse
import com.orion.templete.data.model.artwork_model.RecommendedArtworkDTO
import com.orion.templete.data.model.login_model.LoginResponseDTO
import com.orion.templete.data.model.login_model.TokenRequest
import com.orion.templete.data.model.login_model.User
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.data.model.user_model.UserDetails
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
    suspend fun likeArtwork(@Path("artworkId") artworkId: String, @Path("userId") userId: String): Response<Void>

    @GET("/artist/search")
    suspend fun getAllArtists(
        @Query("query") artistName: String,
        @Query("responseSize") artistId: Int = 18
    ): retrofit2.Response<List<SearchArtistResponse>>

    @GET("/users/getUserByUserId/{userId}")
    suspend fun getUserByUserId(@Path("userId") userId: String): retrofit2.Response<UserDTO>

    @POST("/users")
    suspend fun createUser(@Body request: UserDetails): UserDetails

    companion object {
        var baseurl = "http://20.193.156.98:7040/"
    }
    //this is the change before commit to new branch
}