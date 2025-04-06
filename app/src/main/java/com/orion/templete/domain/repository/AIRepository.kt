package com.orion.templete.domain.repository

import com.orion.templete.data.model.ai_model.GeneratedImageResponse
import com.orion.templete.util.ResponseStates
import kotlinx.coroutines.flow.Flow

interface AIRepository {
    suspend fun generateImage(
        image: String?,
        prompt: String,
        strength: Float,
        guidanceScale: Float,
        steps: Int,
        seed: String?
    ): Flow<ResponseStates<GeneratedImageResponse>>
}
