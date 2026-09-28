package com.orion.templete.presentation.auth

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.templete.data.model.login_model.ForgetPasswordRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.orion.templete.data.model.login_model.FirebaseSignupRequest
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
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
    private val _phoneLoginState = MutableStateFlow<PhoneLoginUiState>(PhoneLoginUiState.Idle)
    val phoneLoginState: StateFlow<PhoneLoginUiState> = _phoneLoginState

    // Username Validation State Management (MVVM - StateFlow for UI)
    private val _usernameValidationMessage = MutableStateFlow("")
    val usernameValidationMessage: StateFlow<String> = _usernameValidationMessage

    private val _isUsernameValid = MutableStateFlow(false)
    val isUsernameValid: StateFlow<Boolean> = _isUsernameValid

    private val _isCheckingUsernameAvailability = MutableStateFlow(false)
    val isCheckingUsernameAvailability: StateFlow<Boolean> = _isCheckingUsernameAvailability

    // One availability check at a time: every keystroke cancels the previous (debounced) check, so a slow
    // answer for an older name can never overwrite the result for the current text
    private var usernameCheckJob: Job? = null

    // Business Logic: All validation logic moved from UI to ViewModel
    fun onUsernameChanged(username: String) {
        usernameCheckJob?.cancel()
        _isUsernameValid.value = false
        usernameCheckJob = viewModelScope.launch {
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
                    delay(500) // wait until the user stops typing
                    validateUsername(username)
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

    // Called once Firebase has signed the user in (SMS code or Google): log in if an account uses the verified
    // phone/email, otherwise ask the user to pick a username and password (CreateAccount screen)
    fun continueWithFirebaseUser() {
        // handled: don't reopen the OTP sheet or repeat this when the user comes back to the phone screen
        _authState.value = AuthScreenUiState.Initial
        _otpState.value = OTPScreenUiState.Initial
        viewModelScope.launch(Dispatchers.IO) {
            _phoneLoginState.value = PhoneLoginUiState.Loading
            _phoneLoginState.value = try {
                val response = authRepository.firebaseAuth(authRepository.firebaseIdToken())
                if (response.registered && response.jwtToken != null && response.username != null) {
                    secureStorage.saveToken(response.jwtToken)
                    secureStorage.saveUserId(response.username)
                    trackEvents.trackLoginSuccess()
                    PhoneLoginUiState.LoggedIn
                } else {
                    PhoneLoginUiState.NeedsAccount(response.phone ?: response.email ?: authRepository.verifiedIdentity().orEmpty())
                }
            } catch (e: Exception) {
                trackEvents.trackLoginError(e.message.toString())
                PhoneLoginUiState.Error(friendlyError(e))
            }
        }
    }

    // "Continue with Google": pick a Google account, sign in to Firebase, then the same backend step as the phone flow
    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _phoneLoginState.value = PhoneLoginUiState.Loading
            try {
                authRepository.signInWithGoogle(activity)
            } catch (e: GetCredentialCancellationException) {
                _phoneLoginState.value = PhoneLoginUiState.Idle // user closed the account picker
                return@launch
            } catch (e: NoCredentialException) {
                _phoneLoginState.value = PhoneLoginUiState.Error("No Google account found on this phone")
                return@launch
            } catch (e: Exception) {
                trackEvents.trackLoginError(e.message.toString())
                _phoneLoginState.value = PhoneLoginUiState.Error(friendlyError(e))
                return@launch
            }
            continueWithFirebaseUser()
        }
    }

    fun createAccount(username: String, password: String) {
        trackEvents.trackSignupAttempted()
        viewModelScope.launch(Dispatchers.IO) {
            _phoneLoginState.value = PhoneLoginUiState.Loading
            _phoneLoginState.value = try {
                val response = authRepository.firebaseSignup(
                    FirebaseSignupRequest(authRepository.firebaseIdToken(), username, password)
                )
                secureStorage.saveToken(response.jwtToken)
                secureStorage.saveUserId(response.username)
                trackEvents.trackSignupSuccess()
                PhoneLoginUiState.LoggedIn
            } catch (e: Exception) {
                trackEvents.trackSignupError(e.message.toString())
                PhoneLoginUiState.Error(friendlyError(e))
            }
        }
    }

    fun verifiedIdentity(): String? = authRepository.verifiedIdentity()

    // Start the phone / Google step again (e.g. "use a different number")
    fun restartPhoneLogin() {
        authRepository.signOutFirebase()
        _authState.value = AuthScreenUiState.Initial
        _otpState.value = OTPScreenUiState.Initial
        _phoneLoginState.value = PhoneLoginUiState.Idle
    }

    // The screen has reacted to the last result (navigated or showed the error)
    fun resetPhoneLoginState() {
        _phoneLoginState.value = PhoneLoginUiState.Idle
    }

    // SafeApiRequest errors look like "error code: 400: {"message":"...","status":false}"; show just the message
    private fun friendlyError(e: Exception): String {
        val raw = e.message.orEmpty()
        val json = raw.substringAfter('{', "").let { if (it.isEmpty()) "" else "{$it" }
        return runCatching { JSONObject(json).optString("message") }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: raw.ifBlank { "Something went wrong, please try again" }
    }

    fun forgetPassword(phoneNumber: String, newPassword: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Set loading state
                forgetPasswordData = ForgetPasswordUiState(isLoading = true)

                // The backend only resets the password of the number Firebase verified
                val request = ForgetPasswordRequest(phoneNumber, newPassword, authRepository.firebaseIdToken())

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

    private suspend fun validateUsername(username: String) {
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
sealed interface PhoneLoginUiState {
    object Idle : PhoneLoginUiState
    object Loading : PhoneLoginUiState
    object LoggedIn : PhoneLoginUiState
    data class NeedsAccount(val identity: String) : PhoneLoginUiState
    data class Error(val message: String) : PhoneLoginUiState
}
