package com.orion.templete.data.model.ai_model

data class GeneratedImageResponse(
    val image: String, // Base64 encoded image
    val seed: String,  // The seed used (useful for reproducibility)
    val metadata: ImageMetadata? = null
)

data class ImageMetadata(
    val processingTime: Long,
    val model: String,
    val promptStrength: Float
)