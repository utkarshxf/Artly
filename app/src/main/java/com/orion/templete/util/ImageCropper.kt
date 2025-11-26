package com.orion.templete.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCrop.Options
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.util.UUID


class ImageCropper(private val activity: ComponentActivity) {
    private var _croppedImageUri = MutableStateFlow<Uri?>(null)
    val croppedImageUri: Flow<Uri?> get() = _croppedImageUri
    private var resultPhotos: ActivityResultLauncher<Intent>? = null

    init {
        try {
            resultPhotos = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                    val data: Intent = result.data!!
                    _croppedImageUri.value = UCrop.getOutput(data)
                }
            }
        } catch (e: Exception) {
            // Handle the exception (e.g., activity already started)
            e.printStackTrace()
        }
    }

    /**
     * Launch the image cropper with the current aspect ratio
     */
    fun launchImageCrop(uri: Uri) {
        val destinationFileName = UUID.randomUUID().toString()
        val uCrop = UCrop.of(uri, Uri.fromFile(File(activity.cacheDir, destinationFileName)))


        val options = Options()
        uCrop.withOptions(options)

        val intent = uCrop.getIntent(activity)
        resultPhotos?.launch(intent)
    }

    /**
     * Create a crop intent with the current aspect ratio
     */
    fun createCropIntent(uri: Uri): Intent {
        val destinationFileName = UUID.randomUUID().toString()
        val uCrop = UCrop.of(uri, Uri.fromFile(File(activity.cacheDir, destinationFileName)))

        val options = Options()
        uCrop.withOptions(options)

        return uCrop.getIntent(activity)
    }

    fun handleActivityResult(data: Intent?) {
        if (data != null) {
            _croppedImageUri.value = UCrop.getOutput(data)
        }
    }

    fun clearCroppedImageUri() {
        _croppedImageUri.value = null
    }
}
