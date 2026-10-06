package com.orion.templete.data.network


import retrofit2.http.DELETE
import com.orion.templete.data.model.ai_model.GeneratedImageResponse
import com.orion.templete.data.model.artist_model.ArtistDTO
import com.orion.templete.data.model.artist_model.ArtistStatsResponse
import com.orion.templete.data.model.artist_model.SearchArtistResponse
import com.orion.templete.data.model.artist_model.TopArtistsResponse
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.artwork_model.ArtworkStatsResponse
import com.orion.templete.data.model.artwork_model.ArtworkUploadDTO
import com.orion.templete.data.model.artwork_model.comments.CommentRequest
import com.orion.templete.data.model.artwork_model.comments.GetCommentsDTO
import com.orion.templete.data.model.call.CallCancelRequest
import com.orion.templete.data.model.call.CallConfigResponse
import com.orion.templete.data.model.call.CallDeclineRequest
import com.orion.templete.data.model.call.CallSessionResponse
import com.orion.templete.data.model.call.CallStartRequest
import com.orion.templete.data.model.chat.ChatNotifyRequest
import com.orion.templete.data.model.chat.ChatTokenResponse
import com.orion.templete.data.model.favorits.favoritesDTO
import com.orion.templete.data.model.login_model.ForgetPasswordRequest
import com.orion.templete.data.model.login_model.LoginResponseDTO
import com.orion.templete.data.model.login_model.FirebaseAuthRequest
import com.orion.templete.data.model.login_model.FirebaseAuthResponse
import com.orion.templete.data.model.login_model.FirebaseSignupRequest
import com.orion.templete.data.model.login_model.Registration
import com.orion.templete.data.model.login_model.TokenRequest
import com.orion.templete.data.model.login_model.User
import com.orion.templete.data.model.user_model.RegisterArtistRequest
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.data.model.user_model.UserDetails
import com.orion.templete.data.model.user_model.SearchUsersResponse
import com.orion.templete.data.model.user_model.TopUsersResponse
import com.orion.templete.data.model.user_model.TopCreatorsResponse
import com.orion.templete.data.model.UsernameValidationResponse
import okhttp3.ResponseBody
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
    ): Response<Unit>

    @POST("login")
    suspend fun loginUser(@Body user: User): retrofit2.Response<LoginResponseDTO>

    @POST("signup")
    suspend fun signup(@Body user: Registration): retrofit2.Response<LoginResponseDTO>

    @POST("auth/firebase")
    suspend fun firebaseAuth(@Body request: FirebaseAuthRequest): Response<FirebaseAuthResponse>

    @POST("auth/firebase/signup")
    suspend fun firebaseSignup(@Body request: FirebaseSignupRequest): Response<LoginResponseDTO>

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

    // Permanently deletes the signed-in user's account and everything it owns (Play account-deletion policy)
    @DELETE("/account")
    suspend fun deleteMyAccount(): Response<okhttp3.ResponseBody>

    // Search screen: artworks (incl. descriptions), artists and people; type = all | artworks | artists | people
    @GET("/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String,
        @Query("skip") skip: Int,
        @Query("limit") limit: Int
    ): Response<com.orion.templete.data.model.search.SearchResponse>

    @GET("/artist/search")
    suspend fun getAllArtists(
        @Query("query") artistName: String,
        @Query("responseSize") artistId: Int = 18
    ): retrofit2.Response<List<SearchArtistResponse>>

    @GET("/users/getUserByUserId/{userId}")
    suspend fun getUserByUserId(@Path("userId") userId: String): retrofit2.Response<UserDTO>

    @GET("/artist/getArtistByArtistId")
    suspend fun getArtistByArtistId( @Query("userId") currentUserId: String , @Query("artistId") userId: String): retrofit2.Response<ArtistDTO>

    /**
     * Get artist statistics (followers, likes, artworks count)
     */
    @GET("/artist/getArtistStats")
    suspend fun getArtistStats(@Query("artistId") artistId: String): retrofit2.Response<ArtistStatsResponse>

    @POST("/users")
    suspend fun createUser(@Body request: UserDetails): Response<UserDetails>

    @PUT("/users/{userId}")
    suspend fun updateUser(
        @Path("userId") userId: String,
        @Body request: UserDetails
    ): Response<UserDetails>

    /**
     * Search users by username or key. Source of truth is `username`.
     */
    @GET("/users/searchUsers")
    suspend fun searchUsers(
        @Query("key") key: String,
        @Query("limit") limit: Int = 20
    ): Response<SearchUsersResponse>

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
     * Update artist profile information
     */
    @PUT("/artist/{artistId}")
    suspend fun updateArtist(
        @Path("artistId") artistId: String,
        @Body request: RegisterArtistRequest
    ): Response<RegisterArtistRequest>

    /**
     * Get current artist details
     */
    @GET("/artist/{artistId}")
    suspend fun getArtistDetails(
        @Path("artistId") artistId: String
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

    /**
     * Get artwork statistics (likes, comments count)
     */
    @GET("/artwork/{artworkId}/stats")
    suspend fun getArtworkStats(
        @Path("artworkId") artworkId: String
    ): Response<ArtworkStatsResponse>

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
     * Validate username availability
     */
    @GET("isValidUsername")
    suspend fun validateUsername(@Query("username") username: String): Response<UsernameValidationResponse>

    /**
     * Get top viewers leaderboard
     */
    @GET("/users/leaderboard/top-viewers")
    suspend fun getTopViewers(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<TopUsersResponse>

    /**
     * Get top creators leaderboard
     */
    @GET("/users/leaderboard/top-creators")
    suspend fun getTopCreators(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<TopCreatorsResponse>

    /**
     * Get top artists leaderboard
     */
    @GET("/artist/leaderboard/top-artists")
    suspend fun getTopArtists(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<TopArtistsResponse>

    /**
     * Chat: Firebase custom token for the signed-in user (uid == username).
     * 503 {"message":"Chat is not configured yet","status":false} while the server has no Firebase credentials.
     */
    @POST("chat/token")
    suspend fun chatToken(): Response<ChatTokenResponse>

    /**
     * Chat: ask the backend to push a just-written message to the recipient's devices
     */
    @POST("chat/notify")
    suspend fun chatNotify(@Body request: ChatNotifyRequest): Response<ResponseBody>

    /**
     * Calls (1:1 audio / video in chat, Agora): whether the server can place calls, and the public App ID.
     * Also wakes a sleeping backend.
     */
    @GET("chat/call/config")
    suspend fun callConfig(): Response<CallConfigResponse>

    /**
     * Calls: create the call ("ringing") and push it to the callee. Answers with the channel and the caller's token.
     * 404 unknown callee, 503 {"message":"Calls are not configured yet","status":false}.
     */
    @POST("chat/call/start")
    suspend fun callStart(@Body request: CallStartRequest): Response<CallSessionResponse>

    /**
     * Calls: the callee picks up ("ringing" -> "accepted"). Answers with the channel and the callee's token;
     * 409 (+ "callStatus") when the call is already over.
     */
    @POST("chat/call/{callId}/accept")
    suspend fun callAccept(@Path("callId") callId: String): Response<CallSessionResponse>

    /**
     * Calls: a fresh token for the same channel and uid (the first one is valid for an hour)
     */
    @POST("chat/call/{callId}/token")
    suspend fun callToken(@Path("callId") callId: String): Response<CallSessionResponse>

    /**
     * Calls: decline / cancel / end. The body of the answer is only informative, so it is read leniently.
     */
    @POST("chat/call/{callId}/decline")
    suspend fun callDecline(
        @Path("callId") callId: String,
        @Body request: CallDeclineRequest
    ): Response<ResponseBody>

    @POST("chat/call/{callId}/cancel")
    suspend fun callCancel(
        @Path("callId") callId: String,
        @Body request: CallCancelRequest
    ): Response<ResponseBody>

    @POST("chat/call/{callId}/end")
    suspend fun callEnd(@Path("callId") callId: String): Response<ResponseBody>

    companion object {
        var baseurl = "https://artly-backend.azurewebsites.net/"
//        var baseurl = "https://pseudointernational-taillessly-rachell.ngrok-free.dev/"
//        var baseurl = "https://backendart-production.up.railway.app/"
//        var baseurl = "http://10.0.2.2:7040"
    }
    // this is the change before commit to new branch
}
