import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size

@Composable
fun ArtViewScreen(url: String) {
    // State for zoom and pan
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    // Transformable state for zoom and pan gestures
    val transformableState = rememberTransformableState { zoomChange, panX, panY ->
        // Update scale with constraints
        scale = (scale * zoomChange).coerceIn(1f, 5f)

        // Update pan offset with constraints based on scale
        val maxOffset = (scale - 1) * 500 // Arbitrary value, adjust based on your needs
        offsetX = (offsetX + panX.x).coerceIn(-maxOffset, maxOffset)
        offsetY = (offsetY + panX.y).coerceIn(-maxOffset, maxOffset)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black // Dark background for art viewing
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Log.d("url" , url)
            // High-quality image with zoom capabilities
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .size(Size.ORIGINAL)
                    .memoryCachePolicy(CachePolicy.DISABLED) // Optional: prevent OOM for very large images
                    .diskCachePolicy(CachePolicy.ENABLED) // Keep disk cache for faster reloads
                    .allowHardware(false)// Request original size for best quality
                    .build(),
                contentDescription = "High-resolution Artwork",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .transformable(state = transformableState),
                // Show loading indicator while image loads
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
            )
        }
    }
}