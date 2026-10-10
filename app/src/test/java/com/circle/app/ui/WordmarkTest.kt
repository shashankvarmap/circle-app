package com.circle.app.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.circle.app.ui.components.Wordmark
import com.circle.app.ui.theme.CircleTheme
import com.circle.app.ui.theme.DayPalette
import com.circle.app.ui.theme.NightPalette
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Owner fix 3: the full stop in "Circle." is the accent colour in night and day. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WordmarkTest {
    @get:Rule val compose = createComposeRule()

    private fun stopColor(night: Boolean): androidx.compose.ui.graphics.Color {
        compose.setContent { CircleTheme(night = night) { Wordmark(fontSize = 24.sp) } }
        val text = compose.onNodeWithText("Circle.").fetchSemanticsNode().config[SemanticsProperties.Text].first()
        val span = text.spanStyles.single { it.start == "Circle".length && it.end == "Circle.".length }
        return span.item.color
    }

    @Test fun day() = assertEquals(DayPalette.accent, stopColor(night = false))

    @Test fun night() = assertEquals(NightPalette.accent, stopColor(night = true))
}
