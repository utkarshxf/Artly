package com.orion.templete.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.orion.templete.R
import com.orion.templete.data.model.login_model.ForgetPasswordRequest
import com.orion.templete.data.model.login_model.LoginResponseDTO
import com.orion.templete.data.model.login_model.FirebaseAuthRequest
import com.orion.templete.data.model.login_model.FirebaseAuthResponse
import com.orion.templete.data.model.login_model.FirebaseSignupRequest
import com.orion.templete.data.model.login_model.Registration
import com.orion.templete.data.model.login_model.TokenRequest
import com.orion.templete.data.model.login_model.User
import com.orion.templete.data.model.UsernameValidationResponse
import com.orion.templete.data.network.ApiService
import com.orion.templete.domain.repository.LoginRepository
import com.orion.templete.util.AuthResultState
import com.orion.templete.util.SafeApiRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// Sent instead of "code sent" when Firebase verified the number by itself (no OTP to enter)
const val AUTO_VERIFIED = "AUTO_VERIFIED"

class LoginRepositoryImplementation @Inject constructor(
    private val apiService: ApiService,
    private val db: FirebaseAuth,
    private val context: Context
) : LoginRepository, SafeApiRequest() {
    private lateinit var omVerificationCode:String

    override suspend fun loginUserDetail(user: User): LoginResponseDTO {
        return safeApiRequest { apiService.loginUser(user) }
    }

    override suspend fun signup(user: Registration): LoginResponseDTO {
        val response =  safeApiRequest {  apiService.signup(user) }
        Log.d("signup" , response.jwtToken.toString() )
        return response
    }

    override fun alreadySignIn(): Flow<AuthResultState<String>> = callbackFlow {
        val currentUser = db.currentUser
        if (currentUser != null) {
            trySend(AuthResultState.Success(context.getString(R.string.loggedInUsers)))
        } else {
            trySend(AuthResultState.Failure(Exception(context.getString(R.string.userNotLoggedIn))))
        }
        awaitClose{
            close()
        }
    }

    override fun createUserWithPhone(phone: String, activity: Activity): Flow<AuthResultState<String>> = callbackFlow{
        Log.d("createUserWithPhone" , phone)
        trySend(AuthResultState.Loading)

        val onVerificationCallback = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
            override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                // Instant verification / SMS auto-retrieval: no code to type, sign in with the credential right away
                db.signInWithCredential(p0)
                    .addOnSuccessListener { trySend(AuthResultState.Success(AUTO_VERIFIED)) }
                    .addOnFailureListener { trySend(AuthResultState.Failure(it)) }
            }

            override fun onVerificationFailed(p0: FirebaseException) {
                trySend(AuthResultState.Failure(p0))
            }

            override fun onCodeSent(verificationCode: String, p1: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(verificationCode, p1)
                trySend(AuthResultState.Success(context.getString(R.string.otpSendSuccessfully)))
                omVerificationCode = verificationCode
            }
        }
        val options = PhoneAuthOptions.newBuilder(db)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(onVerificationCallback)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        awaitClose{
            close()
        }
    }

    override fun signWithCredential(otp: String): Flow<AuthResultState<String>> = callbackFlow{
        trySend(AuthResultState.Loading)
        if (!::omVerificationCode.isInitialized) return@callbackFlow
        val credential = PhoneAuthProvider.getCredential(omVerificationCode,otp)
        db.signInWithCredential(credential)
            .addOnCompleteListener {
                if (it.isSuccessful){
                    trySend(AuthResultState.Success(context.getString(R.string.otpVerified)))
                }
            }.addOnFailureListener {
                trySend(AuthResultState.Failure(it))
            }
        awaitClose{
            close()
        }
    }


    override suspend fun verifyUser(token: TokenRequest): Boolean {
        return safeApiRequest { apiService.verifyUser(token) }
    }

    override suspend fun forgetPassword(request: ForgetPasswordRequest): LoginResponseDTO {
        return safeApiRequest { apiService.forgetPassword(request) }
    }

    // Google account picker (Credential Manager) -> Firebase sign-in with the Google ID token
    override suspend fun signInWithGoogle(activity: Activity) {
        // default_web_client_id is generated from google-services.json once Google sign-in is enabled in Firebase
        val webClientIdRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (webClientIdRes == 0) throw Exception("Google sign-in is not set up yet")
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(context.getString(webClientIdRes)).build())
            .build()
        val credential = CredentialManager.create(activity).getCredential(activity, request).credential
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw Exception("Google sign-in failed, please try again")
        }
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        db.signInWithCredential(GoogleAuthProvider.getCredential(googleIdToken, null)).await()
    }

    override suspend fun firebaseIdToken(): String =
        db.currentUser?.getIdToken(false)?.await()?.token
            ?: throw Exception("Please verify your phone number again")

    // The phone number (SMS sign-in) or email (Google sign-in) Firebase has verified
    override fun verifiedIdentity(): String? =
        db.currentUser?.let { user -> user.phoneNumber?.takeIf { it.isNotBlank() } ?: user.email?.takeIf { it.isNotBlank() } }

    override fun signOutFirebase() = db.signOut()

    override suspend fun firebaseAuth(firebaseIdToken: String): FirebaseAuthResponse =
        safeApiRequest { apiService.firebaseAuth(FirebaseAuthRequest(firebaseIdToken)) }

    override suspend fun firebaseSignup(request: FirebaseSignupRequest): LoginResponseDTO =
        safeApiRequest { apiService.firebaseSignup(request) }

    override fun validateUsername(username: String): Flow<AuthResultState<UsernameValidationResponse>> = callbackFlow {
        trySend(AuthResultState.Loading)
        try {
            val response = apiService.validateUsername(username)
            if (response.isSuccessful && response.body() != null) {
                trySend(AuthResultState.Success(response.body()!!))
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Error validating username"
                trySend(
                    AuthResultState.Failure(
                        Exception(errorMessage)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("validateUsername", "Error: ${e.message}")
            trySend(AuthResultState.Failure(e))
        }
        awaitClose {
            close()
        }
    }
}
