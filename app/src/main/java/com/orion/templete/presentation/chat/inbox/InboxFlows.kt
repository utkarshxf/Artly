package com.orion.templete.presentation.chat.inbox

import android.util.Log
import com.orion.templete.data.model.chat.ChatUser
import com.orion.templete.domain.repository.chat.ChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ChatInbox"
private const val SEARCH_DEBOUNCE_MS = 300L
private const val SEARCH_LIMIT = 20

// Live profiles for a changing set of usernames. Each distinct peer gets exactly one observeUser() subscription:
// it starts when the peer first appears, is cancelled when the peer leaves the set, and a new set that still
// contains a peer never re-subscribes it. A failing profile retries on its own with backoff.
internal fun peerProfilesFlow(
    repository: ChatRepository,
    peers: Flow<Set<String>>,
): Flow<Map<String, ChatUser>> = channelFlow {
    val profiles = MutableStateFlow<Map<String, ChatUser>>(emptyMap())
    val jobs = HashMap<String, Job>() // only touched from the peers.collect block below
    launch { profiles.collect { send(it) } }
    peers.collect { wanted ->
        val gone = jobs.keys.filter { it !in wanted }
        gone.forEach { jobs.remove(it)?.cancel() }
        if (gone.isNotEmpty()) profiles.update { it - gone.toSet() }
        for (peer in wanted) {
            if (peer.isBlank() || jobs[peer]?.isActive == true) continue
            jobs[peer] = launch {
                repository.observeUser(peer)
                    .retryWhen { cause, attempt ->
                        if (cause is CancellationException) return@retryWhen false
                        Log.w(TAG, "Profile of $peer unavailable (attempt ${attempt + 1})", cause)
                        delay((2_000L shl attempt.coerceAtMost(5).toInt()).coerceAtMost(60_000L))
                        true
                    }
                    .collect { user ->
                        if (user != null) profiles.update { it + (peer to user) }
                    }
            }
        }
    }
}

// Remote people search ("More people" / new message results)
internal data class RemoteSearch(
    val query: String = "",
    val loading: Boolean = false,
    val users: List<ChatUser> = emptyList(),
    val failed: Boolean = false,
)

// Debounced (300 ms) searchUsers() for the latest query; a new query cancels the running request. The previous
// results stay visible while the next query is loading. Bumping `retry` repeats the current query.
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
internal fun remoteSearchFlow(
    repository: ChatRepository,
    rawQuery: Flow<String>,
    retry: Flow<Int>,
): Flow<RemoteSearch> {
    var lastUsers: List<ChatUser> = emptyList()
    val query = rawQuery
        .map { normalizeQuery(it) }
        .distinctUntilChanged()
        .debounce { if (it.isEmpty()) 0L else SEARCH_DEBOUNCE_MS }
    return combine(query, retry) { q, _ -> q }
        .flatMapLatest { q ->
            if (q.isEmpty()) {
                lastUsers = emptyList()
                flowOf(RemoteSearch())
            } else {
                flow {
                    emit(RemoteSearch(query = q, loading = true, users = lastUsers))
                    val result = try {
                        repository.searchUsers(q, SEARCH_LIMIT)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                    val users = result.getOrNull()
                    if (users != null) {
                        lastUsers = users
                        emit(RemoteSearch(query = q, users = users))
                    } else {
                        Log.w(TAG, "People search failed", result.exceptionOrNull())
                        lastUsers = emptyList()
                        emit(RemoteSearch(query = q, failed = true))
                    }
                }
            }
        }
}
