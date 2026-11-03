package dev.hakantopcu.authenticator.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

enum class Screen { List, Manual, Scan }

@Composable
fun AppRoot(state: VaultState) {
    var screen by rememberSaveable { mutableStateOf(Screen.List) }
    BackHandler(enabled = screen != Screen.List) { screen = Screen.List }
    when (screen) {
        Screen.List -> AccountsScreen(state, onManual = { screen = Screen.Manual }, onScan = { screen = Screen.Scan })
        Screen.Manual -> AddAccountScreen(state, onDone = { screen = Screen.List })
        Screen.Scan -> ScannerScreen(state, onDone = { screen = Screen.List }, onManual = { screen = Screen.Manual })
    }
}
