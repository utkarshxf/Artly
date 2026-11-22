package com.orion.templete.presentation.chat.share

import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.model.chat.ArtworkRef
import com.orion.templete.data.model.chat.OutgoingMessage
import com.orion.templete.data.model.chat.ProfileRef

// What a chat message needs to show this artwork as a post card; null when the artwork has no id
fun ArtworkDTO.toArtworkRef(): ArtworkRef? {
    val artworkId = id?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return ArtworkRef(
        id = artworkId,
        title = title?.trim()?.takeIf { it.isNotEmpty() },
        imageUrl = image_url_compressed?.takeIf { it.isNotBlank() } ?: imageUrl?.takeIf { it.isNotBlank() },
        artistName = artist?.trim()?.takeIf { it.isNotEmpty() },
    )
}

// Text for "Copy link": there is no per-artwork web page, so it names the artwork and links the app's site
fun ArtworkRef.shareLinkText(): String = buildString {
    title?.let { append("“").append(it).append("”") } ?: append("An artwork")
    artistName?.let { append(" by ").append(it) }
    append(" on Artistry\n").append(ARTISTRY_SITE)
}

const val ARTISTRY_SITE = "https://artwrk.studio/"

// What the share sheet sends: an artwork (post card) or a profile (profile card)
sealed interface SharePayload {
    data class Artwork(val artwork: ArtworkRef) : SharePayload
    data class Profile(val profile: ProfileRef) : SharePayload
}

fun SharePayload.toOutgoing(): OutgoingMessage = when (this) {
    is SharePayload.Artwork -> OutgoingMessage.Artwork(artwork)
    is SharePayload.Profile -> OutgoingMessage.Profile(profile)
}

fun SharePayload.shareLinkText(): String = when (this) {
    is SharePayload.Artwork -> artwork.shareLinkText()
    is SharePayload.Profile -> buildString {
        append(profile.name?.takeIf { it.isNotBlank() } ?: profile.id)
        append(" on Artistry\n").append(ARTISTRY_SITE)
    }
}

// "Share to…" for things without an image (profiles): plain text through the system share sheet
fun shareTextExternally(context: android.content.Context, text: String) {
    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, text)
    }
    val chooser = android.content.Intent.createChooser(send, "Share profile")
    if (context !is android.app.Activity) chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(chooser)
    } catch (e: android.content.ActivityNotFoundException) {
        android.widget.Toast.makeText(context, "No app to share with", android.widget.Toast.LENGTH_SHORT).show()
    }
}
