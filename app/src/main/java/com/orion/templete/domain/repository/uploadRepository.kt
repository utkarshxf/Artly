package com.orion.templete.domain.repository

import android.net.Uri
import com.orion.templete.data.model.artwork_model.ArtworkUploadDTO
import com.orion.templete.presentation.artwork_upload.GenreItem
import com.orion.templete.util.ResponseStates
import kotlinx.coroutines.flow.Flow

interface uploadRepository {
    /**
     * Upload an image to storage and return the URL
     * @param imageUri The URI of the image to upload
     * @param isCompressed Whether the image is compressed or not
     * @return A Flow emitting ResponseStates wrapping the URL of the uploaded image
     */
    suspend fun uploadImage(imageUri: Uri, isCompressed: Boolean = false): Flow<ResponseStates<String>>

    /**
     * Search for genres matching the given query
     * @param query The search query
     * @return A Flow emitting ResponseStates wrapping a list of GenreItems
     */
    suspend fun searchGenres(query: String): Flow<ResponseStates<List<GenreItem>>>

    /**
     * Create a new genre
     * @param name The name of the genre
     * @return A Flow emitting ResponseStates wrapping the created GenreItem
     */
    suspend fun createGenre(name: String): Flow<ResponseStates<GenreItem>>

    /**
     * Upload artwork details to the server
     * @param artistId The ID of the artist
     * @param artwork The artwork details
     * @return A Flow emitting ResponseStates wrapping the created ArtworkDTO
     */
    suspend fun uploadArtwork(artistId: String, artwork: ArtworkUploadDTO): Flow<ResponseStates<ArtworkUploadDTO>>
}
