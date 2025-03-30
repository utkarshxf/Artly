package com.orion.templete.presentation.artwork_detail

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.font.FontWeight.Companion.Medium
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.orion.templete.R
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.artwork_model.comments.CommentRequest
import com.orion.templete.data.model.artwork_model.comments.GetCommentsDTO
import com.orion.templete.data.model.favorits.favoritesDTO
import com.orion.templete.presentation.artist_profile.ArtistProfileScreenUiState
import com.orion.templete.presentation.artist_profile.ArtistProfileViewModel
import com.orion.templete.presentation.artist_profile.GetCommentsOnArtworkUiState
import com.orion.templete.presentation.common.ArtworkItem
import com.orion.templete.presentation.common.ErrorScreen
import com.orion.templete.presentation.common.Screens
import com.orion.templete.presentation.common.ArtistProfileCard
import com.orion.templete.presentation.components.GenreSection
import com.orion.templete.presentation.favorites.AddToFavoritesUiState
import com.orion.templete.presentation.favorites.CollectionCard
import com.orion.templete.presentation.favorites.CollectionViewModel
import com.orion.templete.presentation.favorites.CreateFavoritesUiState
import com.orion.templete.presentation.favorites.FavoritesUiState
import com.orion.templete.presentation.search.dummyArtworks
import com.orion.templete.presentation.swipe.components.BottomSheet
import com.orion.templete.presentation.ui.theme.AppBarCollapsedHeight
import com.orion.templete.presentation.ui.theme.AppBarExpendedHeight
import com.orion.templete.util.SecureStorage
import com.orion.templete.util.extractYear
import com.orion.templete.util.getSourceUrlSiteName
import kotlin.math.max
import kotlin.math.min


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ArtworkDetailScreen(artworkId:String ,navController: NavController ,  viewModel: ArtworkDetailViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) {
        viewModel.getArtworkById(artworkId)
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
            ArtworkDetailContent(uiState.artwork , navController)
            Log.d("qwerty" , uiState.artwork.toString())
        }

        is ArtworkDetailScreenUiState.Error -> {
            ErrorScreen(
                message = uiState.message,
                onRetry = { viewModel.refreshArtwork(artworkId) }
            )
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ArtworkDetailContent(artworkDetailsDTO: ArtworkDTO, navController: NavController) {
    val scrollState = rememberLazyListState()
    var isLiked = remember { mutableStateOf(artworkDetailsDTO.liked) }
    Box {
        Details(artworkDetailsDTO, scrollState , isLiked , navController)
        ParallaxToolbar(artworkDetailsDTO, scrollState , isLiked ,navController)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun Details(
    artworkDetailsDTO: ArtworkDTO,
    scrollState: LazyListState,
    isLiked: MutableState<Boolean?>,
    navController: NavController
) {
    LazyColumn(
        contentPadding = PaddingValues(top = AppBarExpendedHeight),
        state = scrollState,
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            BasicInfo(artworkDetailsDTO, isLiked)
            Description(artworkDetailsDTO)
            ArtworkDetails(artworkDetailsDTO)
            AboutTheArtist(navController, artworkDetailsDTO.id)
            RecommendFromGenre(artworkDetailsDTO, navController)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AboutTheArtist(navController: NavController , id: String? , vm: ArtistProfileViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) {
        vm.getArtistByArtworkId(id ?: "")
    }
    val artistProfileUiState = vm.artistProfileScreenUiState
    Log.d("artistProfileUiState" , artistProfileUiState.toString())
    if(artistProfileUiState is ArtistProfileScreenUiState.Success){
        val it  = artistProfileUiState.artist
        ArtistProfileCard(it.name ?: "name", it.id ?: "id", it.image_url ?: "profilePicture") {
            navController.currentBackStackEntry?.savedStateHandle?.set(key = "UserID", value = it.id)
            navController.navigate(Screens.UserProfile.route)
        }
        if (id != null) {
            RecommendFromArtist(id ,it.id , navController)
        }
    }
}

@Composable
fun RecommendFromArtist(artworkId: String ,artistId: String? , navController: NavController , vm: ArtworkDetailViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) {
        if (artistId != null) {
            vm.getArtworkByArtistId(artistId =artistId , currentArtworkId = artworkId)
        }
    }
    val state = vm.artworksScreenUiState
    if(state is ArtworksScreenUiState.Success){
        Column {
            if(state.artwork.isNotEmpty())
                Text(
                text = "More From Artist",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(state.artwork) { artwork ->
                    ArtworkItem(artwork){ id ->
                        navController.currentBackStackEntry?.savedStateHandle?.set(key = "artworkId", value = id )
                        navController.navigate(Screens.ArtworkDetail.route)
                    }
                }
            }
        }
    }
}
@Composable
fun RecommendFromGenre(artworkDetailsDTO: ArtworkDTO  , navController: NavController , vm: ArtworkDetailViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) {
        if (artworkDetailsDTO.id != null) {
            vm.getSimilarArtwork(artworkId =artworkDetailsDTO.id)
        }
    }
    val state = vm.similarArtworks

    if(state is ArtworksScreenUiState.Success){
        Column {
            if(state.artwork.isNotEmpty())
            Text(
                text = "Similar Genre",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(state.artwork) { artwork ->
                    ArtworkItem(artwork){ id->
                        navController.currentBackStackEntry?.savedStateHandle?.set(key = "artworkId", value = id )
                        navController.navigate(Screens.ArtworkDetail.route)
                    }
                }
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
fun BasicInfo(artworkDetailsDTO:ArtworkDTO, isLiked: MutableState<Boolean?>) {
    val vm: ArtistProfileViewModel = hiltViewModel()
    var showComments by remember { mutableStateOf(false) }
    var showSavedFolders by remember { mutableStateOf(false) }
    val collectionVm : CollectionViewModel = hiltViewModel()
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        ClickableIcon(if (isLiked.value == true) R.drawable.ic_favorite_filled else R.drawable.ic_favorite, tint = if (isLiked.value == true) MaterialTheme.colorScheme.error else  LocalContentColor.current, text = "Like" ){
            if(isLiked.value == true){
                artworkDetailsDTO.id?.let { vm.unLikeArtwork(it) }
            }else{
                artworkDetailsDTO.id?.let { vm.likeArtwork(it) }
            }
            isLiked.value = !isLiked.value!!
        }
        InfoColumn(R.drawable.ic_comment, "Comment"){
            showComments = true
        }
        InfoColumn(R.drawable.ic_save, "Save"){
            showSavedFolders = true
        }
    }
    if (showSavedFolders){
        BottomSheet(onDismiss = { showSavedFolders = false }) {
            SaveSection(
                dismiss = { showSavedFolders = false },
                artworkDetailsDTO.id,
                vm = collectionVm
            )
        }
    }
    if(showComments) {
        LaunchedEffect(Unit) {
            artworkDetailsDTO.id?.let { vm.getAllComments(it) }
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
                    val context = LocalContext.current
                    val currentUser = SecureStorage(context).getUserDetails()
                    CommentSection(
                        comments = comments,
                        onSendComment = { newComment ->
                            comments = listOf(GetCommentsDTO(
                                userId = currentUser?.id,
                                userName = currentUser?.name,
                                userProfilePicture = currentUser?.profilePicture,
                                text = newComment
                            )) + comments
                            artworkDetailsDTO.id?.let {
                                vm.commentOnArtwork(
                                    it,
                                    CommentRequest(text = newComment)
                                )
                            }
                        }
                    )
                }

                is GetCommentsOnArtworkUiState.Error -> {
                    ErrorScreen(
                        message = uiState.message,
                        onRetry = { artworkDetailsDTO.id?.let { vm.refreshComments(artworkId = it) } }
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
fun SaveSection(
    dismiss: () -> Unit = {},
    artworkId: String? = null,
    vm: CollectionViewModel = hiltViewModel()
) {
    val state = vm.favoritesUiState
    LaunchedEffect(key1 = vm.createFavoritesUiState) {
        if (vm.createFavoritesUiState is CreateFavoritesUiState.Success) {
            vm.getFavoritesByUserId()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Save",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        when (state) {
            is FavoritesUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                )

            }

            is FavoritesUiState.Success -> {
                LazyVerticalGrid(
                    userScrollEnabled = true,
                    modifier = Modifier.fillMaxHeight(),
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)){
                    items(state.favorites) {
                        Column {
                            CollectionCard(it , onClick = {
                                it.id?.let { it1 ->
                                    if (artworkId != null) {
                                        vm.saveOnFavorites(favoritesId = it1 , artworkId =artworkId)
                                        dismiss()
                                    }
                                }
                            })
                        }
                    }
                }
            }

            is FavoritesUiState.Error -> {
                ErrorScreen(
                    message = state.message,
                    onRetry = {  }
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
    isLiked: MutableState<Boolean?>,
    navController: NavController
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
                    painter = rememberAsyncImagePainter(artworkDTO.image_url_compressed),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                        .clickable {
                            navController.currentBackStackEntry?.savedStateHandle?.set(key = "url", value = artworkDTO.imageUrl)
                            navController.navigate(Screens.ArtView.route)
                        }
                )
                if (!artworkDTO.medium.isNullOrEmpty()){
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
                        GenreSection(artworkDTO.medium)
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
                    text = artworkDTO.title ?: stringResource(R.string.no_name),
                    fontSize = 24.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineMedium,
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
        CircularButton(R.drawable.ic_arrow_back){
            navController.popBackStack()
        }
        CircularButton(if (isLiked.value == true) R.drawable.ic_favorite_filled else R.drawable.ic_favorite ,  tint = if (isLiked.value == true) MaterialTheme.colorScheme.error else LocalContentColor.current ){
            if(isLiked.value == true){
                artworkDTO.id?.let { vm.unLikeArtwork(it) }
            }else{
                artworkDTO.id?.let { vm.likeArtwork(it) }
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

@Composable
fun ArtworkDetails(artworkDetailsDTO: ArtworkDTO) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (!artworkDetailsDTO.artist.isNullOrEmpty()) DetailItem("Artist", artworkDetailsDTO.artist)
        if (!artworkDetailsDTO.dimensions.isNullOrEmpty()) artworkDetailsDTO.dimensions?.let { DetailItem("Dimensions", it) }
        if (!artworkDetailsDTO.artMovement.isNullOrEmpty()) DetailItem("Art Movement", artworkDetailsDTO.artMovement)
        if (!artworkDetailsDTO.periodStyle.isNullOrEmpty())  { DetailItem("Period/Style", artworkDetailsDTO.periodStyle) }
        if (!artworkDetailsDTO.currentLocation.isNullOrEmpty())  { DetailItem("Currently in", artworkDetailsDTO.currentLocation) }
        if (! extractYear(artworkDetailsDTO.releasedDate).isNullOrEmpty())  { DetailItem("Release Date", extractYear( artworkDetailsDTO.releasedDate)!!) }
        if (!artworkDetailsDTO.licenseInfo.isNullOrEmpty())  { DetailItem("License", artworkDetailsDTO.licenseInfo) }
        // Source URL (if available)
        if (!artworkDetailsDTO.sourceUrl.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            SourceUrlLink(artworkDetailsDTO.sourceUrl)
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SourceUrlLink(url: String) {
    val context = LocalContext.current
    val intent = remember { Intent(Intent.ACTION_VIEW, Uri.parse(url)) }

    Row(
    ) {
        Text(
            text = "Learn more about the artwork on ",
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
        )

        Text(
            text = getSourceUrlSiteName(url),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable { context.startActivity(intent) }
                .weight(1f)
        )
    }
}

@Composable
fun GenreChip(genre: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        modifier = Modifier.height(32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = genre,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}