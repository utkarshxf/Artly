package com.orion.templete.data.chat

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/*
 * Chat push (FCM data messages sent by the backend's POST /chat/notify):
 *   {type: "chat_message", conversationId, messageId, sender, senderName, senderAvatar, preview}
 * Shows an Instagram-style notification unless that conversation is open on screen right now.
 */
@AndroidEntryPoint
class ChatMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var chatSession: ChatSessionImpl

    @Inject
    lateinit var notifications: ChatNotifications

    @Inject
    lateinit var secureStorage: SecureStorage

    @Inject
    lateinit var appScope: ChatCoroutineScope

    override fun onNewToken(token: String) {
        // No-op until chat is signed in; the token is registered again after every chat sign-in anyway
        appScope.launch {
            try {
                chatSession.registerPushToken(token)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't register the new push token", e)
            }
        }
    }

    // Runs on a background thread; the notification must be posted before returning (the process may be
    // stopped right after), so this blocks for at most a few seconds (the avatar download is capped at 1.5 s).
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data[KEY_TYPE] != TYPE_CHAT_MESSAGE) return
        val incoming = parse(data, message.sentTime) ?: return

        val me = secureStorage.getUserId()?.trim()
        // Logged out, addressed to a different account on this phone, or an echo of my own message
        if (me.isNullOrEmpty() || incoming.sender == me) return
        if (ChatIds.peerOf(incoming.conversationId, me) != incoming.sender) return
        // The conversation is on screen: the thread shows the message itself
        if (chatSession.isConversationVisible(incoming.conversationId)) return

        try {
            runBlocking {
                withTimeoutOrNull(POST_TIMEOUT_MS) { notifications.showMessage(incoming) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't show the chat notification", e)
        }
    }

    private fun parse(data: Map<String, String>, sentTime: Long): IncomingChatMessage? {
        val conversationId = data[KEY_CONVERSATION_ID]?.trim().orEmpty()
        val messageId = data[KEY_MESSAGE_ID]?.trim().orEmpty()
        val sender = data[KEY_SENDER]?.trim().orEmpty()
        if (conversationId.isEmpty() || messageId.isEmpty() || sender.isEmpty()) {
            Log.w(TAG, "Ignoring an incomplete chat push")
            return null
        }
        return IncomingChatMessage(
            conversationId = conversationId,
            messageId = messageId,
            sender = sender,
            senderName = data[KEY_SENDER_NAME]?.trim().orEmpty(),
            senderAvatar = data[KEY_SENDER_AVATAR]?.trim()?.takeIf { it.isNotEmpty() },
            preview = data[KEY_PREVIEW]?.trim().orEmpty(),
            sentAt = if (sentTime > 0L) sentTime else System.currentTimeMillis(),
        )
    }

    private companion object {
        const val TAG = "ChatMessaging"
        const val TYPE_CHAT_MESSAGE = "chat_message"
        const val KEY_TYPE = "type"
        const val KEY_CONVERSATION_ID = "conversationId"
        const val KEY_MESSAGE_ID = "messageId"
        const val KEY_SENDER = "sender"
        const val KEY_SENDER_NAME = "senderName"
        const val KEY_SENDER_AVATAR = "senderAvatar"
        const val KEY_PREVIEW = "preview"
        const val POST_TIMEOUT_MS = 8_000L
    }
}
