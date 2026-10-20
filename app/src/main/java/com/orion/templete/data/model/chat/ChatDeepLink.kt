package com.orion.templete.data.model.chat

import kotlinx.coroutines.flow.MutableStateFlow

// A tapped chat notification: MainActivity puts the peer here, Home opens the thread and clears it.
object ChatDeepLink {
    const val EXTRA_PEER = "open_chat_peer"
    val pendingPeer = MutableStateFlow<String?>(null)
}
