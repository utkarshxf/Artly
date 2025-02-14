package com.orion.templete.presentation.order_state

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.orion.templete.presentation.address.Address
import com.orion.templete.presentation.ui.theme.TempleteTheme

data class OrderConfirmationDetails(
    val orderId: String,
    val totalAmount: Double,
    val estimatedDelivery: String,
    val shippingAddress: Address
)

@Composable
fun OrderSuccessScreen(
    orderDetails: OrderConfirmationDetails,
    onViewOrderDetailsClick: () -> Unit,
    onContinueShoppingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAnimation by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        showAnimation = true
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedSuccessIcon(
            visible = showAnimation,
            modifier = Modifier.size(120.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Order Confirmed!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Thank you for your purchase",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OrderDetailsCard(orderDetails)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onViewOrderDetailsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Order Details")
        }
        
        OutlinedButton(
            onClick = onContinueShoppingClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Continue Shopping")
        }
    }
}

@Composable
fun OrderFailedScreen(
    error: String,
    onRetryClick: () -> Unit,
    onContactSupportClick: () -> Unit,
    onBackToCartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAnimation by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        showAnimation = true
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedErrorIcon(
            visible = showAnimation,
            modifier = Modifier.size(120.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Order Failed",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            error,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRetryClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Try Again")
        }
        
        OutlinedButton(
            onClick = onContactSupportClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Icon(
                Icons.Default.Phone,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Contact Support")
        }
        
        TextButton(
            onClick = onBackToCartClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Back to Cart")
        }
    }
}

@Composable
fun OrderDetailsCard(
    orderDetails: OrderConfirmationDetails,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailRow(
                label = "Order ID",
                value = orderDetails.orderId
            )
            
            DetailRow(
                label = "Amount Paid",
                value = "$${String.format("%.2f", orderDetails.totalAmount)}"
            )
            
            DetailRow(
                label = "Estimated Delivery",
                value = orderDetails.estimatedDelivery
            )
            
            Divider()
            
            Text(
                "Shipping Address",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Column {
                Text(orderDetails.shippingAddress.name)
                Text(orderDetails.shippingAddress.streetAddress)
                if (orderDetails.shippingAddress.apartment != null) {
                    Text(orderDetails.shippingAddress.apartment)
                }
                Text("${orderDetails.shippingAddress.city}, ${orderDetails.shippingAddress.state} ${orderDetails.shippingAddress.zipCode}")
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AnimatedSuccessIcon(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(visible, label = "success")
    
    val scale by transition.animateFloat(
        label = "scale",
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        }
    ) { if (it) 1f else 0f }
    
    val alpha by transition.animateFloat(
        label = "alpha",
        transitionSpec = { tween(500) }
    ) { if (it) 1f else 0f }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .alpha(alpha)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = "Success",
            modifier = Modifier
                .fillMaxSize(0.6f)
                .scale(scale)
                .alpha(alpha),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun AnimatedErrorIcon(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(visible, label = "error")
    
    val scale by transition.animateFloat(
        label = "scale",
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        }
    ) { if (it) 1f else 0f }
    
    val alpha by transition.animateFloat(
        label = "alpha",
        transitionSpec = { tween(500) }
    ) { if (it) 1f else 0f }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .alpha(alpha)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer)
        )
        Icon(
            Icons.Default.Close,
            contentDescription = "Error",
            modifier = Modifier
                .fillMaxSize(0.6f)
                .scale(scale)
                .alpha(alpha),
            tint = MaterialTheme.colorScheme.error
        )
    }
}

@Preview
@Composable
private fun prev() {
    TempleteTheme {
        OrderSuccessScreen(
            orderDetails = OrderConfirmationDetails(
                orderId = "123456",
                totalAmount = 100.0,
                estimatedDelivery = "Tomorrow",
                shippingAddress = Address(
                    id = "1",
                    name = "Home",
                    streetAddress = "",
                    apartment = null,
                    city = "",
                    state = "",
                    zipCode = "",
                    phone = "",
                ),
            ),
            onViewOrderDetailsClick = {},
            onContinueShoppingClick = {},
        )
    }
}