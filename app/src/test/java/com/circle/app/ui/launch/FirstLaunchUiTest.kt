package com.circle.app.ui.launch

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.circle.app.CircleApp
import com.circle.app.data.session.InMemorySessionStore
import com.circle.app.data.session.LaunchState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The start screen really shown, per ACCESS_AND_LAUNCH 0.1. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirstLaunchUiTest {

    @get:Rule val compose = createComposeRule()

    private val storyFirstSlide = "A place for the people you actually know — not everyone you've ever met."

    @Test fun loggedInOpensHomeWithoutTheStory() {
        compose.setContent { CircleApp(SessionViewModel(InMemorySessionStore(LaunchState(hasSession = true, onboardingSeen = true)))) }
        compose.onNodeWithText("Circle.").assertIsDisplayed()
        compose.onNodeWithText(storyFirstSlide).assertDoesNotExist()
        compose.onNodeWithText("Create account").assertDoesNotExist()
    }

    @Test fun firstLaunchOpensWelcomeWithCreateAccount() {
        compose.setContent { CircleApp(SessionViewModel(InMemorySessionStore())) }
        compose.onNodeWithText("Create account").assertIsDisplayed()
        compose.onNodeWithText("Log in").assertIsDisplayed()
        compose.onNodeWithText(storyFirstSlide).assertDoesNotExist()
    }

    @Test fun loggedOutAfterOnboardingShowsLogInOnly() {
        compose.setContent { CircleApp(SessionViewModel(InMemorySessionStore(LaunchState(hasSession = false, onboardingSeen = true)))) }
        compose.onNodeWithText("Log in").assertIsDisplayed()
        compose.onNodeWithText("Create account").assertDoesNotExist()
        compose.onNodeWithText(storyFirstSlide).assertDoesNotExist()
    }
}
