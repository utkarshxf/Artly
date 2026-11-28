package com.orion.templete.data.repository

import android.content.Context
import com.orion.templete.data.local.entity.FavouriteEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline image file store for favourite artworks.
 *
 * Files live in the app's private directory (no permissions needed, never
 * shared with Coil's cache). Downloads go to a .tmp file, the size is
 * checked, then the file is renamed into place. After each download the
 * 200 MB cap is enforced: READY copies with the oldest lastViewedAt are
 * evicted (file deleted, row marked EVICTED); they stay favourites and
 * re-download when viewed online.
 *
 * The compressed rendition is stored, not the full museum original: the cap
 * exists so many favourites survive offline, not a handful of 40 MB files.
 */
@Singleton
class OfflineImageManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val store: FavouritesOfflineStore
) {
    private val client = OkHttpClient()

    private val dir: File
        get() = File(appContext.filesDir, OFFLINE_DIR).apply { mkdirs() }

    fun fileFor(artworkId: String): File = File(dir, "$artworkId.img")

    /** Downloads the compressed image into the store and enforces the cap. Returns bytes written. */
    suspend fun downloadAndStore(artworkId: String, imageUrl: String): Long = withContext(Dispatchers.IO) {
        val tmp = File(dir, "$artworkId.tmp")
        try {
            val request = Request.Builder().url(imageUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Download failed (HTTP ${response.code})")
                val body = response.body ?: error("Empty response body")
                body.byteStream().use { input ->
                    tmp.outputStream().use { input.copyTo(it) }
                }
            }
            val bytes = tmp.length()
            if (bytes <= 0) error("Downloaded file is empty")
            val dest = fileFor(artworkId)
            if (dest.exists()) dest.delete()
            if (!tmp.renameTo(dest)) error("Could not move download into place")
            val evicted = store.recordOfflineCopy(artworkId, dest.absolutePath, bytes, CAP_BYTES)
            evicted.forEach { fileFor(it.artworkId).delete() }
            bytes
        } catch (t: Throwable) {
            tmp.delete()
            throw t
        }
    }

    /** Launch cleanup: delete orphaned files and fix rows whose file went missing. */
    suspend fun reconcile() = withContext(Dispatchers.IO) {
        val known = store.readyCopies().map { it.artworkId }.toSet()
        dir.listFiles()?.forEach { file ->
            val id = file.name.removeSuffix(".img").removeSuffix(".tmp")
            if (file.extension == "tmp" || (file.extension == "img" && id !in known)) {
                file.delete()
            }
        }
        store.readyCopies().forEach { row ->
            if (!fileFor(row.artworkId).exists()) {
                store.recordMissingCopy(row.artworkId)
            }
        }
    }

    companion object {
        const val OFFLINE_DIR = "offline_artworks"
        const val CAP_BYTES = 200L * 1024 * 1024 // 200 MB LRU cap (approved scope)
    }
}
