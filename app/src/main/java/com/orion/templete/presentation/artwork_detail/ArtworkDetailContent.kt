package com.orion.templete.presentation.artwork_detail

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.font.FontWeight.Companion.Medium
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.orion.templete.R
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.presentation.artist_profile.ArtistProfileViewModel
import com.orion.templete.presentation.common.ArtworkItem
import com.orion.templete.presentation.common.ErrorScreen
import com.orion.templete.presentation.common.ProfileHeader
import com.orion.templete.presentation.components.GenreSection
import com.orion.templete.presentation.search.dummyArtworks
import com.orion.templete.presentation.ui.theme.AppBarCollapsedHeight
import com.orion.templete.presentation.ui.theme.AppBarExpendedHeight
import com.orion.templete.presentation.ui.theme.MediumSize
import com.orion.templete.presentation.ui.theme.SmallSize
import com.orion.templete.presentation.ui.theme.TempleteTheme
import kotlin.math.max
import kotlin.math.min


@Composable
fun ArtworkDetailScreen(artworkId:String , viewModel: ArtworkDetailViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) {
        viewModel.getArtworkById(artworkId , "test4")
    }
    when (val uiState = viewModel.artworkDetailScreenUiState) {
        is ArtworkDetailScreenUiState.Loading -> {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            )
        }

        is ArtworkDetailScreenUiState.Success -> {
            ArtworkDetailContent(uiState.artwork)
        }

        is ArtworkDetailScreenUiState.Error -> {
            ErrorScreen(
                message = uiState.message,
                onRetry = { viewModel.refreshArtwork("test4" ,artworkId) }
            )
        }
    }
}


@Composable
fun ArtworkDetailContent(artworkDetailsDTO: ArtworkDTO) {
    val scrollState = rememberLazyListState()
    val vm: ArtistProfileViewModel = hiltViewModel()
    val likeState = vm.userLikeArtworkUiState
    val unlikeState = vm.userUnLikeArtworkUiState
    Box {
        Details(artworkDetailsDTO, scrollState)
        ParallaxToolbar(artworkDetailsDTO, scrollState)
    }
}

@Composable
private fun Details(artworkDetailsDTO: ArtworkDTO, scrollState: LazyListState) {
    LazyColumn(
        contentPadding = PaddingValues(top = AppBarExpendedHeight), state = scrollState
    ) {
        item {
            BasicInfo(artworkDetailsDTO)
            Description(artworkDetailsDTO)
            AboutTheArtist()
            RecommendFromArtist()
            RecommendFromGenre()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AboutTheArtist() {
    val user = UserDTO("afsfsdf" , "sfdaffs" , "dfsfdsfd" ,"sdfsfdsfds" , "male" , "english" , "in" , true)
    ProfileHeader(user)
}

@Composable
fun RecommendFromArtist() {
    Column {
        Text(
            text = "More From Artist",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(dummyArtworks) { artwork ->
                ArtworkItem(artwork)
            }
        }
    }
}
@Composable
fun RecommendFromGenre() {
    Column {
        Text(
            text = "Similar Genre",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(dummyArtworks) { artwork ->
                ArtworkItem(artwork)
            }
        }
    }
}

@Composable
fun Description(artworkDetailsDTO: ArtworkDTO) {
    Text(
        text = artworkDetailsDTO.description ?: stringResource(R.string.no_dis),
        fontWeight = Medium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
    )

}

@Composable
fun BasicInfo(artworkDetailsDTO: ArtworkDTO) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        InfoColumn(if (artworkDetailsDTO.liked == true) R.drawable.ic_favorite_filled else R.drawable.ic_favorite, "5")
        InfoColumn(R.drawable.ic_comment, "7")
        InfoColumn(R.drawable.ic_save, "gn")
    }
}

@Composable
fun InfoColumn(@DrawableRes iconResource: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = (painterResource(id = iconResource)),
            contentDescription = null,
            tint =  if(isSystemInDarkTheme()) Color.White else Color.Black,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontWeight = Bold)
    }
}

@SuppressLint("SuspiciousIndentation")
@Composable
private fun ParallaxToolbar(artworkDTO: ArtworkDTO, scrollState: LazyListState) {
    val imageHight = AppBarExpendedHeight - AppBarCollapsedHeight
    val maxOffset = with(LocalDensity.current) {
        imageHight.roundToPx()
    } - WindowInsets.systemBars.getTop(LocalDensity.current)
    val offset = min(scrollState.firstVisibleItemScrollOffset, maxOffset)
    val offsetprogress = max(0f, offset * 3f - 2f * maxOffset) / maxOffset
    val imageHeight = AppBarExpendedHeight - AppBarCollapsedHeight
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .height(AppBarExpendedHeight)
            .offset { IntOffset(x = 0, y = -offset) },
        elevation = CardDefaults.cardElevation(defaultElevation = if (offset == maxOffset) 4.dp else 0.dp),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column {
            Box(
                Modifier
                    .height(imageHeight)
                    .graphicsLayer {
                        alpha = 1f - offsetprogress
                    }) {
                Image(
                    painter = rememberAsyncImagePainter(artworkDTO.imageUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    Pair(0.4f, Transparent),
                                    Pair(1f, MaterialTheme.colorScheme.surface)
                                )
                            )
                        ),
                    contentAlignment = Alignment.BottomStart
                ){
                    GenreSection()
                }
                artworkDTO?.releasedDate?.let {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(
                                horizontal = MediumSize, vertical = SmallSize
                            ), verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            it, fontWeight = Medium, modifier = Modifier
                                .clip(
                                    RoundedCornerShape(4.dp)
                                )
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(vertical = 6.dp, horizontal = 16.dp)
                        )
                    }
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(AppBarCollapsedHeight),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = artworkDTO.name ?: stringResource(R.string.no_name),
                    fontSize = 26.sp,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = Bold,
                    modifier = Modifier
                        .padding(horizontal = (16 + 28 * offsetprogress).dp)
                        .scale(1f - 0.25f * offsetprogress)
                )
            }
        }

    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(AppBarCollapsedHeight)
            .padding(horizontal = 16.dp)
    ) {
        CircularButton(R.drawable.ic_arrow_back)
        CircularButton(if (artworkDTO.liked == true) R.drawable.ic_favorite_filled else R.drawable.ic_favorite)
    }
}

@Composable
fun CircularButton(
    @DrawableRes iconResource: Int,
    color: Color = Gray,
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    onClick: () -> Unit = {}
) {

    Button(
        onClick = onClick,
        contentPadding = PaddingValues(),
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = color),
        elevation = elevation,
        modifier = Modifier
            .width(38.dp)
            .height(38.dp)

    ) {
        Icon(painterResource(id = iconResource), contentDescription = null)
    }
}


@Preview
@Composable
private fun ArtworkPreview() {
    TempleteTheme() {
        ArtworkDetailScreen("4bfde960-4807-421e-b11b-7f5472e848ea")
    }
}