package com.orion.templete.presentation.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orion.templete.presentation.ui.theme.ButtonHeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSearchBar(
    modifier: Modifier = Modifier,
    onQueryChange: (query: String) -> Unit,
    query: String,
    active: Boolean,
    onActiveChange: (active: Boolean) -> Unit,
    placeholder: String,
    content: @Composable () -> Unit,
) {
    val modifierHeight = if (!active) Modifier.height(ButtonHeight) else Modifier.fillMaxSize()
    SearchBar(
        modifier = modifierHeight.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        query = query,
        onQueryChange = { onQueryChange(it) },
        onSearch = {
            onActiveChange(false)
            onQueryChange("")
        },
        active = active,
        onActiveChange = { onActiveChange(it) },
        placeholder = { Text(text = placeholder) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search, contentDescription = null
            )
        },
        trailingIcon = {
            if (active) {
                Icon(imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.clickable {
                        if (query.isNotEmpty()) {
                            onQueryChange("")
                        } else {
                            onActiveChange(false)
                        }
                    }
                )
            }
        },
    ) {
        content()
    }
}
