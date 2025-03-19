package com.nearbuck.android.admin.presentation.add_screen.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.util.UUID


class ImageCropper(private val activity: ComponentActivity) {
    private var _croppedImageUri = MutableStateFlow<Uri?>(null)
    val croppedImageUri: Flow<Uri?> get() = _croppedImageUri
    private var widthRatio:Float = 0.0f
    private var heightRatio:Float = 0.0f
    private val resultPhotos: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val data: Intent = result.data!!
               _croppedImageUri.value = UCrop.getOutput(data)
            }
        }


    fun launchImageCrop(uri: Uri) {
        val destinationFileName = UUID.randomUUID().toString()
        val uCrop = UCrop.of(uri, Uri.fromFile(File(activity.cacheDir, destinationFileName))).withAspectRatio(1f , 1f)
        val intent = uCrop.getIntent(activity)

        resultPhotos.launch(intent)
    }
    fun clearCroppedImageUri() {
        _croppedImageUri.value = null
    }


}
