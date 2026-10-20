package com.orion.templete.data.chat

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.transform.CircleCropTransformation
import com.orion.templete.R
import com.orion.templete.data.model.chat.ChatDeepLink
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

// A chat push as received from the backend (FCM data message of type "chat_message")
data class IncomingChatMessage(
    val conversationId: String,
    val messageId: String,
    val sender: String,
    val senderName: String,
    val senderAvatar: String?,
    val preview: String,
    val sentAt: Long,
)

// Posts Instagram-style message notifications: one MessagingStyle notification per conversation holding its
// last few lines, grouped under a summary, opening the thread when tapped.
@Singleton
class ChatNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private data class Line(val messageId: String?, val text: CharSequence, val time: Long)

    private class ConversationLines(var sender: String, var senderName: String, val lines: ArrayDeque<Line> = ArrayDeque())

    // conversationId -> recent lines; only touched while holding `lock`
    private val threads = LinkedHashMap<String, ConversationLines>() // oldest activity first
    private val lock = Any()

    private val largeIconSize: Int by lazy {
        context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width).coerceAtLeast(96)
    }

    fun ensureChannel() {
        try {
            val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName("Messages")
                .setDescription("Direct messages from other people on Artistry")
                .setShowBadge(true)
                .setVibrationEnabled(true)
                .setLightsEnabled(true)
                .build()
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't create the chat notification channel", e)
        }
    }

    fun canPost(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    suspend fun showMessage(message: IncomingChatMessage) {
        if (!canPost()) return
        ensureChannel()
        val id = notificationId(message.conversationId)
        val senderName = message.senderName.ifBlank { message.sender }
        val text = message.preview.ifBlank { "Sent you a message" }

        val lines: List<Line> = synchronized(lock) {
            val showing = activeNotification(id)
            var thread = threads[message.conversationId]
            if (thread != null && showing == null) {
                // The user dismissed or opened the old notification; start a fresh one
                threads.remove(message.conversationId)
                thread = null
            }
            if (thread == null) {
                thread = ConversationLines(message.sender, senderName)
                // After process death the in-memory lines are gone, but the posted notification still has them
                showing?.let { restoreLines(it) }?.let { thread.lines.addAll(it) }
            }
            if (thread.lines.any { it.messageId == message.messageId }) return // duplicate delivery
            // Most recent conversation last, so the summary lists it first
            threads.remove(message.conversationId)
            threads[message.conversationId] = thread
            thread.sender = message.sender
            thread.senderName = senderName
            // add()/removeAt(0), not addLast()/removeFirst(): against SDK 35+ those can bind to the Java 21 List
            // methods, which don't exist before Android 15
            thread.lines.add(Line(message.messageId, text, message.sentAt))
            while (thread.lines.size > MAX_LINES) thread.lines.removeAt(0)
            thread.lines.toList()
        }

        val avatar = loadAvatar(message.senderAvatar) ?: letterAvatar(senderName)
        val senderPerson = Person.Builder()
            .setKey(message.sender)
            .setName(senderName)
            .setIcon(IconCompat.createWithBitmap(avatar))
            .build()
        val style = NotificationCompat.MessagingStyle(Person.Builder().setKey(SELF_KEY).setName("You").build())
        lines.forEach { style.addMessage(NotificationCompat.MessagingStyle.Message(it.text, it.time, senderPerson)) }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_chat)
            .setColor(ACCENT_COLOR)
            .setLargeIcon(avatar)
            .setContentTitle(senderName)
            .setContentText(text)
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setWhen(message.sentAt)
            .setShowWhen(true)
            .setNumber(lines.size)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .setContentIntent(openChatIntent(message.sender, id))
            .build()

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(id, notification)
            postSummary(manager)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission was revoked", e)
        }
    }

    // Removes a conversation's notification (thread opened, chat read or deleted)
    fun clearConversation(conversationId: String) {
        val remaining = synchronized(lock) {
            threads.remove(conversationId)
            threads.size
        }
        try {
            val manager = NotificationManagerCompat.from(context)
            val id = notificationId(conversationId)
            manager.cancel(id)
            // The system list may still contain the one just cancelled, so it doesn't count
            if (remaining == 0 && postedChatNotificationCount(excludingId = id) == 0) {
                manager.cancel(SUMMARY_ID)
            } else if (remaining > 0) {
                postSummary(manager) // drop the cleared conversation's line from the summary
            }
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't cancel the chat notification", e)
        }
    }

    // Logout: nothing of the previous account may stay on screen
    fun clearAll() {
        synchronized(lock) { threads.clear() }
        try {
            val manager = NotificationManagerCompat.from(context)
            systemManager()?.activeNotifications?.forEach { posted ->
                if (posted.notification.group == GROUP_KEY) manager.cancel(posted.tag, posted.id)
            }
            manager.cancel(SUMMARY_ID)
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't clear chat notifications", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun postSummary(manager: NotificationManagerCompat) {
        val latest: List<Pair<String, CharSequence>> = synchronized(lock) {
            threads.values.mapNotNull { thread -> thread.lines.lastOrNull()?.let { thread.senderName to it.text } }
        }
        if (latest.isEmpty()) return
        val inbox = NotificationCompat.InboxStyle()
        latest.asReversed().take(MAX_LINES).forEach { (name, text) -> inbox.addLine("$name: $text") }
        val title = if (latest.size == 1) "New message" else "${latest.size} conversations"
        inbox.setSummaryText(title)
        val summary = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_chat)
            .setColor(ACCENT_COLOR)
            .setContentTitle(title)
            .setContentText(latest.last().let { (name, text) -> "$name: $text" })
            .setStyle(inbox)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()
        manager.notify(SUMMARY_ID, summary)
    }

    private fun openChatIntent(peer: String, requestCode: Int): PendingIntent {
        val intent = Intent(ACTION_OPEN_CHAT)
            .setClassName(context, MAIN_ACTIVITY)
            .putExtra(ChatDeepLink.EXTRA_PEER, peer)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(Intent.ACTION_MAIN)
            .setClassName(context, MAIN_ACTIVITY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context, SUMMARY_ID, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private suspend fun loadAvatar(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return try {
            withTimeoutOrNull(AVATAR_TIMEOUT_MS) {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(largeIconSize)
                    .allowHardware(false) // notifications are parcelled; hardware bitmaps can't be
                    .transformations(CircleCropTransformation())
                    .build()
                val result = Coil.imageLoader(context).execute(request)
                (result as? SuccessResult)?.drawable?.toBitmap(largeIconSize, largeIconSize, Bitmap.Config.ARGB_8888)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't load the sender's avatar", e)
            null
        }
    }

    private fun letterAvatar(name: String): Bitmap {
        val size = largeIconSize
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = AVATAR_COLORS[(name.hashCode() and Int.MAX_VALUE) % AVATAR_COLORS.size]
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        val trimmed = name.trim()
        val letter = if (trimmed.isEmpty()) "?" else String(Character.toChars(trimmed.codePointAt(0))).uppercase()
        paint.color = Color.WHITE
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = size * 0.42f
        canvas.drawText(letter, size / 2f, size / 2f - (paint.descent() + paint.ascent()) / 2f, paint)
        return bitmap
    }

    private fun restoreLines(posted: Notification): List<Line>? = try {
        NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(posted)
            ?.messages
            ?.mapNotNull { old -> old.text?.let { Line(null, it, old.timestamp) } }
            ?.takeLast(MAX_LINES)
    } catch (e: Exception) {
        null
    }

    private fun activeNotification(id: Int): Notification? = try {
        systemManager()?.activeNotifications?.firstOrNull { it.id == id && it.tag == null }?.notification
    } catch (e: Exception) {
        null
    }

    private fun postedChatNotificationCount(excludingId: Int): Int = try {
        systemManager()?.activeNotifications?.count {
            it.notification.group == GROUP_KEY && it.id != SUMMARY_ID && it.id != excludingId
        } ?: 0
    } catch (e: Exception) {
        0
    }

    private fun systemManager(): NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    companion object {
        private const val TAG = "ChatNotifications"
        const val CHANNEL_ID = "chat_messages"
        const val GROUP_KEY = "artistry_chat"
        const val ACTION_OPEN_CHAT = "com.orion.templete.action.OPEN_CHAT"
        // Referenced by name: MainActivity is owned by the UI layer
        private const val MAIN_ACTIVITY = "com.orion.templete.MainActivity"
        private const val SELF_KEY = "artistry_self"
        private const val SUMMARY_ID = 0x0A57C4A7
        private const val MAX_LINES = 5
        private const val AVATAR_TIMEOUT_MS = 1_500L
        private const val ACCENT_COLOR = 0xFFC2185B.toInt()
        private val AVATAR_COLORS = intArrayOf(
            0xFF91002F.toInt(), 0xFFC2185B.toInt(), 0xFF7B1FA2.toInt(), 0xFF3949AB.toInt(),
            0xFF00897B.toInt(), 0xFFF4511E.toInt(), 0xFF6D4C41.toInt(), 0xFF3897F0.toInt(),
        )

        // Also used by the thread screen to cancel a conversation's notification when it opens
        fun notificationId(conversationId: String): Int = conversationId.hashCode()
    }
}
