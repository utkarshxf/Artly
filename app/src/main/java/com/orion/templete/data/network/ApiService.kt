package com.orion.templete.data.network


import com.orion.templete.data.model.ai_model.GeneratedImageResponse
import com.orion.templete.data.model.artist_model.ArtistDTO
import com.orion.templete.data.model.artist_model.SearchArtistResponse
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.artwork_model.ArtworkUploadDTO
import com.orion.templete.data.model.artwork_model.comments.CommentRequest
import com.orion.templete.data.model.artwork_model.comments.GetCommentsDTO
import com.orion.templete.data.model.favorits.favoritesDTO
import com.orion.templete.data.model.login_model.ForgetPasswordRequest
import com.orion.templete.data.model.login_model.LoginResponseDTO
import com.orion.templete.data.model.login_model.Registration
import com.orion.templete.data.model.login_model.TokenRequest
import com.orion.templete.data.model.login_model.User
import com.orion.templete.data.model.user_model.RegisterArtistRequest
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.data.model.user_model.UserDetails
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("artwork")
    suspend fun getAllArtworks(): retrofit2.Response<ArtworkDTO>

    /**
     * Search genres by name
     */
    @GET("genres")
    suspend fun searchGenres(@Query("genreName") genreName: String): Response<List<com.orion.templete.presentation.artwork_upload.GenreItem>>

    /**
     * Get all genres
     */
    @GET("genres/all")
    suspend fun getAllGenres(): Response<List<com.orion.templete.presentation.artwork_upload.GenreItem>>

    /**
     * Create a new genre
     */
    @POST("genres")
    suspend fun createGenre(@Body genre: com.orion.templete.presentation.artwork_upload.GenreItem): Response<com.orion.templete.presentation.artwork_upload.GenreItem>

    /**
     * Upload artwork
     */
    @POST("artwork/artist/{artistId}")
    suspend fun uploadArtwork(
        @Path("artistId") artistId: String,
        @Body artwork: ArtworkUploadDTO
    ): Response<ArtworkUploadDTO>

    @POST("login")
    suspend fun loginUser(@Body user: User): retrofit2.Response<LoginResponseDTO>

    @POST("signup")
    suspend fun signup(@Body user: Registration): retrofit2.Response<LoginResponseDTO>

    @POST("check")
    suspend fun verifyUser(@Body token: TokenRequest): retrofit2.Response<Boolean>

    @PUT("forgetPassword")
    suspend fun forgetPassword(@Body request: ForgetPasswordRequest): retrofit2.Response<LoginResponseDTO>

    @GET("artwork/recommend")
    suspend fun paginationArtwork(@Query("userId") userId: String ,@Query("skip") skip: Int , @Query("limit") limit:Int):retrofit2.Response<List<ArtworkDTO>>

    @PUT("artwork/user/like/{artworkId}/{userId}")
    suspend fun likeArtwork(@Path("artworkId") artworkId: String, @Path("userId") userId: String): Response<Unit>

    @PUT("artwork/user/unlike/{artworkId}/{userId}")
    suspend fun unLikeArtwork(@Path("artworkId") artworkId: String, @Path("userId") userId: String): Response<Unit>

    @PUT("artwork/user/dislike/{artworkId}/{userId}")
    suspend fun disLikeArtwork(@Path("artworkId") artworkId: String, @Path("userId") userId: String): Response<Unit>

    @GET("/artist/search")
    suspend fun getAllArtists(
        @Query("query") artistName: String,
        @Query("responseSize") artistId: Int = 18
    ): retrofit2.Response<List<SearchArtistResponse>>

    @GET("/users/getUserByUserId/{userId}")
    suspend fun getUserByUserId(@Path("userId") userId: String): retrofit2.Response<UserDTO>

    @GET("/artist/getArtistByArtistId")
    suspend fun getArtistByArtistId( @Query("userId") currentUserId: String , @Query("artistId") userId: String): retrofit2.Response<ArtistDTO>

    @POST("/users")
    suspend fun createUser(@Body request: UserDetails): Response<UserDetails>

    @PUT("/users/{userId}")
    suspend fun updateUser(
        @Path("userId") userId: String,
        @Body request: UserDetails
    ): Response<UserDetails>

    /**
     * Follow an artist
     */
    @PUT("/users/{userId}/artist/{artistId}/follow")
    suspend fun followUser(
        @Path("userId") userId: String,
        @Path("artistId") artistId: String
    ): Response<Unit>


    /**
     * Unfollow an artist
     */
    @PUT("/users/{userId}/artist/{artistId}/unfollow")
    suspend fun unfollowArtist(
        @Path("userId") userId: String,
        @Path("artistId") artistId: String
    ): Response<Unit>

    /**
     * Register user as an artist
     */
    @POST("/artist")
    suspend fun registerAsArtist(
        @Query("userId") currentUserId: String?,
        @Body request: RegisterArtistRequest
    ): Response<RegisterArtistRequest>

    /**
     * Comment on artwork
     */
    @POST("/comments/artwork/{artworkId}/user/{userId}")
    suspend fun commentOnArtwork(
        @Path("userId") userId: String,
        @Path("artworkId") artworkId: String,
        @Body comment: CommentRequest
    ): Response<Unit>

    /**
     * Get All Comment on artwork
     */
    @GET("/comments/artwork/{artworkId}")
    suspend fun getCommentOnArtwork(
        @Path("artworkId") artworkId: String,
    ): Response<List<GetCommentsDTO>>


    /**
     * Save artwork to favorites
     */
    @PUT("/favorites/artwork/add-artwork/{id}/{artworkId}")
    suspend fun saveOnFavorites(
        @Path("id") id: String,
        @Path("artworkId") artworkId: String
    ): Response<Unit>

    /**
     * Get artworks by artist ID
     */
    @GET("/artist/getArtworkByArtistId")
    suspend fun getArtistArtworks(
        @Query("userId") userId:String,
        @Query("artistId") artistId: String
    ): Response<List<ArtworkDTO>>

    @GET("artwork/{userId}/{artworkId}")
    suspend fun getArtworkById(
        @Path("userId") userId: String,
        @Path("artworkId") artworkId: String
    ): Response<ArtworkDTO>

    @GET("favorites/user/{userId}")
    suspend fun getFavoritesByUserId(
        @Path("userId") userId: String
    ): Response<List<favoritesDTO>>

    @POST("favorites/user/{userId}")
    suspend fun createNewFavorites(
        @Path("userId") userId: String,
        @Body favorites: favoritesDTO
    ): Response<favoritesDTO>


    @GET("artwork/popular")
    suspend fun getPopularArtworks(
        @Query("userId") userId: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 4
    ): Response<List<ArtworkDTO>>

    @GET("artwork/new-arrivals")
    suspend fun getNewArtworks(
        @Query("userId") userId: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 4
    ): Response<List<ArtworkDTO>>


    @GET("artwork/recommended-today")
    suspend fun getRecommendedForToday(
        @Query("userId") userId: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 4
    ): Response<List<ArtworkDTO>>

    @GET("artwork/today-biggest-hit")
    suspend fun getTodayBiggestHit(
    ): Response<ArtworkDTO>

    @GET("favorites/artworks/{favoriteId}")
    suspend fun getArtworkByFavoriteId(
        @Path("favoriteId") favoriteId: String
    ): Response<List<ArtworkDTO>>

    /**
     * Check if a user is an artist
     */
    @GET("users/isUserIsArtistByUserId/{userId}")
    suspend fun isUserArtist(
        @Path("userId") userId: String
    ): Response<Boolean>

    @GET("artist/getArtistByArtworkId")
    suspend fun getArtistByArtworkId(
        @Query("userId") userId: String,
        @Query("artworkId") artworkId: String
    ): Response<ArtistDTO>


    @GET("artwork/similarGenreArtworks")
    suspend fun similarGenreArtworks(
        @Query("currentArtworkId") artworkId: String,
        @Query("userId") userId: String
    ): Response<List<ArtworkDTO>>

    @GET("artwork/moreFromArtist")
    suspend fun moreFromArtist(
        @Query("userId") userId: String,
        @Query("currentArtworkId") currentArtworkId: String,
        @Query("artistId") artistId: String
    ): Response<List<ArtworkDTO>>


    /**
     * Generates an image based on a prompt and optional source image
     *
     * @param image Base64 encoded image data or URL to image (optional)
     * @param prompt Text prompt to guide the image generation
     * @param strength How much to preserve of the original image (0.0-1.0)
     * @param guidanceScale How closely to follow the prompt (higher = more faithful)
     * @param steps Number of inference steps (higher = more detail but slower)
     * @param seed Random seed for reproducible results (optional)
     * @return Response containing the generated image
     */

    @POST("https://your-image-generation-api.com/generate_image")
    @FormUrlEncoded
    suspend fun generateImage(
        @Field("image") image: String? = null,
        @Field("prompt") prompt: String,
        @Field("strength") strength: Float = 0.75f,
        @Field("guidance_scale") guidanceScale: Float = 7.5f,
        @Field("steps") steps: Int = 50,
        @Field("seed") seed: String? = null
    ): Response<GeneratedImageResponse>

    companion object {
//        var baseurl = "https://backendart-production.up.railway.app/"
        var baseurl = "https://hammerhead-app-zgpcv.ondigitalocean.app/"
//        var baseurl = "http://10.0.2.2:7040"
    }
    // this is the change before commit to new branch
}
