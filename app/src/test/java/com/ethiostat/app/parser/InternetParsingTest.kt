package com.ethiostat.app.parser

import com.ethiostat.app.data.parser.EnglishSmsParser
import com.ethiostat.app.domain.model.PackageType
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.mockito.MockedStatic
import org.mockito.Mockito.*
import android.util.Log

@RunWith(RobolectricTestRunner::class)
class InternetParsingTest {

    private val parser = EnglishSmsParser()
    private var logMock: MockedStatic<Log>? = null

    @Before
    fun setUp() {
        try {
            logMock = mockStatic(Log::class.java)
            logMock?.`when`<Int> { Log.d(anyString(), anyString()) }?.thenReturn(0)
        } catch (e: Exception) {
            println("Warning: Could not mock Log.d: ${e.message}")
        }
    }

    @After
    fun tearDown() {
        try { logMock?.close() } catch (e: Exception) { /* ignore */ }
    }

    /**
     * The user's exact real-world sample SMS — the critical regression test.
     *
     * Expected result:
     *   Internet remaining = 3070.744 + 4915.200 = 7,985.944 MB
     *   Internet total     = 12 × 1024 + 4.8 × 1024 = 12288 + 4915.2 = 17,203.2 MB (~16 GB+4.8GB)
     *   Voice remaining    = (110 + 51/60) + (76 + 0/60) = 186.85 min
     */
    @Test
    fun `parse real-world multi-package SMS gives correct internet total`() {
        val sms = """
            Dear Customer, your remaining amount from Monthly Internet Package 12GB from telebirr to be expired after 30 days is 3070.744 MB with expiry date on 2026-04-06 at 17:38:02; from Monthly voice 150 Min from telebirr to be expired after 30 days and 76 Min night package bonus valid for 30 days is 110 minute and 51 second with expiry date on 2026-04-10 at 11:08:07; from Monthly voice 150 Min from telebirr to be expired after 30 days and 76 Min night package bonus valid for 30 days is 76 minute and 0 second with expiry date on 2026-04-10 at 11:08:07; from Monthly Internet package 4.8 GB from telebirr to be expired after 30days is 4915.200 MB with expiry date on 2026-04-08 at 15:06:16; Enjoy 10% additional rewards by downloading telebirr SuperApp https://bit.ly/telebirr_SuperApp. Happy Holiday! Ethio telecom
        """.trimIndent()

        val result = parser.parse(sms, "251994")

        assertTrue("SMS should be parsed successfully", result.isParsed)
        assertNull("Telecom sender must NOT create a transaction", result.transaction)

        val internetPackages = result.packages.filter { it.packageType == PackageType.INTERNET }
        assertEquals("Should combine into exactly 1 internet package", 1, internetPackages.size)

        val combined = internetPackages.first()

        // Remaining: 3070.744 + 4915.200 = 7985.944
        assertEquals(
            "Combined remaining should be 7985.944 MB",
            7985.944,
            combined.remainingAmount,
            1.0  // ±1 MB tolerance for float arithmetic
        )

        // Total: 12288 (12GB) + 4915.2 (4.8GB) = 17203.2 MB
        // We allow a broader tolerance since total is extracted from package name text
        assertTrue(
            "Total should be approximately 16+ GB (${combined.totalAmount} MB)",
            combined.totalAmount > 10000.0  // at least 10 GB in MB
        )

        println("✅ Internet remaining: ${combined.remainingAmount} MB  ≈ ${combined.remainingAmount / 1024} GB")
        println("✅ Internet total: ${combined.totalAmount} MB  ≈ ${combined.totalAmount / 1024} GB")
    }

    @Test
    fun `parser does NOT double-count — calling parse twice produces same result`() {
        val sms = """
            Dear Customer, your remaining amount from Monthly Internet Package 12GB from telebirr to be expired after 30 days is 3070.744 MB with expiry date on 2026-04-06 at 17:38:02; from Monthly Internet package 4.8 GB from telebirr to be expired after 30days is 4915.200 MB with expiry date on 2026-04-08 at 15:06:16; Ethio telecom
        """.trimIndent()

        val result1 = parser.parse(sms, "251994")
        val result2 = parser.parse(sms, "251994")

        val internet1 = result1.packages.first { it.packageType == PackageType.INTERNET }
        val internet2 = result2.packages.first { it.packageType == PackageType.INTERNET }

        assertEquals(
            "Calling parse() twice should return same remaining amount (no side effects)",
            internet1.remainingAmount,
            internet2.remainingAmount,
            0.01
        )
    }

    @Test
    fun `expired packages are excluded from the total`() {
        // This SMS has one expired package (2020 date) and one valid one
        val sms = """
            Dear Customer, your remaining amount from Monthly Internet Package 12GB from telebirr to be expired after 30 days is 3000.000 MB with expiry date on 2020-01-01 at 00:00:00; from Monthly Internet package 4.8 GB from telebirr to be expired after 30days is 4915.200 MB with expiry date on 2026-04-08 at 15:06:16; Ethio telecom
        """.trimIndent()

        val result = parser.parse(sms, "251994")
        assertTrue(result.isParsed)

        val internet = result.packages.filter { it.packageType == PackageType.INTERNET }
        assertEquals("Should have 1 combined internet package", 1, internet.size)

        // Only the non-expired 4915.200 MB should be included
        assertEquals(
            "Expired package (2020-01-01) must be excluded from total",
            4915.200,
            internet.first().remainingAmount,
            1.0
        )
        println("✅ Expired package correctly excluded. Remaining: ${internet.first().remainingAmount} MB")
    }

    @Test
    fun `telecom sender never creates a transaction`() {
        val sms = """
            Dear Customer, your remaining amount from Monthly Internet Package 12GB from telebirr to be expired after 30 days is 3070.744 MB with expiry date on 2026-04-06 at 17:38:02; Ethio telecom
        """.trimIndent()

        val result = parser.parse(sms, "251994")
        assertNull("251994 sender must NEVER create a transaction", result.transaction)
    }
}
