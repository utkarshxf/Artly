package com.orion.templete.util

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.orion.templete.domain.repository.chat.ChatSession
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UploadEntryPoint {
    fun chatSession(): ChatSession
}

private val uploadScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

// Uploads a picked/cropped image to Firebase Storage (images/<random>.jpg) and returns its download URL.
// Exactly one of onSuccess / onFailure is called; failures are logged and shown as a toast.
fun uploadImage(
    uri: Uri?,
    context: Context,
    onFailure: (Exception) -> Unit = {},
    onSuccess: (String) -> Unit,
) {
    val appContext = context.applicationContext
    uploadScope.launch {
        try {
            requireNotNull(uri) { "No image selected" }
            // Storage rules only accept signed-in uploads. The Firebase session is created at app start (chat
            // sign-in), which can fail while the backend is waking up, so make sure it exists first.
            try {
                EntryPointAccessors.fromApplication(appContext, UploadEntryPoint::class.java)
                    .chatSession().ensureSignedIn()
            } catch (e: Exception) {
                Log.w("UploadingImageToFirebase", "Firebase sign-in before upload failed", e)
            }
            val type = appContext.contentResolver.getType(uri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
            val ref = FirebaseStorage.getInstance().reference.child("images/${UUID.randomUUID()}.jpg")
            ref.putFile(uri, StorageMetadata.Builder().setContentType(type).build()).await()
            val url = ref.downloadUrl.await().toString()
            Log.d("UploadingImageToFirebase", "Uploaded ${ref.path}")
            onSuccess(url)
        } catch (e: Exception) {
            Log.e("UploadingImageToFirebase", "Image upload failed", e)
            Toast.makeText(appContext, "Image upload failed. Please try again.", Toast.LENGTH_SHORT).show()
            onFailure(e)
        }
    }
}

fun extractYear(dateString: String?): String? {
    return if (dateString.isNullOrEmpty()) {
        null
    } else {
        try {
            // Extract just the year portion (first 4 characters)
            dateString.substring(0, 4)
        } catch (e: Exception) {
            null
        }
    }
}
fun getSourceUrlSiteName(url: String): String {
    return try {
        val uri = java.net.URI(url)
        val host = uri.host

        // Remove "www." if present
        val siteName = if (host.startsWith("www.")) {
            host.substring(4)
        } else {
            host
        }

        // Get domain without TLD (e.g., "example" from "example.com")
        val parts = siteName.split(".")
        if (parts.size >= 2) {
            parts[parts.size - 2]
        } else {
            siteName
        }
    } catch (e: Exception) {
        "Unknown site"
    }
}