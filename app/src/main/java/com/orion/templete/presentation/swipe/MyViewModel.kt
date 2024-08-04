package com.orion.templete.presentation.swipe

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.usecase.GetArtworkUseCase
import com.orion.templete.util.ArtWorkStateHolder
import com.orion.templete.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class MyViewModel @Inject constructor(
    private val getArtworkUseCase: GetArtworkUseCase
) : ViewModel() {
    val artWorkData = mutableStateOf(ArtWorkStateHolder())
    init {
        getAllArtWork()
    }
    private fun getAllArtWork() {
        getArtworkUseCase()
            .onEach {
            when(it){
                is Resource.Loading -> {
                    artWorkData.value = ArtWorkStateHolder(isLoading = true)
                }
                is Resource.Success ->{
                    artWorkData.value = ArtWorkStateHolder(data = it.data)
                }
                is Resource.Error -> {
                    artWorkData.value = ArtWorkStateHolder(error = it.message.toString())
                }
            }
        }.launchIn(viewModelScope)
    }
}


