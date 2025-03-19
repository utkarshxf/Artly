package com.orion.templete.data.model.artist_model

data class ArtistDTO(
    val art_movement: String,
    val awards: String,
    val birth_date: String,
    val death_date: String,
    val description: String,
    val education: String,
    val id: String,
    val image_url: String,
    val name: String,
    val nationality: String,
    val notable_works: String,
    val wikipedia_url: String,
    val follow: Boolean
)