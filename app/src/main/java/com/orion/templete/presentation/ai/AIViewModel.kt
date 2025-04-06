package com.orion.templete.presentation.ai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.ai_model.GeneratedImageResponse
import com.orion.templete.domain.repository.AIRepository
import com.orion.templete.domain.repository.UserRepository
import com.orion.templete.presentation.auth.AuthScreenUiState
import com.orion.templete.util.AuthResultState
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AIViewModel @Inject constructor(
    private val aiRepository: AIRepository,
    private val secureStorage: SecureStorage
) : ViewModel() {

    private val _generatedImage = MutableStateFlow<AIScreenUiState>(AIScreenUiState.Initial)
    val generatedImage: StateFlow<AIScreenUiState> = _generatedImage

    fun generateImage(
        image: String? = null,
        prompt: String,
        strength: Float = 0.75f,
        guidanceScale: Float = 7.5f,
        steps: Int = 50,
        seed: String? = null
    ) {
        viewModelScope.launch {
            aiRepository.generateImage(image, prompt, strength, guidanceScale, steps, seed)
                .collect { result ->
                    _generatedImage.value = when (result) {
                        is ResponseStates.Error -> AIScreenUiState.Error(result.error)
                        is ResponseStates.Loading -> AIScreenUiState.Loading
                        is ResponseStates.Success -> AIScreenUiState.Success(result.data)
                    }
                }
        }
    }

    fun reset() {
        _generatedImage.value = AIScreenUiState.Initial
    }
}
sealed interface AIScreenUiState {
    object Initial : AIScreenUiState
    object Loading : AIScreenUiState
    data class Success(val value: GeneratedImageResponse) : AIScreenUiState
    data class Error(val message: String) : AIScreenUiState
}