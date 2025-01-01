package com.orion.templete.presentation.artwork_detail

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.font.FontWeight
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
import com.orion.templete.data.model.artwork_model.comments.CommentRequest
import com.orion.templete.data.model.artwork_model.comments.GetCommentsDTO
import com.orion.templete.data.model.user_model.UserDTO
import com.orion.templete.presentation.artist_profile.ArtistProfileViewModel
import com.orion.templete.presentation.artist_profile.GetCommentsOnArtworkUiState
import com.orion.templete.presentation.common.ArtworkItem
import com.orion.templete.presentation.common.ErrorScreen
import com.orion.templete.presentation.common.ProfileHeader
import com.orion.templete.presentation.components.GenreSection
import com.orion.templete.presentation.search.dummyArtworks
import com.orion.templete.presentation.swipe.components.BottomSheet
import com.orion.templete.presentation.ui.theme.AppBarCollapsedHeight
import com.orion.templete.presentation.ui.theme.AppBarExpendedHeight
import com.orion.templete.presentation.ui.theme.Black
import com.orion.templete.presentation.ui.theme.MediumSize
import com.orion.templete.presentation.ui.theme.SmallSize
import com.orion.templete.presentation.ui.theme.TempleteTheme
import kotlin.math.max
import kotlin.math.min


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ArtworkDetailScreen(artworkId:String , viewModel: ArtworkDetailViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) {
        viewModel.getArtworkById("test4" ,artworkId)
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


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ArtworkDetailContent(artworkDetailsDTO: ArtworkDTO) {
    val scrollState = rememberLazyListState()
    var isLiked = remember { mutableStateOf(artworkDetailsDTO.liked) }
    Box {
        Details(artworkDetailsDTO, scrollState , isLiked)
        ParallaxToolbar(artworkDetailsDTO, scrollState , isLiked)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun Details(
    artworkDetailsDTO: ArtworkDTO,
    scrollState: LazyListState,
    isLiked: MutableState<Boolean?>
) {
    LazyColumn(
        contentPadding = PaddingValues(top = AppBarExpendedHeight), state = scrollState
    ) {
        item {
            BasicInfo(isLiked)
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BasicInfo(isLiked: MutableState<Boolean?>) {
    val vm: ArtistProfileViewModel = hiltViewModel()
    var showComments by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        ClickableIcon(if (isLiked.value == true) R.drawable.ic_favorite_filled else R.drawable.ic_favorite, tint = if (isLiked.value == true) MaterialTheme.colorScheme.error else  LocalContentColor.current, text = "5" ){
            if(isLiked.value == true){
                vm.unLikeArtwork("test4" , "4bfde960-4807-421e-b11b-7f5472e848ea")
            }else{
                vm.likeArtwork("test4" , "4bfde960-4807-421e-b11b-7f5472e848ea")
            }
            isLiked.value = !isLiked.value!!
        }
        InfoColumn(R.drawable.ic_comment, "7"){
            showComments = true
        }
        InfoColumn(R.drawable.ic_save, "Save"){
            vm.saveOnFavorites("test4" , "051c47a6-e7fc-449a-a7de-e4653d046938")
        }
    }
    if(showComments) {
        LaunchedEffect(Unit) {
            vm.getAllComments("4bfde960-4807-421e-b11b-7f5472e848ea")
        }
        BottomSheet(onDismiss = { showComments = false }) {
            when (val uiState = vm.getCommentsOnArtworkUiState) {
                is GetCommentsOnArtworkUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center)
                    )

                }

                is GetCommentsOnArtworkUiState.Success -> {
                    var comments by remember { mutableStateOf(uiState.data) }
                    CommentSection(
                        comments = comments,
                        onSendComment = { newComment ->
                            comments = listOf(GetCommentsDTO(
                                userId = "current_user",
                                text = newComment
                            )) + comments
                            vm.commentOnArtwork(
                                "test4",
                                "4bfde960-4807-421e-b11b-7f5472e848ea",
                                CommentRequest(text = newComment)
                            )
                        }
                    )
                }

                is GetCommentsOnArtworkUiState.Error -> {
                    ErrorScreen(
                        message = uiState.message,
                        onRetry = { vm.refreshComments("4bfde960-4807-421e-b11b-7f5472e848ea") }
                    )
                }
            }
        }
    }
}

@Composable
fun CommentSection(
    comments: List<GetCommentsDTO>,
    onSendComment: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Comments",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(comments) { comment ->
                CommentItem(comment)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            TextField(
                value = commentText,
                onValueChange = { commentText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add a comment...") },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent
                ),
                maxLines = 5
            )

            IconButton(
                onClick = {
                    if (commentText.isNotBlank()) {
                        onSendComment(commentText)
                        commentText = ""
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (commentText.isBlank()) Color.Gray else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun CommentItem(comment: GetCommentsDTO) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = comment.userProfilePicture),
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
        )
        Text(
            text = comment.userName?:"Unknown",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = comment.text?:"Unknown Comment",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

data class Comment(
    val username: String,
    val text: String
)


@Composable
fun InfoColumn(@DrawableRes iconResource: Int, text: String , onClick: () -> Unit ={}) {
    Row(verticalAlignment = Alignment.CenterVertically , modifier = Modifier.clickable { onClick() }) {
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
@Composable
fun ClickableIcon(@DrawableRes iconResource: Int, text: String, tint: Color = LocalContentColor.current , onClick: () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically , modifier = Modifier.clickable { onClick() }) {
        Icon(
            painter = (painterResource(id = iconResource)),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = tint
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontWeight = Bold)
    }
}

@SuppressLint("SuspiciousIndentation")
@Composable
private fun ParallaxToolbar(
    artworkDTO: ArtworkDTO,
    scrollState: LazyListState,
    isLiked: MutableState<Boolean?>
) {
    val vm: ArtistProfileViewModel = hiltViewModel()
    val imageHeight = AppBarExpendedHeight - AppBarCollapsedHeight
    val maxOffset = with(LocalDensity.current) {
        imageHeight.roundToPx()
    } - WindowInsets.systemBars.getTop(LocalDensity.current)
    val offset = min(scrollState.firstVisibleItemScrollOffset, maxOffset)
    val offsetprogress = max(0f, offset * 3f - 2f * maxOffset) / maxOffset

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
        CircularButton(if (isLiked.value == true) R.drawable.ic_favorite_filled else R.drawable.ic_favorite ,  tint = if (isLiked.value == true) MaterialTheme.colorScheme.error else LocalContentColor.current ){
            if(isLiked.value == true){
                vm.unLikeArtwork("test4" , "4bfde960-4807-421e-b11b-7f5472e848ea")
            }else{
                vm.likeArtwork("test4" , "4bfde960-4807-421e-b11b-7f5472e848ea")
            }
            isLiked.value = !isLiked.value!!
        }
    }
}

@Composable
fun CircularButton(
    @DrawableRes iconResource: Int,
    color: Color = Gray,
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    tint: Color = LocalContentColor.current,
    onClick: () -> Unit = {}
) {

    Button(
        onClick = onClick,
        contentPadding = PaddingValues(),
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(containerColor =  LocalContentColor.current.copy(alpha = 0.2f), contentColor = color),
        elevation = elevation,
        modifier = Modifier
            .width(38.dp)
            .height(38.dp)

    ) {
        Icon(painterResource(id = iconResource), contentDescription = null , modifier = Modifier.size(24.dp) , tint = tint)
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
private fun ArtworkPreview() {
    TempleteTheme() {
        ArtworkDetailScreen("4bfde960-4807-421e-b11b-7f5472e848ea")
    }
}