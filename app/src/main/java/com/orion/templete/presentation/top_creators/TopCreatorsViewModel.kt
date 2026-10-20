package com.orion.templete.presentation.top_creators

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.orion.templete.data.model.user_model.TopCreatorProjection
import com.orion.templete.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class TopCreatorsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    val topCreators: Flow<PagingData<TopCreatorProjection>> =
        userRepository.getTopCreatorsPaged()
            .cachedIn(viewModelScope)
}

