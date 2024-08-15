package com.orion.templete.presentation.swipe.detail

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.LightGray
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.templete.R
import com.orion.templete.data.model.Content
import com.orion.templete.presentation.ui.theme.AppBarCollapsedHeight
import com.orion.templete.presentation.ui.theme.AppBarExpendedHeight
import com.orion.templete.presentation.ui.theme.MediumSize
import com.orion.templete.presentation.ui.theme.SmallSize
import kotlin.math.max
import kotlin.math.min

@Composable
fun ArtworkDetailScreen(content: Content) {
    val scrollState = rememberLazyListState()
    Box{
        Details(content,scrollState)
        ParallaxToolbar(content,scrollState)
    }
}

@Composable
fun Details(content: Content, scrollState: LazyListState) {
    LazyColumn(
        contentPadding = PaddingValues(top = AppBarExpendedHeight),
        state = scrollState
    ) {
        item {
            BasicInfo(content)
            Description(content)

        }
    }
}
@Composable
fun Description(content: Content) {
    Text(
        text = content.description ?: stringResource(R.string.no_dis),
        fontWeight = Medium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
    )

}
@Composable
fun BasicInfo(content: Content) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        InfoColumn(R.drawable.bookmark_border_24px,content.type)
        InfoColumn(R.drawable.bookmark_border_24px,content.type)
        InfoColumn(R.drawable.bookmark_border_24px,content.type)
    }
}
@Composable
fun InfoColumn(@DrawableRes iconResource: Int,text:String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painter = (painterResource(id = iconResource)),
            contentDescription = null,
            tint = Black,
            modifier = Modifier.height(24.dp)
        )
        Text(text = text, fontWeight = Bold)
    }
}

@SuppressLint("SuspiciousIndentation")
@Composable
fun ParallaxToolbar(content: Content, scrollState: LazyListState) {
    val imageHight = AppBarExpendedHeight - AppBarCollapsedHeight
    val maxOffset = with(LocalDensity.current){
        imageHight.roundToPx()
    } - WindowInsets.systemBars.getTop(LocalDensity.current)
    val offset = min(scrollState.firstVisibleItemScrollOffset,maxOffset)
    val offsetprogress = max(0f,offset*3f-2f*maxOffset) /maxOffset
    val imageHeight = AppBarExpendedHeight- AppBarCollapsedHeight
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
                    painter = painterResource(id = R.drawable.russ),
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
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(
                            horizontal = MediumSize,
                            vertical = SmallSize
                        ),
                    verticalAlignment = Alignment.Bottom
                )
                {
                    Text(
                        content.releasedDate, fontWeight = FontWeight.Medium, modifier = Modifier
                            .clip(
                                RoundedCornerShape(4.dp)
                            )
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(vertical = 6.dp, horizontal = 16.dp)
                    )
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(AppBarCollapsedHeight),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = content.name,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
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
        CircularButton(R.drawable.home)
        CircularButton(R.drawable.user)
    }
}
@Composable
fun CircularButton(
    @DrawableRes iconResource: Int,
    color: Color = Gray,
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    onClick:()->Unit={}
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
