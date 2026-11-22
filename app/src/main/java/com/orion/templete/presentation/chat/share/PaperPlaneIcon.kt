package com.orion.templete.presentation.chat.share

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Outlined paper plane ("send to a chat"), drawn as a 24dp stroke icon so it matches the app's outlined action icons.
 * The stroke colour is replaced by the tint of the Icon() that shows it.
 */
val PaperPlaneIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "PaperPlane",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Plane outline: nose at the top right, wing tip on the left, tail at the bottom
            moveTo(21.5f, 2.5f)
            lineTo(2.5f, 9.6f)
            lineTo(10.6f, 13.4f)
            lineTo(14.4f, 21.5f)
            close()
            // Centre fold from the nose to the wing root
            moveTo(21.5f, 2.5f)
            lineTo(10.6f, 13.4f)
        }
    }.build()
}
