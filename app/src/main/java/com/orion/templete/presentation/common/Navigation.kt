package com.orion.templete.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.orion.templete.presentation.swipe.SwipeScreen
import com.orion.templete.presentation.ui.theme.TempleteTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Navigation() {
    val navController = rememberNavController()
    var navigationSelectedItem by remember { mutableStateOf(0) }
    NavHost(
        navController = navController,
        startDestination = Screens.Swipe.route,
        modifier = Modifier.padding(bottom = 82.dp)
    ) {
        composable(Screens.Swipe.route) {
            SwipeScreen()
        }
        composable(Screens.Profile.route) {
            // Search profile content
        }
    }
    Box(modifier = Modifier.fillMaxSize() , contentAlignment = Alignment.BottomCenter) {
        NavigationBar(
            modifier = Modifier.height(82.dp),
            containerColor = Color.White,
        ) {
            BottomNavigationItem().bottomNavigationItems().forEachIndexed { index, navigationItem ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = {
                            navigationSelectedItem = index
                            navController.navigate(navigationItem.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    ) {
                        Icon(
                            painterResource(id = navigationItem.icon),
                            contentDescription = navigationItem.label,
                            tint = if (index == navigationSelectedItem) Color.Black else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = navigationItem.label,
                        color = if (index == navigationSelectedItem) Color.Black else Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}