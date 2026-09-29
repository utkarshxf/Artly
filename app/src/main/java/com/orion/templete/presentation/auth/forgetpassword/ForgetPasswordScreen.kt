package com.orion.templete.presentation.auth.forgetpassword

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nameisjayant.composeprojects.components.SpacerHeight
import com.orion.templete.R
import com.orion.templete.data.repository.AUTO_VERIFIED
import com.orion.templete.presentation.auth.AuthScreenUiState
import com.orion.templete.presentation.auth.OTPScreenUiState
import com.orion.templete.presentation.auth.components.GoogleSignInButton
import com.orion.templete.presentation.auth.components.OrDivider
import com.orion.templete.presentation.common.CustomTextField
import com.orion.templete.presentation.components.AnimatedPreloader
import com.orion.templete.presentation.ui.theme.ButtonHeight
import com.orion.templete.presentation.ui.theme.ExtraLargeSpacing
import com.orion.templete.presentation.ui.theme.LargeSize
import com.orion.templete.presentation.ui.theme.MediumSize
import com.orion.templete.presentation.ui.theme.TempleteTheme
import com.orion.templete.util.ForgetPasswordUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgetPasswordScreen(
    modifier: Modifier = Modifier,
    uiState: ForgetPasswordUiState,
    forgetPassword: (String, String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    createUserWithPhone: (String, Activity) -> Unit = { _, _ -> },
    signInWithCredential: (String) -> Unit = { _ -> },
    authState: AuthScreenUiState = AuthScreenUiState.Initial,
    otpState: OTPScreenUiState = OTPScreenUiState.Initial,
    onGoogleSignIn: () -> Unit = {},
    googleLoading: Boolean = false
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Step state for the forget password flow
    // 1: Enter phone number
    // 2: Enter OTP
    // 3: Enter new password and confirm password
    var currentStep by remember { mutableStateOf(1) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.data) {
        uiState.data?.let {
            Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show()
            onNavigateToHome()
        }
    }

    // OTP verification state handling
    LaunchedEffect(authState) {
        when (authState) {
            is AuthScreenUiState.Success -> {
                if (authState.verificationId == AUTO_VERIFIED) {
                    // Firebase verified the number by itself; there is no code to type
                    currentStep = 3
                } else {
                    Toast.makeText(context, "OTP sent successfully", Toast.LENGTH_SHORT).show()
                    currentStep = 2
                }
            }
            is AuthScreenUiState.Error -> {
                Toast.makeText(context, authState.message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    LaunchedEffect(otpState) {
        when (otpState) {
            is OTPScreenUiState.Success -> {
                Toast.makeText(context, "OTP verified successfully", Toast.LENGTH_SHORT).show()
                currentStep = 3
            }
            is OTPScreenUiState.Error -> {
                Toast.makeText(context, otpState.message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    var phoneNumber by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordsMatch by remember { mutableStateOf(true) }
    var countryCode by remember { mutableStateOf("+91") }

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
        Icon(
            painter = painterResource(id = R.drawable.ic_logo_no_bacground), 
            contentDescription = null, 
            modifier = Modifier.size(82.dp)
        )

        SpacerHeight(ExtraLargeSpacing)

        // Step 1: Enter phone number (or skip the password entirely with Google)
        if (currentStep == 1) {
            GoogleSignInButton(
                onClick = onGoogleSignIn,
                enabled = !googleLoading && authState !is AuthScreenUiState.Loading,
                isLoading = googleLoading
            )
            SpacerHeight(MediumSize)
            OrDivider()
            SpacerHeight(MediumSize)
            Text(
                text = "Enter your phone number to receive an OTP",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = LargeSize)
            )

            CustomTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                hint = R.string.phone_number_hint,
                keyboardType = KeyboardType.Phone,
                countrySelected = {
                    countryCode = it.code
                }
            )

            SpacerHeight(LargeSize)

            Button(
                onClick = {
                    activity?.let { createUserWithPhone(countryCode+phoneNumber, it) }
                },
                modifier = modifier
                    .fillMaxWidth()
                    .height(ButtonHeight),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp
                ),
                shape = MaterialTheme.shapes.medium,
                enabled = phoneNumber.isNotEmpty() && authState !is AuthScreenUiState.Loading
            ) {
                if (authState is AuthScreenUiState.Loading) {
                    AnimatedPreloader()
                } else {
                    Text(text = "Send OTP")
                }
            }
        }

        // Step 2: Enter OTP
        if (currentStep == 2) {
            Text(
                text = "Enter the OTP sent to your phone",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = LargeSize)
            )

            CustomTextField(
                value = otp,
                onValueChange = { otp = it },
                hint = R.string.otp_hint,
                keyboardType = KeyboardType.Number
            )

            SpacerHeight(LargeSize)

            Button(
                onClick = {
                    signInWithCredential(otp)
                },
                modifier = modifier
                    .fillMaxWidth()
                    .height(ButtonHeight),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp
                ),
                shape = MaterialTheme.shapes.medium,
                enabled = otp.isNotEmpty() && otpState !is OTPScreenUiState.Loading
            ) {
                if (otpState is OTPScreenUiState.Loading) {
                    AnimatedPreloader()
                } else {
                    Text(text = "Verify OTP")
                }
            }
        }

        // Step 3: Enter new password and confirm password
        if (currentStep == 3) {
            Text(
                text = "Create a new password",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = LargeSize)
            )

            CustomTextField(
                value = newPassword,
                onValueChange = { 
                    newPassword = it
                    passwordsMatch = newPassword == confirmPassword
                },
                hint = R.string.new_password_hint,
                keyboardType = KeyboardType.Password,
                isPasswordTextField = true
            )

            SpacerHeight(LargeSize)

            CustomTextField(
                value = confirmPassword,
                onValueChange = { 
                    confirmPassword = it
                    passwordsMatch = newPassword == confirmPassword
                },
                hint = R.string.confirm_password_hint,
                keyboardType = KeyboardType.Password,
                isPasswordTextField = true
            )

            if (!passwordsMatch) {
                Text(
                    text = "Passwords do not match",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            SpacerHeight(LargeSize)

            Button(
                onClick = {
                    if (passwordsMatch && newPassword.isNotEmpty()) {
                        forgetPassword(phoneNumber, newPassword)
                    }
                },
                modifier = modifier
                    .fillMaxWidth()
                    .height(ButtonHeight),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp
                ),
                shape = MaterialTheme.shapes.medium,
                enabled = !uiState.isLoading && passwordsMatch && newPassword.isNotEmpty() && confirmPassword.isNotEmpty()
            ) {
                if (uiState.isLoading) {
                    AnimatedPreloader()
                } else {
                    Text(text = "Reset Password")
                }
            }
        }

        SpacerHeight(LargeSize)
        Text(text = "Back to Login",
            modifier = Modifier.clickable {
                onNavigateToLogin()
            },
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Preview
@Composable
private fun ForgetPasswordScreenPreview() {
    TempleteTheme {
        ForgetPasswordScreen(
            uiState = ForgetPasswordUiState(),
            forgetPassword = { _, _ -> },
            onNavigateToLogin = {},
            onNavigateToHome = {},
            createUserWithPhone = { _, _ -> },
            signInWithCredential = { _ -> },
            authState = AuthScreenUiState.Initial,
            otpState = OTPScreenUiState.Initial
        )
    }
}
