package dev.hakantopcu.authenticator.ui

import dev.hakantopcu.authenticator.R
import androidx.compose.ui.res.stringResource
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.hakantopcu.authenticator.core.Account
import dev.hakantopcu.authenticator.core.BackupCrypto
import java.time.LocalDate
import javax.crypto.AEADBadTagException

enum class BackupMode { Export, Import }

private const val MAX_BACKUP_BYTES = 1_000_000

@Composable
fun BackupDialog(mode: BackupMode, state: VaultState, onClose: () -> Unit) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }

    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        message = try {
            val file = BackupCrypto.encrypt(Account.listToJson(state.accounts.toList()), password.toCharArray())
            context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(file.toByteArray()) }
            done = true
            context.getString(R.string.backup_done, state.accounts.size)
        } catch (e: Exception) {
            context.getString(R.string.backup_write_failed, e.message.orEmpty())
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        message = try {
            val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readNBytes(MAX_BACKUP_BYTES + 1) }
            require(bytes.size <= MAX_BACKUP_BYTES) { "too large" }
            val accounts = Account.listFromJson(BackupCrypto.decrypt(String(bytes), password.toCharArray()))
            val added = state.addAll(accounts)
            done = true
            context.getString(R.string.restore_done, accounts.size, added)
        } catch (_: AEADBadTagException) {
            context.getString(R.string.restore_bad_password)
        } catch (_: Exception) {
            context.getString(R.string.restore_not_backup)
        }
    }

    val isExport = mode == BackupMode.Export
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(if (isExport) R.string.backup_export else R.string.backup_import)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!done) {
                    Text(stringResource(if (isExport) R.string.backup_export_hint else R.string.backup_import_hint))
                    OutlinedTextField(password, { password = it }, label = { Text(stringResource(R.string.password)) }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                    if (isExport) OutlinedTextField(repeat, { repeat = it }, label = { Text(stringResource(R.string.password_repeat)) }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                }
                message?.let { Text(it) }
            }
        },
        confirmButton = {
            if (done) {
                TextButton(onClick = onClose) { Text(stringResource(R.string.ok)) }
            } else {
                TextButton(onClick = {
                    when {
                        password.length < 8 -> message = context.getString(R.string.password_short)
                        isExport && password != repeat -> message = context.getString(R.string.password_mismatch)
                        isExport -> create.launch("hakan-authenticator-${LocalDate.now()}.json")
                        else -> open.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                    }
                }) { Text(stringResource(if (isExport) R.string.save_file else R.string.choose_file)) }
            }
        },
        dismissButton = { if (!done) TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) } },
    )
}
