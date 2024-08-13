package com.orion.templete.presentation.registration

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.orion.templete.data.model.User

@Composable
fun RegisterScreen( navigateToSignInScreen:() -> Unit = {},navigateToHomeScreen: () -> Unit = {} , viewModel: RegisterViewModel = hiltViewModel()) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val signingData  =  viewModel.signingData
    val signupData = viewModel.signupData
    val context = LocalContext.current
    LaunchedEffect(signupData) {
        signupData.error?.let { error ->
            Toast.makeText(context, "Signup error :$error", Toast.LENGTH_SHORT).show()
        }
        signupData.data?.let {
            viewModel.loginUser(User(email, password))
        }
    }
    LaunchedEffect(signingData) {
        signingData.error?.let { error ->
            Toast.makeText(context, "signIn error :$error", Toast.LENGTH_SHORT).show()
        }
        signingData.data?.let {
            navigateToHomeScreen()
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 30.dp, end = 30.dp, top = 20.dp, bottom = 20.dp)
    ) {
        LaunchedEffect(signingData) {
            signingData.data?.let {
                navigateToHomeScreen()
            }
        }
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                CommonText(
                    text = "Create Account,",
                    fontSize = 34,
                    fontWeight = FontWeight.Bold
                ) {}
                Spacer(modifier = Modifier.height(5.dp))
                CommonText(
                    text = "Sign up to get started!",
                    fontSize = 28,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    fontWeight = FontWeight.Medium
                ) {}
            }
            Spacer(modifier = Modifier.height(60.dp))
            CommonTextField(
                text = fullName,
                placeholder = "Full Name",
                onValueChange = { fullName = it },
                isPasswordTextField = false
            )
            Spacer(modifier = Modifier.height(16.dp))
            CommonTextField(
                text = email,
                placeholder = "Email",
                onValueChange = { email = it },
                isPasswordTextField = false
            )
            Spacer(modifier = Modifier.height(16.dp))
            CommonTextField(
                text = password,
                placeholder = "Password",
                onValueChange = { password = it },
                isPasswordTextField = true
            )
            Spacer(modifier = Modifier.weight(0.2f))
            CommonLoginButton(text = "Register", modifier = Modifier.fillMaxWidth()) {
                if (email.isNotBlank() && password.isNotBlank() && fullName.isNotBlank()) {
                    viewModel.signup(User(email, password))
                } else {
                    println("Kayit Basarisiz")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            CommonGoogleButton(text = "Connect with Google")
            Spacer(modifier = Modifier.weight(0.3f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                CommonText(text = "I'm a new user,", fontSize = 18 , fontWeight = FontWeight.Medium) {}
                Spacer(modifier = Modifier.width(4.dp))
                CommonText(
                    text = "Sign In",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18,
                    fontWeight = FontWeight.W500
                ) {
                    navigateToSignInScreen()
                }
            }
        }
    }
}