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
class BalanceParsingTest {
    
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

    @Test
    fun `test recharge message matches correct balance`() {
        // Sample 1 from user
        val sms = "Dear Customer, Your prepaid account has been recharged successfully. Your Recharged balance is 50.00 Birr. Your balance is 50.01 Birr. Download and use telebirr SuperApp from https://onelink.to/uecbbr and get bonus during airtime & package purchase. Ethio telecom"
        
        val result = parser.parse(sms, "251994")
        assertTrue(result.isParsed)

        val accounts = result.packages.filter { it.packageName == "Account Balance" }
        assertEquals("Should have found account balance", 1, accounts.size)
        // User reports it reads 50.00 (recharged balance) instead of 50.01
        assertEquals("Balance should be 50.01, not 50.00", 50.01, accounts.first().remainingAmount, 0.001)
    }

    @Test
    fun `test bonus award message handles bonus separately`() {
        // Sample 2 from user
        val sms = "Dear Customer, You have been awarded an ETB 7.50 bonus for recharging your prepaid account using telebirr. Thank you! Ethio telecom"
        
        val result = parser.parse(sms, "251994")
        assertTrue(result.isParsed)

        val accounts = result.packages.filter { it.packageName == "Account Balance" }
        assertTrue("Bonus award should not be parsed as main 'Account Balance'", accounts.isEmpty())

        val bonuses = result.packages.filter { it.packageName == "Recharge Bonus" }
        assertEquals("Should have found recharge bonus", 1, bonuses.size)
        assertEquals(7.50, bonuses.first().remainingAmount, 0.001)
    }
}
