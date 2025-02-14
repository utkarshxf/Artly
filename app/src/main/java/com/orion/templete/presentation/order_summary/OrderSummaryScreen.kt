package com.orion.templete.presentation.order_summary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.orion.templete.presentation.address.Address
import com.orion.templete.presentation.ui.theme.TempleteTheme
import java.util.UUID

data class OrderItem(
    val id: String,
    val title: String,
    val artist: String,
    val price: Double,
    val quantity: Int,
    val imageUrl: String
)

data class OrderSummary(
    val items: List<OrderItem>,
    val subtotal: Double,
    val shipping: Double,
    val tax: Double,
    val total: Double,
    val shippingAddress: Address,
    val paymentMethod: PaymentMethod
)

data class PaymentMethod(
    val type: String,
    val lastFourDigits: String,
    val expiryDate: String
)

object DummyData {

    val dummyAddress = Address(
        id = UUID.randomUUID().toString(),
        name = "Home",
        streetAddress = "123 Main St",
        apartment = null,
        city = "New York",
        state = "NY",
        zipCode = "10001",
        phone = "212-555-0123"
    )

    val dummyPaymentMethod = PaymentMethod(
        type = "Visa",
        lastFourDigits = "4242",
        expiryDate = "12/25"
    )

    val dummyOrderItems = listOf(
        OrderItem(
            id = UUID.randomUUID().toString(),
            title = "Sunset Serenade",
            artist = "Jane Doe",
            price = 25.99,
            quantity = 1,
            imageUrl = "https://example.com/sunset.jpg"
        ),
        OrderItem(
            id = UUID.randomUUID().toString(),
            title = "Midnight Bloom",
            artist = "John Smith",
            price = 30.50,
            quantity = 2,
            imageUrl = "https://example.com/midnight.jpg"
        ),
        OrderItem(
            id = UUID.randomUUID().toString(),
            title = "Urban Haze",
            artist = "Alice Johnson",
            price = 15.00,
            quantity = 3,
            imageUrl = "https://example.com/urban.jpg"
        )
    )

    val dummyOrderSummary = OrderSummary(
        items = dummyOrderItems,
        subtotal = dummyOrderItems.sumOf { it.price * it.quantity },
        shipping = 5.99,
        tax = 3.50,
        total = dummyOrderItems.sumOf { it.price * it.quantity } + 5.99 + 3.50,
        shippingAddress = dummyAddress,
        paymentMethod = dummyPaymentMethod
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderSummaryScreen(
    orderSummary: OrderSummary,
    onBackClick: () -> Unit,
    onPlaceOrderClick: () -> Unit,
    onEditShippingClick: () -> Unit,
    onEditPaymentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Summary") },
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
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Order Items Section
                item {
                    Text(
                        "Order Items",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                
                items(orderSummary.items) { item ->
                    OrderItemCard(item = item)
                }
                
                // Shipping Address Section
                item {
                    AddressSection(
                        address = orderSummary.shippingAddress,
                        onEditClick = onEditShippingClick
                    )
                }
                
                // Payment Method Section
                item {
                    PaymentMethodSection(
                        paymentMethod = orderSummary.paymentMethod,
                        onEditClick = onEditPaymentClick
                    )
                }
                
                // Order Summary Section
                item {
                    OrderTotalSection(orderSummary = orderSummary)
                }
                
                // Bottom spacing
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
            
            // Place Order Button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Total: $${String.format("%.2f", orderSummary.total)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Button(
                        onClick = onPlaceOrderClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            "Place Order",
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderItemCard(
    item: OrderItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .size(80.dp),
                contentScale = ContentScale.Crop
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "by ${item.artist}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${String.format("%.2f", item.price)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Qty: ${item.quantity}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun AddressSection(
    address: Address,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(
        title = "Shipping Address",
        onEditClick = onEditClick,
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(address.name, fontWeight = FontWeight.Medium)
            Text(address.streetAddress)
            if (address.apartment != null) {
                Text(address.apartment)
            }
            Text("${address.city}, ${address.state} ${address.zipCode}")
            Text(address.phone)
        }
    }
}

@Composable
fun PaymentMethodSection(
    paymentMethod: PaymentMethod,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(
        title = "Payment Method",
        onEditClick = onEditClick,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                when (paymentMethod.type) {
                    "VISA" -> Icons.Default.PlayArrow
                    "MASTERCARD" -> Icons.Default.PlayArrow
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = null
            )
            Column {
                Text(
                    "${paymentMethod.type} ending in ${paymentMethod.lastFourDigits}",
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Expires ${paymentMethod.expiryDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun OrderTotalSection(
    orderSummary: OrderSummary,
    modifier: Modifier = Modifier
) {
    SectionCard(
        title = "Order Total",
        showEditButton = false,
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PriceRow("Subtotal", orderSummary.subtotal)
            PriceRow("Shipping", orderSummary.shipping)
            PriceRow("Tax", orderSummary.tax)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            PriceRow(
                "Total",
                orderSummary.total,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    showEditButton: Boolean = true,
    onEditClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (showEditButton) {
                    TextButton(onClick = onEditClick) {
                        Text("Edit")
                    }
                }
            }
            content()
        }
    }
}

@Composable
fun PriceRow(
    label: String,
    amount: Double,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = style)
        Text(
            "$${String.format("%.2f", amount)}",
            style = style,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview
@Composable
private fun myprev() {
    TempleteTheme {
        OrderSummaryScreen(
            orderSummary = DummyData.dummyOrderSummary,
            onBackClick = {},
            onPlaceOrderClick = {},
            onEditShippingClick = {},
            onEditPaymentClick = {}
        )
    }


}