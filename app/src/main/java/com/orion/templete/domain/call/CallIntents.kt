package com.orion.templete.domain.call

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.orion.templete.data.model.call.CallKind
import com.orion.templete.data.model.call.CallPeer

// How the call screen is opened. The activity is addressed by name so the engine/notification code does not
// depend on the UI class. Intents carry no tokens and no call state: the screen reads CallManager.state.
object CallIntents {
    const val CALL_ACTIVITY = "com.orion.templete.presentation.call.CallActivity"

    // Show the current call (incoming ring, outgoing, or in progress)
    const val ACTION_SHOW = "com.orion.templete.call.SHOW"

    // "Answer" on the incoming-call notification: show the call and accept it
    const val ACTION_ANSWER = "com.orion.templete.call.ANSWER"

    // Place a call to EXTRA_PEER_* (e.g. "Call back" on a missed-call notification)
    const val ACTION_CALL = "com.orion.templete.call.CALL"
    const val EXTRA_PEER_USERNAME = "peer_username"
    const val EXTRA_PEER_NAME = "peer_name"
    const val EXTRA_PEER_AVATAR = "peer_avatar"
    const val EXTRA_VIDEO = "video"

    // Broadcasts handled by the call receiver (notification buttons)
    const val ACTION_DECLINE = "com.orion.templete.call.DECLINE"
    const val ACTION_HANG_UP = "com.orion.templete.call.HANG_UP"

    fun show(context: Context): Intent = base(context, ACTION_SHOW)

    fun answer(context: Context): Intent = base(context, ACTION_ANSWER)

    fun call(context: Context, peer: CallPeer, kind: CallKind): Intent = base(context, ACTION_CALL)
        .putExtra(EXTRA_PEER_USERNAME, peer.username)
        .putExtra(EXTRA_PEER_NAME, peer.name)
        .putExtra(EXTRA_PEER_AVATAR, peer.avatar)
        .putExtra(EXTRA_VIDEO, kind == CallKind.VIDEO)

    private fun base(context: Context, action: String): Intent = Intent(action)
        .setClassName(context.packageName, CALL_ACTIVITY)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
}

// Runtime permissions for calls. Ask before placing the first call; on an incoming call ask when Accept is tapped.
object CallPermissions {
    // Without these the call cannot work: the microphone always; the camera only to send video
    fun required(kind: CallKind): List<String> =
        if (kind == CallKind.VIDEO) listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        else listOf(Manifest.permission.RECORD_AUDIO)

    // Asked together with the required ones, never blocking: Bluetooth headsets (Android 12+)
    fun optional(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) listOf(Manifest.permission.BLUETOOTH_CONNECT) else emptyList()

    fun hasMicrophone(context: Context): Boolean = granted(context, Manifest.permission.RECORD_AUDIO)

    fun hasCamera(context: Context): Boolean = granted(context, Manifest.permission.CAMERA)

    fun hasBluetooth(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || granted(context, Manifest.permission.BLUETOOTH_CONNECT)

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
