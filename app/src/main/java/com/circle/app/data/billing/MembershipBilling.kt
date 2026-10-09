package com.circle.app.data.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

enum class Plan { MONTHLY, YEARLY }

/**
 * Membership through Google Play Billing (ACCESS_AND_LAUNCH 4). The app never formats prices
 * itself: it shows the strings the store returns, in the user's currency. A Play Billing
 * implementation replaces [FakeBilling] when the products exist in Play Console.
 */
interface MembershipBilling {
    /** Price strings per plan as the store returns them, e.g. "€12.00". Empty until loaded. */
    val prices: Flow<Map<Plan, String>>
    val isMember: Flow<Boolean>
    suspend fun start(plan: Plan): Boolean
}

/** Placeholder prices (€1 a month, €12 a year) standing in for Play's localized strings. */
class FakeBilling : MembershipBilling {
    override val prices = MutableStateFlow(mapOf(Plan.MONTHLY to "€1", Plan.YEARLY to "€12"))
    override val isMember = MutableStateFlow(false)
    override suspend fun start(plan: Plan): Boolean {
        isMember.value = true
        return true
    }
}
