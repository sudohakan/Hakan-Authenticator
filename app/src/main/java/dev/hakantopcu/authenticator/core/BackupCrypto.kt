package dev.hakantopcu.authenticator.core

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

/** Password-protected export: PBKDF2-HMAC-SHA256 derives an AES-256-GCM key; the file is JSON. */
object BackupCrypto {
    const val FORMAT = "hakan-authenticator-backup"
    private const val ITERATIONS = 310_000
    private const val MIN_PASSWORD = 8

    fun encrypt(plain: String, password: CharArray, random: SecureRandom = SecureRandom()): String {
        require(password.size >= MIN_PASSWORD) { "password too short" }
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, derive(password, salt, ITERATIONS), GCMParameterSpec(128, iv))
        cipher.updateAAD(FORMAT.toByteArray())
        val sealed = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val b64 = Base64.getEncoder()
        return JSONObject().put("format", FORMAT).put("version", 1).put("kdf", "PBKDF2WithHmacSHA256")
            .put("iterations", ITERATIONS).put("salt", b64.encodeToString(salt)).put("iv", b64.encodeToString(iv))
            .put("data", b64.encodeToString(sealed)).toString()
    }

    /** Throws [javax.crypto.AEADBadTagException] on a wrong password or tampered file. */
    fun decrypt(file: String, password: CharArray): String {
        val o = JSONObject(file)
        require(o.optString("format") == FORMAT) { "not a Hakan Authenticator backup" }
        val iterations = o.getInt("iterations")
        require(iterations in 100_000..5_000_000) { "unsupported iteration count" }
        val b64 = Base64.getDecoder()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, derive(password, b64.decode(o.getString("salt")), iterations), GCMParameterSpec(128, b64.decode(o.getString("iv"))))
        cipher.updateAAD(FORMAT.toByteArray())
        return String(cipher.doFinal(b64.decode(o.getString("data"))), Charsets.UTF_8)
    }

    private fun derive(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        try {
            val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(key, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
