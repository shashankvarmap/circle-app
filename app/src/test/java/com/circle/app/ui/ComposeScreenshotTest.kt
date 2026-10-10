package com.circle.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.circle.app.data.CircleStore
import com.circle.app.data.access.Audience
import com.circle.app.data.media.MediaKind
import com.circle.app.data.media.MediaRef
import com.circle.app.data.repo.FakeCircleRepository
import com.circle.app.data.repo.NoteView
import com.circle.app.ui.screens.ComposeLayout
import com.circle.app.ui.theme.CircleTheme
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The composer with no media, 1 photo, 4 photos, a video and a quote; keyboard closed and open
 * (a 300dp stand-in below the layout, which is what imePadding does); night and day; font 1.0 and 2.0.
 * Each case also checks the order: text, then media/quote, then the bottom bar, then the keyboard.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ComposeScreenshotTest(private val keyboard: Boolean, private val night: Boolean, private val font: Float) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "keyboard={0} night={1} font={2}")
        fun cases(): List<Array<Any>> =
            listOf(false, true).flatMap { k -> listOf(true, false).flatMap { n -> listOf(1.0f, 2.0f).map { f -> arrayOf<Any>(k, n, f) } } }

        private const val KEYBOARD = 300
        private const val TEXT = "Walked the long way home along the canal."
    }

    @get:Rule val compose = createComposeRule()

    private fun photo(i: Int) = MediaRef("p$i", "", MediaKind.PHOTO, null)
    private val video = MediaRef("v1", "", MediaKind.VIDEO, 42_000)

    private fun render(name: String, media: List<MediaRef> = emptyList(), quote: Boolean = false) {
        RuntimeEnvironment.setQualifiers("w360dp-h780dp")
        RuntimeEnvironment.setFontScale(font)
        CircleStore.reset()
        val quoted = if (quote) runBlocking { FakeCircleRepository().note("jonah-coffee").first() as NoteView.Full } else null
        compose.setContent {
            CircleTheme(night = night) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        ComposeLayout(
                            text = TEXT, onText = {}, placeholder = "What's on your mind?", focus = FocusRequester(),
                            media = media, onRemoveMedia = {}, quoted = quoted, onRemoveQuote = {},
                            postLabel = "Post", canPost = true, onCancel = {}, onPost = {},
                            canAttach = true, onAttach = {}, remaining = 400, editing = false, groupName = null,
                            audience = Audience.FRIENDS, onToggleAudience = {}, innerCount = 8, onEditInner = {}
                        )
                    }
                    if (keyboard) Spacer(Modifier.fillMaxWidth().height(KEYBOARD.dp).background(Color(0xFF555555)))
                }
            }
        }
        compose.waitForIdle()
        checkOrder(media.isNotEmpty(), quote)
        val suffix = "${if (keyboard) "kb" else "nokb"}_${if (night) "night" else "day"}_font${(font * 10).toInt()}"
        compose.onRoot().captureRoboImage("src/test/screenshots/compose_${name}_$suffix.png")
    }

    private fun checkOrder(hasMedia: Boolean, quote: Boolean) {
        val root = compose.onRoot().getBoundsInRoot()
        val field = compose.onNodeWithText(TEXT).getBoundsInRoot()
        val attach = compose.onNodeWithContentDescription("Add photos or a video").getBoundsInRoot()
        val pill = compose.onNodeWithText("Friends").getBoundsInRoot()
        val note = compose.onNodeWithText("All your friends can see this.").getBoundsInRoot()
        val link = compose.onNodeWithText("Edit who's in your inner circle").getBoundsInRoot()
        val keyboardTop = root.bottom - (if (keyboard) KEYBOARD.dp else 0.dp)
        // The bottom bar sits on the keyboard (or the screen bottom), never in the middle.
        assertTrue("link above keyboard", link.bottom <= keyboardTop)
        assertTrue("bar hugs the keyboard", keyboardTop - link.bottom <= 24.dp)
        assertTrue("note under attach row", note.top >= attach.bottom - 1.dp && note.top >= pill.bottom - 1.dp)
        assertTrue("attach below the text area", attach.top >= field.top)
        if (hasMedia) {
            val thumb = compose.onAllNodesWithContentDescription("Remove this photo or video", useUnmergedTree = true)[0].getBoundsInRoot()
            assertTrue("media directly under text", thumb.top >= field.bottom - 4.dp)
        }
        if (quote) {
            val remove = compose.onNodeWithContentDescription("Remove the quoted note").getBoundsInRoot()
            assertTrue("quote directly under text", remove.top >= field.bottom - 4.dp)
        }
    }

    @Test fun noMedia() = render("none")
    @Test fun onePhoto() = render("photo1", listOf(photo(1)))
    @Test fun fourPhotos() = render("photo4", (1..4).map(::photo))
    @Test fun video() = render("video", listOf(video))
    @Test fun quote() = render("quote", quote = true)
}
