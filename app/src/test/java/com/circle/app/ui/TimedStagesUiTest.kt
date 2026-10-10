package com.circle.app.ui

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.circle.app.AppGraph
import com.circle.app.CircleApp
import com.circle.app.data.CircleStore
import com.circle.app.data.Clock
import com.circle.app.data.lifetime.LifeState
import com.circle.app.data.repo.FakeCircleRepository
import com.circle.app.data.session.InMemorySessionStore
import com.circle.app.data.session.LaunchState
import com.circle.app.navigation.Routes
import com.circle.app.ui.launch.SessionViewModel
import java.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * TIMED_GROUPS T5: every stage on screen, driven by the time-travel control while the screen is open:
 * Active > Ending soon > Wrap-up (read-only, composer locked, host sees Extend) > Deleted.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TimedStagesUiTest {

    @get:Rule val compose = createComposeRule()
    private lateinit var repo: FakeCircleRepository
    private lateinit var nav: NavHostController

    @Before fun setUp() {
        androidx.work.testing.WorkManagerTestInitHelper.initializeTestWorkManager(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        CircleStore.reset()
        repo = FakeCircleRepository()
        AppGraph.install(repository = repo)
        compose.setContent {
            nav = rememberNavController()
            CircleApp(SessionViewModel(InMemorySessionStore(LaunchState(hasSession = true, onboardingSeen = true))), nav)
        }
        compose.waitForIdle()
    }

    @After fun tearDown() { Clock.shift = Duration.ZERO }

    private fun open(route: String) { compose.runOnUiThread { nav.navigate(route) }; compose.waitForIdle() }
    private fun jump(id: String, stage: LifeState) { runBlocking { repo.jumpTo(id, stage) }; compose.waitForIdle() }

    @Test fun `a chat goes through every stage while it is open`() {
        open(Routes.chat("alps-trip"))
        compose.onNodeWithContentDescription("Ends in 12 days").assertExists()
        compose.onNode(hasSetTextAction()).assertExists()

        jump("alps-trip", LifeState.ENDING_SOON)
        compose.onNodeWithText("ENDING SOON").assertExists()
        compose.onNodeWithText("Ends in 3 days.").assertExists()

        jump("alps-trip", LifeState.WRAP_UP)
        compose.onNodeWithText("THIS CHAT HAS ENDED").assertExists()
        compose.onNodeWithText("Read-only. This chat has ended.").assertExists()
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        compose.onNodeWithText("Extend or keep it").assertExists() // you started it: you're the host

        jump("alps-trip", LifeState.DELETED)
        compose.onNodeWithText("This chat has ended and was deleted.").assertExists()
        compose.onNodeWithText("Back to Messages").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Alps trip").assertDoesNotExist()
    }

    @Test fun `a member never sees the host's buttons`() {
        open(Routes.chat("study-group"))
        jump("study-group", LifeState.WRAP_UP)
        compose.onNodeWithText("THIS CHAT HAS ENDED").assertExists()
        compose.onNodeWithText("Save a copy").assertExists()
        compose.onNodeWithText("Extend or keep it").assertDoesNotExist()
    }

    @Test fun `the host extends from wrap-up and the composer comes back`() {
        open(Routes.chat("exam-prep"))
        compose.onNodeWithText("Extend or keep it").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onNodeWithText("Keep it going").assertExists()
        // The sheet is still animating in: click by semantics, not by touch.
        compose.onNodeWithText("+7 days").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onNodeWithText("THIS CHAT HAS ENDED").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).assertExists()
    }

    @Test fun `a temporary group asks for consent before a join request`() {
        open(Routes.joinGroup("alps-hiking-crew"))
        compose.onNodeWithText("By joining you agree to this.").assertExists()
        compose.onNodeWithText("Request to join").performClick()
        compose.waitForIdle()
        assert(CircleStore.group("alps-hiking-crew")!!.joinRequested)
        // Once it has ended, nobody can ask to join.
        jump("alps-hiking-crew", LifeState.WRAP_UP)
        open(Routes.groupDetail("alps-hiking-crew"))
        compose.onNodeWithText("Request to join").assertDoesNotExist()
    }

    @Test fun `save a copy opens the sheet with both options`() {
        open(Routes.chat("study-group"))
        compose.onNodeWithText("Save a copy").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Everything").assertExists()
        compose.onNodeWithText("Text only").assertExists()
        compose.onNodeWithText("Member names only. No phone numbers.").assertExists()
    }
}
