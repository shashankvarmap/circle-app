package com.circle.app.data.signup

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

/*
 * The rules of getting in (ACCESS_AND_LAUNCH 1), as pure functions so each one is unit-tested.
 * Nothing here is tied to one country.
 */

/** A country a calling code can belong to. */
data class Country(val region: String, val name: String, val code: Int)

object PhoneRules {
    private val util: PhoneNumberUtil = PhoneNumberUtil.getInstance()
    private val WELL_KNOWN = setOf("CA", "KZ", "GB", "NO", "FI", "IT", "MA")

    /** The line under the code field: one or two countries, then "and N more" for widely shared codes. */
    fun countryLine(names: List<String>, andMore: (Int) -> String): String =
        if (names.size <= 2) names.joinToString(" / ") else names.take(2).joinToString(" / ") + " " + andMore(names.size - 2)

    /** Every country, by name in the viewer's language. */
    fun allCountries(locale: Locale = Locale.getDefault()): List<Country> =
        util.supportedRegions.map { r -> Country(r, Locale("", r).getDisplayCountry(locale), util.getCountryCodeForRegion(r)) }
            .filter { it.name.isNotBlank() && it.code > 0 }
            .sortedBy { it.name }

    /** Search by name ("ger") or by code ("+44", "44"). */
    fun search(query: String, locale: Locale = Locale.getDefault()): List<Country> {
        val q = query.trim().lowercase(locale)
        if (q.isEmpty()) return allCountries(locale)
        val digits = q.removePrefix("+")
        return allCountries(locale).filter {
            it.name.lowercase(locale).contains(q) || (digits.all(Char::isDigit) && it.code.toString().startsWith(digits))
        }
    }

    /** The countries a typed code belongs to: "India", or "United States / Canada / …" for a shared one. Empty if unknown. */
    fun countriesFor(code: String, locale: Locale = Locale.getDefault()): List<String> {
        val c = code.trim().removePrefix("+").toIntOrNull() ?: return emptyList()
        val main = util.getRegionCodeForCountryCode(c)
        val regions = util.getRegionCodesForCountryCode(c).filter { it != "001" }
            // The main country first, then the best-known sharers (Canada for +1), then the rest.
            .sortedBy { r -> if (r == main) 0 else if (r in WELL_KNOWN) 1 else 2 }
        return regions.map { Locale("", it).getDisplayCountry(locale) }.filter { it.isNotBlank() }
    }

    /** The calling code for a region (prefill from the SIM, network or device region). */
    fun codeForRegion(region: String?): String =
        region?.uppercase()?.let { util.getCountryCodeForRegion(it) }?.takeIf { it > 0 }?.toString() ?: ""

    /** The number in E.164 ("+919876543210") if it's valid for that code; null otherwise. Spaces, dashes and a leading zero are fine. */
    fun toE164(code: String, national: String): String? {
        val c = code.trim().removePrefix("+").toIntOrNull() ?: return null
        val region = util.getRegionCodeForCountryCode(c).takeIf { it != "ZZ" } ?: return null
        return try {
            val n = util.parse(national, region)
            if (n.countryCode == c && util.isValidNumber(n)) util.format(n, PhoneNumberUtil.PhoneNumberFormat.E164) else null
        } catch (e: NumberParseException) {
            null
        }
    }

    /** A contact's number, normalised to E.164 using the device region for numbers written without a code. */
    fun normalise(raw: String, defaultRegion: String): String? = try {
        val n = util.parse(raw, defaultRegion.uppercase())
        if (util.isValidNumber(n)) util.format(n, PhoneNumberUtil.PhoneNumberFormat.E164) else null
    } catch (e: NumberParseException) {
        null
    }

    /** The scrambled form sent for contact matching: SHA-256 of the E.164 number. The number itself never leaves the phone. */
    fun hash(e164: String): String =
        MessageDigest.getInstance("SHA-256").digest(e164.toByteArray()).joinToString("") { "%02x".format(it) }
}

object AgeRule {
    const val MINIMUM = 18

    /** A real date (rejects 31 February), or null. */
    fun date(dd: String, mm: String, yyyy: String): LocalDate? = runCatching {
        val y = yyyy.toInt()
        if (yyyy.length != 4 || y < 1900) return null
        LocalDate.of(y, mm.toInt(), dd.toInt())
    }.getOrNull()

    /** Old enough by the real calendar: someone who turns 18 tomorrow is not, someone who turns 18 today is. */
    fun isAdult(birth: LocalDate, today: LocalDate): Boolean {
        var eighteenth = birth.plusYears(MINIMUM.toLong())
        // A 29 February birthday counts from 1 March in a non-leap year (the later, safer reading).
        if (birth.monthValue == 2 && birth.dayOfMonth == 29 && eighteenth.dayOfMonth == 28) eighteenth = eighteenth.plusDays(1)
        return !eighteenth.isAfter(today)
    }
}

/** Why a handle can't be used, or null when its shape is fine (availability is the repository's question). */
enum class HandleProblem { EMPTY, TOO_SHORT, TOO_LONG, CHARACTERS }

object SignupHandle {
    const val MIN = 3
    const val MAX = 20
    private val allowed = Regex("^[a-z0-9._]+$")

    /** What the field keeps as you type: lowercase letters, numbers, dots, underscores. */
    fun clean(typed: String): String = typed.lowercase().removePrefix("@").filter { it in 'a'..'z' || it in '0'..'9' || it == '.' || it == '_' }.take(MAX)

    fun problem(handle: String): HandleProblem? = when {
        handle.isEmpty() -> HandleProblem.EMPTY
        handle.length < MIN -> HandleProblem.TOO_SHORT
        handle.length > MAX -> HandleProblem.TOO_LONG
        !allowed.matches(handle) -> HandleProblem.CHARACTERS
        else -> null
    }
}

/**
 * The code screen's rules: a new code after 30 seconds; after 5 wrong tries, locked for 5 minutes.
 * Pure state: the screen asks it what to show.
 */
data class CodeGate(
    val sentAt: Instant,
    val wrongTries: Int = 0,
    val lockedUntil: Instant? = null,
) {
    fun canResend(now: Instant): Boolean = !now.isBefore(sentAt.plus(RESEND_AFTER))
    fun secondsUntilResend(now: Instant): Long = Duration.between(now, sentAt.plus(RESEND_AFTER)).seconds.coerceAtLeast(0)
    fun isLocked(now: Instant): Boolean = lockedUntil?.let { now.isBefore(it) } == true
    fun wrong(now: Instant): CodeGate {
        val tries = wrongTries + 1
        return if (tries >= MAX_TRIES) copy(wrongTries = 0, lockedUntil = now.plus(LOCK)) else copy(wrongTries = tries)
    }
    fun resent(now: Instant): CodeGate = copy(sentAt = now)

    companion object {
        val RESEND_AFTER: Duration = Duration.ofSeconds(30)
        val LOCK: Duration = Duration.ofMinutes(5)
        const val MAX_TRIES = 5
    }
}
