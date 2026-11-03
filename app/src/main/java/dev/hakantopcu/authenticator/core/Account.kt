package dev.hakantopcu.authenticator.core

import org.json.JSONArray
import org.json.JSONObject

data class Account(
    val id: String,
    val issuer: String,
    val label: String,
    val secret: String,
    val algorithm: Algorithm = Algorithm.SHA1,
    val digits: Int = 6,
    val period: Int = 30,
) {
    init {
        require(digits in 6..8) { "digits must be 6..8" }
        require(period in 10..300) { "period must be 10..300" }
        Base32.decode(secret)
    }

    val title: String get() = issuer.ifBlank { label }
    val subtitle: String get() = if (issuer.isBlank()) "" else label

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("issuer", issuer).put("label", label).put("secret", secret)
        .put("algorithm", algorithm.name).put("digits", digits).put("period", period)

    companion object {
        fun fromJson(o: JSONObject) = Account(
            id = o.getString("id"),
            issuer = o.optString("issuer"),
            label = o.optString("label"),
            secret = o.getString("secret"),
            algorithm = Algorithm.valueOf(o.optString("algorithm", "SHA1")),
            digits = o.optInt("digits", 6),
            period = o.optInt("period", 30),
        )

        fun listToJson(accounts: List<Account>): String =
            JSONObject().put("version", 1).put("accounts", JSONArray(accounts.map { it.toJson() })).toString()

        fun listFromJson(text: String): List<Account> {
            val arr = JSONObject(text).getJSONArray("accounts")
            return (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        }
    }
}
