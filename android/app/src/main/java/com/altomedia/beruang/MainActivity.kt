package com.altomedia.beruang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.altomedia.beruang.ui.auth.AuthViewModel
import com.altomedia.beruang.ui.nav.RootNav
import com.altomedia.beruang.ui.theme.BERUANGTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BERUANGTheme {
                val authVm: AuthViewModel = hiltViewModel()
                val user by authVm.session.collectAsState()
                if (user != null) RootNav() else com.altomedia.beruang.ui.auth.AuthScreen()
            }
        }
    }
}
