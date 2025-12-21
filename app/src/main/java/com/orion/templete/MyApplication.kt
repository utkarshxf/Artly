package com.orion.templete

import android.app.Application
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import coil.Coil
import coil.ImageLoader
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.facebook.FacebookSdk
import com.freshchat.consumer.sdk.Freshchat
import com.freshchat.consumer.sdk.FreshchatConfig
import com.orion.templete.util.TrackEvents
import dagger.hilt.android.HiltAndroidApp
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit


@HiltAndroidApp
class MyApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize FreshChat
        val freshchatConfig = FreshchatConfig(
            "90f57c79-7961-43bb-9684-a2877adaa8d8",
            "7fd48cef-b81a-4da8-8612-d2c266fa1b9a"
        )
        freshchatConfig.domain = "msdk.in.freshchat.com"
        freshchatConfig.setCameraCaptureEnabled(true)
        freshchatConfig.setGallerySelectionEnabled(true)
        freshchatConfig.setResponseExpectationEnabled(true)
        freshchatConfig.setTeamMemberInfoVisible(true)
        freshchatConfig.setUserEventsTrackingEnabled(true)
        freshchatConfig.setFileSelectionEnabled(true)
        Freshchat.getInstance(applicationContext)?.init(freshchatConfig)

        TrackEvents(this).trackAppOpened()

        // Create custom OkHttpClient that handles both Firebase and Wikimedia
        val okHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val originalUrl = originalRequest.url.toString()

                try {
                    Log.d("ImageLoading", "Processing URL: $originalUrl")

                    val requestBuilder = originalRequest.newBuilder()

                    when {
                        // Handle Wikimedia URLs
                        originalUrl.contains("wikimedia.org") -> {
                            Log.d("ImageLoading", "Detected Wikimedia URL")

                            requestBuilder.apply {
                                header("User-Agent", "Artistry/1.0 (Android; ${Build.MODEL}) Coil/2.0")
                                header("Referer", "https://en.wikipedia.org/")
                                header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                                header("Accept-Encoding", "gzip, deflate, br")
                                header("Accept-Language", "en-US,en;q=0.9")
                                header("Cache-Control", "no-cache")
                                header("Pragma", "no-cache")
                            }

                            // Handle URL decoding for Wikimedia
                            val decodedUrl = try {
                                URLDecoder.decode(originalUrl, StandardCharsets.UTF_8.name())
                            } catch (e: Exception) {
                                originalUrl
                            }

                            if (decodedUrl != originalUrl) {
                                Log.d("ImageLoading", "Using decoded URL: $decodedUrl")
                                try {
                                    requestBuilder.url(decodedUrl.toHttpUrl())
                                } catch (e: Exception) {
                                    Log.e("ImageLoading", "Error with decoded URL", e)
                                }
                            }
                        }

                        // Handle Firebase URLs
                        originalUrl.contains("firebasestorage.googleapis.com") -> {
                            Log.d("ImageLoading", "Detected Firebase URL")

                            requestBuilder.apply {
                                header("User-Agent", "Artwrk/1.0 (Android; ${Build.MODEL})")
                                header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                                // Firebase doesn't need special referer headers
                            }
                        }

                        // Handle other URLs with generic headers
                        else -> {
                            Log.d("ImageLoading", "Using generic headers")

                            requestBuilder.apply {
                                header("User-Agent", "Artwrk/1.0 (Android; ${Build.MODEL})")
                                header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                            }
                        }
                    }

                    val newRequest = requestBuilder.build()
                    var response = chain.proceed(newRequest)

                    // Retry logic specifically for Wikimedia 403 errors
                    if (!response.isSuccessful &&
                        originalUrl.contains("wikimedia.org") &&
                        response.code == 403) {

                        response.close()
                        Log.d("ImageLoading", "Retrying Wikimedia with browser User-Agent")

                        val retryRequest = originalRequest.newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
                            .header("Referer", "https://en.wikipedia.org/")
                            .build()

                        response = chain.proceed(retryRequest)
                    }

                    Log.d("ImageLoading", "Response code: ${response.code}")
                    response

                } catch (e: Exception) {
                    Log.e("ImageLoading", "Error processing URL: $originalUrl", e)

                    // Fallback with generic headers
                    val fallbackRequest = originalRequest.newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                        .build()
                    chain.proceed(fallbackRequest)
                }
            }
            .build()

        // Configure Coil ImageLoader
        val imageLoader = ImageLoader.Builder(this)
            .allowRgb565(false) // High quality
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .respectCacheHeaders(false)
            .memoryCachePolicy(CachePolicy.ENABLED) // Enable memory cache
            .diskCachePolicy(CachePolicy.ENABLED) // Enable disk cache
            .logger(DebugLogger())
            .build()

        Coil.setImageLoader(imageLoader)
    }
}