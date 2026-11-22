package com.orion.templete.data.chat

// Thrown by ChatSession.ensureSignedIn() (and therefore by every repository call) when the chat session
// can't be established. `message` is safe to show to the user as is.
class ChatUnavailableException(
    override val message: String,
    val notConfigured: Boolean = false,
    cause: Throwable? = null,
) : Exception(message, cause)

object ChatErrors {
    const val SIGNED_OUT = "Log in to use messages."
    const val NOT_CONFIGURED = "Messages aren't available yet. Please try again later."
    const val SESSION_EXPIRED = "Your session has expired. Log in again to use messages."
    const val OFFLINE = "No internet connection. Check your connection and try again."
    const val SERVER = "Messages are temporarily unavailable. Please try again in a moment."
    const val GENERIC = "Couldn't connect to messages. Please try again."
    const val WRONG_ACCOUNT = "Messages are signed in to a different account. Log in again to continue."
}
