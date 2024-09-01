package com.orion.templete.presentation.search

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orion.templete.presentation.common.MySearchBar

@Composable
fun SearchScreen()
{
    SearchRow(
        header = {},
        search = {
            MySearchBar {

            }
        },
        content = {}
    )
}
@Composable
fun SearchRow(
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    search: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    Column {
        header?.invoke()
        search?.invoke()
        content?.invoke()
    }
}