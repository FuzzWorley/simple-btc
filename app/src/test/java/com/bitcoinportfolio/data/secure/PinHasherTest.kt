package com.bitcoinportfolio.data.secure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PinHasherTest {

    private val pinHasher = PinHasher()

    @Test
    fun `hash is consistent for same pin and salt`() {
        val salt = pinHasher.generateSalt()
        assertEquals(pinHasher.hash("1234", salt), pinHasher.hash("1234", salt))
    }

    @Test
    fun `verify returns true for correct pin`() {
        val salt = pinHasher.generateSalt()
        val hash = pinHasher.hash("1234", salt)
        assertTrue(pinHasher.verify("1234", salt, hash))
    }

    @Test
    fun `verify returns false for wrong pin`() {
        val salt = pinHasher.generateSalt()
        val hash = pinHasher.hash("1234", salt)
        assertFalse(pinHasher.verify("9999", salt, hash))
    }

    @Test
    fun `verify returns false for empty pin`() {
        val salt = pinHasher.generateSalt()
        val hash = pinHasher.hash("1234", salt)
        assertFalse(pinHasher.verify("", salt, hash))
    }

    @Test
    fun `generateSalt produces unique values each call`() {
        val salts = (1..20).map { pinHasher.generateSalt() }.toSet()
        assertEquals(20, salts.size)
    }

    @Test
    fun `hash produces 64-character lowercase hex string`() {
        val hash = pinHasher.hash("1234", pinHasher.generateSalt())
        assertEquals(64, hash.length)
        assertTrue(hash.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `same pin with different salts produces different hashes`() {
        val hash1 = pinHasher.hash("1234", pinHasher.generateSalt())
        val hash2 = pinHasher.hash("1234", pinHasher.generateSalt())
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `different pins with same salt produce different hashes`() {
        val salt = pinHasher.generateSalt()
        assertNotEquals(pinHasher.hash("1234", salt), pinHasher.hash("5678", salt))
    }
}
