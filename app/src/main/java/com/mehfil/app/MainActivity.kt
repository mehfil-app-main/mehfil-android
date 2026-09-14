package com.mehfil.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mehfil.app.model.AppStore
import com.mehfil.app.model.Appearance
import com.mehfil.app.ui.MehfilApp
import com.mehfil.app.ui.theme.MehfilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val store: AppStore = viewModel()
            val dark = when (store.appearance) {
                Appearance.SYSTEM -> isSystemInDarkTheme()
                Appearance.LIGHT -> false
                Appearance.DARK -> true
            }
            MehfilTheme(dark = dark) { MehfilApp(store) }
        }
    }
}
