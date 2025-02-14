package com.orion.templete.presentation.order_tracking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.orion.templete.presentation.ui.theme.TempleteTheme

data class OrderStatus(
    val orderId: String,
    val currentStatus: DeliveryStatus,
    val estimatedDelivery: String,
    val trackingNumber: String?,
    val carrier: String?,
    val deliveryPartner: String,
    val updates: List<StatusUpdate>
)

enum class DeliveryStatus {
    ORDERED,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED
}

data class StatusUpdate(
    val status: DeliveryStatus,
    val timestamp: String,
    val location: String?,
    val description: String,
    val isCompleted: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    orderStatus: OrderStatus,
    onBackClick: () -> Unit,
    onContactSupportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Track Order") },
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
                .verticalScroll(rememberScrollState())
        ) {
            // Delivery Status Card
            DeliveryStatusCard(orderStatus)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tracking Timeline
            TrackingTimeline(orderStatus.updates)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Delivery Details Card
            DeliveryDetailsCard(orderStatus)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Support Button
            OutlinedButton(
                onClick = onContactSupportClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Contact Support")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DeliveryStatusCard(
    orderStatus: OrderStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Estimated Delivery",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                orderStatus.estimatedDelivery,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = when (orderStatus.currentStatus) {
                    DeliveryStatus.ORDERED -> 0.2f
                    DeliveryStatus.CONFIRMED -> 0.4f
                    DeliveryStatus.PROCESSING -> 0.6f
                    DeliveryStatus.SHIPPED -> 0.8f
                    DeliveryStatus.OUT_FOR_DELIVERY -> 0.9f
                    DeliveryStatus.DELIVERED -> 1.0f
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (orderStatus.trackingNumber != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Tracking Number",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            orderStatus.trackingNumber,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(onClick = { /* Copy tracking number */ }) {
                        Icon(Icons.Default.Create, "Copy tracking number")
                    }
                }
            }
        }
    }
}

@Composable
fun TrackingTimeline(
    updates: List<StatusUpdate>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            "Tracking Updates",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        updates.forEachIndexed { index, update ->
            TimelineItem(
                update = update,
                isLast = index == updates.lastIndex
            )
        }
    }
}

@Composable
fun TimelineItem(
    update: StatusUpdate,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        // Timeline dots and line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (update.isCompleted)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(64.dp)
                        .background(
                            if (update.isCompleted)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
        
        // Status content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, bottom = if (isLast) 0.dp else 16.dp)
        ) {
            Text(
                update.status.name.replace("_", " "),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                update.timestamp,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (update.location != null) {
                Text(
                    update.location,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                update.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DeliveryDetailsCard(
    orderStatus: OrderStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Delivery Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            DetailRow(
                "Order ID",
                orderStatus.orderId
            )
            if (orderStatus.carrier != null) {
                DetailRow(
                    "Carrier",
                    orderStatus.carrier
                )
            }
            DetailRow(
                "Delivery Partner",
                orderStatus.deliveryPartner
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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

@Preview
@Composable
private fun myPrev() {
    TempleteTheme {
        val orderStatus = OrderStatus(
            orderId = "ORD-123456",
            currentStatus = DeliveryStatus.SHIPPED,
            estimatedDelivery = "March 20, 2025",
            trackingNumber = "1Z999AA1234567890",
            carrier = "FedEx",
            deliveryPartner = "Express Delivery",
            updates = listOf(
                StatusUpdate(
                    status = DeliveryStatus.ORDERED,
                    timestamp = "March 15, 2025 10:30 AM",
                    location = null,
                    description = "Order placed successfully",
                    isCompleted = true
                ),
                StatusUpdate(
                    status = DeliveryStatus.SHIPPED,
                    timestamp = "March 16, 2025 2:45 PM",
                    location = "New York Distribution Center",
                    description = "Package has left the facility",
                    isCompleted = true
                ), StatusUpdate(
                    status = DeliveryStatus.SHIPPED,
                    timestamp = "March 16, 2025 2:45 PM",
                    location = "New York Distribution Center",
                    description = "Package has left the facility",
                    isCompleted = true
                )
                // Add more updates as needed
            )
        )

        OrderTrackingScreen(
            orderStatus = orderStatus,
            onBackClick = { /* Navigate back */ },
            onContactSupportClick = { /* Open support */ }
        )
    }

}