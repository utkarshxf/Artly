package com.orion.templete.data.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Content(
    val description: String,
    val id: String,
    val imageUrl: String,
    val madeWith: String,
    val name: String,
    val releasedDate: String,
    val status: String,
    val storageType: String,
    val type: String,
):Parcelable