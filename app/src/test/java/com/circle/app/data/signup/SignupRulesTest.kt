package com.circle.app.data.signup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

class SignupRulesTest {
    private val en = Locale.ENGLISH

    @Test fun `a typed code names its country, shared codes name them all, unknown codes none`() {
        assertEquals(listOf("India"), PhoneRules.countriesFor("91", en))
        assertEquals(listOf("Germany"), PhoneRules.countriesFor("+49", en))
        val one = PhoneRules.countriesFor("1", en)
        assertEquals(listOf("United States", "Canada"), one.take(2)) // main country first, then Canada
        assertEquals("United States / Canada and 3 more", PhoneRules.countryLine(one.take(5)) { "and $it more" })
        assertTrue(PhoneRules.countriesFor("999", en).isEmpty())
        assertTrue(PhoneRules.countriesFor("", en).isEmpty())
    }

    @Test fun `numbers are validated per country and stored as E164`() {
        assertEquals("+919876543210", PhoneRules.toE164("91", "98765 43210"))
        assertEquals("+4915112345678", PhoneRules.toE164("49", "0151 1234-5678")) // leading zero and dashes are fine
        assertEquals("+12025550123", PhoneRules.toE164("1", "(202) 555-0123"))
        assertNull(PhoneRules.toE164("91", "12345")) // too short for India
        assertNull(PhoneRules.toE164("999", "12345678")) // no such code
    }

    @Test fun `the country list is searchable by name or code`() {
        assertTrue(PhoneRules.search("ger", en).any { it.name == "Germany" })
        assertTrue(PhoneRules.search("+44", en).any { it.name == "United Kingdom" })
        assertTrue(PhoneRules.allCountries(en).size > 200) // every country, not a short list
        assertEquals("91", PhoneRules.codeForRegion("in"))
    }

    @Test fun `contact numbers are normalised and only their hash leaves the phone`() {
        val e164 = PhoneRules.normalise("0151 12345678", "DE")
        assertEquals("+4915112345678", e164)
        val h = PhoneRules.hash(e164!!)
        assertEquals(64, h.length)
        assertFalse(h.contains("15112345678"))
        assertEquals(h, PhoneRules.hash("+4915112345678"))
    }

    @Test fun `18 by the real calendar - 18 today is fine, 18 tomorrow is not`() {
        val today = LocalDate.of(2026, 10, 9)
        assertTrue(AgeRule.isAdult(LocalDate.of(2008, 10, 9), today))
        assertFalse(AgeRule.isAdult(LocalDate.of(2008, 10, 10), today))
        assertTrue(AgeRule.isAdult(LocalDate.of(1980, 1, 1), today))
        // A 29 February birthday turns 18 on 1 March in a non-leap year.
        assertFalse(AgeRule.isAdult(LocalDate.of(2008, 2, 29), LocalDate.of(2026, 2, 28)))
        assertTrue(AgeRule.isAdult(LocalDate.of(2008, 2, 29), LocalDate.of(2026, 3, 1)))
    }

    @Test fun `dates must be real`() {
        assertNull(AgeRule.date("31", "02", "2000"))
        assertNull(AgeRule.date("10", "10", "98"))
        assertEquals(LocalDate.of(2000, 2, 29), AgeRule.date("29", "2", "2000"))
    }

    @Test fun `handles - lowercase letters, numbers, dots, underscores, 3 to 20`() {
        assertEquals("sam.lee_1", SignupHandle.clean("@Sam.Lee_1!"))
        assertEquals(HandleProblem.TOO_SHORT, SignupHandle.problem("ab"))
        assertNull(SignupHandle.problem("abc"))
        assertEquals(HandleProblem.EMPTY, SignupHandle.problem(""))
        assertEquals(20, SignupHandle.clean("a".repeat(30)).length)
    }

    @Test fun `codes - resend after 30 seconds, five wrong tries lock for five minutes`() {
        val t0 = Instant.parse("2026-10-09T10:00:00Z")
        var gate = CodeGate(sentAt = t0)
        assertFalse(gate.canResend(t0.plusSeconds(29)))
        assertEquals(1, gate.secondsUntilResend(t0.plusSeconds(29)))
        assertTrue(gate.canResend(t0.plusSeconds(30)))
        repeat(4) { gate = gate.wrong(t0) }
        assertFalse(gate.isLocked(t0))
        gate = gate.wrong(t0)
        assertTrue(gate.isLocked(t0.plusSeconds(299)))
        assertFalse(gate.isLocked(t0.plusSeconds(300)))
    }
}
