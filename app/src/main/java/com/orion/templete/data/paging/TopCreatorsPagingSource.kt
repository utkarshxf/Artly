package com.orion.templete.data.paging

import android.content.Context
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.orion.templete.data.model.user_model.TopCreatorProjection
import com.orion.templete.data.network.ApiService
import com.orion.templete.util.isNetworkAvailable

class TopCreatorsPagingSource(
    private val apiService: ApiService,
    private val context: Context
) : PagingSource<Int, TopCreatorProjection>() {

    override fun getRefreshKey(state: PagingState<Int, TopCreatorProjection>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TopCreatorProjection> {
        return try {
            val page = params.key ?: 0
            val size = params.loadSize

            // Fetch from network if available
            if (isNetworkAvailable(context)) {
                val response = apiService.getTopCreators(page, size)
                if (response.isSuccessful) {
                    val body = response.body()
                    LoadResult.Page(
                        data = body?.creators ?: emptyList(),
                        prevKey = if (page == 0) null else page - 1,
                        nextKey = if (page >= (body?.totalPages ?: 0) - 1) null else page + 1
                    )
                } else {
                    Log.d("TopCreatorsPagingSource", "Error: ${response.message()}")
                    LoadResult.Error(Exception(response.message() ?: "Unknown error occurred"))
                }
            } else {
                Log.d("TopCreatorsPagingSource", "No internet connection")
                LoadResult.Error(Exception("No internet connection"))
            }
        } catch (e: Exception) {
            Log.d("TopCreatorsPagingSource", "Error: ${e.message}")
            LoadResult.Error(e)
        }
    }
}

