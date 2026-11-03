package dev.hakantopcu.authenticator.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import dev.hakantopcu.authenticator.core.Account
import dev.hakantopcu.authenticator.store.VaultStore

class VaultState(private val store: VaultStore, private val loadError: (String) -> String) {
    val accounts = mutableStateListOf<Account>()
    val error = mutableStateOf<String?>(null)

    fun reload() {
        accounts.clear()
        try {
            accounts.addAll(store.load())
            error.value = null
        } catch (e: Exception) {
            error.value = loadError(e.message.orEmpty())
        }
    }

    fun clear() = accounts.clear()

    /** Adds the accounts not already present (same secret and label); returns how many were added. */
    fun addAll(incoming: List<Account>): Int {
        val fresh = incoming.filter { n -> accounts.none { it.secret == n.secret && it.label == n.label } }
        if (fresh.isEmpty()) return 0
        persist(accounts + fresh)
        return fresh.size
    }

    fun remove(account: Account) = persist(accounts.filter { it.id != account.id })

    fun rename(account: Account, issuer: String, label: String) =
        persist(accounts.map { if (it.id == account.id) it.copy(issuer = issuer.trim(), label = label.trim()) else it })

    private fun persist(next: List<Account>) {
        store.save(next)
        accounts.clear()
        accounts.addAll(next)
    }
}
