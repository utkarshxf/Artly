package com.orion.templete.presentation.auth.signup

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.nameisjayant.composeprojects.components.SpacerHeight
import com.nearbuck.android.admin.presentation.login_screen.components.OtpView
import com.orion.templete.R
import com.orion.templete.data.repository.AUTO_VERIFIED
import com.orion.templete.presentation.auth.AuthScreenUiState
import com.orion.templete.presentation.auth.AuthViewModel
import com.orion.templete.presentation.auth.OTPScreenUiState
import com.orion.templete.presentation.auth.PhoneLoginUiState
import com.orion.templete.presentation.common.CustomTextField
import com.orion.templete.presentation.common.Screens
import com.orion.templete.presentation.components.AnimatedPreloader
import com.orion.templete.presentation.ui.theme.ButtonHeight
import com.orion.templete.presentation.ui.theme.ExtraLargeSpacing
import com.orion.templete.presentation.ui.theme.LargeSize
import com.orion.templete.presentation.ui.theme.MediumSize
import com.orion.templete.presentation.ui.theme.SmallSize

// Step 1 of login/signup: phone number + SMS code. Existing numbers go straight to Home,
// new numbers continue to CreateAccount for a username and password.
@Composable
fun Signup(
    navController: NavController,
    activity: Activity
) {
    val viewModel: AuthViewModel = hiltViewModel()
    PhoneLoginScreen(
        viewModel = viewModel,
        activity = activity,
        onLoggedIn = {
            navController.navigate(Screens.Home.route) {
                popUpTo(Screens.Signup.route) { inclusive = true }
            }
        },
        onNeedsAccount = { navController.navigate(Screens.CreateAccount.route) },
        onNavigateToLogin = {
            navController.navigate(Screens.Login.route) {
                popUpTo(Screens.Signup.route) { inclusive = true }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneLoginScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel,
    activity: Activity,
    onLoggedIn: () -> Unit,
    onNeedsAccount: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    var mobileNumber by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+91") }
    var showOtpBottomSheet by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()
    val otpState by viewModel.otpState.collectAsState()
    val phoneLoginState by viewModel.phoneLoginState.collectAsState()
    val isBusy = authState is AuthScreenUiState.Loading || phoneLoginState is PhoneLoginUiState.Loading

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthScreenUiState.Success -> {
                if (state.verificationId == AUTO_VERIFIED) {
                    viewModel.continueWithVerifiedPhone()
                } else {
                    showOtpBottomSheet = true
                }
            }
            is AuthScreenUiState.Error -> Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            else -> {}
        }
    }
    LaunchedEffect(otpState) {
        when (val state = otpState) {
            is OTPScreenUiState.Success -> {
                showOtpBottomSheet = false
                viewModel.continueWithVerifiedPhone()
            }
            is OTPScreenUiState.Error -> Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            else -> {}
        }
    }
    LaunchedEffect(phoneLoginState) {
        when (val state = phoneLoginState) {
            is PhoneLoginUiState.LoggedIn -> onLoggedIn()
            is PhoneLoginUiState.NeedsAccount -> {
                viewModel.resetPhoneLoginState()
                onNeedsAccount()
            }
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
            text = "Welcome to Artly",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        SpacerHeight(SmallSize)
        Text(
            text = "Enter your phone number. We'll send you a code to log in or create your account.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        SpacerHeight(LargeSize)
        CustomTextField(
            value = mobileNumber,
            onValueChange = { mobileNumber = it.filter(Char::isDigit) },
            hint = R.string.mobile_hint,
            keyboardType = KeyboardType.Phone,
            countrySelected = {
                countryCode = it.code
            }
        )
        SpacerHeight(LargeSize)
        Button(
            onClick = {
                val number = mobileNumber.trimStart('0')
                if (number.length < 6) {
                    Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.createUserWithPhone(countryCode + number, activity)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(ButtonHeight),
            enabled = !isBusy,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            if (isBusy) {
                AnimatedPreloader()
            } else {
                Text(text = "Continue")
            }
        }
        SpacerHeight(LargeSize)
        GoToLogin(question = "Have a username?", action = "Log in with password") {
            onNavigateToLogin()
        }
    }

    if (showOtpBottomSheet) {
        OtpVerificationBottomSheet(
            phoneNumber = countryCode + mobileNumber.trimStart('0'),
            onDismiss = { showOtpBottomSheet = false },
            onVerify = { otp -> viewModel.signInWithCredential(otp) },
            isLoading = otpState is OTPScreenUiState.Loading || phoneLoginState is PhoneLoginUiState.Loading
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationBottomSheet(
    modifier: Modifier = Modifier,
    phoneNumber: String? = null,
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit,
    isLoading: Boolean
) {
    var otp by remember { mutableStateOf("") }
    ModalBottomSheet(
        modifier = Modifier.imePadding(),
        onDismissRequest = { onDismiss() }) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(LargeSize),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Verify Your Number",
                style = MaterialTheme.typography.headlineSmall
            )
            SpacerHeight(MediumSize)
            Text(
                text = if (phoneNumber.isNullOrBlank()) "Enter the verification code we sent to your mobile number"
                else "Enter the 6-digit code we sent to $phoneNumber",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            SpacerHeight(LargeSize)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                OtpView(
                    otpText = otp,
                    charColor = MaterialTheme.colorScheme.onSurface,
                    charBackground = Color.Transparent,
                    charSize = 20.sp,
                    containerSize = 46.dp,
                    otpCount = 6,
                ) {
                    otp = it
                    if (otp.length == 6) {
                        onVerify(otp)
                    }
                }
            }

            SpacerHeight(LargeSize)
            Button(
                onClick = { onVerify(otp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ButtonHeight),
                enabled = otp.length == 6 && !isLoading,
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                if (isLoading) {
                    AnimatedPreloader()
                } else {
                    Text(text = "Verify")
                }
            }
        }
    }
}

@Composable
fun GoToLogin(
    modifier: Modifier = Modifier,
    question: String = "Already have an account?",
    action: String = "Login",
    onNavigateToLogin: () -> Unit
) {
    Row(
        modifier = modifier, horizontalArrangement = Arrangement.spacedBy(
            SmallSize
        )
    ) {
        Text(text = question, style = MaterialTheme.typography.bodySmall)
        Text(text = action,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onNavigateToLogin() })
    }
}
