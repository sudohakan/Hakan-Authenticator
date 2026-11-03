package dev.hakantopcu.authenticator.core

import javax.crypto.AEADBadTagException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BackupCryptoTest {
    private val accounts = listOf(Account("1", "HCD Panel", "hakan", "JBSWY3DPEHPK3PXP"), Account("2", "", "solo", "MZXW6YTBOI", Algorithm.SHA512, 8, 60))

    @Test fun roundTrip() {
        val file = BackupCrypto.encrypt(Account.listToJson(accounts), "dogru-parola".toCharArray())
        assertFalse("secret must not appear in clear", file.contains("JBSWY3DPEHPK3PXP"))
        assertEquals(accounts, Account.listFromJson(BackupCrypto.decrypt(file, "dogru-parola".toCharArray())))
    }

    @Test(expected = AEADBadTagException::class) fun wrongPassword() {
        val file = BackupCrypto.encrypt("{}", "dogru-parola".toCharArray())
        BackupCrypto.decrypt(file, "yanlis-parola".toCharArray())
    }

    @Test(expected = AEADBadTagException::class) fun tamperedData() {
        val o = JSONObject(BackupCrypto.encrypt("{\"a\":1}", "dogru-parola".toCharArray()))
        val data = o.getString("data").toCharArray()
        data[2] = if (data[2] == 'A') 'B' else 'A'
        BackupCrypto.decrypt(o.put("data", String(data)).toString(), "dogru-parola".toCharArray())
    }

    @Test(expected = IllegalArgumentException::class) fun shortPassword() { BackupCrypto.encrypt("{}", "kisa".toCharArray()) }
}
