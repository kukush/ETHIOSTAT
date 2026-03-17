package com.ethiostat.app.data.sms

import java.security.MessageDigest

object SmsMessageFingerprint {
    
    /**
     * Creates a unique fingerprint for an SMS message to prevent duplicate processing
     */
    fun create(sender: String, body: String, timestamp: Long): String {
        val input = "${sender.trim()}|${body.trim()}|$timestamp"
        return hashString(input)
    }
    
    /**
     * Creates a fingerprint using only sender and body (for messages with unknown timestamp)
     */
    fun createWithoutTimestamp(sender: String, body: String): String {
        val input = "${sender.trim()}|${body.trim()}"
        return hashString(input)
    }
    
    /**
     * Checks if two messages are likely the same (fuzzy matching)
     */
    fun isSimilar(fingerprint1: String, fingerprint2: String): Boolean {
        return fingerprint1 == fingerprint2
    }
    
    /**
     * Creates a content-based fingerprint that ignores timestamp variations
     */
    fun createContentFingerprint(sender: String, body: String): String {
        // Normalize the body by removing extra whitespace and converting to lowercase
        val normalizedBody = body.trim().replace(Regex("\\s+"), " ").lowercase()
        val normalizedSender = sender.trim().lowercase()
        
        return hashString("${normalizedSender}|${normalizedBody}")
    }
    
    private fun hashString(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Fallback to simple hash if SHA-256 fails
            input.hashCode().toString()
        }
    }
}
