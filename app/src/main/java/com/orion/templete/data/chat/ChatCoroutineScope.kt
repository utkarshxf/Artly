package com.orion.templete.data.chat

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

// Application-wide scope for chat work that must outlive a screen (sign-in, push registration, presence,
// "notify the recipient" calls). One failing task never cancels the others.
@Singleton
class ChatCoroutineScope @Inject constructor() : CoroutineScope {
    override val coroutineContext: CoroutineContext =
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, error ->
            Log.w("ChatScope", "Background chat task failed", error)
        }
}
