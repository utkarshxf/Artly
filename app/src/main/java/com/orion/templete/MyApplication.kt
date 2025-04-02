package com.orion.templete

import android.app.Application
import android.graphics.Bitmap
import coil.Coil
import coil.ImageLoader
import coil.util.DebugLogger
import com.freshchat.consumer.sdk.Freshchat
import com.freshchat.consumer.sdk.FreshchatConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication: Application(){
    override fun onCreate() {
        super.onCreate()
        // Initialize FreshChat
        val freshchatConfig = FreshchatConfig("90f57c79-7961-43bb-9684-a2877adaa8d8", "7fd48cef-b81a-4da8-8612-d2c266fa1b9a")
        freshchatConfig.domain = "msdk.in.freshchat.com"
        freshchatConfig.setCameraCaptureEnabled(true)
        freshchatConfig.setGallerySelectionEnabled(true)
        freshchatConfig.setResponseExpectationEnabled(true)
        freshchatConfig.setTeamMemberInfoVisible(true)
        freshchatConfig.setUserEventsTrackingEnabled(true)
        freshchatConfig.setFileSelectionEnabled(true)
        Freshchat.getInstance(applicationContext)?.init(freshchatConfig)

        val imageLoader = ImageLoader.Builder(this)
            .allowRgb565(false) // Don't use RGB565 which reduces quality
            .bitmapConfig(Bitmap.Config.ARGB_8888) // Use full quality config
            .components {
                // Add custom decoders that preserve quality
//                add(HighQualityJpegDecoder(this))
            }
            .logger(DebugLogger()) // For debugging loading issues
            .build()

        Coil.setImageLoader(imageLoader)
    }

}