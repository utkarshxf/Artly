package com.orion.templete.data.chat

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.orion.templete.data.model.call.CallKind
import com.orion.templete.data.model.call.CallPeer
import com.orion.templete.data.model.call.CallPush
import com.orion.templete.data.model.call.IncomingCallInvite
import com.orion.templete.domain.call.CallManager
import com.orion.templete.util.SecureStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/*
 * Chat push (FCM data messages sent by the backend's POST /chat/notify):
 *   {type: "chat_message", conversationId, messageId, sender, senderName, senderAvatar, preview}
 * Shows an Instagram-style notification unless that conversation is open on screen right now.
 *
 * Call pushes (data-only, high priority; keys in CallPush) are handed to CallManager:
 *   {type: "incoming_call", callId, kind, caller, callerName, callerAvatar, callee, conversationId, sentAt, ringTimeoutSec}
 *   {type: "call_ended", callId, reason, kind, caller, callerName, callerAvatar, callee, conversationId, sentAt}
 */
@AndroidEntryPoint
class ChatMessagingService : FirebaseMessagingService() {

    // Lazy: a chat push never touches the call code, and CallManager is first created on the main thread
    @Inject
    lateinit var callManager: dagger.Lazy<CallManager>

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
        val type: String = data[KEY_TYPE].orEmpty()
        if (type == CallPush.TYPE_INCOMING_CALL || type == CallPush.TYPE_CALL_ENDED) {
            try {
                handleCallPush(type, data, message.sentTime)
            } catch (e: Exception) {
                Log.e(TAG, "Couldn't handle the call push \"$type\"", e)
            }
            return
        }
        if (type != TYPE_CHAT_MESSAGE) return
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

    // "incoming_call": ring. "call_ended": the caller gave up, or the call was answered / declined on another
    // device of this account. CallManager decides what that means for this phone.
    private fun handleCallPush(type: String, data: Map<String, String>, sentTime: Long) {
        val callId = data[CallPush.KEY_CALL_ID]?.trim().orEmpty()
        val caller = data[CallPush.KEY_CALLER]?.trim().orEmpty()
        if (callId.isEmpty() || caller.isEmpty()) {
            Log.w(TAG, "Ignoring an incomplete call push")
            return
        }
        val me = secureStorage.getUserId()?.trim()
        if (me.isNullOrEmpty() || caller == me) {
            Log.i(TAG, "Ignoring a call push: logged out, or it is this account's own call")
            return
        }
        // A phone that still holds another account's push token must not ring for that account's calls
        val callee = data[CallPush.KEY_CALLEE]?.trim().orEmpty()
        val pushedConversation = data[CallPush.KEY_CONVERSATION_ID]?.trim().orEmpty()
        val forMe = if (callee.isNotEmpty()) callee == me else ChatIds.peerOf(pushedConversation, me) == caller
        if (!forMe) {
            Log.i(TAG, "Ignoring a call push addressed to another account")
            return
        }
        val kind = CallKind.fromWire(data[CallPush.KEY_KIND]?.trim())
        val peer = CallPeer(
            username = caller,
            name = data[CallPush.KEY_CALLER_NAME]?.trim()?.takeIf { it.isNotEmpty() },
            avatar = data[CallPush.KEY_CALLER_AVATAR]?.trim()?.takeIf { it.isNotEmpty() },
        )
        val conversationId = ChatIds.conversationId(me, caller)
        if (type == CallPush.TYPE_INCOMING_CALL) {
            val invite = IncomingCallInvite(
                callId = callId,
                kind = kind,
                caller = peer,
                conversationId = conversationId,
                sentAt = data[CallPush.KEY_SENT_AT]?.trim()?.toLongOrNull()
                    ?: if (sentTime > 0L) sentTime else System.currentTimeMillis(),
                ringTimeoutSec = data[CallPush.KEY_RING_TIMEOUT_SEC]?.trim()?.toIntOrNull()?.takeIf { it > 0 }
                    ?: CallPush.DEFAULT_RING_TIMEOUT_SEC,
            )
            onMainAndWait("incoming call") { callManager.get().onIncomingCall(invite) }
        } else {
            val reason = data[CallPush.KEY_REASON]?.trim().orEmpty()
            onMainAndWait("call ended") {
                callManager.get().onCallEndedPush(callId, reason, kind, peer, conversationId)
            }
        }
    }

    // CallManager lives on the main thread, and this method runs on a background one. It must not return before
    // the call has been handed over: the foreground service for a ringing call can only be started while the
    // high-priority push is being handled. If the main thread is busy for too long the hand-over still happens,
    // just without being waited for.
    private fun onMainAndWait(what: String, block: () -> Unit) {
        val done = CountDownLatch(1)
        Handler(Looper.getMainLooper()).post {
            try {
                block()
            } catch (e: Exception) {
                Log.e(TAG, "Couldn't handle the push: $what", e)
            } finally {
                done.countDown()
            }
        }
        try {
            if (!done.await(MAIN_THREAD_WAIT_MS, TimeUnit.MILLISECONDS)) {
                Log.w(TAG, "The main thread is busy; \"$what\" is handled late")
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
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
        const val MAIN_THREAD_WAIT_MS = 8_000L
    }
}
