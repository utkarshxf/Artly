package com.orion.templete.data.call

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.orion.templete.data.model.call.CallStatus
import com.orion.templete.domain.repository.chat.ChatSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/*
 * Firestore half of the call signalling: a realtime listener on calls/{callId} (written only by the backend,
 * readable by the two participants). It is the fast path; a call must also finish without it (pushes, REST answers
 * and local timeouts), so every failure here is only reported, never thrown.
 */
@Singleton
class CallDocumentWatcher @Inject constructor(
    private val db: FirebaseFirestore,
    private val chatSession: ChatSession,
) {
    /**
     * Listens until the calling coroutine is cancelled (the call is over), which also removes the listener.
     * [onStatus] gets every status the document shows; fromServer = false means it came from the local cache.
     * [onFailure] is called each time the listener breaks; it is re-attached a few times, then given up.
     * Both run in the caller's context.
     */
    suspend fun watch(
        callId: String,
        onFailure: () -> Unit,
        onStatus: (status: CallStatus, fromServer: Boolean) -> Unit,
    ) {
        if (callId.isBlank() || '/' in callId) {
            onFailure()
            return
        }
        var failures = 0
        while (true) {
            try {
                // The rules only let a participant read the call, and the process may have just been started by a
                // push: the Firebase session (uid == username) has to be there first, or the read is denied
                val signedIn = withTimeoutOrNull(SIGN_IN_TIMEOUT_MS) {
                    chatSession.ensureSignedIn()
                    true
                }
                if (signedIn == null) throw IllegalStateException("The chat sign-in took too long")
                snapshots(callId).collect { snapshot ->
                    val status = if (snapshot.exists()) CallStatus.fromWire(snapshot.get(FIELD_STATUS) as? String) else null
                    if (status != null) {
                        val fromServer = !snapshot.metadata.isFromCache
                        if (fromServer) failures = 0
                        onStatus(status, fromServer)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "The call listener failed (${failures + 1})", e)
            }
            onFailure()
            if (failures >= RETRY_DELAYS_MS.size) {
                Log.w(TAG, "Giving up on the call listener; the call continues without it")
                return
            }
            delay(RETRY_DELAYS_MS[failures])
            failures++
        }
    }

    private fun snapshots(callId: String): Flow<DocumentSnapshot> = callbackFlow {
        // INCLUDE: also tells when a status first seen in the cache is confirmed by the server
        val registration = db.collection(COLLECTION_CALLS).document(callId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) trySend(snapshot)
            }
        awaitClose { registration.remove() }
    }.conflate() // a call's status only moves forward: the latest snapshot is all that matters

    private companion object {
        const val TAG = "CallWatcher"
        const val COLLECTION_CALLS = "calls"
        const val FIELD_STATUS = "status"
        const val SIGN_IN_TIMEOUT_MS = 20_000L
        val RETRY_DELAYS_MS = longArrayOf(1_000L, 3_000L, 6_000L)
    }
}
