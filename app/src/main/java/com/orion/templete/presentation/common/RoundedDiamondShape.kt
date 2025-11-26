package com.orion.templete.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Custom Rounded Diamond Shape (Rotated Square with Rounded Corners)
 * @param cornerRadius The radius for rounding the corners (default: 0.2f = 20% of size)
 */
class RoundedDiamondShape(
    private val cornerRadius: Float = 0.2f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            path = drawRoundedDiamondPath(size = size, cornerRadius = cornerRadius)
        )
    }
}

/**
 * Draws a rounded diamond (rotated square) path
 */
private fun drawRoundedDiamondPath(size: Size, cornerRadius: Float): Path {
    val path = Path()
    
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val radius = min(centerX, centerY)
    val cornerRadiusPx = radius * cornerRadius
    
    // Define the four points of the diamond
    val topX = centerX
    val topY = centerY - radius
    
    val rightX = centerX + radius
    val rightY = centerY
    
    val bottomX = centerX
    val bottomY = centerY + radius
    
    val leftX = centerX - radius
    val leftY = centerY
    
    // Start from top point
    path.moveTo(topX, topY + cornerRadiusPx)
    
    // Top to Right with rounded corner
    path.lineTo(topX + (rightX - topX) / 2 - cornerRadiusPx, topY + (rightY - topY) / 2 - cornerRadiusPx)
    path.quadraticBezierTo(
        rightX - cornerRadiusPx, rightY - cornerRadiusPx,
        rightX - cornerRadiusPx, rightY
    )
    
    // Right to Bottom with rounded corner
    path.lineTo(rightX - cornerRadiusPx, rightY)
    path.lineTo(rightX - (rightX - bottomX) / 2 + cornerRadiusPx, rightY + (bottomY - rightY) / 2 - cornerRadiusPx)
    path.quadraticBezierTo(
        bottomX + cornerRadiusPx, bottomY - cornerRadiusPx,
        bottomX, bottomY - cornerRadiusPx
    )
    
    // Bottom to Left with rounded corner
    path.lineTo(bottomX, bottomY - cornerRadiusPx)
    path.lineTo(bottomX - (bottomX - leftX) / 2 + cornerRadiusPx, bottomY - (bottomY - leftY) / 2 + cornerRadiusPx)
    path.quadraticBezierTo(
        leftX + cornerRadiusPx, leftY + cornerRadiusPx,
        leftX + cornerRadiusPx, leftY
    )
    
    // Left to Top with rounded corner
    path.lineTo(leftX + cornerRadiusPx, leftY)
    path.lineTo(leftX + (topX - leftX) / 2 - cornerRadiusPx, leftY - (leftY - topY) / 2 + cornerRadiusPx)
    path.quadraticBezierTo(
        topX - cornerRadiusPx, topY + cornerRadiusPx,
        topX, topY + cornerRadiusPx
    )
    
    path.close()
    return path
}

/**
 * Rounded Diamond shaped button composable
 */
@Composable
fun RoundedDiamondButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    cornerRadius: Float = 0.2f
) {
    Box(
        modifier = modifier
            .size(60.dp) // Adjust size as needed
            .clip(RoundedDiamondShape(cornerRadius = cornerRadius))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

// Usage Example for your code:
/*
RoundedDiamondButton(
    onClick = { navController.navigate(Screens.ArtistRegister.route) },
    text = "Register",
    modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 12.dp)
        .size(60.dp)
)
*/

// Alternative: Use it with your existing Button structure
/*
Box(
    modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 12.dp)
        .size(60.dp)
        .clip(RoundedDiamondShape(cornerRadius = 0.2f))
        .background(MaterialTheme.colorScheme.primary)
        .clickable { navController.navigate(Screens.ArtistRegister.route) },
    contentAlignment = Alignment.Center
) {
    Text(
        text = "Register",
        color = MaterialTheme.colorScheme.onPrimary,
        style = MaterialTheme.typography.labelSmall
    )
}
*/

// Or replace the shape in your original Button:
/*
androidx.compose.material3.Button(
    onClick = { navController.navigate(Screens.ArtistRegister.route) },
    modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 12.dp)
        .height(60.dp)
        .width(60.dp),
    shape = RoundedDiamondShape(cornerRadius = 0.2f),
    content = {
        Text(text = "Register", color = MaterialTheme.colorScheme.onPrimary)
    }
)
*/