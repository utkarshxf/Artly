package com.orion.templete.presentation.top_artists

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.orion.templete.data.model.artist_model.SearchArtistResponse
import com.orion.templete.data.model.artist_model.TopArtistProjection
import com.orion.templete.domain.repository.ArtistRepository
import com.orion.templete.util.TrackEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TopArtistsViewModel @Inject constructor(
    private val artistRepository: ArtistRepository,
    private val trackEvents: TrackEvents
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    private val _searchResults = MutableStateFlow<List<SearchArtistResponse>>(emptyList())
    val searchResults: StateFlow<List<SearchArtistResponse>> = _searchResults

    val topArtists: StateFlow<PagingData<TopArtistProjection>> = artistRepository.getTopArtistsPaged()
        .cachedIn(viewModelScope)
        .stateIn(viewModelScope, SharingStarted.Lazily, PagingData.empty())

    fun searchArtist(query: String) {
        viewModelScope.launch {
            artistRepository.searchArtist(query)
                .catch { e ->
                    _searchResults.value = emptyList()
                }
                .collect { result ->
                    Log.e("query", result.toString())
                    _searchResults.value = result // Emit the results
                }
        }
    }
    fun artistSearched(name: String?) {
        if (name != null) {
            trackEvents.trackArtistSearched(name)
        }
    }

}
