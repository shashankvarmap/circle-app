package com.circle.app.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.circle.app.AppGraph
import com.circle.app.CircleApp
import com.circle.app.data.CircleStore
import com.circle.app.data.ThemeMode
import com.circle.app.data.repo.FakeCircleRepository
import com.circle.app.data.session.InMemorySessionStore
import com.circle.app.data.session.LaunchState
import com.circle.app.navigation.Routes
import com.circle.app.ui.launch.SessionViewModel
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The four orbit screens at 320, 360, 390 and 430dp, night and day, font scale 1.0 and 2.0
 * (ACCESS_AND_LAUNCH 5). `./gradlew recordRoborazziDebug` writes the images to
 * app/src/test/screenshots; a plain test run renders them all and fails on any crash.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class OrbitScreenshotTest(private val width: Int, private val night: Boolean, private val font: Float) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}dp night={1} font={2}")
        fun cases(): List<Array<Any>> =
            listOf(320, 360, 390, 430).flatMap { w -> listOf(true, false).flatMap { n -> listOf(1.0f, 2.0f).map { f -> arrayOf<Any>(w, n, f) } } }
    }

    @get:Rule val compose = createComposeRule()
    private lateinit var nav: NavHostController

    private fun open(route: String, then: () -> Unit = {}): String {
        RuntimeEnvironment.setQualifiers("w${width}dp-h800dp")
        RuntimeEnvironment.setFontScale(font)
        CircleStore.reset()
        val repo = FakeCircleRepository()
        runBlocking { repo.setTheme(if (night) ThemeMode.NIGHT else ThemeMode.DAY) }
        AppGraph.install(repository = repo)
        compose.setContent {
            nav = rememberNavController()
            CircleApp(SessionViewModel(InMemorySessionStore(LaunchState(hasSession = true, onboardingSeen = true))), nav)
        }
        compose.waitForIdle()
        compose.runOnUiThread { nav.navigate(route) }
        compose.waitForIdle()
        then()
        compose.waitForIdle()
        return "w${width}_${if (night) "night" else "day"}_font${(font * 10).toInt()}"
    }

    private fun shot(screen: String, suffix: String) =
        compose.onRoot().captureRoboImage("src/test/screenshots/${screen}_$suffix.png")

    @Test fun explore() = shot("explore", open(Routes.EXPLORE))

    @Test fun yourCircle() = shot("your_circle", open(Routes.YOUR_CIRCLE))

    @Test fun groupPeople() = shot("group_people", open(Routes.groupDetail("photo-walk")) {
        compose.onNodeWithText("People").performSemanticsAction(SemanticsActions.OnClick)
    })

    @Test fun eventTable() = shot("event_table", open(Routes.eventDetail("board-game-night")))
}
