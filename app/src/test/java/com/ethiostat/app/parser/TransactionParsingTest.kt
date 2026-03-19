package com.ethiostat.app.parser

import com.ethiostat.app.data.parser.AmharicSmsParser
import com.ethiostat.app.data.parser.EnglishSmsParser
import com.ethiostat.app.data.parser.OromoSmsParser
import com.ethiostat.app.domain.model.TransactionType
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
class TransactionParsingTest {

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

    // -----------------------------------------------------------------------
    // ENGLISH Transaction Tests
    // -----------------------------------------------------------------------

    @Test
    fun `parse CBE Credit English`() {
        val sms = "Dear Customer, your account 1000xxxxxx has been credited with ETB 5,000.00 on 15/03/26 10:30. Balance: ETB 15,500.00. Thank you for banking with CBE."
        val result = enParser.parse(sms, "CBE")
        assertTrue(result.isParsed)
        assertEquals(TransactionType.INCOME, result.transaction!!.type)
        assertEquals(5000.00, result.transaction!!.amount, 0.01)
    }

    @Test
    fun `parse CBE Debit English`() {
        val sms = "Dear Customer, your account 1000xxxxxx has been debited with ETB 1,000.00 on 15/03/26 14:00. Balance: ETB 14,500.00. Thank you for banking with CBE."
        val result = enParser.parse(sms, "CBE")
        assertTrue(result.isParsed)
        assertEquals(TransactionType.EXPENSE, result.transaction!!.type)
        assertEquals(1000.00, result.transaction!!.amount, 0.01)
    }

    @Test
    fun `parse BOA Debit English`() {
        val sms = "Bank of Abyssinia: Account XXXXXX2345 has been Debited ETB 1,000.00 on 19-Mar-2026. Available Bal: ETB 14,250.00. Thank you."
        val result = enParser.parse(sms, "BOA")
        assertTrue(result.isParsed)
        assertEquals(TransactionType.EXPENSE, result.transaction!!.type)
        assertEquals(1000.00, result.transaction!!.amount, 0.01)
    }

    @Test
    fun `parse Awash Credit English`() {
        val sms = "Dear Customer, Account 0123456789100 has been credited with ETB 5,000.00 on 19-Mar-26. Bal: ETB 15,000.00. Thank you for using Awash Bank."
        val result = enParser.parse(sms, "AWASHBANK")
        assertTrue(result.isParsed)
        assertEquals(TransactionType.INCOME, result.transaction!!.type)
        assertEquals(5000.00, result.transaction!!.amount, 0.01)
    }

    // -----------------------------------------------------------------------
    // AMHARIC Transaction Tests
    // -----------------------------------------------------------------------

    @Test
    fun `parse CBE Credit Amharic`() {
        val sms = "የተከበሩ ደንበኛ፣ አካውንትዎ 1000xxxxxx በ15/03/26 10:30 በብር 5,000.00 ተመዝግቧል። ቀሪ ሂሳብ፡ ETB 15,500.00። የኢትዮጵያ ንግድ ባንክን ስለመረጡ እናመሰግናለን።"
        val result = amParser.parse(sms, "CBE")
        assertTrue(result.isParsed)
        assertEquals(TransactionType.INCOME, result.transaction!!.type)
        assertEquals(5000.00, result.transaction!!.amount, 0.01)
    }

    @Test
    fun `parse BOA Debit Amharic`() {
        val sms = "የአቢሲንያ ባንክ፡ አካውንት ቁጥር XXXXXX2345 በ 19-Mar-2026 ብር 1,000.00 ወጪ ተደርጓል። የቀሪ ሂሳብ፡ ብር 14,250.00። እናመሰግናለን።"
        val result = amParser.parse(sms, "BOA")
        assertTrue(result.isParsed)
        assertEquals(TransactionType.EXPENSE, result.transaction!!.type)
        assertEquals(1000.00, result.transaction!!.amount, 0.01)
    }

    @Test
    fun `parse Awash Credit Amharic`() {
        val sms = "ውድ ደንበኛችን አካውንትዎ ...9100 በ 5,000.00 ብር ገቢ ሆኗል። ቀሪ ሂሳብዎ 15,000.00 ብር ነው። አዋሽ ባንክ!"
        val result = amParser.parse(sms, "AWASHBANK")
        assertTrue(result.isParsed)
        assertEquals(TransactionType.INCOME, result.transaction!!.type)
        assertEquals(5000.00, result.transaction!!.amount, 0.01)
    }

    @Test
    fun `parse Telebirr Debit Amharic`() {
        val sms = "ለ 0912XXXXXX ብር 200.00 በተሳካ ሁኔታ ልከዋል። የግብይት መታወቂያ: 9F431M2YC5። ቀሪ ሂሳብ: 1,300.00 ብር።"
        val result = amParser.parse(sms, "127")
        assertTrue(result.isParsed)
        assertEquals(TransactionType.EXPENSE, result.transaction!!.type)
        assertEquals(200.00, result.transaction!!.amount, 0.01)
    }

    // -----------------------------------------------------------------------
    // OROmIFA Transaction Tests
    // -----------------------------------------------------------------------

    @Test
    fun `parse CBE Credit Oromifa`() {
        val sms = "Maamilaa kabajamoo, herregni keessan 1000xxxxxx guyyaa 15/03/26 10:30 irratti qarshii 5,000.00 akka seene galmeeffameera."
        val result = orParser.parse(sms, "CBE")
        assertTrue(result.isParsed)
        assertEquals(TransactionType.INCOME, result.transaction!!.type)
        assertEquals(5000.00, result.transaction!!.amount, 0.01)
    }

    @Test
    fun `parse Awash Debit Oromifa`() {
        val sms = "Maamila Kabajamoo, Herregni keessan ...9100 qarshii 2,000.00 baasii ta'eera."
        val result = orParser.parse(sms, "AWASHBANK")
        assertTrue(result.isParsed)
        assertEquals(TransactionType.EXPENSE, result.transaction!!.type)
        assertEquals(2000.00, result.transaction!!.amount, 0.01)
    }

    @Test
    fun `parse Telebirr Credit Oromifa`() {
        val sms = "0911XXXXXX irraa Qarshii 500.00 qaqqabeera. ID gurgurtaa: 8E320N1XB4."
        val result = orParser.parse(sms, "127")
        assertTrue(result.isParsed)
        assertEquals(TransactionType.INCOME, result.transaction!!.type)
        assertEquals(500.00, result.transaction!!.amount, 0.01)
    }
}
