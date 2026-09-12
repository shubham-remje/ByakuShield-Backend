package com.byakushield.app.ui.screens

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.byakushield.app.data.network.RetrofitClient
import com.byakushield.app.data.security.TokenManager
import com.byakushield.app.model.RegisterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun RegisterScreen(
    tokenManager: TokenManager,
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val background = Color(0xFF07110D)
    val green = Color(0xFF35F47F)
    val textPrimary = Color(0xFFF1F5F2)
    val textSecondary = Color(0xFF9EAAA4)

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "ByakuShield",
            color = green,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Create your account",
            color = textPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = {
                if (it.length <= 50) {
                    username = it
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Username")
            },
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                if (it.length <= 255) {
                    email = it
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Email")
            },
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                if (it.length <= 100) {
                    password = it
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Password")
            },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                if (it.length <= 100) {
                    confirmPassword = it
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Confirm Password")
            },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(22.dp)
        )

        Button(
            onClick = {

                when {

                    username.isBlank() -> {
                        Toast.makeText(
                            context,
                            "Username is required",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    username.trim().length < 3 -> {
                        Toast.makeText(
                            context,
                            "Username must contain at least 3 characters",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    email.isBlank() -> {
                        Toast.makeText(
                            context,
                            "Email is required",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    !Patterns.EMAIL_ADDRESS
                        .matcher(email.trim())
                        .matches() -> {
                        Toast.makeText(
                            context,
                            "Please enter a valid email address",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    password.isBlank() -> {
                        Toast.makeText(
                            context,
                            "Password is required",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    password.length < 8 -> {
                        Toast.makeText(
                            context,
                            "Password must contain at least 8 characters",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    password != confirmPassword -> {
                        Toast.makeText(
                            context,
                            "Passwords do not match",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    else -> {

                        isLoading = true

                        scope.launch {

                            try {

                                val api =
                                    RetrofitClient.getClient(
                                        tokenManager
                                    )

                                val request =
                                    RegisterRequest(
                                        username = username.trim(),
                                        email = email.trim(),
                                        password = password
                                    )

                                val response =
                                    withContext(Dispatchers.IO) {
                                        api.register(request)
                                    }

                                if (response.isSuccessful) {

                                    Toast.makeText(
                                        context,
                                        "Registration successful",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    onRegisterSuccess()

                                } else {

                                    val errorMessage =
                                        try {
                                            val errorBody =
                                                response.errorBody()
                                                    ?.string()

                                            if (!errorBody.isNullOrBlank()) {
                                                JSONObject(errorBody)
                                                    .optString(
                                                        "message",
                                                        "Registration failed"
                                                    )
                                            } else {
                                                "Registration failed"
                                            }
                                        } catch (_: Exception) {
                                            "Registration failed"
                                        }

                                    Toast.makeText(
                                        context,
                                        errorMessage,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                            } catch (_: Exception) {

                                Toast.makeText(
                                    context,
                                    "Unable to connect to server",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } finally {

                                isLoading = false
                            }
                        }
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = green,
                contentColor = Color.Black
            )
        ) {

            if (isLoading) {

                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )

            } else {

                Text(
                    text = "Create Account",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        TextButton(
            onClick = onBackToLogin
        ) {

            Text(
                text = "Already have an account? Login",
                color = textSecondary
            )
        }
    }
}