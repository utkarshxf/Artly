package com.orion.templete.data.model.artwork_model.comments

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime

@RequiresApi(Build.VERSION_CODES.O)
data class CommentRequest(
    val text: String
)