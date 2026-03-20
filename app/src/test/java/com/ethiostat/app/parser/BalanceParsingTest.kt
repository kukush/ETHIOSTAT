package com.ethiostat.app.parser

import com.ethiostat.app.data.parser.AmharicSmsParser
import com.ethiostat.app.data.parser.EnglishSmsParser
import com.ethiostat.app.data.parser.OromoSmsParser
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
class BalanceParsingTest {
    
    private val enParser = EnglishSmsParser()
    private val amParser = AmharicSmsParser()
    private val orParser = OromoSmsParser()
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

    @Test
    fun `test recharge message matches correct balance`() {
        // Sample 1 from user
        val sms = "Dear Customer, Your prepaid account has been recharged successfully. Your Recharged balance is 50.00 Birr. Your balance is 50.01 Birr. Download and use telebirr SuperApp from https://onelink.to/uecbbr and get bonus during airtime & package purchase. Ethio telecom"
        
        val result = enParser.parse(sms, "251994")
        assertTrue(result.isParsed)

        val accounts = result.packages.filter { it.packageType == PackageType.MAIN_BALANCE }
        assertEquals("Should have found account balance", 1, accounts.size)
        // User reports it reads 50.00 (recharged balance) instead of 50.01
        assertEquals("Balance should be 50.01, not 50.00", 50.01, accounts.first().remainingAmount, 0.001)
    }

    @Test
    fun `test bonus award message handles bonus separately`() {
        // Sample 2 from user
        val sms = "Dear Customer, You have been awarded an ETB 7.50 bonus for recharging your prepaid account using telebirr. Thank you! Ethio telecom"
        
        val result = enParser.parse(sms, "251994")
        assertTrue(result.isParsed)

        val accounts = result.packages.filter { it.packageType == PackageType.MAIN_BALANCE }
        assertTrue("Bonus award should not be parsed as main balance", accounts.isEmpty())

        val bonuses = result.packages.filter { it.packageType == PackageType.BONUS_FUND }
        assertEquals("Should have found recharge bonus", 1, bonuses.size)
        assertEquals(7.50, bonuses.first().remainingAmount, 0.001)
    }

    @Test
    fun `test Amharic account balance parsing`() {
        val sms = "ቀሪ ሂሳብዎ 50.01 ብር::"
        val result = amParser.parse(sms, "251994")
        assertTrue(result.isParsed)
        val accounts = result.packages.filter { it.packageType == PackageType.MAIN_BALANCE }
        assertEquals(50.01, accounts.first().remainingAmount, 0.001)
    }

    @Test
    fun `test Amharic bonus award parsing`() {
        val sms = "የ 7.50 ብር ቦነስ ተሸልመዋል"
        val result = amParser.parse(sms, "251994")
        assertTrue(result.isParsed)
        val bonuses = result.packages.filter { it.packageType == PackageType.BONUS_FUND }
        assertEquals(7.50, bonuses.first().remainingAmount, 0.001)
    }

    @Test
    fun `test Oromo account balance parsing`() {
        val sms = "Hafteen herregaa amma qabdan Qarshii 13.95 dha."
        val result = orParser.parse(sms, "251994")
        assertTrue(result.isParsed)
        val accounts = result.packages.filter { it.packageType == PackageType.MAIN_BALANCE }
        assertEquals(13.95, accounts.first().remainingAmount, 0.001)
    }

    @Test
    fun `test Oromo bonus award parsing`() {
        val sms = "Boonasii Qarshii 7.50 badhaafamtaniittu"
        val result = orParser.parse(sms, "251994")
        assertTrue(result.isParsed)
        val bonuses = result.packages.filter { it.packageType == PackageType.BONUS_FUND }
        assertEquals(7.50, bonuses.first().remainingAmount, 0.001)
    }

    @Test
    fun `test recharge message returns last balance not recharged balance`() {
        // Bug regression: "Your Recharged balance is 50.00" should NOT be returned;
        // "Your balance is 50.01" (the final balance) should be used instead.
        val sms = "Dear Customer, Your prepaid account has been recharged successfully. " +
                "Your Recharged balance is 50.00 Birr. Your balance is 50.01 Birr. " +
                "Download and use telebirr SuperApp from https://onelink.to/uecbbr " +
                "and get bonus during airtime & package purchase. Ethio telecom"

        val result = enParser.parse(sms, "251994")
        assertTrue("SMS should be parsed", result.isParsed)

        val accounts = result.packages.filter { it.packageType == PackageType.MAIN_BALANCE }
        assertEquals("Should have exactly one MAIN_BALANCE", 1, accounts.size)
        assertEquals(
            "Balance must be 50.01 (final 'Your balance is'), not 50.00 (recharged balance)",
            50.01, accounts.first().remainingAmount, 0.001
        )
    }

    @Test
    fun `test 804 response with SMS package segment is parsed correctly`() {
        // Full *804# response SMS from sender 251994 containing internet, voice, and SMS segments
        val sms = "Dear Customer, your remaining amount  from Monthly Internet Package 12GB " +
                "from telebirr to be expired after 30 days is 2735.426 MB with expiry date on " +
                "2026-04-06 at 17:38:02;  from Monthly voice 150 Min from telebirr to be expired " +
                "after 30 days and 76 Min night package bonus valid for 30 days is 99 minute and " +
                "43 second with expiry date on 2026-04-10 at 11:08:07;   from Monthly voice 150 Min " +
                "from telebirr to be expired after 30 days and 76 Min night package bonus valid for " +
                "30 days is 76 minute and 0 second with expiry date on 2026-04-10 at 11:08:07;     " +
                "from Create Your Own Package Monthly is 157 SMS with expiry date on " +
                "2026-04-19 at 00:22:19;  from Monthly Internet package 4.8 GB from telebirr to be " +
                "expired after 30days is 4915.200 MB with expiry date on 2026-04-08 at 15:06:16;   " +
                "Enjoy 10% additional rewards by downloading telebirr SuperApp " +
                "https://bit.ly/telebirr_SuperApp. Happy Holiday! Ethio telecom."

        val result = enParser.parse(sms, "251994")
        assertTrue("SMS should be parsed", result.isParsed)

        // Verify SMS package was found
        val smsPackages = result.packages.filter { it.packageType == PackageType.SMS }
        assertEquals("Should have exactly one SMS package", 1, smsPackages.size)

        val smsPkg = smsPackages.first()
        assertEquals("SMS count should be 157", 157.0, smsPkg.remainingAmount, 0.001)
        assertEquals("SMS unit should be 'SMS'", "SMS", smsPkg.unit)
        assertTrue("SMS expiry should contain 2026-04-19", smsPkg.expiryDate.contains("2026-04-19"))

        // Verify internet packages are still parsed
        val internetPackages = result.packages.filter { it.packageType == PackageType.INTERNET }
        assertTrue("Should have internet packages", internetPackages.isNotEmpty())

        // Verify voice packages are still parsed
        val voicePackages = result.packages.filter { it.packageType == PackageType.VOICE }
        assertTrue("Should have voice packages", voicePackages.isNotEmpty())
    }
}
