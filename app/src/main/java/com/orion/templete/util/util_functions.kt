package com.orion.templete.util

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage

//Firebase Fun to Upload
fun uploadImage(uri: Uri?, context: Context, onSuccess: (String) -> Unit) {
    Log.d("UploadingImageToFirebase","Image Url: ${uri.toString()}")
    val storage = FirebaseStorage.getInstance()
    val storageRef = storage.reference
    val imageRef = storageRef.child("images/${uri!!.lastPathSegment}.jpg")

    val uploadTask = uri.let {
        imageRef.putFile(it)
    }

    uploadTask.addOnSuccessListener {
        imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
            onSuccess(downloadUrl.toString())
        }
    }.addOnFailureListener {
        Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
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