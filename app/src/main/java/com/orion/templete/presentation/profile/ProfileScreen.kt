package com.orion.templete.presentation.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.Medium
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.templete.presentation.components.AppIconButton
import com.nameisjayant.composeprojects.components.SpacerHeight
import com.nameisjayant.composeprojects.components.SpacerWidth
import com.orion.templete.R
import com.orion.templete.presentation.components.AppIcon
import com.orion.templete.presentation.ui.theme.Blue
import com.orion.templete.presentation.ui.theme.ButtonHeight
import com.orion.templete.presentation.ui.theme.LargeSize
import com.orion.templete.presentation.ui.theme.MediumSize
import com.orion.templete.presentation.ui.theme.SmallSize
import com.orion.templete.presentation.ui.theme.TempleteTheme

@Composable
fun ProfileScreen() {
    Surface {
        ProfileRow(
            topBarSection = {
                TopBar(
                    name = "UserName",
                )
            },
            profileSection = {
                ProfileSection()
            },
            buttonSection = {
                ButtonSection(modifier = Modifier.fillMaxWidth())
            }
        )
        {

        }
    }
}

@Composable
private fun ProfileRow(
    modifier: Modifier = Modifier,
    topBarSection: (@Composable () -> Unit)? = null,
    profileSection: (@Composable () -> Unit)? = null,
    buttonSection: (@Composable () -> Unit)? = null,
    savedSection: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MediumSize),
    ) {
        topBarSection?.invoke()
        profileSection?.invoke()
        buttonSection?.invoke()
        savedSection?.invoke()
    }
}




@Composable
private fun TopBar(
    modifier: Modifier = Modifier,
    name: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier.fillMaxWidth(0.5F)
    ) {
        AppIconButton(icon = R.drawable.ic_arrow_back , tint = MaterialTheme.colorScheme.onSurface){}
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(
                text = name,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
private fun ProfileSection(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(),Arrangement.Center , Alignment.CenterHorizontally) {
        RoundImage(
            image = painterResource(id = R.drawable.user),
            modifier = Modifier.size(80.dp)

        )
        SpacerHeight(SmallSize)
        Text(
            text = "DisplayName",
            fontWeight = FontWeight.Bold,
        )
        SpacerHeight(MediumSize)
        StatSection()
        SpacerHeight(MediumSize)
        Genre()
        SpacerHeight(MediumSize)
        ProfileDescriptionSection(
            description = "Lorem Ipsum is simply dummy text of the printing and typesetting industry.",
            url = "https://www.instagram.com/usrname/",
            followedBy = listOf("viratkohli", "mrbeast"),
        )
        SpacerHeight(MediumSize)
    }
}

@Composable
fun Genre() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "genre", fontWeight = Medium, modifier = Modifier
                .clip(
                    RoundedCornerShape(4.dp)
                )
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 6.dp, horizontal = 16.dp)
        )
    }
}

@Composable
private fun RoundImage(
    image: Painter,
    modifier: Modifier = Modifier
) {
    Image(
        painter = image,
        contentDescription = null,
        modifier = modifier
            .aspectRatio(1f, matchHeightConstraintsFirst = true)
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = CircleShape
            )
            .padding(3.dp)
            .clip(CircleShape)
    )
}

@Composable
private fun StatSection(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        ProfileStatSection(number = "32", text = "Artworks")
        ProfileStatSection(number = "32", text = "Followers")
        ProfileStatSection(number = "32", text = "Following")
    }
}
@Composable
private fun ProfileStatSection(
    number: String,
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = number,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        SpacerHeight(SmallSize)
        Text(text = text)
    }
}

@Composable
private fun ProfileDescriptionSection(
    description: String?,
    url: String?,
    followedBy: List<String>?,
    otherCount: Int = 18
) {
    val letterSpacing = 0.5.sp
    val lineHeight = 20.sp
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        if (description != null) {
            Text(
                text = description,
                letterSpacing = letterSpacing,
                lineHeight = lineHeight
            )
        }
        if (url != null) {
            Text(
                text = url,
                color = Blue,
                letterSpacing = letterSpacing,
                lineHeight = lineHeight
            )
        }
        if (followedBy != null) {
            Text(
                text = buildAnnotatedString {
                    val boldStyle = SpanStyle(
                        fontWeight = FontWeight.Bold
                    )
                    append("followed by ")
                    followedBy.forEachIndexed { index, name ->
                        pushStyle(boldStyle)
                        append(name)
                        pop()
                        if (index < followedBy.size - 1) {
                            append(" , ")
                        }
                    }
                    if (otherCount > 2) {
                        append(" and ")
                        pushStyle(boldStyle)
                        append("other")
                    }
                },
                letterSpacing = letterSpacing,
                lineHeight = lineHeight
            )
        }
    }
}

@Composable
private fun ButtonSection(
    modifier: Modifier = Modifier
) {
    val height = 32.dp
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.size(height)
            .border(
                width = 1.dp,
                color =MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(5.dp),
            )
    ) {
        Text(
            text = "Following",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}



@Preview(showBackground = true , showSystemUi = true , )
@Composable
private fun prev() {
    TempleteTheme(darkTheme = true) {
        ProfileScreen()
    }
    
}