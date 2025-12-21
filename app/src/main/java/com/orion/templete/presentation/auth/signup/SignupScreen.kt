package com.orion.templete.presentation.auth.signup

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.nameisjayant.composeprojects.components.SpacerHeight
import com.orion.templete.R
import com.orion.templete.data.model.login_model.User
import com.orion.templete.presentation.auth.AuthScreenUiState
import com.orion.templete.presentation.auth.AuthViewModel
import com.orion.templete.presentation.common.CustomTextField
import com.orion.templete.presentation.common.Screens
import com.orion.templete.presentation.ui.theme.ButtonHeight
import com.orion.templete.presentation.ui.theme.ExtraLargeSpacing
import com.orion.templete.presentation.ui.theme.LargeSize
import com.orion.templete.presentation.ui.theme.MediumSize
import com.orion.templete.presentation.ui.theme.SmallSize
import com.orion.templete.util.SignupUiState
import com.nearbuck.android.admin.presentation.login_screen.components.OtpView
import com.orion.templete.data.model.login_model.Registration
import com.orion.templete.presentation.auth.OTPScreenUiState
import com.orion.templete.presentation.components.AnimatedPreloader
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.Locale.getDefault

@Composable
fun Signup(
    navController: NavController,
    activity: Activity
) {
    val viewModel: AuthViewModel = hiltViewModel()
    SignupScreen(
        uiState = viewModel.signupData,
        signupUser = { viewModel.signup(it) },
        onNavigateToLogin = {
            navController.navigate(Screens.Login.route) {
                popUpTo(Screens.Signup.route) { inclusive = true }
            }
        },
        onNavigateToHomeScreen = {
            navController.navigate(Screens.Home.route) {
                popUpTo(Screens.Signup.route) { inclusive = true }
            }
        },
        viewModel = viewModel,
        activity = activity
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    modifier: Modifier = Modifier,
    uiState: SignupUiState,
    signupUser: (Registration) -> Unit,
    onNavigateToHomeScreen: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel,
    activity: Activity
) {
    val context = LocalContext.current
    var showOtpBottomSheet by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+91") }
    var password by remember { mutableStateOf("") }
    var isOtpSending by remember { mutableStateOf(false) }

    // MVVM: Collect StateFlow from ViewModel instead of managing local state
    val usernameValidationMessage by viewModel.usernameValidationMessage.collectAsState()
    val isUsernameValid by viewModel.isUsernameValid.collectAsState()
    val isCheckingUsernameAvailability by viewModel.isCheckingUsernameAvailability.collectAsState()

    val authState = viewModel.authState.collectAsState()
    val otpState = viewModel.otpState.collectAsState()
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            isOtpSending = false
        }
    }

    LaunchedEffect(userName) {
        viewModel.onUsernameChanged(userName)
    }

    LaunchedEffect(uiState.data) {
        uiState.data?.let {
            onNavigateToHomeScreen()
        }
    }
    LaunchedEffect(authState.value) {
        when(authState.value){
            is AuthScreenUiState.Error -> {
                isOtpSending = false
                Toast.makeText(context, (authState.value as AuthScreenUiState.Error).message, Toast.LENGTH_SHORT).show()
            }
            is AuthScreenUiState.Initial -> {
                isOtpSending = false
            }
            is AuthScreenUiState.Loading -> {
                isOtpSending = true
            }
            is AuthScreenUiState.Success -> {
                isOtpSending = false
                Toast.makeText(context, (authState.value as AuthScreenUiState.Success).verificationId, Toast.LENGTH_SHORT).show()
                delay(1000)
                showOtpBottomSheet = true
            }
        }
    }
    LaunchedEffect(otpState.value) {
        when (otpState.value) {
            is OTPScreenUiState.Error -> {
                isOtpSending = false
                Toast.makeText(context, "Fail to create an account", Toast.LENGTH_SHORT).show()
            }
            is OTPScreenUiState.Initial -> {

            }
            is OTPScreenUiState.Loading -> {
                isOtpSending = true
            }
            is OTPScreenUiState.Success -> {
                signupUser(Registration(userName, password , countryCode + mobileNumber))
                showOtpBottomSheet = false
            }
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
        CustomTextField(
            value = userName,
            onValueChange = { userName = it.lowercase(getDefault()) },
            hint = R.string.username_hint, // You need to add this string resource,
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
            value = mobileNumber,
            onValueChange = { mobileNumber = it },
            hint = R.string.mobile_hint,
            keyboardType = KeyboardType.Phone,
            countrySelected = {
                countryCode = it.code
            }
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
                if (!isUsernameValid) {
                    Toast.makeText(context, "Please enter a valid username", Toast.LENGTH_SHORT).show()
                } else if (userName.isNotBlank() && mobileNumber.isNotBlank() && password.isNotBlank()) {
                    viewModel.createUserWithPhone(countryCode+mobileNumber, activity)
                } else {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = modifier
                .fillMaxWidth()
                .height(ButtonHeight),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            if (isOtpSending) {
                AnimatedPreloader()
            } else
            Text(text = stringResource(id = R.string.signup_button_label))
        }
        SpacerHeight(LargeSize)
        GoToLogin(modifier) {
            onNavigateToLogin()
        }
    }

    if (showOtpBottomSheet) {
        OtpVerificationBottomSheet(
            onDismiss = {
                showOtpBottomSheet = false
                isOtpSending = false
            },
            onVerify = { otp ->
                viewModel.signInWithCredential(otp)

            },
            isLoading = isOtpSending
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationBottomSheet(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit,
    isLoading: Boolean
) {
    var otp by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
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
                text = "Enter the verification code we sent to your mobile number",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            SpacerHeight(LargeSize)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
//                val otpCellConfig = OhTeePeeCellConfiguration.withDefaults(
//                    borderColor = OTPBorder,
//                    borderWidth = 1.dp,
//                    shape = RoundedCornerShape(12.dp),
//                    backgroundColor = Color.Transparent,
//                    textStyle = TextStyle(
//                        color = Color.Black,
//                        fontSize = 20.sp,
//                        fontWeight = FontWeight.Bold
//                    )
//                )
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
//                OhTeePeeInput(
//                    value = otp,
//                    onValueChange = { newValue, isValid ->
//                        otp = newValue
//                        if (otp.length == 6 && isValid) {
//                            // Avoid multiple calls by checking that the length is exactly 6
//                            keyboardController?.hide()
//                            onVerify(otp)
//                        }
//                    },
//                    configurations = OhTeePeeConfigurations.withDefaults(
//                        cellsCount = 6,
//                        activeCellConfig = otpCellConfig.copy(
//                            borderColor = MaterialTheme.colorScheme.primary,
//                            borderWidth = 3.dp
//                        ),
//                        emptyCellConfig = otpCellConfig,
//                        cellModifier = Modifier
//                            .padding(horizontal = 4.dp)
//                            .width(46.dp)
//                            .height(50.dp)
//                            .focusRequester(focusRequester),
//                        elevation = 4.dp
//                    )
//                )
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
    modifier: Modifier = Modifier, onNavigateToLogin: () -> Unit
) {
    Row(
        modifier = modifier, horizontalArrangement = Arrangement.spacedBy(
            SmallSize
        )
    ) {
        Text(text = "Already have an account?", style = MaterialTheme.typography.bodySmall)
        Text(text = "Login",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier.clickable { onNavigateToLogin() })
    }
}