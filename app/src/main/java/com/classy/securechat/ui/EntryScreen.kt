package com.classy.securechat.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.classy.securechat.data.AuthRepository
import com.classy.securechat.data.UserSharedPreferences
import com.classy.securechat.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun EntryScreen(onLoginClick: () -> Unit, onCreateAccountClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SecureDarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = SecureEmerald,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "SecureChat",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = SecureTextPrimary
        )
        Text(
            text = "Military-grade encryption",
            fontSize = 16.sp,
            color = SecureTextSecondary
        )
        Spacer(modifier = Modifier.height(64.dp))
        SecureButton(
            text = "Create Account",
            onClick = onCreateAccountClick,
            isPrimary = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        SecureButton(
            text = "Log In",
            onClick = onLoginClick,
            isPrimary = false
        )
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onNavigateToRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SecureDarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        Text(
            text = "Welcome Back",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = SecureTextPrimary
        )
        Spacer(modifier = Modifier.height(32.dp))

        SecureTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            icon = { Icon(Icons.Default.Email, contentDescription = null, tint = SecureTextSecondary) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        SecureTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SecureTextSecondary) },
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator(color = SecureEmerald)
        } else {
            SecureButton(
                text = "Log In",
                onClick = {
                    isLoading = true
                    AuthRepository.signIn(email, password,
                        onSuccess = {

                            val uid = AuthRepository.getUserId()
                            if (uid != null) {
                                FirebaseFirestore.getInstance().collection("users").document(uid).get()
                                    .addOnSuccessListener { document ->

                                        val realName = document.getString("displayName") ?: email.substringBefore("@")


                                        UserSharedPreferences.saveUser(context, realName)

                                        isLoading = false
                                        onLoginSuccess()
                                    }
                                    .addOnFailureListener {

                                        UserSharedPreferences.saveUser(context, email.substringBefore("@"))
                                        isLoading = false
                                        onLoginSuccess()
                                    }
                            } else {
                                isLoading = false
                            }
                        },
                        onError = { error ->
                            isLoading = false
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Create new account",
            color = SecureEmerald,
            modifier = Modifier.clickable { onNavigateToRegister() }
        )
    }
}

@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit, onNavigateToLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SecureDarkBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "Create Account",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = SecureTextPrimary
        )
        Spacer(modifier = Modifier.height(32.dp))

        SecureTextField(
            value = name,
            onValueChange = { name = it },
            label = "Nickname (Display Name)",
            icon = { Icon(Icons.Default.Person, contentDescription = null, tint = SecureTextSecondary) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        SecureTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            icon = { Icon(Icons.Default.Email, contentDescription = null, tint = SecureTextSecondary) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        SecureTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password (min 6 chars)",
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SecureTextSecondary) },
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator(color = SecureEmerald)
        } else {
            SecureButton(
                text = "Create Account",
                onClick = {
                    isLoading = true
                    AuthRepository.signUp(email, password, name,
                        onSuccess = {
                            isLoading = false
                            UserSharedPreferences.saveUser(context, name)
                            onRegisterSuccess()
                        },
                        onError = { error ->
                            isLoading = false
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Already have an account? Log in",
            color = SecureEmerald,
            modifier = Modifier.clickable { onNavigateToLogin() }
        )
    }
}