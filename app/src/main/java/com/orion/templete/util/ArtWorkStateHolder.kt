package com.orion.templete.util

import com.orion.templete.data.model.RecommendedArtworkDTO

data class ArtWorkStateHolder(
    val isLoading: Boolean = false,
    val data: RecommendedArtworkDTO? = null,
    val error: String = ""
)