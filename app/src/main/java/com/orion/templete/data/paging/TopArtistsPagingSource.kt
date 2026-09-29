package com.orion.templete.data.paging

import android.content.Context
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.orion.templete.data.model.artist_model.TopArtistProjection
import com.orion.templete.data.network.ApiService
import com.orion.templete.util.isNetworkAvailable

class TopArtistsPagingSource(
    private val apiService: ApiService,
    private val context: Context,
    private val searchQuery: String = ""
) : PagingSource<Int, TopArtistProjection>() {

    override fun getRefreshKey(state: PagingState<Int, TopArtistProjection>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TopArtistProjection> {
        return try {
            val page = params.key ?: 0
            val size = params.loadSize

            // Fetch from network if available
            if (isNetworkAvailable(context)) {
                val response = apiService.getTopArtists(page, size)
                if (response.isSuccessful) {
                    val body = response.body()
                    val artists = body?.artists ?: emptyList()

                    // Filter by search query if provided
                    val filteredArtists = if (searchQuery.isNotEmpty()) {
                        artists.filter { artist ->
                            artist.name.contains(searchQuery, ignoreCase = true)
                        }
                    } else {
                        artists
                    }

                    LoadResult.Page(
                        data = filteredArtists,
                        prevKey = if (page == 0) null else page - 1,
                        nextKey = if (page >= (body?.totalPages ?: 0) - 1) null else page + 1
                    )
                } else {
                    Log.d("TopArtistsPagingSource", "Error: ${response.message()}")
                    LoadResult.Error(Exception(response.message() ?: "Unknown error occurred"))
                }
            } else {
                Log.d("TopArtistsPagingSource", "No internet connection")
                LoadResult.Error(Exception("No internet connection"))
            }
        } catch (e: Exception) {
            Log.d("TopArtistsPagingSource", "Error: ${e.message}")
            LoadResult.Error(e)
        }
    }
}

