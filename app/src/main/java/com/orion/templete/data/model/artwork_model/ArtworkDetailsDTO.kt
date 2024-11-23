package com.orion.templete.data.model.artwork_model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ArtworkDetailsDTO(
    val artwork: ArtworkDTO?,
    val userSimilarity: Int?,
    val likes: Int?,
    val noOfComments: Int?,
    val artworkGenre: String?,
    val artistName: String?,
    val artistId: String?
) : Parcelable