package com.orion.templete.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.orion.templete.R
import com.orion.templete.data.model.login_model.ForgetPasswordRequest
import com.orion.templete.data.model.login_model.LoginResponseDTO
import com.orion.templete.data.model.login_model.PhoneAuthRequest
import com.orion.templete.data.model.login_model.PhoneAuthResponse
import com.orion.templete.data.model.login_model.PhoneSignupRequest
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

    override suspend fun firebaseIdToken(): String =
        db.currentUser?.getIdToken(false)?.await()?.token
            ?: throw Exception("Please verify your phone number again")

    override fun verifiedPhoneNumber(): String? = db.currentUser?.phoneNumber?.takeIf { it.isNotBlank() }

    override fun signOutFirebase() = db.signOut()

    override suspend fun phoneAuth(firebaseIdToken: String): PhoneAuthResponse =
        safeApiRequest { apiService.phoneAuth(PhoneAuthRequest(firebaseIdToken)) }

    override suspend fun phoneSignup(request: PhoneSignupRequest): LoginResponseDTO =
        safeApiRequest { apiService.phoneSignup(request) }

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
