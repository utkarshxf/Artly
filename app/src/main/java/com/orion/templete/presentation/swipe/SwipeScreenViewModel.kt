package com.orion.templete.presentation.swipe

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.domain.paginator.DefaultPaginator
import com.orion.templete.domain.repository.ArtworkRepository
import com.orion.templete.usecase.GetArtworkUseCase
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class SwipeScreenViewModel @Inject constructor(
    private val repository: ArtworkRepository,
    private val likeArtworkUseCase: GetArtworkUseCase,
    private val secureStorage: SecureStorage
) : ViewModel() {
    val userId = secureStorage.getUserDetails()?.id?:""
    var state by mutableStateOf(ScreenState())
        private set
    var likeArtworkState by mutableStateOf<ResponseStates<Boolean>>(ResponseStates.Loading)
        private set

    private val pagination: DefaultPaginator<Int, ArtworkDTO> = DefaultPaginator(
        initialKey = state.page,
        onLoadUpdated = { isLoading ->
            state = state.copy(isLoading = isLoading)
        },
        onRequest = { nextPage ->
            repository.paginationArtwork(userId =userId , nextPage, 10)
        },
        getNextKey = {
            state.page + 1
        },
        onError = { throwable ->
            state = state.copy(error = throwable?.localizedMessage)
        },
        onSuccess = { items, newKey ->
            state = state.copy(
                items = items,
                page = newKey,
                endReached = items.isEmpty()
            )
        }
    )

    init {
        loadNextItems()
    }

    fun loadNextItems() {
        viewModelScope.launch {
            try {
                pagination.loadNextItems()
            } catch (e: Exception) {
                Log.e("MyViewModel", "Error loading items", e)
            }
        }
    }

    fun resetPagination() {
        pagination.reset()
        state = ScreenState()
        loadNextItems()
    }

    fun likeArtwork(artworkId: String) {
        viewModelScope.launch {
            likeArtworkUseCase.likeArtwork(artworkId, userId).collect { resource ->
                likeArtworkState = resource
            }
        }
    }
}


data class ScreenState(
    val isLoading: Boolean = false,
    val items: List<ArtworkDTO> = emptyList(),
    val error: String? = null,
    val endReached: Boolean = false,
    val page: Int = 0
)