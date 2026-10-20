package com.orion.templete.data.model.artwork_model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ArtworkDTO(
    val title: String? = null,
    val status: String? = null,
    val storageType: String? = null,
    val releasedDate: String? = null,
    val type: String? = null,
    val medium: String? = null,
    val artist: String? = null,
    val description: String? = null,
    val dimensions: String? = null,
    @SerializedName("current_location")
    val currentLocation: String? = null,
    @SerializedName("period_style")
    val periodStyle: String? = null,
    @SerializedName("art_movement")
    val artMovement: String? = null,
    @SerializedName("image_url_compressed")
    val image_url_compressed: String? = null,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("license_info")
    val licenseInfo: String? = null,
    @SerializedName("source_url")
    val sourceUrl: String? = null,
    val liked: Boolean? = null,
    val id: String? = null,
    val genreId: String? = null,
): Parcelable


@Parcelize
data class ArtworkUploadDTO(
    val title: String,
    val imageUrl: String,
    val imageUrlCompressed: String,
    val storageType: String = "Firebase",
    val medium: String? = null,
    val artist: String,
    val artType: String = "IMAGE",
    val genreId: String?=null,
    val description: String? = null,
    val releasedDate: String? = null,
    val releaseYear: Int? = null,
    val dimensions: String? = null,
    val currentLocation: String? = null,
    val periodStyle: String? = null,
    val artMovement: String? = null,
    val licenseInfo: String? = null,
    val sourceUrl: String? = null
) : Parcelable