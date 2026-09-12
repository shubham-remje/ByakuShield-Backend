package com.byakushield.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.byakushield.app.data.network.RetrofitClient
import com.byakushield.app.data.security.TokenManager
import com.byakushield.app.model.LoginRequest
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    tokenManager: TokenManager,
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val background = Color(0xFF07110D)
    val card = Color(0xFF101B16)
    val green = Color(0xFF35F47F)
    val textPrimary = Color(0xFFF1F5F2)
    val textSecondary = Color(0xFF9EAAA4)
    val errorRed = Color(0xFFFF6B6B)

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        Text(
            text = "ByakuShield",
            color = green,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = "Secure Login",
            color = textPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Protect your digital world.",
            color = textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = {
                Text("Email")
            },
            singleLine = true,
            enabled = !isLoading,
            isError = errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = green,
                unfocusedBorderColor = textSecondary.copy(alpha = 0.5f),
                focusedLabelColor = green,
                unfocusedLabelColor = textSecondary,
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                cursorColor = green,
                errorBorderColor = errorRed,
                errorLabelColor = errorRed,
                errorCursorColor = errorRed
            )
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = {
                Text("Password")
            },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !isLoading,
            isError = errorMessage != null,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = green,
                unfocusedBorderColor = textSecondary.copy(alpha = 0.5f),
                focusedLabelColor = green,
                unfocusedLabelColor = textSecondary,
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                cursorColor = green,
                errorBorderColor = errorRed,
                errorLabelColor = errorRed,
                errorCursorColor = errorRed
            )
        )

        if (errorMessage != null) {

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = errorMessage!!,
                color = errorRed,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Button(
            onClick = {

                errorMessage = null

                if (email.isBlank()) {
                    errorMessage = "Please enter your email."
                    return@Button
                }

                if (!android.util.Patterns.EMAIL_ADDRESS
                        .matcher(email.trim())
                        .matches()
                ) {
                    errorMessage = "Please enter a valid email address."
                    return@Button
                }

                if (password.isBlank()) {
                    errorMessage = "Please enter your password."
                    return@Button
                }

                scope.launch {

                    isLoading = true

                    try {

                        val api =
                            RetrofitClient.getClient(
                                tokenManager
                            )

                        val response =
                            api.login(
                                LoginRequest(
                                    email = email.trim(),
                                    password = password
                                )
                            )

                        if (response.isSuccessful) {

                            val loginResponse =
                                response.body()

                            if (
                                loginResponse != null &&
                                loginResponse.token.isNotBlank()
                            ) {

                                tokenManager.saveToken(
                                    loginResponse.token
                                )

                                Toast.makeText(
                                    context,
                                    "Login successful!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                onLoginSuccess()

                            } else {

                                errorMessage =
                                    "Invalid response received from server."
                            }

                        } else {

                            errorMessage =
                                when (response.code()) {

                                    400 ->
                                        "Invalid login request."

                                    401 ->
                                        "Invalid email or password."

                                    403 ->
                                        "Access denied."

                                    404 ->
                                        "Login service not found."

                                    500 ->
                                        "Server error. Please try again later."

                                    else ->
                                        "Login failed. Error ${response.code()}."
                                }
                        }

                    } catch (e: Exception) {

                        errorMessage =
                            if (e.localizedMessage.isNullOrBlank()) {
                                "Unable to connect to the server."
                            } else {
                                "Connection error. Please check your network."
                            }

                    } finally {

                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = green,
                contentColor = Color.Black,
                disabledContainerColor = green.copy(alpha = 0.4f),
                disabledContentColor = Color.Black.copy(alpha = 0.5f)
            )
        ) {

            if (isLoading) {

                CircularProgressIndicator(
                    modifier = Modifier.height(18.dp),
                    color = Color.Black,
                    strokeWidth = 2.5.dp
                )

                Spacer(
                    modifier = Modifier.height(0.dp)
                )

                Text(
                    text = "  Signing in...",
                    fontWeight = FontWeight.Bold
                )

            } else {

                Text(
                    text = "Login",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        TextButton(
            onClick = onRegisterClick,
            enabled = !isLoading
        ) {

            Text(
                text = "Don't have an account? Register",
                color = green
            )
        }

        Spacer(
            modifier = Modifier.height(32.dp)
        )
    }
}