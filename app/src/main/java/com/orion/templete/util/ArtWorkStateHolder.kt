package com.orion.templete.util

import com.orion.templete.data.model.artwork_model.RecommendedArtworkDTO

data class ArtWorkStateHolder(
    val isLoading: Boolean = false,
    val data: RecommendedArtworkDTO? = null,
    val error: String = ""
)