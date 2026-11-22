package com.orion.templete.data.call

import android.util.Log
import com.google.gson.Gson
import com.orion.templete.data.model.call.CallCancelRequest
import com.orion.templete.data.model.call.CallConfigResponse
import com.orion.templete.data.model.call.CallDeclineRequest
import com.orion.templete.data.model.call.CallErrorResponse
import com.orion.templete.data.model.call.CallKind
import com.orion.templete.data.model.call.CallSessionResponse
import com.orion.templete.data.model.call.CallStartRequest
import com.orion.templete.data.model.call.CallStatus
import com.orion.templete.data.network.ApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

// What a call endpoint answered
sealed class CallReply<out T> {
    data class Ok<T>(val value: T) : CallReply<T>()

    // The server refused: 403 not a participant, 404 unknown call / callee, 409 impossible transition (callStatus
    // then says where the call is), 503 calls not configured
    data class Rejected(val code: Int, val message: String?, val callStatus: CallStatus?) : CallReply<Nothing>()

    // No usable answer: offline, timed out, or the hosting platform's own error page while the app restarts
    data class Unreachable(val cause: Throwable?) : CallReply<Nothing>()
}

// What is needed to join the Agora channel of a call. Not a data class: the token must never end up in a log.
class CallCredentials(
    val appId: String,
    val channel: String,
    val token: String,
    val uid: Int,
)

class CallSessionInfo(
    val callId: String,
    val status: CallStatus?,
    val conversationId: String?,
    val ringTimeoutSec: Int?,
    // null when the answer carried no (complete) channel / token
    val credentials: CallCredentials?,
)

/*
 * REST half of the call signalling (the backend is the only writer of calls/{callId}).
 * - start / config go through the app's normal client, whose 90 s read timeout covers a backend that is waking up.
 * - accept / token / decline / cancel / end must fail fast instead: each attempt is cut off after 12 s by cancelling
 *   the coroutine (Retrofit cancels the HTTP call with it) and is repeated. The backend answers a repeated
 *   transition with 200, so repeating is always safe.
 * Nothing here throws (except cancellation) and nothing logs a token.
 */
@Singleton
class CallBackend @Inject constructor(
    private val api: ApiService,
) {
    private val gson = Gson()

    suspend fun config(): CallReply<CallConfigResponse> =
        request(SLOW_TIMEOUT_MS, "config") { api.callConfig() }

    suspend fun start(callee: String, kind: CallKind): CallReply<CallSessionInfo> =
        session(null, request(SLOW_TIMEOUT_MS, "start") { api.callStart(CallStartRequest(callee, kind.wire)) })

    suspend fun accept(callId: String): CallReply<CallSessionInfo> = retrying {
        session(callId, request(FAST_TIMEOUT_MS, "accept") { api.callAccept(callId) })
    }

    // One attempt only: the callers have their own rhythm (token renewal, status probe)
    suspend fun token(callId: String): CallReply<CallSessionInfo> =
        session(callId, request(FAST_TIMEOUT_MS, "token") { api.callToken(callId) })

    suspend fun decline(callId: String, busy: Boolean): CallReply<Unit> = retrying {
        done(request(FAST_TIMEOUT_MS, "decline") { api.callDecline(callId, CallDeclineRequest(busy)) })
    }

    suspend fun cancel(callId: String, timeout: Boolean): CallReply<Unit> = retrying {
        done(request(FAST_TIMEOUT_MS, "cancel") { api.callCancel(callId, CallCancelRequest(timeout)) })
    }

    suspend fun end(callId: String): CallReply<Unit> = retrying {
        done(request(FAST_TIMEOUT_MS, "end") { api.callEnd(callId) })
    }

    private suspend fun <T : Any> request(
        timeoutMs: Long,
        what: String,
        block: suspend () -> Response<T>,
    ): CallReply<T> {
        return try {
            // withTimeoutOrNull, not withTimeout: its exception is a CancellationException and would look like the
            // caller being cancelled
            val response = withTimeoutOrNull(timeoutMs) { block() }
            when {
                response == null -> {
                    Log.w(TAG, "Call $what timed out")
                    CallReply.Unreachable(null)
                }
                response.isSuccessful -> {
                    val body = response.body()
                    if (body != null) CallReply.Ok(body) else CallReply.Unreachable(null)
                }
                else -> rejected(what, response)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Call $what failed: ${e.javaClass.simpleName}")
            CallReply.Unreachable(e)
        }
    }

    private fun rejected(what: String, response: Response<*>): CallReply<Nothing> {
        val code = response.code()
        val error: CallErrorResponse? = try {
            // Already buffered by Retrofit, so this does no network I/O
            val text = response.errorBody()?.string()
            if (text.isNullOrBlank() || text.trimStart().startsWith("<")) null
            else gson.fromJson(text, CallErrorResponse::class.java)
        } catch (e: Exception) {
            null
        }
        Log.w(TAG, "Call $what answered HTTP $code")
        // A 5xx without the backend's own JSON comes from the hosting platform (the app is restarting): temporary
        if (code >= 500 && error == null) return CallReply.Unreachable(null)
        return CallReply.Rejected(
            code = code,
            message = error?.message?.trim()?.takeIf { it.isNotEmpty() },
            callStatus = CallStatus.fromWire(error?.callStatus),
        )
    }

    private fun session(expectedCallId: String?, reply: CallReply<CallSessionResponse>): CallReply<CallSessionInfo> =
        when (reply) {
            is CallReply.Ok -> {
                val body = reply.value
                val callId = body.callId?.trim().orEmpty().ifEmpty { expectedCallId.orEmpty() }
                if (callId.isEmpty()) {
                    CallReply.Rejected(code = 0, message = null, callStatus = null)
                } else {
                    val appId = body.appId?.trim().orEmpty()
                    val token = body.token?.trim().orEmpty()
                    // One channel per call, named after it
                    val channel = body.channel?.trim().orEmpty().ifEmpty { callId }
                    val uid = body.uid ?: 0
                    val credentials =
                        if (appId.isEmpty() || token.isEmpty() || uid <= 0) null
                        else CallCredentials(appId = appId, channel = channel, token = token, uid = uid)
                    CallReply.Ok(
                        CallSessionInfo(
                            callId = callId,
                            status = CallStatus.fromWire(body.status),
                            conversationId = body.conversationId?.trim()?.takeIf { it.isNotEmpty() },
                            ringTimeoutSec = body.ringTimeoutSec?.takeIf { it > 0 },
                            credentials = credentials,
                        )
                    )
                }
            }
            is CallReply.Rejected -> reply
            is CallReply.Unreachable -> reply
        }

    private fun done(reply: CallReply<ResponseBody>): CallReply<Unit> = when (reply) {
        is CallReply.Ok -> {
            try {
                reply.value.close()
            } catch (e: Exception) {
                // nothing to do
            }
            CallReply.Ok(Unit)
        }
        is CallReply.Rejected -> reply
        is CallReply.Unreachable -> reply
    }

    // Repeats while the server could not be reached (or answered 5xx, except "not configured"); a clear answer,
    // good or bad, is final
    private suspend fun <T> retrying(block: suspend () -> CallReply<T>): CallReply<T> {
        var reply = block()
        var attempt = 0
        while (isTemporary(reply) && attempt < RETRY_DELAYS_MS.size) {
            delay(RETRY_DELAYS_MS[attempt])
            attempt++
            reply = block()
        }
        return reply
    }

    private fun isTemporary(reply: CallReply<*>): Boolean = when (reply) {
        is CallReply.Ok -> false
        is CallReply.Rejected -> reply.code >= 500 && reply.code != HTTP_NOT_CONFIGURED
        is CallReply.Unreachable -> true
    }

    companion object {
        private const val TAG = "CallBackend"
        const val HTTP_NOT_FOUND = 404
        const val HTTP_CONFLICT = 409
        const val HTTP_NOT_CONFIGURED = 503

        // The backend sleeps when idle: its first answer can take up to a minute
        private const val SLOW_TIMEOUT_MS = 75_000L
        private const val FAST_TIMEOUT_MS = 12_000L
        private val RETRY_DELAYS_MS = longArrayOf(1_000L, 3_000L)
    }
}
