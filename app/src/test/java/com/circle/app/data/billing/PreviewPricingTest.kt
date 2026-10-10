package com.circle.app.data.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Owner fix 5: preview prices follow the sign-up country code; the billing interface is unchanged. */
class PreviewPricingTest {

    @Test fun `each listed country shows its own currency`() {
        assertEquals("₹49" to "₹588", PreviewPricing.forCallingCode("91").let { it.monthly to it.yearly })
        assertEquals("EUR", PreviewPricing.forCallingCode("49").currency)
        assertEquals("USD", PreviewPricing.forCallingCode("+1").currency)
        assertEquals("GBP", PreviewPricing.forCallingCode("44").currency)
        assertEquals("AUD", PreviewPricing.forCallingCode("61").currency)
        assertEquals("¥150", PreviewPricing.forCallingCode("81").monthly)
    }

    @Test fun `unknown or missing code falls back to euro`() {
        assertEquals("EUR", PreviewPricing.forCallingCode("999").currency)
        assertEquals("EUR", PreviewPricing.forCallingCode(null).currency)
    }

    @Test fun `fake billing follows the saved code and says the price is a preview`() = runTest {
        val code = MutableStateFlow<String?>("44")
        val billing = FakeBilling(code)
        assertTrue(billing.isPreviewPrice)
        assertEquals(mapOf(Plan.MONTHLY to "£1", Plan.YEARLY to "£12"), billing.prices.first())
        code.value = "91"
        assertEquals("₹49", billing.prices.first()[Plan.MONTHLY])
    }
}
