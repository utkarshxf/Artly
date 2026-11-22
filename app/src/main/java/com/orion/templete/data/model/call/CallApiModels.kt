package com.orion.templete.data.model.call

// Wire models of the call endpoints (chat/call/...). Everything is nullable: Gson fills these by reflection and
// does not know about Kotlin's non-null types, so a field the server leaves out must not blow up later.

// GET chat/call/config
data class CallConfigResponse(
    val enabled: Boolean? = null,
    val appId: String? = null,
    val ringTimeoutSec: Int? = null,
)

// POST chat/call/start
data class CallStartRequest(
    val callee: String,
    val kind: String, // CallKind.wire
)

// POST chat/call/{callId}/decline; busy = this phone is on another call
data class CallDeclineRequest(
    val busy: Boolean = false,
)

// POST chat/call/{callId}/cancel; timeout = nobody answered within the ring time (the call becomes "missed")
data class CallCancelRequest(
    val timeout: Boolean = false,
)

// The "session JSON" every call endpoint answers with. channel / appId / token / uid / expiresInSec come with
// start, accept and token; ringTimeoutSec only with start.
data class CallSessionResponse(
    val callId: String? = null,
    val status: String? = null, // CallStatus.wire
    val kind: String? = null,
    val caller: String? = null,
    val callee: String? = null,
    val conversationId: String? = null,
    val durationSec: Int? = null,
    val channel: String? = null,
    val appId: String? = null,
    val token: String? = null,
    val uid: Int? = null,
    val expiresInSec: Int? = null,
    val ringTimeoutSec: Int? = null,
) {
    // The token is a credential: it must never reach a log through an accidental "$response"
    override fun toString(): String = "CallSessionResponse(callId=$callId, status=$status, kind=$kind)"
}

// Error body of the call endpoints: {"message": "...", "status": false} and, with 409, the call's current status.
// "status" is left out on purpose: the server framework's own error pages put a number there, which would make
// the whole body unreadable for Gson.
data class CallErrorResponse(
    val message: String? = null,
    val callStatus: String? = null,
)
