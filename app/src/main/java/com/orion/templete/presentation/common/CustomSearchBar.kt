package com.orion.templete.presentation.common

import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    content: @Composable () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val colors1 = SearchBarDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surface,
        dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
    )
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                // results update while typing; the keyboard's search key only closes the keyboard
                onSearch = { keyboardController?.hide() },
                expanded = active,
                onExpandedChange = onActiveChange,
                placeholder = { Text(text = placeholder , style = MaterialTheme.typography.titleSmall , color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))},
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (active) {
                        Icon(
                            imageVector = Icons.Default.Close,
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
                colors = colors1.inputFieldColors,
            )
        },
        expanded = active,
        onExpandedChange = onActiveChange,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = colors1,
        tonalElevation = SearchBarDefaults.TonalElevation,
        windowInsets = SearchBarDefaults.windowInsets,
        content = {
            content()
        },
    )
}
