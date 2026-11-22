package com.orion.templete.domain.tts

import android.content.Context
import android.util.Log
import coil.imageLoader
import coil.request.ImageRequest
import com.orion.templete.data.model.artwork_model.ArtworkDTO
import com.orion.templete.data.network.ApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Warms the reels feed: story text, narrated audio and artwork images are fetched
 * before the user reaches them, so a reel opens and speaks with no visible wait.
 * The backend caches generated audio forever, so each story is generated once
 * (friendly to the Gemini free-tier quota).
 */
@Singleton
class StoryPrefetcher @Inject constructor(
    @ApplicationContext context: Context,
    private val apiService: ApiService
) {
    private val appContext = context.applicationContext
    private val textCache = ConcurrentHashMap<String, String>()

    fun audioFile(artworkId: String, lang: String) =
        File(appContext.cacheDir, "story_${artworkId}_$lang.wav")

    /** Story text from the backend, cached in memory per artwork + language. Null = unavailable. */
    suspend fun storyText(artworkId: String, lang: String, region: String): String? {
        val cacheKey = "$artworkId:$lang"
        textCache[cacheKey]?.let { return it }
        val fetched = try {
            val response = apiService.getStoryText(artworkId, lang, region)
            if (response.isSuccessful) response.body()?.text else null
        } catch (e: Exception) {
            Log.w(TAG, "story text fetch failed for $artworkId", e)
            null
        }
        return fetched?.takeIf { it.isNotBlank() }?.also { textCache[cacheKey] = it }
    }

    /** Downloads the narrated WAV into the cache unless it is already there. False = unavailable. */
    suspend fun downloadAudio(artworkId: String, lang: String, region: String): Boolean {
        val file = audioFile(artworkId, lang)
        if (file.exists() && file.length() > 0) return true
        return try {
            val response = apiService.getStoryAudio(artworkId, lang, region)
            if (!response.isSuccessful) return false
            val bytes = response.body()?.bytes() ?: return false
            if (bytes.isEmpty()) return false
            file.writeBytes(bytes)
            true
        } catch (e: Exception) {
            Log.w(TAG, "story audio download failed for $artworkId", e)
            false
        }
    }

    /** Full warmup for one artwork: text, then audio (audio needs the story to exist server-side). */
    suspend fun warmupStory(artworkId: String, lang: String, region: String) {
        if (storyText(artworkId, lang, region) != null) {
            downloadAudio(artworkId, lang, region)
        }
    }

    /** Let coil cache the first images so the feed renders instantly. */
    fun enqueueImages(artworks: List<ArtworkDTO>, count: Int = 5) {
        val loader = appContext.imageLoader
        artworks.take(count).forEach { artwork ->
            val url = artwork.imageUrl ?: artwork.image_url_compressed ?: return@forEach
            loader.enqueue(ImageRequest.Builder(appContext).data(url).build())
        }
    }

    private companion object {
        const val TAG = "StoryPrefetcher"
    }
}
