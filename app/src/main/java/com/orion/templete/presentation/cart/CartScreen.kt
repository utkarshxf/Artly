package com.orion.templete.presentation.cart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.orion.templete.presentation.ui.theme.TempleteTheme

data class CartItem(
    val id: String,
    val title: String,
    val artist: String,
    val price: Double,
    val imageUrl: String,
    var quantity: Int = 1
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBackClick: () -> Unit,
    onCheckoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var cartItems by remember { mutableStateOf(
        listOf(
            CartItem("1", "Abstract Sunset", "John Doe", 99.99, "https://example.com/art1.jpg"),
            CartItem("2", "Urban Dreams", "Jane Smith", 149.99, "https://example.com/art2.jpg")
        )
    ) }
    
    var expandedItemId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shopping Cart") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.Add, contentDescription = "Back")
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
                items(cartItems) { item ->
                    CartItemCard(
                        item = item,
                        isExpanded = expandedItemId == item.id,
                        onExpandClick = { expandedItemId = if (expandedItemId == item.id) null else item.id },
                        onQuantityChange = { newQuantity ->
                            cartItems = cartItems.map {
                                if (it.id == item.id) it.copy(quantity = newQuantity)
                                else it
                            }
                        },
                        onRemoveClick = {
                            cartItems = cartItems.filter { it.id != item.id }
                        }
                    )
                }
            }
            
            CartSummary(
                items = cartItems,
                onCheckoutClick = onCheckoutClick,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun CartItemCard(
    item: CartItem,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onExpandClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "by ${item.artist}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$${item.price}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (isExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { if (item.quantity > 1) onQuantityChange(item.quantity - 1) }
                        ) {
                            Icon(Icons.Default.Close, "Decrease quantity")
                        }
                        Text(
                            text = item.quantity.toString(),
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(
                            onClick = { onQuantityChange(item.quantity + 1) }
                        ) {
                            Icon(Icons.Default.Add, "Increase quantity")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = onRemoveClick) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove item",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartSummary(
    items: List<CartItem>,
    onCheckoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtotal = items.sumOf { it.price * it.quantity }
    val shipping = if (items.isNotEmpty()) 10.0 else 0.0
    val total = subtotal + shipping

    Surface(
        modifier = modifier,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal")
                Text("$${String.format("%.2f", subtotal)}")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Shipping")
                Text("$${String.format("%.2f", shipping)}")
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Total",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$${String.format("%.2f", total)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Button(
                onClick = onCheckoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                enabled = items.isNotEmpty()
            ) {
                Text(
                    "Proceed to Checkout",
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun newPrev() {
    TempleteTheme {
        CartScreen(onBackClick = {}, onCheckoutClick = {})
    }
}