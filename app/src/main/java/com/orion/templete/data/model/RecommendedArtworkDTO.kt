package com.orion.templete.data.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RecommendedArtworkDTO(
    val artwork: ArtworkDTO?,
    val userSimilarity: Int?,
    val likes: Int?,
    val noOfComments: Int?,
    val artworkGenre: String?,
    val artistName: String?,
    val artistId: String?
) : Parcelable