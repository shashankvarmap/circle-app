package com.circle.app.ui

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Phase 11: with the fail switch on, a sent message says "Not sent · tap to retry"; tapping it sends it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChatSendingUiTest {

    @get:Rule val compose = createComposeRule()
    private lateinit var repo: FakeCircleRepository
    private lateinit var nav: NavHostController

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

    @Test fun `failed message shows Not sent and retries`() {
        val chatId = CircleStore.chats.first { it.groupId == null && it.memberIds.size == 1 }.id
        compose.runOnUiThread { nav.navigate(Routes.chat(chatId)) }
        compose.waitForIdle()
        runBlocking { repo.setFailing(true) }

        compose.onNode(hasSetTextAction()).performTextInput("See you at seven")
        compose.onNodeWithContentDescription("Send").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Not sent · tap to retry").assertExists()
        assertTrue(CircleStore.chat(chatId)!!.messages.none { it.text == "See you at seven" })

        runBlocking { repo.setFailing(false) }
        compose.onNodeWithText("Not sent · tap to retry").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Not sent · tap to retry").assertDoesNotExist()
        assertTrue(CircleStore.chat(chatId)!!.messages.any { it.text == "See you at seven" })
    }
}
