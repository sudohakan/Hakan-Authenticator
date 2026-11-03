package dev.hakantopcu.authenticator.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// HCD panel palette: near-black ground, violet accent, cyan focus.
object Hcd {
    val bg = Color(0xFF07070C)
    val panel = Color(0xFF0E0E17)
    val border = Color(0x3306B6D4)
    val accent = Color(0xFF8B5CF6)
    val cyan = Color(0xFF06B6D4)
    val text = Color(0xFFE4E4E7)
    val muted = Color(0xFFA1A1AA)
    val danger = Color(0xFFF87171)
}

@Composable
fun HakanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Hcd.accent,
            onPrimary = Color.White,
            secondary = Hcd.cyan,
            background = Hcd.bg,
            surface = Hcd.panel,
            surfaceVariant = Hcd.panel,
            onBackground = Hcd.text,
            onSurface = Hcd.text,
            onSurfaceVariant = Hcd.muted,
            outline = Hcd.border,
            error = Hcd.danger,
        ),
        content = content,
    )
}
