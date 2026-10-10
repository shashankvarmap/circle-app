package com.circle.app.data.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

enum class Plan { MONTHLY, YEARLY }

/**
 * Membership through Google Play Billing (ACCESS_AND_LAUNCH 4). In production the price strings
 * come only from Play (ProductDetails...formattedPrice), in the currency of the user's Play account;
 * the app never formats or converts prices itself. Prices per country are set in Play Console.
 * A Play Billing implementation replaces [FakeBilling] when the products exist.
 */
interface MembershipBilling {
    /** Price strings per plan exactly as the store returns them, e.g. "€1.00". Empty until loaded. */
    val prices: Flow<Map<Plan, String>>
    /** True while the prices are the preview table, not the store's: the screen labels them "Preview price". */
    val isPreviewPrice: Boolean
    val isMember: Flow<Boolean>
    suspend fun start(plan: Plan): Boolean
}

/**
 * Preview builds only (no Play Billing yet): a monthly and a yearly price per country calling code
 * chosen at sign-up, written out with the right currency symbol. Never used to format or convert a
 * real price; Play's formattedPrice replaces all of it.
 */
object PreviewPricing {
    data class Price(val currency: String, val monthly: String, val yearly: String)

    private val EUR = Price("EUR", "€1", "€12")

    /** Calling code (digits, no "+") to price. Euro countries share one row. */
    val table: Map<String, Price> = buildMap {
        put("91", Price("INR", "₹49", "₹588"))
        put("1", Price("USD", "$1", "$12"))
        put("44", Price("GBP", "£1", "£12"))
        put("61", Price("AUD", "A$1.50", "A$18"))
        put("81", Price("JPY", "¥150", "¥1,800"))
        listOf("49", "33", "34", "39", "31", "32", "43", "351", "353", "358", "30", "352", "356", "357", "372", "371", "370", "421", "386", "385")
            .forEach { put(it, EUR) }
    }

    /** The row for [callingCode]; euro when the code is unknown or missing. */
    fun forCallingCode(callingCode: String?): Price = table[callingCode?.removePrefix("+")?.trim()] ?: EUR

    fun prices(callingCode: String?): Map<Plan, String> =
        forCallingCode(callingCode).let { mapOf(Plan.MONTHLY to it.monthly, Plan.YEARLY to it.yearly) }
}

/** Stands in for Play Billing: preview prices for the sign-up country code, and a membership that starts at once. */
class FakeBilling(callingCode: Flow<String?> = flowOf(null)) : MembershipBilling {
    override val prices: Flow<Map<Plan, String>> = callingCode.map { PreviewPricing.prices(it) }
    override val isPreviewPrice = true
    override val isMember = MutableStateFlow(false)
    override suspend fun start(plan: Plan): Boolean {
        isMember.value = true
        return true
    }
}
