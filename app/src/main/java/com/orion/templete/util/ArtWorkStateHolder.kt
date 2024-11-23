package com.orion.templete.util

import com.orion.templete.data.model.artwork_model.ArtworkDetailsDTO

data class ArtWorkStateHolder(
    val isLoading: Boolean = false,
    val data: ArtworkDetailsDTO? = null,
    val error: String = ""
)