package com.orion.templete.data.model.story_model

/**
 * Story text for one artwork in one language, from GET /story/{artworkId}?lang=.
 * The backend translates on demand (Gemini) and caches per artwork + language.
 */
data class StoryTextResponse(
    val artworkId: String,
    val language: String,
    val text: String
)
