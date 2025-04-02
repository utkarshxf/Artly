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
import com.orion.templete.data.model.login_model.LoginResponseDTO
import com.orion.templete.data.model.login_model.Registration
import com.orion.templete.data.model.login_model.TokenRequest
import com.orion.templete.data.model.login_model.User
import com.orion.templete.data.network.ApiService
import com.orion.templete.domain.repository.LoginRepository
import com.orion.templete.util.AuthResultState
import com.orion.templete.util.SafeApiRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject

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
                trySend(AuthResultState.Success("Verification Completed"))
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
            .setPhoneNumber("+91$phone")
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
}
