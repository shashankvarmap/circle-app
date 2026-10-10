package com.circle.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.circle.app.AppGraph
import com.circle.app.data.CircleStore
import com.circle.app.data.lifetime.LifeState
import com.circle.app.data.repo.FakeCircleRepository
import com.circle.app.data.repo.LifetimeView
import com.circle.app.ui.components.TemporaryGroupTile
import com.circle.app.ui.components.TimerChip
import com.circle.app.ui.screens.ExploreScreen
import com.circle.app.ui.theme.CircleTheme
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** TIMED_GROUPS T5: TimerChip in each state and Explore's temporary-group node, night and day. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class TimedScreenshotTest(private val night: Boolean) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "night={0}")
        fun cases(): List<Array<Any>> = listOf(arrayOf(true), arrayOf(false))
    }

    @get:Rule val compose = createComposeRule()
    private val theme get() = if (night) "night" else "day"

    private fun life(state: LifeState, days: Int, elapsed: Float) = LifetimeView(
        state, Instant.EPOCH, Instant.EPOCH, days, days + 7, elapsed, canChange = false, canSaveCopy = true,
    )

    @Test fun timerChips() {
        compose.setContent {
            CircleTheme(night = night) {
                Column(Modifier.background(CircleTheme.colors.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimerChip(life(LifeState.ACTIVE, 12, 0.6f))
                        TimerChip(life(LifeState.ENDING_SOON, 3, 0.9f))
                        TimerChip(life(LifeState.WRAP_UP, 0, 1f))
                    }
                    TemporaryGroupTile("P")
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/timed_chips_$theme.png")
    }

    @Test fun exploreTemporaryNode() {
        CircleStore.reset()
        AppGraph.install(repository = FakeCircleRepository())
        compose.setContent { CircleTheme(night = night) { ExploreScreen(rememberNavController()) } }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/timed_explore_$theme.png")
    }
}
