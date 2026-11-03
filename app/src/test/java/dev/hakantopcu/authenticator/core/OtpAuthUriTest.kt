package dev.hakantopcu.authenticator.core

import org.junit.Assert.assertEquals
import org.junit.Test

class OtpAuthUriTest {
    @Test fun parsesFullUri() {
        val a = OtpAuthUri.parse("otpauth://totp/HCD%20Panel:hakan?secret=JBSWY3DPEHPK3PXP&issuer=HCD%20Panel&algorithm=SHA256&digits=8&period=60", id = "x")
        assertEquals("HCD Panel", a.issuer)
        assertEquals("hakan", a.label)
        assertEquals("JBSWY3DPEHPK3PXP", a.secret)
        assertEquals(Algorithm.SHA256, a.algorithm)
        assertEquals(8, a.digits)
        assertEquals(60, a.period)
    }

    @Test fun defaultsAndPathIssuer() {
        val a = OtpAuthUri.parse("otpauth://totp/Authelia:hakan?secret=jbswy3dpehpk3pxp", id = "x")
        assertEquals("Authelia", a.issuer)
        assertEquals(Algorithm.SHA1, a.algorithm)
        assertEquals(6, a.digits)
        assertEquals(30, a.period)
        assertEquals("JBSWY3DPEHPK3PXP", a.secret)
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsHotp() { OtpAuthUri.parse("otpauth://hotp/x?secret=JBSWY3DPEHPK3PXP&counter=1") }
    @Test(expected = IllegalArgumentException::class) fun rejectsMissingSecret() { OtpAuthUri.parse("otpauth://totp/x?issuer=y") }
    @Test(expected = IllegalArgumentException::class) fun rejectsOtherScheme() { OtpAuthUri.parse("https://example.com/?secret=JBSWY3DPEHPK3PXP") }
}
