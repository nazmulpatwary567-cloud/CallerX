package com.example.util

import com.example.domain.model.Contact

object PhoneUtils {

    private val t9Map = mapOf(
        'a' to '2', 'b' to '2', 'c' to '2',
        'd' to '3', 'e' to '3', 'f' to '3',
        'g' to '4', 'h' to '4', 'i' to '4',
        'j' to '5', 'k' to '5', 'l' to '5',
        'm' to '6', 'n' to '6', 'o' to '6',
        'p' to '7', 'q' to '7', 'r' to '7', 's' to '7',
        't' to '8', 'u' to '8', 'v' to '8',
        'w' to '9', 'x' to '9', 'y' to '9', 'z' to '9'
    )

    fun normalizeNumber(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.contains("@")) {
            // Preserve SIP URI user@domain
            return trimmed.lowercase()
        }
        val hasLeadingPlus = trimmed.startsWith("+")
        val digits = trimmed.filter { it.isDigit() || it == '*' || it == '#' }
        return if (hasLeadingPlus) "+$digits" else digits
    }

    fun numbersMatch(a: String, b: String): Boolean {
        val na = normalizeNumber(a)
        val nb = normalizeNumber(b)
        if (na.isEmpty() || nb.isEmpty()) return false
        if (na == nb) return true
        val da = na.filter { it.isDigit() }
        val db = nb.filter { it.isDigit() }
        if (da.length >= 7 && db.length >= 7) {
            return da.takeLast(7) == db.takeLast(7)
        }
        return false
    }

    fun isUnknownOrHiddenNumber(number: String?): Boolean {
        if (number.isNullOrBlank()) return true
        val lower = number.trim().lowercase()
        return lower == "unknown" || lower == "private" || lower == "anonymous" || lower == "-1" || lower == "-2"
    }

    fun nameToT9Digits(name: String): String {
        return buildString {
            for (ch in name.lowercase()) {
                when {
                    ch in '0'..'9' -> append(ch)
                    t9Map.containsKey(ch) -> append(t9Map[ch])
                }
            }
        }
    }

    fun matchesT9(contact: Contact, query: String): Boolean {
        if (query.isBlank()) return true
        val cleanQuery = query.trim().lowercase()
        if (contact.name.lowercase().contains(cleanQuery)) return true
        if (normalizeNumber(contact.number).contains(normalizeNumber(cleanQuery))) return true
        val digitsOnly = cleanQuery.filter { it.isDigit() }
        if (digitsOnly.isNotEmpty() && nameToT9Digits(contact.name).contains(digitsOnly)) {
            return true
        }
        return contact.tags.any { it.lowercase().contains(cleanQuery) }
    }

    fun toVCard(contact: Contact): String {
        val escapedName = contact.name.replace(";", "\\;")
        val cleanNumber = contact.number.trim()
        val escapedNote = contact.note.replace("\n", "\\n").replace(";", "\\;")
        return buildString {
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            appendLine("FN:$escapedName")
            appendLine("N:$escapedName;;;;")
            appendLine("TEL;TYPE=CELL:$cleanNumber")
            if (escapedNote.isNotBlank()) {
                appendLine("NOTE:$escapedNote")
            }
            if (contact.tags.isNotEmpty()) {
                appendLine("CATEGORIES:${contact.tags.joinToString(",")}")
            }
            appendLine("END:VCARD")
        }
    }
}
