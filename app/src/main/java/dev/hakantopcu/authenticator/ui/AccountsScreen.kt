package dev.hakantopcu.authenticator.ui

import dev.hakantopcu.authenticator.R
import androidx.compose.ui.res.stringResource
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hakantopcu.authenticator.core.Account
import dev.hakantopcu.authenticator.core.Totp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(state: VaultState, onManual: () -> Unit, onScan: () -> Unit) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(250)
        }
    }
    var menuOpen by remember { mutableStateOf(false) }
    var addOpen by remember { mutableStateOf(false) }
    var backup by remember { mutableStateOf<BackupMode?>(null) }
    var editing by remember { mutableStateOf<Account?>(null) }

    Scaffold(
        containerColor = Hcd.bg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Hcd.bg, titleContentColor = Hcd.text),
                actions = {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.menu), tint = Hcd.text) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.backup_export)) }, onClick = { menuOpen = false; backup = BackupMode.Export })
                        DropdownMenuItem(text = { Text(stringResource(R.string.backup_import)) }, onClick = { menuOpen = false; backup = BackupMode.Import })
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { addOpen = true }, containerColor = Hcd.accent) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_account), tint = Hcd.text)
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            state.error.value?.let { Text(it, color = Hcd.danger, modifier = Modifier.padding(16.dp)) }
            if (state.accounts.isEmpty()) {
                EmptyState(onScan, onManual)
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.accounts, key = { it.id }) { account -> AccountCard(account, nowMs, onEdit = { editing = account }) }
                }
            }
        }
    }

    if (addOpen) {
        AlertDialog(
            onDismissRequest = { addOpen = false },
            title = { Text(stringResource(R.string.add_account)) },
            text = { Text(stringResource(R.string.add_account_hint)) },
            confirmButton = { TextButton(onClick = { addOpen = false; onScan() }) { Text(stringResource(R.string.scan_qr)) } },
            dismissButton = { TextButton(onClick = { addOpen = false; onManual() }) { Text(stringResource(R.string.enter_manually)) } },
        )
    }
    backup?.let { mode -> BackupDialog(mode, state, onClose = { backup = null }) }
    editing?.let { account -> EditDialog(account, state, onClose = { editing = null }) }
}

@Composable
private fun EmptyState(onScan: () -> Unit, onManual: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.empty_title), color = Hcd.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.empty_body), color = Hcd.muted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onScan) { Text(stringResource(R.string.scan_qr)) }
            TextButton(onClick = onManual) { Text(stringResource(R.string.enter_manually)) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountCard(account: Account, nowMs: Long, onEdit: () -> Unit) {
    val context = LocalContext.current
    val seconds = nowMs / 1000
    val code = Totp.code(account, seconds)
    val left = Totp.secondsLeft(seconds, account.period)
    val fraction = ((account.period * 1000 - (nowMs % (account.period * 1000L))).toFloat() / (account.period * 1000f))
    val grouped = if (code.length == 6) code.chunked(3).joinToString(" ") else code.chunked(4).joinToString(" ")
    Row(
        Modifier
            .fillMaxWidth()
            .background(Hcd.panel, RoundedCornerShape(12.dp))
            .border(1.dp, Hcd.border, RoundedCornerShape(12.dp))
            .combinedClickable(onClick = { copyCode(context, code) }, onLongClick = onEdit)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(account.title, color = Hcd.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (account.subtitle.isNotEmpty()) Text(account.subtitle, color = Hcd.muted, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Text(grouped, color = if (left <= 5) Hcd.danger else Hcd.cyan, fontSize = 30.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { fraction }, modifier = Modifier.size(36.dp), color = Hcd.accent, trackColor = Hcd.border, strokeWidth = 3.dp)
            Text("$left", color = Hcd.muted, fontSize = 11.sp)
        }
    }
}

private fun copyCode(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(context.getString(R.string.clip_label), code)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
    }
    clipboard.setPrimaryClip(clip)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast.makeText(context, context.getString(R.string.code_copied), Toast.LENGTH_SHORT).show()
}

@Composable
private fun EditDialog(account: Account, state: VaultState, onClose: () -> Unit) {
    var issuer by remember { mutableStateOf(account.issuer) }
    var label by remember { mutableStateOf(account.label) }
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_body, account.title)) },
            confirmButton = { TextButton(onClick = { state.remove(account); onClose() }) { Text(stringResource(R.string.delete), color = Hcd.danger) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.edit_account)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = issuer, onValueChange = { issuer = it }, label = { Text(stringResource(R.string.field_issuer)) }, singleLine = true)
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text(stringResource(R.string.field_label)) }, singleLine = true)
                TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.delete_account), color = Hcd.danger) }
            }
        },
        confirmButton = { TextButton(onClick = { state.rename(account, issuer, label); onClose() }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) } },
    )
}
