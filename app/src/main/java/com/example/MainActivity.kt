package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.VaultViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.VaultDashboardScreen
import com.example.ui.theme.DarkBg
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg
                ) {
                    SecureVaultApp()
                }
            }
        }
    }
}

@Composable
fun SecureVaultApp(viewModel: VaultViewModel = viewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    if (!authState.isUnlocked) {
        AuthScreen(
            authState = authState,
            onCompleteSetup = { phone, pin, hint ->
                viewModel.completePhoneSetup(phone, pin, hint)
            },
            onUnlockWithPin = { pin ->
                viewModel.unlockWithPin(pin)
            },
            onResetVault = {
                viewModel.wipeAllAndReset()
            }
        )
    } else {
        VaultDashboardScreen(
            viewModel = viewModel,
            onLockVault = {
                viewModel.lockVault()
            }
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}

