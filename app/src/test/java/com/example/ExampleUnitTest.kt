package com.example

import com.example.domain.model.Contact
import com.example.util.CryptoUtils
import com.example.util.DurationUtils
import com.example.util.PhoneUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun t9_matching_works_for_names_and_numbers() {
        val alice = Contact(id = 1, name = "Alice Vance", number = "+1 (555) 234-5678")
        assertTrue(PhoneUtils.matchesT9(alice, "254")) // ALI
        assertTrue(PhoneUtils.matchesT9(alice, "5678"))
        assertFalse(PhoneUtils.matchesT9(alice, "9999"))
    }

    @Test
    fun aes_256_gcm_backup_roundtrip_succeeds() {
        val data = """{"sipAccounts":[{"username":"1001"}]}""".toByteArray(Charsets.UTF_8)
        val encrypted = CryptoUtils.encryptAes256Gcm(data, "StrongPassphrase!1")
        val decrypted = CryptoUtils.decryptAes256Gcm(encrypted, "StrongPassphrase!1")
        assertArrayEquals(data, decrypted)
    }

    @Test
    fun salted_pin_hash_verifies_correctly() {
        val salt = CryptoUtils.generateSaltBase64()
        val hash = CryptoUtils.hashPinSha256("482910", salt)
        assertTrue(CryptoUtils.verifyPinSha256("482910", salt, hash))
        assertFalse(CryptoUtils.verifyPinSha256("000000", salt, hash))
    }

    @Test
    fun duration_formatting_outputs_expected_strings() {
        assertEquals("01:05", DurationUtils.formatMmSs(65))
        assertEquals("12m 34s", DurationUtils.formatHumanReadable(754))
    }
}
