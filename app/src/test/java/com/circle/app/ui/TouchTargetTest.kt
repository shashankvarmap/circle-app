package com.circle.app.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Every tappable thing on the main screens is at least 48×48dp (ACCESS_AND_LAUNCH 7, BUILD_BRIEF).
 * Walks the merged semantics tree of each screen and lists anything with a click action that is smaller.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w390dp-h2400dp")
class TouchTargetTest {

    @get:Rule val compose = createComposeRule()
    private lateinit var nav: NavHostController

    private val routes = listOf(
        Routes.HOME, Routes.EXPLORE, Routes.MESSAGES, Routes.NOTIFICATIONS, Routes.PROFILE, Routes.SETTINGS,
        Routes.YOUR_CIRCLE, Routes.thread("you-coffee"), Routes.person("PK"), Routes.groupDetail("photo-walk"),
        Routes.eventDetail("board-game-night"), Routes.EXPLORE_SEARCH, Routes.MEMBERSHIP, Routes.YOUR_DATA,
    )

    private fun SemanticsNode.walk(): List<SemanticsNode> = listOf(this) + children.flatMap { it.walk() }

    @Test fun `tap targets are at least 48dp`() {
        CircleStore.reset()
        AppGraph.install(repository = FakeCircleRepository())
        compose.setContent {
            nav = rememberNavController()
            CircleApp(SessionViewModel(InMemorySessionStore(LaunchState(hasSession = true, onboardingSeen = true))), nav)
        }
        compose.waitForIdle()
        val density = compose.density.density
        val small = mutableListOf<String>()
        routes.forEach { route ->
            compose.runOnUiThread { nav.navigate(route) }
            compose.waitForIdle()
            compose.onRoot(useUnmergedTree = false).fetchSemanticsNode().walk()
                .filter { it.config.getOrNull(SemanticsActions.OnClick) != null }
                .forEach { n ->
                    val w = n.boundsInRoot.width / density
                    val h = n.boundsInRoot.height / density
                    // Compose extends a small pointer target to 48dp (touchBounds); both must fall short to fail.
                    if (w > 0f && h > 0f && (w < 47.5f || h < 47.5f) && n.touchBoundsInRoot.let { it.width / density < 47.5f || it.height / density < 47.5f }) {
                        val label = n.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
                            ?: n.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text ?: "?"
                        small += "$route: \"${label.take(40)}\" ${w.toInt()}x${h.toInt()}dp"
                    }
                }
        }
        assertTrue("Tap targets under 48dp:\n" + small.distinct().joinToString("\n"), small.isEmpty())
    }
}
