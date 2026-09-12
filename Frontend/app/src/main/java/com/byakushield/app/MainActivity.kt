package com.byakushield.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.byakushield.app.data.security.TokenManager
import com.byakushield.app.ui.navigation.AppNavigation
import com.byakushield.app.ui.theme.ByakushieldTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenManager = TokenManager(this)

        val startDestination =
            if (tokenManager.getToken().isNullOrEmpty()) {
                "login"
            } else {
                "home"
            }

        setContent {
            ByakushieldTheme {
                AppNavigation(
                    tokenManager = tokenManager,
                    startDestination = startDestination
                )
            }
        }
    }
}