package dev.hakantopcu.authenticator.ui

import androidx.compose.ui.platform.LocalContext
import dev.hakantopcu.authenticator.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.hakantopcu.authenticator.core.Account
import dev.hakantopcu.authenticator.core.Algorithm
import dev.hakantopcu.authenticator.core.OtpAuthUri
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(state: VaultState, onDone: () -> Unit) {
    val context = LocalContext.current
    var issuer by rememberSaveable { mutableStateOf("") }
    var label by rememberSaveable { mutableStateOf("") }
    var secret by rememberSaveable { mutableStateOf("") }
    var algorithm by rememberSaveable { mutableStateOf(Algorithm.SHA1) }
    var digits by rememberSaveable { mutableStateOf(6) }
    var period by rememberSaveable { mutableStateOf("30") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    fun save() {
        error = try {
            val account = if (secret.trim().startsWith("otpauth://", ignoreCase = true)) {
                OtpAuthUri.parse(secret)
            } else {
                Account(UUID.randomUUID().toString(), issuer.trim(), label.trim(), secret.uppercase().filter { !it.isWhitespace() }, algorithm, digits, period.toIntOrNull() ?: 0)
            }
            when {
                account.title.isBlank() -> context.getString(R.string.error_title_required)
                state.addAll(listOf(account)) == 0 -> context.getString(R.string.error_duplicate)
                else -> { onDone(); null }
            }
        } catch (_: IllegalArgumentException) {
            context.getString(R.string.error_invalid)
        }
    }

    Scaffold(
        containerColor = Hcd.bg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.enter_manually)) },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Hcd.text) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Hcd.bg, titleContentColor = Hcd.text),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(issuer, { issuer = it }, label = { Text(stringResource(R.string.issuer_hint)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(label, { label = it }, label = { Text(stringResource(R.string.label_hint)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                secret, { secret = it },
                label = { Text(stringResource(R.string.secret_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false, keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.algorithm), color = Hcd.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Algorithm.entries.forEach { a -> FilterChip(selected = algorithm == a, onClick = { algorithm = a }, label = { Text(a.name) }) }
            }
            Text(stringResource(R.string.digits), color = Hcd.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(6, 8).forEach { d -> FilterChip(selected = digits == d, onClick = { digits = d }, label = { Text("$d") }) }
            }
            OutlinedTextField(period, { period = it.filter(Char::isDigit).take(3) }, label = { Text(stringResource(R.string.period)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = Hcd.danger) }
            Button(onClick = ::save, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
        }
    }
}
