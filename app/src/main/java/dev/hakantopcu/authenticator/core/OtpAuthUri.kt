package dev.hakantopcu.authenticator.core

import java.net.URI
import java.net.URLDecoder
import java.util.UUID

/** Parses otpauth://totp/Issuer:label?secret=...&issuer=...&algorithm=...&digits=...&period=... */
object OtpAuthUri {
    fun parse(raw: String, id: String = UUID.randomUUID().toString()): Account {
        val uri = URI(raw.trim())
        require(uri.scheme.equals("otpauth", ignoreCase = true)) { "not an otpauth URI" }
        require(uri.host.equals("totp", ignoreCase = true)) { "only TOTP is supported" }
        val params = (uri.rawQuery ?: "").split('&').filter { it.contains('=') }.associate {
            val (k, v) = it.split('=', limit = 2)
            k.lowercase() to URLDecoder.decode(v, "UTF-8")
        }
        val path = URLDecoder.decode((uri.rawPath ?: "").removePrefix("/"), "UTF-8")
        val pathIssuer = path.substringBefore(':', "").trim()
        val label = path.substringAfter(':').trim()
        val secret = params["secret"] ?: throw IllegalArgumentException("secret missing")
        return Account(
            id = id,
            issuer = params["issuer"]?.trim().takeUnless { it.isNullOrEmpty() } ?: pathIssuer,
            label = label,
            secret = secret.uppercase().filter { it != ' ' && it != '=' },
            algorithm = params["algorithm"]?.let { Algorithm.valueOf(it.uppercase()) } ?: Algorithm.SHA1,
            digits = params["digits"]?.toInt() ?: 6,
            period = params["period"]?.toInt() ?: 30,
        )
    }
}
