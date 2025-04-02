package com.orion.templete.util

import androidx.annotation.DrawableRes

data class ImageWithText(
    @DrawableRes val image: Int,
    val text: String
)