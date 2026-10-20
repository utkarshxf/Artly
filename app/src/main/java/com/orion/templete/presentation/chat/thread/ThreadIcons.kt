package com.orion.templete.presentation.chat.thread

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// Material icons the thread needs that are not in material-icons-core (keeps the screen independent of the
// extended icon artifact). Path data from Material Symbols, 24 x 24.
object ThreadIcons {

    val Reply: ImageVector by lazy {
        icon("Thread.Reply", autoMirror = true, "M10,9V5l-7,7 7,7v-4.1c5,0 8.5,1.6 11,5.1 -1,-5 -4,-10 -11,-11z")
    }

    val Copy: ImageVector by lazy {
        icon(
            "Thread.Copy",
            autoMirror = false,
            "M16,1H4C2.9,1 2,1.9 2,3v14h2V3h12V1zM19,5H8C6.9,5 6,5.9 6,7v14c0,1.1 0.9,2 2,2h11c1.1,0 2,-0.9 2,-2V7C21,5.9 20.1,5 19,5zM19,21H8V7h11V21z",
        )
    }

    val Unsend: ImageVector by lazy {
        icon(
            "Thread.Unsend",
            autoMirror = true,
            "M12.5,8c-2.65,0 -5.05,0.99 -6.9,2.6L2,7v9h9l-3.62,-3.62c1.39,-1.16 3.16,-1.88 5.12,-1.88 3.54,0 6.55,2.31 7.6,5.5l2.37,-0.78C21.08,11.03 17.15,8 12.5,8z",
        )
    }

    val Gallery: ImageVector by lazy {
        icon(
            "Thread.Gallery",
            autoMirror = false,
            "M19,5v14H5V5H19M19,3H5C3.9,3 3,3.9 3,5v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V5C21,3.9 20.1,3 19,3L19,3zM14.14,11.86l-3,3.87L9,13.14L6,17h12L14.14,11.86z",
        )
    }

    val Camera: ImageVector by lazy {
        icon(
            "Thread.Camera",
            autoMirror = false,
            "M12,12m-3.2,0a3.2,3.2 0,1 1,6.4 0a3.2,3.2 0,1 1,-6.4 0",
            "M9,2L7.17,4H4c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V6c0,-1.1 -0.9,-2 -2,-2h-3.17L15,2H9zM12,17c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5z",
        )
    }

    val Error: ImageVector by lazy {
        icon(
            "Thread.Error",
            autoMirror = false,
            "M11,15h2v2h-2zM11,7h2v6h-2zM11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8z",
        )
    }

    val Delete: ImageVector by lazy {
        icon(
            "Thread.Delete",
            autoMirror = false,
            "M16,9v10H8V9h8m-1.5,-6h-5l-1,1H5v2h14V4h-3.5l-1,-1zM18,7H6v12c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7z",
        )
    }

    val Chat: ImageVector by lazy {
        icon(
            "Thread.Chat",
            autoMirror = true,
            "M20,2H4C2.9,2 2.01,2.9 2.01,4L2,22l4,-4h14c1.1,0 2,-0.9 2,-2V4C22,2.9 21.1,2 20,2zM18,14H6v-2h12V14zM18,11H6V9h12V11zM18,8H6V6h12V8z",
        )
    }

    private fun icon(name: String, autoMirror: Boolean, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = autoMirror,
        )
        paths.forEach { data ->
            builder.addPath(pathData = addPathNodes(data), fill = SolidColor(Color.Black))
        }
        return builder.build()
    }
}
