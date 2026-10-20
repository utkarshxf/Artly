package com.orion.templete.presentation.auth.signup

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.nameisjayant.composeprojects.components.SpacerHeight
import com.orion.templete.R
import com.orion.templete.presentation.auth.AuthViewModel
import com.orion.templete.presentation.auth.PhoneLoginUiState
import com.orion.templete.presentation.common.CustomTextField
import com.orion.templete.presentation.common.Screens
import com.orion.templete.presentation.components.AnimatedPreloader
import com.orion.templete.presentation.ui.theme.ButtonHeight
import com.orion.templete.presentation.ui.theme.ExtraLargeSpacing
import com.orion.templete.presentation.ui.theme.LargeSize
import com.orion.templete.presentation.ui.theme.MediumSize
import com.orion.templete.presentation.ui.theme.SmallSize
import java.util.Locale.getDefault

private const val MIN_PASSWORD_LENGTH = 6

// Step 2 for a new phone number or Google account: pick a username and password. The phone number / Google
// email itself was verified in step 1.
@Composable
fun CreateAccount(navController: NavController) {
    val viewModel: AuthViewModel = hiltViewModel()
    val identity = remember { viewModel.verifiedIdentity() }
    val backToPhoneStep = {
        viewModel.restartPhoneLogin()
        // back to whichever page opened us (phone, password login or forgot password)
        if (!navController.popBackStack()) {
            navController.navigate(Screens.Signup.route) { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }
    // The verified Firebase session is gone (e.g. app restarted): start from the phone number again
    LaunchedEffect(identity) {
        if (identity == null) backToPhoneStep()
    }
    BackHandler { backToPhoneStep() }

    CreateAccountScreen(
        identity = identity.orEmpty(),
        viewModel = viewModel,
        onLoggedIn = {
            // clear the whole auth back stack (we may have come from the phone page or the password page)
            navController.navigate(Screens.Home.route) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        },
        onUseDifferentNumber = backToPhoneStep
    )
}

@Composable
fun CreateAccountScreen(
    modifier: Modifier = Modifier,
    identity: String,
    viewModel: AuthViewModel,
    onLoggedIn: () -> Unit,
    onUseDifferentNumber: () -> Unit
) {
    val context = LocalContext.current
    var userName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val usernameValidationMessage by viewModel.usernameValidationMessage.collectAsState()
    val isUsernameValid by viewModel.isUsernameValid.collectAsState()
    val isCheckingUsernameAvailability by viewModel.isCheckingUsernameAvailability.collectAsState()
    val phoneLoginState by viewModel.phoneLoginState.collectAsState()
    val isLoading = phoneLoginState is PhoneLoginUiState.Loading

    LaunchedEffect(userName) {
        viewModel.onUsernameChanged(userName)
    }
    LaunchedEffect(phoneLoginState) {
        when (val state = phoneLoginState) {
            is PhoneLoginUiState.LoggedIn -> onLoggedIn()
            is PhoneLoginUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetPhoneLoginState()
            }
            else -> {}
        }
    }

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
        Icon(painter = painterResource(id = R.drawable.ic_logo_no_bacground), contentDescription = null, modifier = Modifier.size(82.dp))
        SpacerHeight(ExtraLargeSpacing)
        Text(
            text = "Create your account",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        SpacerHeight(SmallSize)
        Text(
            text = "✓ $identity verified",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF2E7D32),
            textAlign = TextAlign.Center
        )
        SpacerHeight(SmallSize)
        Text(
            text = "Choose a username and password for Artistry.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        SpacerHeight(LargeSize)
        CustomTextField(
            value = userName,
            onValueChange = { userName = it.lowercase(getDefault()) },
            hint = R.string.username_hint,
            keyboardType = KeyboardType.Text
        )
        if (userName.isNotEmpty()) {
            SpacerHeight(SmallSize)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isCheckingUsernameAvailability) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.Gray
                    )
                }
                Text(
                    text = usernameValidationMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUsernameValid) Color(0xFF2E7D32) else Color(0xFFC62828),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        SpacerHeight(LargeSize)
        CustomTextField(
            value = password,
            onValueChange = { password = it },
            hint = R.string.password_hint,
            keyboardType = KeyboardType.Password,
            isPasswordTextField = true
        )
        if (password.isNotEmpty() && password.length < MIN_PASSWORD_LENGTH) {
            SpacerHeight(SmallSize)
            Text(
                text = "Password must be at least $MIN_PASSWORD_LENGTH characters",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFC62828),
                modifier = Modifier.fillMaxWidth()
            )
        }
        SpacerHeight(LargeSize)
        Button(
            onClick = {
                when {
                    !isUsernameValid -> Toast.makeText(context, "Please choose an available username", Toast.LENGTH_SHORT).show()
                    password.length < MIN_PASSWORD_LENGTH -> Toast.makeText(context, "Password must be at least $MIN_PASSWORD_LENGTH characters", Toast.LENGTH_SHORT).show()
                    else -> viewModel.createAccount(userName, password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(ButtonHeight),
            enabled = !isLoading,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            if (isLoading) {
                AnimatedPreloader()
            } else {
                Text(text = "Create account")
            }
        }
        SpacerHeight(LargeSize)
        Text(
            text = "Use a different number or account",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onUseDifferentNumber() }
        )
    }
}
