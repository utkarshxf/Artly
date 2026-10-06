package com.orion.templete.data.call

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
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
import android.os.SystemClock
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
import com.orion.templete.data.model.call.CallDirection
import com.orion.templete.data.model.call.CallKind
import com.orion.templete.data.model.call.CallPeer
import com.orion.templete.data.model.call.CallPhase
import com.orion.templete.data.model.call.CallUiState
import com.orion.templete.data.model.chat.ChatDeepLink
import com.orion.templete.domain.call.CallIntents
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/*
 * Notifications of calls, built from CallUiState only (no tokens, no call state in any Intent):
 *   - the current call: ringing in (Answer / Decline, full-screen intent), ringing out or in progress (Hang up).
 *     It is normally the foreground service's notification; when the service can't run it is posted on its own;
 *   - missed calls: one per caller, opening the chat, with "Call back".
 * Channels: incoming (high, silent - CallRinger rings and vibrates), ongoing (low), missed (default).
 * Ids and channels can't collide with ChatNotifications: the call has its own id, missed calls carry a tag.
 */
@Singleton
class CallNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val largeIconSize: Int by lazy {
        context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width).coerceAtLeast(96)
    }

    // The last downloaded photo; a call only ever needs one
    @Volatile
    private var avatarUrl: String? = null

    @Volatile
    private var avatarBitmap: Bitmap? = null

    fun ensureChannels() {
        try {
            val manager = NotificationManagerCompat.from(context)
            manager.createNotificationChannel(
                NotificationChannelCompat.Builder(CHANNEL_INCOMING, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName("Incoming calls")
                    .setDescription("Audio and video calls from other people on Artistry")
                    // Silent on purpose: the ringer plays the ringtone and vibrates, following the ringer mode
                    .setSound(null, null)
                    .setVibrationEnabled(false)
                    .setShowBadge(false)
                    .build()
            )
            manager.createNotificationChannel(
                NotificationChannelCompat.Builder(CHANNEL_ONGOING, NotificationManagerCompat.IMPORTANCE_LOW)
                    .setName("Ongoing call")
                    .setDescription("Shown while you are on a call")
                    .setShowBadge(false)
                    .build()
            )
            manager.createNotificationChannel(
                NotificationChannelCompat.Builder(CHANNEL_MISSED, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName("Missed calls")
                    .setDescription("Calls you didn't answer")
                    .setShowBadge(true)
                    .build()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't create the call notification channels", e)
        }
    }

    fun canPost(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    // ------------------------------------------------------------------------------------------ current call

    /**
     * The notification for [state], built without waiting for anything (the photo is used only if it is already
     * loaded). inService = it will be shown by the foreground service, which Android requires for the "ongoing
     * call" style.
     */
    fun callNotification(state: CallUiState, inService: Boolean): Notification {
        ensureChannels()
        val peer = state.peer
        if (peer == null || state.phase == CallPhase.IDLE || state.phase == CallPhase.ENDED) return plain("Call ended")
        val name = displayName(peer)
        val avatar = cachedAvatar(peer) ?: letterAvatar(name)
        val person = Person.Builder()
            .setName(name)
            .setKey(peer.username)
            .setIcon(IconCompat.createWithBitmap(avatar))
            .setImportant(true)
            .build()
        val video = state.kind == CallKind.VIDEO
        return if (state.phase == CallPhase.INCOMING_RINGING) {
            incoming(person, name, avatar, video)
        } else {
            ongoing(state, person, name, avatar, inService)
        }
    }

    private fun incoming(person: Person, name: String, avatar: Bitmap, video: Boolean): Notification {
        val show = showIntent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            !NotificationManagerCompat.from(context).canUseFullScreenIntent()
        ) {
            // The call then only shows as a heads-up notification; the call screen can't come up over the lock screen
            Log.w(TAG, "Full-screen intents are not allowed for this app")
        }
        return NotificationCompat.Builder(context, CHANNEL_INCOMING)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setColor(ACCENT_COLOR)
            .setLargeIcon(avatar)
            .setContentTitle(name)
            .setContentText(if (video) "Incoming video call" else "Incoming audio call")
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(person, declineIntent(), answerIntent()).setIsVideo(video)
            )
            .addPerson(person)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true) // updating it (photo loaded) must not pop it up again
            .setShowWhen(false)
            .setContentIntent(show)
            // Always set: brings the call screen up over the lock screen, and Android only accepts the call style
            // outside a foreground service together with it
            .setFullScreenIntent(show, true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun ongoing(
        state: CallUiState,
        person: Person,
        name: String,
        avatar: Bitmap,
        inService: Boolean,
    ): Notification {
        val video = state.kind == CallKind.VIDEO || state.showsVideo
        val text = when (state.phase) {
            CallPhase.OUTGOING_STARTING -> "Calling…"
            CallPhase.OUTGOING_RINGING -> "Ringing…"
            CallPhase.CONNECTING -> "Connecting…"
            CallPhase.RECONNECTING -> "Reconnecting…"
            else -> if (video) "Video call" else "Audio call"
        }
        val hangUp = hangUpIntent()
        val builder = NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setColor(ACCENT_COLOR)
            .setLargeIcon(avatar)
            .setContentTitle(name)
            .setContentText(text)
            .addPerson(person)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(showIntent())
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        val connectedAt = state.connectedAtElapsed
        if (connectedAt != null && state.phase != CallPhase.OUTGOING_STARTING && state.phase != CallPhase.OUTGOING_RINGING) {
            // The call timer, counted from the moment both sides were connected
            builder.setWhen(System.currentTimeMillis() - (SystemClock.elapsedRealtime() - connectedAt))
                .setShowWhen(true)
                .setUsesChronometer(true)
        } else {
            builder.setShowWhen(false)
        }
        if (inService) {
            builder.setStyle(NotificationCompat.CallStyle.forOngoingCall(person, hangUp).setIsVideo(video))
        } else {
            // Posted without the service: Android 12+ refuses the "ongoing call" style there
            val label = if (state.direction == CallDirection.OUTGOING && state.phase.isRinging) "Cancel" else "Hang up"
            builder.addAction(0, label, hangUp)
        }
        return builder.build()
    }

    private fun plain(text: String): Notification = fallbackNotification(context, text)

    // The service could not be started (Android refused it): the call still needs a notification
    @SuppressLint("MissingPermission")
    fun postStandalone(state: CallUiState) {
        try {
            if (state.phase == CallPhase.IDLE) {
                cancelStandalone()
                return
            }
            if (!canPost()) return
            NotificationManagerCompat.from(context)
                .notify(CALL_NOTIFICATION_ID, callNotification(state, inService = false))
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't post the call notification", e)
        }
    }

    fun cancelStandalone() {
        try {
            NotificationManagerCompat.from(context).cancel(CALL_NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't remove the call notification", e)
        }
    }

    // ------------------------------------------------------------------------------------------ missed calls

    // Posted at once, with the photo only if it is already loaded. alert = false: a silent update of the same one.
    @SuppressLint("MissingPermission")
    fun showMissedCall(caller: CallPeer, kind: CallKind, alert: Boolean = true) {
        try {
            if (!canPost()) return
            ensureChannels()
            val name = displayName(caller)
            val notification = NotificationCompat.Builder(context, CHANNEL_MISSED)
                .setSmallIcon(R.drawable.ic_stat_call_missed)
                .setColor(ACCENT_COLOR)
                .setLargeIcon(cachedAvatar(caller) ?: letterAvatar(name))
                .setContentTitle(name)
                .setContentText(if (kind == CallKind.VIDEO) "Missed video call" else "Missed audio call")
                .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setAutoCancel(true)
                .setOnlyAlertOnce(!alert)
                .setContentIntent(openChatIntent(caller.username))
                .addAction(0, "Call back", callBackIntent(caller, kind))
                .build()
            NotificationManagerCompat.from(context).notify(TAG_MISSED, missedId(caller.username), notification)
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't post the missed call", e)
        }
    }

    // Downloads the caller's photo and puts it on the missed-call notification, if that is still showing
    suspend fun attachMissedCallAvatar(caller: CallPeer, kind: CallKind) {
        if (cachedAvatar(caller) != null) return // it was posted with the photo already
        loadAvatar(caller) ?: return
        val id = missedId(caller.username)
        val showing = try {
            NotificationManagerCompat.from(context).activeNotifications.any { it.tag == TAG_MISSED && it.id == id }
        } catch (e: Exception) {
            false
        }
        if (showing) showMissedCall(caller, kind, alert = false)
    }

    // Calling them back (or any new call to them) settles the missed call
    fun cancelMissedCall(username: String) {
        try {
            NotificationManagerCompat.from(context).cancel(TAG_MISSED, missedId(username))
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't remove the missed call", e)
        }
    }

    // ------------------------------------------------------------------------------------------ photos

    fun cachedAvatar(peer: CallPeer?): Bitmap? {
        val url = peer?.avatar?.takeIf { it.isNotBlank() } ?: return null
        return if (avatarUrl == url) avatarBitmap else null
    }

    // The peer's photo as a round bitmap, or null (no photo, slow network). Never blocks for long.
    suspend fun loadAvatar(peer: CallPeer): Bitmap? {
        val url = peer.avatar?.takeIf { it.isNotBlank() } ?: return null
        cachedAvatar(peer)?.let { return it }
        return try {
            val bitmap = withTimeoutOrNull(AVATAR_TIMEOUT_MS) {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(largeIconSize)
                    .allowHardware(false) // notifications are parcelled; hardware bitmaps can't be
                    .transformations(CircleCropTransformation())
                    .build()
                val result = Coil.imageLoader(context).execute(request)
                (result as? SuccessResult)?.drawable?.toBitmap(largeIconSize, largeIconSize, Bitmap.Config.ARGB_8888)
            }
            if (bitmap != null) {
                avatarBitmap = bitmap
                avatarUrl = url
            }
            bitmap
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't load the caller's photo", e)
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

    // The call style refuses a person without a name
    private fun displayName(peer: CallPeer): String =
        peer.displayName.trim().ifEmpty { "Artistry" }

    // ------------------------------------------------------------------------------------------ intents

    private fun showIntent(): PendingIntent =
        PendingIntent.getActivity(context, REQUEST_SHOW, CallIntents.show(context), PENDING_FLAGS)

    // An activity, not a broadcast: answering needs the screen (permissions, unlocking)
    private fun answerIntent(): PendingIntent =
        PendingIntent.getActivity(context, REQUEST_ANSWER, CallIntents.answer(context), PENDING_FLAGS)

    private fun declineIntent(): PendingIntent = receiverIntent(REQUEST_DECLINE, CallIntents.ACTION_DECLINE)

    private fun hangUpIntent(): PendingIntent = receiverIntent(REQUEST_HANG_UP, CallIntents.ACTION_HANG_UP)

    private fun receiverIntent(requestCode: Int, action: String): PendingIntent {
        val intent = Intent(action).setClass(context, CallActionReceiver::class.java)
        return PendingIntent.getBroadcast(context, requestCode, intent, PENDING_FLAGS)
    }

    // The same deep link a chat notification uses (ChatDeepLink.EXTRA_PEER on MainActivity). Its own action keeps
    // this PendingIntent apart from the chat notification's for the same person.
    private fun openChatIntent(peer: String): PendingIntent {
        val intent = Intent(ACTION_OPEN_CHAT_FROM_CALL)
            .setClassName(context, MAIN_ACTIVITY)
            .putExtra(ChatDeepLink.EXTRA_PEER, peer)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(context, missedId(peer), intent, PENDING_FLAGS)
    }

    private fun callBackIntent(caller: CallPeer, kind: CallKind): PendingIntent =
        PendingIntent.getActivity(
            context,
            // Extras don't tell PendingIntents apart: one request code per person and kind
            (caller.username + "|" + kind.wire).hashCode(),
            CallIntents.call(context, caller, kind),
            PENDING_FLAGS,
        )

    private fun missedId(username: String): Int = username.trim().hashCode()

    companion object {
        private const val TAG = "CallNotifications"

        const val CHANNEL_INCOMING = "call_incoming"
        const val CHANNEL_ONGOING = "call_ongoing"
        const val CHANNEL_MISSED = "call_missed"

        // The current call's notification (also the foreground service's)
        const val CALL_NOTIFICATION_ID = 0x0CA11001

        // Missed calls: this tag + a per-caller id. Chat notifications have no tag, so the two can never clash.
        private const val TAG_MISSED = "missed_call"

        private const val ACTION_OPEN_CHAT_FROM_CALL = "com.orion.templete.action.OPEN_CHAT_FROM_CALL"

        // Referenced by name: MainActivity is owned by the UI layer
        private const val MAIN_ACTIVITY = "com.orion.templete.MainActivity"

        private const val REQUEST_SHOW = 0x0CA11010
        private const val REQUEST_ANSWER = 0x0CA11011
        private const val REQUEST_DECLINE = 0x0CA11012
        private const val REQUEST_HANG_UP = 0x0CA11013
        private const val PENDING_FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        private const val AVATAR_TIMEOUT_MS = 2_500L
        private const val ACCENT_COLOR = 0xFFC2185B.toInt()
        private val AVATAR_COLORS = intArrayOf(
            0xFF91002F.toInt(), 0xFFC2185B.toInt(), 0xFF7B1FA2.toInt(), 0xFF3949AB.toInt(),
            0xFF00897B.toInt(), 0xFFF4511E.toInt(), 0xFF6D4C41.toInt(), 0xFF3897F0.toInt(),
        )

        // A notification that needs nothing but a Context: for the service's startForeground() when there is no
        // call to describe (any more), or when the real one could not be built
        fun fallbackNotification(context: Context, text: String = "Call"): Notification {
            try {
                NotificationManagerCompat.from(context).createNotificationChannel(
                    NotificationChannelCompat.Builder(CHANNEL_ONGOING, NotificationManagerCompat.IMPORTANCE_LOW)
                        .setName("Ongoing call")
                        .setShowBadge(false)
                        .build()
                )
            } catch (e: Exception) {
                Log.w(TAG, "Couldn't create the call notification channel", e)
            }
            val show = PendingIntent.getActivity(context, REQUEST_SHOW, CallIntents.show(context), PENDING_FLAGS)
            return NotificationCompat.Builder(context, CHANNEL_ONGOING)
                .setSmallIcon(R.drawable.ic_stat_call)
                .setColor(ACCENT_COLOR)
                .setContentTitle("Artistry")
                .setContentText(text)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setContentIntent(show)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build()
        }
    }
}
