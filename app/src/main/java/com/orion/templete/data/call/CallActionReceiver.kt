package com.orion.templete.data.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.orion.templete.domain.call.CallIntents
import com.orion.templete.domain.call.CallManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// The buttons of the call notification that need no screen: Decline (incoming ring) and Hang up / Cancel.
// Both are no-ops in CallManager when there is no matching call (e.g. a button pressed on a stale notification).
@AndroidEntryPoint
class CallActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var callManager: CallManager

    override fun onReceive(context: Context, intent: Intent) {
        try {
            when (intent.action) {
                CallIntents.ACTION_DECLINE -> callManager.decline()
                CallIntents.ACTION_HANG_UP -> callManager.hangUp()
                else -> Unit
            }
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't handle the call action ${intent.action}", e)
        }
    }

    private companion object {
        const val TAG = "CallActionReceiver"
    }
}
