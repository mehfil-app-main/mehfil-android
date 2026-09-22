package com.mehfil.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mehfil.app.model.AppStore
import com.mehfil.app.model.Appearance
import com.mehfil.app.model.AuthService
import com.mehfil.app.model.Feature
import com.mehfil.app.model.Features
import com.mehfil.app.ui.MehfilApp
import com.mehfil.app.ui.screens.OnboardingScreen
import com.mehfil.app.ui.screens.SplashScreen
import com.mehfil.app.ui.screens.VerifyEmailScreen
import com.mehfil.app.ui.screens.WelcomeScreen
import com.mehfil.app.ui.theme.MehfilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val store: AppStore = viewModel()
            val dark = if (!Features[Feature.DARK_MODE]) false else when (store.appearance) {
                Appearance.SYSTEM -> isSystemInDarkTheme()
                Appearance.LIGHT -> false
                Appearance.DARK -> true
            }
            MehfilTheme(dark = dark) { Session(store) }
        }
    }
}

/** Routes the session: loading → welcome (signed out) → verify email → onboarding (no profile yet) → the app. */
@Composable
private fun Session(store: AppStore) {
    val state = store.auth.state
    LaunchedEffect(state) { if (state is AuthService.State.SignedOut) store.unbind() }
    when (state) {
        AuthService.State.Loading -> SplashScreen()
        AuthService.State.SignedOut -> WelcomeScreen(store)
        is AuthService.State.SignedIn -> {
            val account = state.account
            LaunchedEffect(account.uid) { store.bind(account.uid) }
            when {
                !store.profileChecked || store.uid != account.uid -> SplashScreen()
                store.auth.needsEmailVerification -> VerifyEmailScreen(store, account)
                store.needsOnboarding -> OnboardingScreen(store, account)
                else -> MehfilApp(store)
            }
        }
    }
}
