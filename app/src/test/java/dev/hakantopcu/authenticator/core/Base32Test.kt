package dev.hakantopcu.authenticator.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class Base32Test {
    @Test fun rfc4648Vectors() {
        val cases = mapOf("" to "", "f" to "MY", "fo" to "MZXQ", "foo" to "MZXW6", "foob" to "MZXW6YQ", "fooba" to "MZXW6YTB", "foobar" to "MZXW6YTBOI")
        cases.forEach { (plain, enc) ->
            assertEquals(enc, Base32.encode(plain.toByteArray()))
            if (enc.isNotEmpty()) assertArrayEquals(plain.toByteArray(), Base32.decode(enc))
        }
    }

    @Test fun decodeIgnoresSpacesCaseAndPadding() {
        assertArrayEquals("foobar".toByteArray(), Base32.decode("mzxw 6ytb-oi======"))
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsInvalid() { Base32.decode("ABC1") }
}
