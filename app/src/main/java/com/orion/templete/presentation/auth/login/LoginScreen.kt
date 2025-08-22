package com.orion.templete.presentation.auth.login

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nameisjayant.composeprojects.components.SpacerHeight
import com.orion.templete.R
import com.orion.templete.data.model.login_model.User
import com.orion.templete.presentation.common.CustomTextField
import com.orion.templete.presentation.components.AnimatedPreloader
import com.orion.templete.presentation.ui.theme.ButtonHeight
import com.orion.templete.presentation.ui.theme.ExtraLargeSpacing
import com.orion.templete.presentation.ui.theme.LargeSize
import com.orion.templete.presentation.ui.theme.MediumSize
import com.orion.templete.presentation.ui.theme.SmallSize
import com.orion.templete.presentation.ui.theme.TempleteTheme
import com.orion.templete.util.LoginUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    uiState: LoginUiState,
    loginUser: (User) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateToForgetPassword: () -> Unit = {},
) {
    val context = LocalContext.current
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(uiState.data) {
        uiState.data?.let {
            onNavigateToHome()
        }
    }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(
                color = MaterialTheme.colorScheme.surface
            )
            .padding(
                top = ExtraLargeSpacing + LargeSize,
                start = LargeSize + MediumSize,
                end = LargeSize + MediumSize,
                bottom = LargeSize
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(painter = painterResource(id = R.drawable.ic_logo_no_bacground), contentDescription = null , modifier = Modifier.size(82.dp))
        SpacerHeight(ExtraLargeSpacing)
        CustomTextField(
            value = username,
            onValueChange = { username = it },
            hint = R.string.username_hint,
            keyboardType = KeyboardType.Email
        )
        SpacerHeight(LargeSize)
        CustomTextField(
            value = password,
            onValueChange = { password = it },
            hint = R.string.password_hint,
            keyboardType = KeyboardType.Password,
            isPasswordTextField = true
        )
        SpacerHeight(LargeSize)
        Button(
            onClick = {
                loginUser(User(username, password))
            },
            modifier = modifier
                .fillMaxWidth()
                .height(ButtonHeight),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp
            ),
            shape = MaterialTheme.shapes.medium,
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                AnimatedPreloader()
            }
            else
                Text(text = stringResource(id = R.string.login_button_label))
        }
        SpacerHeight(LargeSize)
        GoToSignup(modifier) {
            onNavigateToSignup()
        }
        SpacerHeight(LargeSize)
        Text(
            text = "Forgot Password?",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier.clickable { onNavigateToForgetPassword() }
        )
    }

}

@Composable
fun GoToSignup(
    modifier: Modifier = Modifier, onNavigateToSignup: () -> Unit
) {
    Row(
        modifier = modifier, horizontalArrangement = Arrangement.spacedBy(
            SmallSize
        )
    ) {
        Text(text = "Don't have an account?", style = MaterialTheme.typography.bodySmall)
        Text(text = "SignUp",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier.clickable { onNavigateToSignup() })
    }
}


@Preview
@Composable
private fun LoginScreenPrev() {
    TempleteTheme {
        LoginScreen(Modifier, LoginUiState(), {}, {}, {}, {})
    }
}
