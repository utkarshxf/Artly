package com.orion.templete.presentation.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.orion.templete.data.model.user_model.TopUserProjection
import com.orion.templete.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    val topUsers: Flow<PagingData<TopUserProjection>> =
        userRepository.getTopViewersPaged()
            .cachedIn(viewModelScope)
}


