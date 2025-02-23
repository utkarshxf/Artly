package com.orion.templete.presentation.address

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.orion.templete.presentation.ui.theme.TempleteTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppShippingAddressScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var savedAddresses by remember {
        mutableStateOf(
            listOf(
                Address(
                    "1",
                    "Home",
                    "123 Main St",
                    null,
                    "New York",
                    "NY",
                    "10001",
                    "212-555-0123",
                    true
                ),
                Address(
                    "2",
                    "Office",
                    "456 Work Ave",
                    "Suite 789",
                    "New York",
                    "NY",
                    "10002",
                    "212-555-0124"
                )
            )
        )
    }

    var selectedAddressId by remember { mutableStateOf<String?>(null) }
    var showNewAddressForm by remember { mutableStateOf(false) }
    var editingAddress by remember { mutableStateOf<Address?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shipping Addresses") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Manage your shipping addresses",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                items(savedAddresses) { address ->
                    SavedAddressCard(
                        address = address,
                        isSelected = address.id == selectedAddressId,
                        onSelect = { selectedAddressId = address.id },
                        onEdit = { editingAddress = address },
                        onSetDefault = {
                            if (!address.isDefault) {
                                savedAddresses = savedAddresses.map { addr ->
                                    when (addr.id) {
                                        address.id -> addr.copy(isDefault = true)
                                        else -> addr.copy(isDefault = false)
                                    }
                                }
                            }
                        }
                    )
                }

                item {
                    OutlinedButton(
                        onClick = { showNewAddressForm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Add New Address")
                    }
                }
            }
        }
    }

    if (showNewAddressForm) {
        AddressEntryDialog(
            onDismiss = { showNewAddressForm = false },
            onSave = { newAddress ->
                savedAddresses = savedAddresses + newAddress
                selectedAddressId = newAddress.id
                showNewAddressForm = false
            }
        )
    }

    editingAddress?.let { address ->
        AddressEntryDialog(
            address = address,
            onDismiss = { editingAddress = null },
            onSave = { updatedAddress ->
                savedAddresses = savedAddresses.map {
                    if (it.id == updatedAddress.id) updatedAddress else it
                }
                editingAddress = null
            }
        )
    }
}

@Composable
fun SavedAddressCard(
    address: Address,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onSetDefault: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect
            )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        address.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (address.isDefault) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                "Default",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(address.streetAddress)
                if (address.apartment != null) {
                    Text(address.apartment)
                }
                Text("${address.city}, ${address.state} ${address.zipCode}")
                Text(
                    address.phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Address",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (!address.isDefault) {
                    TextButton(
                        onClick = onSetDefault,
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("Set as Default", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressEntryDialog(
    address: Address? = null,
    onDismiss: () -> Unit,
    onSave: (Address) -> Unit
) {
    var name by remember { mutableStateOf(address?.name ?: "") }
    var street by remember { mutableStateOf(address?.streetAddress ?: "") }
    var apartment by remember { mutableStateOf(address?.apartment ?: "") }
    var city by remember { mutableStateOf(address?.city ?: "") }
    var state by remember { mutableStateOf(address?.state ?: "") }
    var zipCode by remember { mutableStateOf(address?.zipCode ?: "") }
    var phone by remember { mutableStateOf(address?.phone ?: "") }
    val isDefault by remember { mutableStateOf(address?.isDefault ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (address == null) "Add New Address" else "Edit Address") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Address Name (e.g., Home, Office)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it },
                    label = { Text("Street Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    value = apartment,
                    onValueChange = { apartment = it },
                    label = { Text("Apartment/Suite (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state,
                        onValueChange = { state = it },
                        label = { Text("State") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next
                        )
                    )

                    OutlinedTextField(
                        value = zipCode,
                        onValueChange = { zipCode = it },
                        label = { Text("ZIP Code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedAddress = Address(
                        id = address?.id ?: System.currentTimeMillis().toString(),
                        name = name,
                        streetAddress = street,
                        apartment = apartment.takeIf { it.isNotBlank() },
                        city = city,
                        state = state,
                        zipCode = zipCode,
                        phone = phone,
                        isDefault = isDefault || (address?.isDefault == true)
                    )
                    onSave(updatedAddress)
                },
                enabled = name.isNotBlank() && street.isNotBlank() &&
                        city.isNotBlank() && state.isNotBlank() &&
                        zipCode.isNotBlank() && phone.isNotBlank()
            ) {
                Text(if (address == null) "Save Address" else "Update Address")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview
@Composable
private fun myPrv() {
    TempleteTheme {
        InAppShippingAddressScreen(onBackClick = {},)
    }
}