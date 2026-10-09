package com.circle.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.hasScrollAction
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.circle.app.AppGraph
import com.circle.app.CircleApp
import com.circle.app.data.CircleStore
import com.circle.app.data.repo.FakeCircleRepository
import com.circle.app.data.session.InMemorySessionStore
import com.circle.app.data.session.LaunchState
import com.circle.app.navigation.Routes
import com.circle.app.ui.launch.SessionViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The owner's privacy scenarios (MASTER_PROMPT 2.4), on the real screens. Each asserts that no
 * restricted text appears: Elena Voss is not a friend; her note's full text must never show.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrivacyScenariosUiTest {

    @get:Rule val compose = createComposeRule()

    private lateinit var repo: FakeCircleRepository
    private lateinit var nav: NavHostController

    /** The part of Elena's note beyond the 90-character snippet. */
    private val elenaHiddenText = "before the school groups arrive"
    private val elenaNoteStart = "Spent the whole afternoon at the Ludwig Forum"

    @Before fun setUp() {
        CircleStore.reset()
        repo = FakeCircleRepository()
        AppGraph.install(repository = repo)
        compose.setContent {
            nav = rememberNavController()
            CircleApp(SessionViewModel(InMemorySessionStore(LaunchState(hasSession = true, onboardingSeen = true))), nav)
        }
        compose.waitForIdle()
    }

    private fun go(route: String) {
        compose.runOnUiThread { nav.navigate(route) }
        compose.waitForIdle()
    }

    /** Scrolls the chat to Elena's shared card and taps it. */
    private fun tapElenasSharedCard() {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(elenaNoteStart, substring = true))
        compose.onNodeWithText(elenaNoteStart, substring = true).performClick()
        compose.waitForIdle()
    }

    private fun assertLimitedProfileOf(name: String) {
        compose.onNodeWithText(name).assertIsDisplayed()
        compose.onNodeWithText("Message").assertDoesNotExist()
        compose.onNodeWithText("NOTES").assertDoesNotExist()
        compose.onNodeWithText("You're not friends yet. You'll see their notes and friends once they accept. Until then you see only their photo and pinned thought.").assertExists()
        compose.onAllNodesWithText(elenaNoteStart, substring = true).assertCountEquals(0)
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.assertCountEquals(n: Int) =
        fetchSemanticsNodes().size.let { check(it == n) { "expected $n nodes, found $it" } }

    @Test fun `1 - group chat, tap a non-friend's avatar - Limited profile with Add friend, no notes, no Message`() {
        go(Routes.chat("g-photo-walk"))
        compose.onNodeWithText("Elena").performClick() // her name above her message
        compose.waitForIdle()
        assertLimitedProfileOf("Elena Voss")
        compose.onNodeWithText("Add friend").assertIsDisplayed()
    }

    @Test fun `2 - shared note from a non-friend opens the gate, not the thread, and no comment is possible`() {
        go(Routes.chat("pk"))
        tapElenasSharedCard()
        compose.onNodeWithText("You're not friends with Elena yet").assertIsDisplayed()
        compose.onNodeWithText("Thread").assertDoesNotExist()
        compose.onNodeWithContentDescription("Add a reply…").assertDoesNotExist()
        compose.onNodeWithText(elenaHiddenText, substring = true).assertDoesNotExist()
    }

    @Test fun `3 - deep link to a thread you can't open shows the gate, not the thread`() {
        go(Routes.thread("elena-forum"))
        compose.onNodeWithText("You're not friends with Elena yet").assertIsDisplayed()
        compose.onAllNodesWithText(elenaNoteStart, substring = true).assertCountEquals(0)
        compose.onNodeWithContentDescription("Add a reply…").assertDoesNotExist()
    }

    @Test fun `4a - a search result opens the Limited profile`() {
        go(Routes.EXPLORE_SEARCH)
        compose.onNodeWithContentDescription("Search Explore").performTextInput("Elena")
        compose.waitForIdle()
        compose.onNodeWithText("Elena Voss").performClick()
        compose.waitForIdle()
        assertLimitedProfileOf("Elena Voss")
    }

    @Test fun `4b - the group People tab opens the Limited profile`() {
        go(Routes.groupDetail("photo-walk"))
        compose.onNodeWithText("People").performClick()
        compose.waitForIdle()
        compose.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Elena Voss, member"))
        compose.onNodeWithContentDescription("Elena Voss, member").performClick()
        compose.waitForIdle()
        assertLimitedProfileOf("Elena Voss")
    }

    @Test fun `4c - an event seat opens the Limited profile`() {
        go(Routes.eventDetail("board-game-night"))
        compose.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Riya Sharma"))
        compose.onNodeWithContentDescription("Riya Sharma").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Riya Sharma").assertIsDisplayed()
        compose.onNodeWithText("Message").assertDoesNotExist()
        compose.onNodeWithText("Accept").assertIsDisplayed() // she asked you
    }

    @Test fun `5 - once the request is accepted the same card opens the thread`() {
        go(Routes.chat("pk"))
        tapElenasSharedCard()
        compose.onNodeWithText(elenaHiddenText, substring = true).assertDoesNotExist() // the card is a snippet
        // The sheet animates in; use its click action rather than a tap on moving bounds.
        compose.onNodeWithText("Send friend request").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        check("EV" in CircleStore.outgoingRequestIds) { "request not sent" }
        compose.onNodeWithText("You're not friends with Elena yet").assertDoesNotExist() // the sheet closed
        repo.acceptedByThem("EV") // Elena accepts (a server event in the real app)
        compose.waitForIdle()
        compose.onNodeWithText("Open the thread").assertExists() // the same card, now open
        tapElenasSharedCard()
        compose.onNodeWithText("Thread").assertIsDisplayed()
        compose.onNodeWithText(elenaHiddenText, substring = true).assertExists()
    }

    @Test fun `6 - unfriending while the thread is open closes it to the gate and purges the cache`() {
        go(Routes.thread("amara-puzzle"))
        compose.onNodeWithText("Finally finished the puzzle", substring = true).assertIsDisplayed()
        check(repo.cache.holdsAnythingBy("AO"))

        runBlocking { repo.unfriend("AO") }
        compose.waitForIdle()

        compose.onNodeWithText("You're not friends with Amara yet").assertIsDisplayed()
        compose.onAllNodesWithText("Finally finished the puzzle", substring = true).assertCountEquals(0)
        compose.onNodeWithContentDescription("Add a reply…").assertDoesNotExist()
        check(!repo.cache.holdsAnythingBy("AO")) { "cached content by Amara must be purged" }
    }
}
