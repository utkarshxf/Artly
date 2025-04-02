package com.orion.templete.util

import com.orion.templete.data.model.artwork_model.ArtworkDTO

data class ArtWorkStateHolder(
    val isLoading: Boolean = false,
    val data: ArtworkDTO? = null,
    val error: String = ""
)