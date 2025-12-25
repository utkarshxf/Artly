package com.orion.templete.data.paging

import android.content.Context
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.orion.templete.data.model.user_model.TopUserProjection
import com.orion.templete.data.network.ApiService
import com.orion.templete.util.isNetworkAvailable

class TopUsersPagingSource(
    private val apiService: ApiService,
    private val context: Context
) : PagingSource<Int, TopUserProjection>() {

    override fun getRefreshKey(state: PagingState<Int, TopUserProjection>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TopUserProjection> {
        return try {
            val page = params.key ?: 0
            val size = params.loadSize

            // Fetch from network if available
            if (isNetworkAvailable(context)) {
                val response = apiService.getTopViewers(page, size)
                if (response.isSuccessful) {
                    val body = response.body()
                    LoadResult.Page(
                        data = body?.users ?: emptyList(),
                        prevKey = if (page == 0) null else page - 1,
                        nextKey = if (page >= (body?.totalPages ?: 0) - 1) null else page + 1
                    )
                } else {
                    Log.d("TopUsersPagingSource", "Error: ${response.message()}")
                    LoadResult.Error(Exception(response.message() ?: "Unknown error occurred"))
                }
            } else {
                Log.d("TopUsersPagingSource", "No internet connection")
                LoadResult.Error(Exception("No internet connection"))
            }
        } catch (e: Exception) {
            Log.d("TopUsersPagingSource", "Error: ${e.message}")
            LoadResult.Error(e)
        }
    }
}

