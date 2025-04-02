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
    val id: String? = null
): Parcelable