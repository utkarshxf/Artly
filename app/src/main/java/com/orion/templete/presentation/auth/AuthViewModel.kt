package com.orion.templete.presentation.auth

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.login_model.ForgetPasswordRequest
import com.orion.templete.data.model.login_model.Registration
import com.orion.templete.data.model.login_model.User
import com.orion.templete.data.model.UsernameValidationResponse
import com.orion.templete.domain.repository.LoginRepository
import com.orion.templete.usecase.RegisterUseCase
import com.orion.templete.util.AuthResultState
import com.orion.templete.util.ResponseStates
import com.orion.templete.util.SecureStorage
import com.orion.templete.util.LoginUiState
import com.orion.templete.util.UserCheckStateHolder
import com.orion.templete.util.SignupUiState
import com.orion.templete.util.ForgetPasswordUiState
import com.orion.templete.util.TrackEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: RegisterUseCase,
    private val secureStorage: SecureStorage,
    private val authRepository: LoginRepository,
    private val trackEvents: TrackEvents
) : ViewModel() {

    var signingData by mutableStateOf(LoginUiState())
    var signupData by mutableStateOf(SignupUiState())
    var forgetPasswordData by mutableStateOf(ForgetPasswordUiState())
    var checkUser by mutableStateOf(UserCheckStateHolder())
    private val _authState = MutableStateFlow<AuthScreenUiState>(AuthScreenUiState.Initial)
    val authState: StateFlow<AuthScreenUiState> = _authState
    private val _otpState = MutableStateFlow<OTPScreenUiState>(OTPScreenUiState.Initial)
    val otpState: StateFlow<OTPScreenUiState> = _otpState

    // Username Validation State Management (MVVM - StateFlow for UI)
    private val _usernameValidationMessage = MutableStateFlow("")
    val usernameValidationMessage: StateFlow<String> = _usernameValidationMessage

    private val _isUsernameValid = MutableStateFlow(false)
    val isUsernameValid: StateFlow<Boolean> = _isUsernameValid

    private val _isCheckingUsernameAvailability = MutableStateFlow(false)
    val isCheckingUsernameAvailability: StateFlow<Boolean> = _isCheckingUsernameAvailability

    // Internal StateFlow to handle username input changes with debounce
    private val _usernameInputFlow = MutableStateFlow("")

    init {
        // Debounce username input and validate
        _usernameInputFlow
            .debounce(500) // Wait 500ms after user stops typing
            .distinctUntilChanged() // Only proceed if username actually changed
            .filter { it.isNotEmpty() && it.length >= 3 && it.matches(Regex("^[a-z]+$")) } // Filter invalid inputs
            .onEach { validateUsername(it) } // Validate when conditions met
            .launchIn(viewModelScope)
    }

    // Business Logic: All validation logic moved from UI to ViewModel
    fun onUsernameChanged(username: String) {
        viewModelScope.launch {
            // Perform local validation first (instant feedback)
            when {
                username.isEmpty() -> {
                    _usernameValidationMessage.value = ""
                    _isUsernameValid.value = false
                    _isCheckingUsernameAvailability.value = false
                }
                username.contains(" ") -> {
                    _usernameValidationMessage.value = "Username must not contain spaces"
                    _isUsernameValid.value = false
                    _isCheckingUsernameAvailability.value = false
                }
                username.length < 3 -> {
                    _usernameValidationMessage.value = "Username must be at least 3 characters"
                    _isUsernameValid.value = false
                    _isCheckingUsernameAvailability.value = false
                }
                username != username.lowercase() -> {
                    _usernameValidationMessage.value = "Username must contain only lowercase letters"
                    _isUsernameValid.value = false
                    _isCheckingUsernameAvailability.value = false
                }
                !username.matches(Regex("^[a-z]+$")) -> {
                    _usernameValidationMessage.value = "Username can only contain lowercase letters"
                    _isUsernameValid.value = false
                    _isCheckingUsernameAvailability.value = false
                }
                else -> {
                    _usernameInputFlow.value = username
                }
            }
        }
    }

    fun checkSignInStatus() {
        viewModelScope.launch {
            authRepository.alreadySignIn()
                .collect { result ->
                    // Track event if needed
                }
        }
    }

    fun createUserWithPhone(mobile: String, activity: Activity) {
        // Track OTP generation attempt
        trackEvents.trackPhoneLoginAttempted(mobile)

        viewModelScope.launch {
            Log.d("createUserWithPhone", "createUserWithPhone")
            authRepository.createUserWithPhone(mobile, activity)
                .collect { result ->
                    _authState.value = when (result) {
                        is AuthResultState.Loading -> AuthScreenUiState.Loading
                        is AuthResultState.Success -> {
                            // Track successful OTP sent
                            trackEvents.trackPhoneLoginOTPSent(mobile)
                            AuthScreenUiState.Success(result.data)
                        }
                        is AuthResultState.Failure -> {
                            Log.d("Failure to send otp", result.msg.message.toString())
                            // Track OTP sending failure
                            trackEvents.trackPhoneLoginOTPError(mobile, result.msg.message.toString())
                            AuthScreenUiState.Error(result.msg.message.toString())
                        }
                    }
                }
        }
    }

    fun signInWithCredential(code: String) {
        // Track OTP verification attempt
        trackEvents.trackPhoneLoginOTPSubmitted()

        viewModelScope.launch {
            authRepository.signWithCredential(code)
                .collect { result ->
                    _otpState.value = when (result) {
                        is AuthResultState.Loading -> OTPScreenUiState.Loading
                        is AuthResultState.Success -> {
                            // Track successful OTP verification
                            trackEvents.trackPhoneLoginSuccess()
                            OTPScreenUiState.Success(result.data)
                        }
                        is AuthResultState.Failure -> {
                            // Track OTP verification failure
                            trackEvents.trackPhoneLoginOTPVerificationError(result.msg.message.toString())
                            OTPScreenUiState.Error(result.msg.message.toString())
                        }
                    }
                }
        }
    }

    fun isValidToken(token: String) {
        // Track token validation attempt
        trackEvents.trackTokenValidationAttempt()

        viewModelScope.launch {
            loginUseCase(token).collect { isValid ->
                when (isValid) {
                    is ResponseStates.Success -> {
                        // Track successful token validation
                        trackEvents.trackTokenValidationSuccess()
                        checkUser = UserCheckStateHolder(data = isValid.data, isLoading = false)
                    }

                    is ResponseStates.Error -> {
                        // Track token validation failure
                        trackEvents.trackTokenValidationError(isValid.error)
                        checkUser = UserCheckStateHolder(error = isValid.error, isLoading = false)
                    }

                    is ResponseStates.Loading -> {
                        checkUser = UserCheckStateHolder(isLoading = true)
                    }
                }
            }
        }
    }

    fun signup(user: Registration) {
        // Track signup attempt
        trackEvents.trackSignupAttempted()

        viewModelScope.launch(Dispatchers.IO) {
            loginUseCase.signup(user).collect{
                when (it) {
                    is ResponseStates.Error -> {
                        Log.d("viewModelScope", "Error")
                        // Track signup error
                        trackEvents.trackSignupError(it.error)
                        signupData = SignupUiState(error = it.error)
                    }

                    is ResponseStates.Loading -> {
                        signupData = SignupUiState(isLoading = true)
                    }

                    is ResponseStates.Success -> {
                        Log.d("viewModelScope", "Success")
                        it.data.let { loginResponse ->
                            secureStorage.saveToken(loginResponse.jwtToken)
                            secureStorage.saveUserId(loginResponse.username)
                        }
                        // Track signup success
                        trackEvents.trackSignupSuccess()
                        signupData = SignupUiState(data = it.data)
                    }
                }
            }
        }
    }

    fun loginUser(user: User) {
        // Track login attempt
        trackEvents.trackLoginAttempted()

        viewModelScope.launch(Dispatchers.IO) {
            loginUseCase.signin(user).collect{
                when (it) {
                    is ResponseStates.Error -> {
                        // Track login error
                        trackEvents.trackLoginError(it.error.toString())
                        signingData = LoginUiState(error = it.error.toString())
                    }

                    is ResponseStates.Loading -> {
                        signingData = LoginUiState(isLoading = true)
                    }

                    is ResponseStates.Success -> {
                        it.data.let { loginResponse ->
                            secureStorage.saveToken(loginResponse.jwtToken)
                            secureStorage.saveUserId(loginResponse.username)
                        }
                        // Track login success
                        trackEvents.trackLoginSuccess()
                        signingData = LoginUiState(data = it.data)
                    }
                }
            }
        }
    }

    fun forgetPassword(phoneNumber: String, newPassword: String) {
        // Create the request object
        val request = ForgetPasswordRequest(phoneNumber, newPassword)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Set loading state
                forgetPasswordData = ForgetPasswordUiState(isLoading = true)

                // Call the repository method
                val response = authRepository.forgetPassword(request)

                // Save the token and user ID
                secureStorage.saveToken(response.jwtToken)
                secureStorage.saveUserId(response.username)

                // Update the UI state with success
                forgetPasswordData = ForgetPasswordUiState(data = response)
            } catch (e: Exception) {
                // Update the UI state with error
                forgetPasswordData = ForgetPasswordUiState(error = e.message)
            }
        }
    }

    private fun validateUsername(username: String) {
        viewModelScope.launch {
            authRepository.validateUsername(username)

                .collect { authState ->
                when (authState) {
                    is AuthResultState.Success -> {
                        val response = authState.data
                        _isCheckingUsernameAvailability.value = false
                        _isUsernameValid.value = response.isValid
                        _usernameValidationMessage.value = if (response.isValid) {
                            "Username is available ✓"
                        } else {
                            response.message ?: "Username is not available"
                        }
                        Log.d("validateUsername", "Success: ${response.message}")
                    }
                    is AuthResultState.Failure -> {
                        _isCheckingUsernameAvailability.value = false
                        _isUsernameValid.value = false
                        _usernameValidationMessage.value = authState.msg.message ?: "Error checking username"
                        Log.e("validateUsername", "Error: ${authState.msg.message}")
                    }
                    is AuthResultState.Loading -> {
                        _isCheckingUsernameAvailability.value = true
                        _usernameValidationMessage.value = "Checking availability..."
                        Log.d("validateUsername", "Loading...")
                    }
                }
            }
        }
    }
}
sealed interface AuthScreenUiState {
    object Initial : AuthScreenUiState
    object Loading : AuthScreenUiState
    data class Success(val verificationId: String) : AuthScreenUiState
    data class Error(val message: String) : AuthScreenUiState
}
sealed interface OTPScreenUiState {
    object Initial : OTPScreenUiState
    object Loading : OTPScreenUiState
    data class Success(val value: String) : OTPScreenUiState
    data class Error(val message: String) : OTPScreenUiState
}
