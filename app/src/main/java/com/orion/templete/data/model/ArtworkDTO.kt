package com.orion.templete.data.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ArtworkDTO (
    val description: String? = null,
    val id: String? = null,
    val imageUrl: String? = null,
    val madeWith: String? = null,
    val name: String? = null,
    val releasedDate: String? = null,
    val status: String? = null,
    val storageType: String? = null,
    val type: String? = null
):Parcelable