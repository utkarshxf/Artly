package com.orion.templete.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.artist_model.SearchArtistResponse
import com.orion.templete.domain.repository.ArtistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class SearchScreenViewModel @Inject constructor(
    private val artistRepository: ArtistRepository
) : ViewModel() {

    // Flow to hold search results
    private val _searchResults = MutableStateFlow<List<SearchArtistResponse>>(emptyList())
    val searchResults: StateFlow<List<SearchArtistResponse>> = _searchResults

    // Function to search for artists
    fun searchArtist(query: String) {
        viewModelScope.launch {
            artistRepository.searchArtist(query)
                .flowOn(Dispatchers.IO) // Execute on background thread
                .catch { e ->
                    // Handle error if needed
                    _searchResults.value = emptyList()
                }
                .collect { result ->
                    _searchResults.value = result // Emit the results
                }
        }
    }
}
