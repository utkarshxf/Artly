package com.orion.templete.domain.repository.chat

import com.orion.templete.data.model.chat.ChatAuthState
import kotlinx.coroutines.flow.StateFlow

// App-wide chat session: Firebase custom-token sign-in (uid == username), presence heartbeat and FCM token.
interface ChatSession {
    val authState: StateFlow<ChatAuthState>

    // Conversation currently on screen; its push notifications are suppressed
    var activeConversationId: String?

    // Idempotent: signs in (if needed), publishes the profile, registers the push token, starts presence
    fun start()

    // Suspends until signed in with uid == current username; throws with a user-facing message otherwise
    suspend fun ensureSignedIn()

    suspend fun registerPushToken(token: String)

    // Logout: remove this device's push token, stop presence, sign out of Firebase
    suspend fun signOut()
}
