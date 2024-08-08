package com.orion.templete.domain.paginator

import com.orion.templete.data.model.ArtworkDTO
import com.plcoding.composepagingyt.Paginator

class DefaultPaginator<Key, Item>(
    private val initialKey: Key,
    private inline val onLoadUpdated: (Boolean) -> Unit,
    private inline val onRequest: suspend (nextKey: Key) -> ArtworkDTO,
    private inline val getNextKey: suspend (ArtworkDTO) -> Key,
    private inline val onError: suspend (Throwable?) -> Unit,
    private inline val onSuccess: suspend (items: ArtworkDTO, newKey: Key) -> Unit
) : Paginator<Key, Item> {

    private var currentKey = initialKey
    private var isMakingRequest = false

    override suspend fun loadNextItems() {
        if (isMakingRequest) {
            return
        }
        isMakingRequest = true
        onLoadUpdated(true)
        try {
            val result = onRequest(currentKey)
            if (result.content.isEmpty()) {
                onError(null)
                onLoadUpdated(false)
                return
            }
            currentKey = getNextKey(result)
            onSuccess(result, currentKey)
        } catch (e: Throwable) {
            onError(e)
        } finally {
            isMakingRequest = false
            onLoadUpdated(false)
        }

    }

    override fun reset() {
        currentKey = initialKey
    }
}