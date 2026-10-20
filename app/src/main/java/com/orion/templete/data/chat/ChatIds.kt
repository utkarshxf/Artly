package com.orion.templete.data.chat

// Conversation ids for 1:1 chats: the two usernames sorted ascending and joined with "__" (e.g. "alice__bob").
// Usernames may themselves contain underscores, so a conversation id is never split blindly: the peer is whatever
// remains after removing "<me>__" / "__<me>", and the result must rebuild the same id (mirrors the security rules).
object ChatIds {
    const val SEPARATOR = "__"

    fun conversationId(a: String, b: String): String {
        val x = a.trim()
        val y = b.trim()
        return if (x <= y) "$x$SEPARATOR$y" else "$y$SEPARATOR$x"
    }

    // The other party of `conversationId` for `me`, or null when `me` is not one of its two parties
    fun peerOf(conversationId: String, me: String): String? {
        if (me.isEmpty() || conversationId.isEmpty()) return null
        val candidates = ArrayList<String>(2)
        val prefix = me + SEPARATOR
        val suffix = SEPARATOR + me
        if (conversationId.startsWith(prefix)) candidates += conversationId.substring(prefix.length)
        if (conversationId.endsWith(suffix)) candidates += conversationId.substring(0, conversationId.length - suffix.length)
        return candidates.firstOrNull { peer ->
            peer.isNotEmpty() && peer != me && peer.trim() == peer && conversationId(me, peer) == conversationId
        }
    }

    // The two parties, sorted as stored in `usernames`; null when `me` is not a party
    fun partiesOf(conversationId: String, me: String): List<String>? {
        val peer = peerOf(conversationId, me) ?: return null
        return if (me < peer) listOf(me, peer) else listOf(peer, me)
    }
}
