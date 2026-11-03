package dev.hakantopcu.authenticator.ui

import dev.hakantopcu.authenticator.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LockScreen(message: String?, onUnlock: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Hcd.bg).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(64.dp).background(Hcd.accent, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = Hcd.text, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.app_name), color = Hcd.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.locked), color = Hcd.muted, fontSize = 12.sp, letterSpacing = 2.sp)
        if (message != null) {
            Spacer(Modifier.height(20.dp))
            Text(message, color = Hcd.danger, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(28.dp))
        Button(onClick = onUnlock) { Text(stringResource(R.string.unlock)) }
    }
}
