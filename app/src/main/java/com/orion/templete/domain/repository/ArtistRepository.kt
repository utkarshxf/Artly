package com.orion.templete.domain.repository

import com.orion.templete.data.model.artist_model.SearchArtistResponse
import kotlinx.coroutines.flow.Flow

interface ArtistRepository {
    suspend fun searchArtist(query: String): Flow<List<SearchArtistResponse>>
}
