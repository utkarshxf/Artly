package com.orion.templete.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.orion.templete.data.model.artwork_model.ArtworkUploadDTO
import com.orion.templete.data.network.ApiService
import com.orion.templete.domain.repository.uploadRepository
import com.orion.templete.presentation.artwork_upload.GenreItem
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SafeApiRequest
import com.orion.templete.util.uploadImage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume

class uploadRepositoryImplementation @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context
): uploadRepository, SafeApiRequest() {

    override suspend fun uploadImage(imageUri: Uri, isCompressed: Boolean): Flow<ResponseStates<String>> = flow {
        emit(ResponseStates.Loading)
        try {
            // Create a folder path based on whether the image is compressed or not
            val folderPath = if (isCompressed) "compressed_images" else "original_images"

            // Use suspendCancellableCoroutine to convert the callback-based uploadImage function to a suspend function
            val downloadUrl = suspendCancellableCoroutine<String> { continuation ->
                // Create a new URI with the folder path
                val newUri = Uri.parse(imageUri.toString())

                // Upload the image to Firebase Storage
                uploadImage(newUri, context) { url ->
                    continuation.resume(url)
                }

                // Handle cancellation
                continuation.invokeOnCancellation {
                    Log.d("UploadRepository", "Image upload cancelled")
                }
            }

            Log.d("UploadRepository", "Image uploaded successfully: $downloadUrl")
            emit(ResponseStates.Success(downloadUrl))
        } catch (e: Exception) {
            Log.e("UploadRepository", "Error uploading image", e)
            emit(ResponseStates.Error("Failed to upload image: ${e.message}"))
        }
    }

    override suspend fun searchGenres(query: String): Flow<ResponseStates<List<GenreItem>>> = flow {
        emit(ResponseStates.Loading)
        try {
            val response = apiService.searchGenres(query)
            if (response.isSuccessful) {
                val genres = response.body() ?: emptyList()
                Log.d("UploadRepository", "Genres search successful: $genres")
                emit(ResponseStates.Success(genres))
            } else {
                Log.e("UploadRepository", "Error searching genres: ${response.message()}")
                emit(ResponseStates.Error("Failed to search genres: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("UploadRepository", "Error searching genres", e)
            emit(ResponseStates.Error("Failed to search genres: ${e.message}"))
        }
    }

    override suspend fun createGenre(name: String): Flow<ResponseStates<GenreItem>> = flow {
        emit(ResponseStates.Loading)
        try {
            // Create a new GenreItem with a temporary ID and key
            // The server will assign the actual ID and key
            val genreItem = GenreItem(
                id = UUID.randomUUID().toString(),
                key = name.lowercase().replace(" ", "-"),
                name = name
            )

            val response = apiService.createGenre(genreItem)
            if (response.isSuccessful) {
                val createdGenre = response.body()
                if (createdGenre != null) {
                    Log.d("UploadRepository", "Genre created successfully: $createdGenre")
                    emit(ResponseStates.Success(createdGenre))
                } else {
                    Log.e("UploadRepository", "Error creating genre: Response body is null")
                    emit(ResponseStates.Error("Failed to create genre: Response body is null"))
                }
            } else {
                Log.e("UploadRepository", "Error creating genre: ${response.message()}")
                emit(ResponseStates.Error("Failed to create genre: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("UploadRepository", "Error creating genre", e)
            emit(ResponseStates.Error("Failed to create genre: ${e.message}"))
        }
    }

    override suspend fun uploadArtwork(artistId: String, artwork: ArtworkUploadDTO): Flow<ResponseStates<ArtworkUploadDTO>> = flow {
        emit(ResponseStates.Loading)
        try {
            val response = apiService.uploadArtwork(artistId, artwork)
            if (response.isSuccessful) {
                val uploadedArtwork = response.body()
                if (uploadedArtwork != null) {
                    Log.d("UploadRepository", "Artwork uploaded successfully: $uploadedArtwork")
                    emit(ResponseStates.Success(uploadedArtwork))
                } else {
                    Log.e("UploadRepository", "Error uploading artwork: Response body is null")
                    emit(ResponseStates.Error("Failed to upload artwork: Response body is null"))
                }
            } else {
                Log.e("UploadRepository", "Error uploading artwork: ${response.message()}")
                emit(ResponseStates.Error("Failed to upload artwork: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("UploadRepository", "Error uploading artwork", e)
            emit(ResponseStates.Error("Failed to upload artwork: ${e.message}"))
        }
    }
}
