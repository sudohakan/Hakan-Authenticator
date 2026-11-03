package dev.hakantopcu.authenticator.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TotpTest {
    private val sha1Key = "12345678901234567890".toByteArray()
    private val sha256Key = "12345678901234567890123456789012".toByteArray()
    private val sha512Key = "1234567890123456789012345678901234567890123456789012345678901234".toByteArray()
    private val times = longArrayOf(59, 1111111109, 1111111111, 1234567890, 2000000000, 20000000000)

    private fun check(key: ByteArray, alg: Algorithm, expected: List<String>) {
        times.forEachIndexed { i, t -> assertEquals("t=$t $alg", expected[i], Totp.hotp(key, Totp.counterAt(t, 30), 8, alg)) }
    }

    @Test fun rfc6238Sha1() = check(sha1Key, Algorithm.SHA1, listOf("94287082", "07081804", "14050471", "89005924", "69279037", "65353130"))
    @Test fun rfc6238Sha256() = check(sha256Key, Algorithm.SHA256, listOf("46119246", "68084774", "67062674", "91819424", "90698825", "77737706"))
    @Test fun rfc6238Sha512() = check(sha512Key, Algorithm.SHA512, listOf("90693936", "25091201", "99943326", "93441116", "38618901", "47863826"))

    @Test fun rfc4226Hotp() {
        val expected = listOf("755224", "287082", "359152", "969429", "338314", "254676", "287922", "162583", "399871", "520489")
        expected.forEachIndexed { counter, code -> assertEquals(code, Totp.hotp(sha1Key, counter.toLong(), 6, Algorithm.SHA1)) }
    }

    @Test fun accountCodeUsesBase32Secret() {
        val account = Account("a", "T", "l", Base32.encode(sha1Key), Algorithm.SHA1, 8, 30)
        assertEquals("94287082", Totp.code(account, 59))
    }

    @Test fun secondsLeft() {
        assertEquals(30, Totp.secondsLeft(60, 30))
        assertEquals(1, Totp.secondsLeft(59, 30))
    }
}
