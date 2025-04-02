package com.orion.templete.presentation.favorites

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun CollectionMoreMenu(
    showMenu: Boolean,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onAddToAlbum: () -> Unit,
    onRemoveFromSaved: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        DropdownMenuItem(
            text = { Text("Rename Collection") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
            onClick = {
                onDismiss()
                onRename()
            }
        )
        
        DropdownMenuItem(
            text = { Text("Share") },
            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
            onClick = {
                onDismiss()
                onShare()
            }
        )

        DropdownMenuItem(
            text = { Text("Download All") },
            leadingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            onClick = {
                onDismiss()
                onDownload()
            }
        )

        DropdownMenuItem(
            text = { Text("Add to Album") },
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
            onClick = {
                onDismiss()
                onAddToAlbum()
            }
        )

        Divider()

        DropdownMenuItem(
            text = { Text("Remove from Saved") },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
            onClick = {
                onDismiss()
                onRemoveFromSaved()
            }
        )

        DropdownMenuItem(
            text = { Text("Delete Collection") },
            leadingIcon = { 
                Icon(
                    Icons.Default.Delete, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            onClick = {
                onDismiss()
                onDelete()
            },
            colors = MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.error
            )
        )
    }
}
