package com.bitcoinportfolio.data.secure

import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject

class PinHasher @Inject constructor() {

    fun generateSalt(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.toHexString()
    }

    fun hash(pin: String, salt: String): String {
        val input = (pin + salt).toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(input)
        return digest.toHexString()
    }

    fun verify(input: String, salt: String, storedHash: String): Boolean =
        hash(input, salt) == storedHash

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
}
