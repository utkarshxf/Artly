package com.orion.templete.presentation.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.orion.templete.presentation.components.AnimatedPreloader

@Composable
fun LoadingScreen() {
    AnimatedPreloader()
}