package dev.hakantopcu.authenticator.store

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import dev.hakantopcu.authenticator.core.Account
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Accounts are kept in one file sealed with AES-256-GCM. The key is generated inside the Android
 * Keystore (hardware-backed where the phone has it) and never leaves it, so the file is useless
 * when copied off the device.
 */
class VaultStore(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "vault.bin"))

    fun load(): List<Account> {
        val bytes = try {
            file.readFully()
        } catch (_: java.io.FileNotFoundException) {
            return emptyList()
        }
        require(bytes.size > IV_SIZE) { "vault file is truncated" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, IV_SIZE))
        cipher.updateAAD(AAD)
        val plain = cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE)
        return Account.listFromJson(String(plain, Charsets.UTF_8))
    }

    fun save(accounts: List<Account>) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(AAD)
        val sealed = cipher.doFinal(Account.listToJson(accounts).toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        check(iv.size == IV_SIZE) { "unexpected IV size ${iv.size}" }
        val out = file.startWrite()
        try {
            out.write(iv)
            out.write(sealed)
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            throw e
        }
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    private companion object {
        const val ALIAS = "hakan-authenticator-vault"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        val AAD = "hakan-authenticator-vault-v1".toByteArray()
    }
}
