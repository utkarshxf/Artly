package com.orion.templete.presentation.search

import com.orion.templete.presentation.common.VerifiedBadge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.orion.templete.data.model.search.SearchArtistHit
import com.orion.templete.data.model.search.SearchArtworkHit
import com.orion.templete.data.model.search.SearchPersonHit
import com.orion.templete.presentation.chat.components.ChatAvatar

private const val TOP_PEOPLE = 3
private const val LOAD_MORE_THRESHOLD = 5

// Search results inside the expanded search bar: Top / Artworks / Artists / People, updated while typing
@Composable
fun SearchResultsContent(
    state: SearchUiState,
    onTabSelected: (SearchTab) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onOpenArtwork: (SearchArtworkHit) -> Unit,
    onOpenArtist: (SearchArtistHit) -> Unit,
    onOpenPerson: (SearchPersonHit) -> Unit,
) {
    val query = state.query.trim()
    if (query.isEmpty()) {
        SearchHint()
        return
    }
    val terms = remember(query) { highlightTerms(query) }
    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = state.tab.ordinal,
            containerColor = Color.Transparent,
            edgePadding = 12.dp,
            divider = {},
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(positions[state.tab.ordinal]),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        ) {
            SearchTab.entries.forEach { tab ->
                Tab(
                    selected = tab == state.tab,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Text(
                            text = tab.label,
                            fontWeight = if (tab == state.tab) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.onSurface,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        val results = state.current
        when {
            results.isEmpty && (results.loading || results.query != query) -> CenterBox { CircularProgressIndicator() }
            results.isEmpty && results.error != null -> CenterBox {
                Message(title = results.error, body = null, onRetry = onRetry)
            }
            results.isEmpty -> CenterBox {
                Message(
                    title = "No results for “$query”",
                    body = "Try another title, artist, style or a word from a description.",
                    onRetry = null
                )
            }
            else -> ResultsList(state, results, terms, onTabSelected, onLoadMore, onOpenArtwork, onOpenArtist, onOpenPerson)
        }
    }
}

@Composable
private fun ResultsList(
    state: SearchUiState,
    results: SearchTabResults,
    terms: List<String>,
    onTabSelected: (SearchTab) -> Unit,
    onLoadMore: () -> Unit,
    onOpenArtwork: (SearchArtworkHit) -> Unit,
    onOpenArtist: (SearchArtistHit) -> Unit,
    onOpenPerson: (SearchPersonHit) -> Unit,
) {
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current
    // scrolling the results closes the keyboard, like Instagram
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) keyboard?.hide()
    }
    // new query / tab: back to the top
    LaunchedEffect(results.query, state.tab) { listState.scrollToItem(0) }
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(nearEnd, results.artworks.size, results.artists.size, results.people.size) {
        if (nearEnd) onLoadMore()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        if (results.loading) {
            item(key = "refreshing") { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }
        when (state.tab) {
            SearchTab.TOP -> topResults(results, terms, onTabSelected, onOpenArtwork, onOpenArtist, onOpenPerson)
            SearchTab.ARTWORKS -> items(results.artworks, key = { "artwork_${it.id}" }) { hit ->
                ArtworkResultRow(hit, terms) { onOpenArtwork(hit) }
            }
            SearchTab.ARTISTS -> items(results.artists, key = { "artist_${it.id}" }) { hit ->
                ArtistResultRow(hit, terms) { onOpenArtist(hit) }
            }
            SearchTab.PEOPLE -> items(results.people, key = { "person_${it.username}" }) { hit ->
                PersonResultRow(hit, terms) { onOpenPerson(hit) }
            }
        }
        if (results.loadingMore) {
            item(key = "more") {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        }
        if (results.error != null && !results.isEmpty) {
            item(key = "error") {
                Text(
                    text = results.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }
        }
    }
}

// "Top": a few artists and people, then the artworks
private fun LazyListScope.topResults(
    results: SearchTabResults,
    terms: List<String>,
    onTabSelected: (SearchTab) -> Unit,
    onOpenArtwork: (SearchArtworkHit) -> Unit,
    onOpenArtist: (SearchArtistHit) -> Unit,
    onOpenPerson: (SearchPersonHit) -> Unit,
) {
    if (results.artists.isNotEmpty()) {
        item(key = "h_artists") { SectionHeader("Artists") { onTabSelected(SearchTab.ARTISTS) } }
        items(results.artists.take(TOP_PEOPLE), key = { "top_artist_${it.id}" }) { hit ->
            ArtistResultRow(hit, terms) { onOpenArtist(hit) }
        }
    }
    if (results.people.isNotEmpty()) {
        item(key = "h_people") { SectionHeader("People") { onTabSelected(SearchTab.PEOPLE) } }
        items(results.people.take(TOP_PEOPLE), key = { "top_person_${it.username}" }) { hit ->
            PersonResultRow(hit, terms) { onOpenPerson(hit) }
        }
    }
    if (results.artworks.isNotEmpty()) {
        item(key = "h_artworks") { SectionHeader("Artworks") { onTabSelected(SearchTab.ARTWORKS) } }
        items(results.artworks, key = { "top_artwork_${it.id}" }) { hit ->
            ArtworkResultRow(hit, terms) { onOpenArtwork(hit) }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onSeeAll) {
            Text(text = "See all", color = Color(0xFF3897F0))
        }
    }
}

@Composable
private fun ArtworkResultRow(hit: SearchArtworkHit, terms: List<String>, onClick: () -> Unit) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = hit.imageUrl?.takeIf { it.isNotBlank() },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = highlight(hit.title?.takeIf { it.isNotBlank() } ?: "Untitled", terms),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val details = listOfNotNull(hit.artist, hit.year, hit.medium).filter { it.isNotBlank() }.joinToString(" · ")
            if (details.isNotEmpty()) {
                Text(
                    text = highlight(details, terms),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // the words were found in the description: show where
            hit.snippet?.takeIf { it.isNotBlank() }?.let { snippet ->
                Text(
                    text = highlight(snippet, terms),
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ArtistResultRow(hit: SearchArtistHit, terms: List<String>, onClick: () -> Unit) {
    val name = hit.name?.takeIf { it.isNotBlank() } ?: "Artist"
    val subtitle = when {
        !hit.username.isNullOrBlank() -> "@${hit.username} · Artistry artist"
        else -> listOfNotNull(hit.nationality, hit.artMovement).filter { it.isNotBlank() }.joinToString(" · ")
            .ifEmpty { "Artist" }
    }
    // no Artistry account behind it: a real artist, so the blue tick
    PersonLikeRow(
        avatar = hit.imageUrl, title = name, subtitle = subtitle, terms = terms,
        verified = hit.username.isNullOrBlank(), onClick = onClick
    )
}

@Composable
private fun PersonResultRow(hit: SearchPersonHit, terms: List<String>, onClick: () -> Unit) {
    val username = hit.username.orEmpty()
    val name = hit.name?.takeIf { it.isNotBlank() } ?: username
    val subtitle = "@$username" + if (!hit.artistId.isNullOrBlank()) " · Artist" else ""
    PersonLikeRow(avatar = hit.profilePicture, title = name, subtitle = subtitle, terms = terms, onClick = onClick)
}

@Composable
private fun PersonLikeRow(
    avatar: String?,
    title: String,
    subtitle: String,
    terms: List<String>,
    verified: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatAvatar(url = avatar, name = title, size = 52.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = highlight(title, terms),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (verified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    VerifiedBadge(size = 14.dp)
                }
            }
            Text(
                text = highlight(subtitle, terms),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchHint() {
    CenterBox {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Search artworks, artists and people",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Try a title, an artist, a style or words from a description",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun Message(title: String, body: String?, onRetry: (() -> Unit)?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        if (body != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (onRetry != null) {
            TextButton(onClick = onRetry) { Text(text = "Try again", color = Color(0xFF3897F0)) }
        }
    }
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        contentAlignment = Alignment.TopCenter
    ) { content() }
}

// Words of the query, for bolding them in the results (case-insensitive)
private fun highlightTerms(query: String): List<String> =
    query.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }.distinct()

private fun highlight(text: String, terms: List<String>): AnnotatedString = buildAnnotatedString {
    append(text)
    if (terms.isEmpty()) return@buildAnnotatedString
    val lower = text.lowercase()
    for (term in terms) {
        var from = 0
        while (true) {
            val at = lower.indexOf(term, from)
            if (at < 0) break
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), at, at + term.length)
            from = at + term.length
        }
    }
}
