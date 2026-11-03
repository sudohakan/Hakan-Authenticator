package dev.hakantopcu.authenticator.core

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

enum class Algorithm(val macName: String) { SHA1("HmacSHA1"), SHA256("HmacSHA256"), SHA512("HmacSHA512") }

/** RFC 4226 HOTP and RFC 6238 TOTP. */
object Totp {
    private val POWERS = intArrayOf(1, 10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000)

    fun hotp(secret: ByteArray, counter: Long, digits: Int, algorithm: Algorithm): String {
        require(digits in 6..8) { "digits must be 6..8" }
        val mac = Mac.getInstance(algorithm.macName)
        mac.init(SecretKeySpec(secret, algorithm.macName))
        val msg = ByteArray(8) { i -> (counter ushr (56 - 8 * i)).toByte() }
        val hash = mac.doFinal(msg)
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        return (binary % POWERS[digits]).toString().padStart(digits, '0')
    }

    fun counterAt(epochSeconds: Long, period: Int): Long = Math.floorDiv(epochSeconds, period.toLong())

    fun code(account: Account, epochSeconds: Long): String =
        hotp(Base32.decode(account.secret), counterAt(epochSeconds, account.period), account.digits, account.algorithm)

    fun secondsLeft(epochSeconds: Long, period: Int): Int = period - Math.floorMod(epochSeconds, period.toLong()).toInt()
}
