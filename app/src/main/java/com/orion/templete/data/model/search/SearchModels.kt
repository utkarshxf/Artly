package com.orion.templete.data.model.search

// GET /search: artworks (title, artist, medium, movement or description matched), artists and people
data class SearchResponse(
    val artworks: List<SearchArtworkHit>? = null,
    val artists: List<SearchArtistHit>? = null,
    val people: List<SearchPersonHit>? = null,
)

// snippet: excerpt of the description when that is where the words matched
data class SearchArtworkHit(
    val id: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val imageUrl: String? = null,
    val medium: String? = null,
    val year: String? = null,
    val snippet: String? = null,
)

// username: the Artistry account of an artist who is also a user
data class SearchArtistHit(
    val id: String? = null,
    val name: String? = null,
    val imageUrl: String? = null,
    val nationality: String? = null,
    val artMovement: String? = null,
    val username: String? = null,
)

// artistId: set when this person is also an artist
data class SearchPersonHit(
    val username: String? = null,
    val name: String? = null,
    val profilePicture: String? = null,
    val artistId: String? = null,
)
