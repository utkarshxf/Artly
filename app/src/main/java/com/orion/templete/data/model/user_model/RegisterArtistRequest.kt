package com.orion.templete.data.model.user_model

data class RegisterArtistRequest(
    val id: String? = null,
    val name: String? = null,
    val birth_date: String? = null,
    val death_date: String? = null,
    val nationality: String? = null,
    val notable_works: String? = null,
    val art_movement: String? = null,
    val education: String? = null,
    val awards: String? = null,
    val image_url: String? = null,
    val wikipedia_url: String? = null,
    val description: String? = null
)
