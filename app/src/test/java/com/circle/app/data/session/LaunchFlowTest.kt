package com.circle.app.data.session

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** ACCESS_AND_LAUNCH 0.1: the five cases, then the same rules walked through a session store. */
class LaunchFlowTest {

    @Test fun `case 1 - a session goes straight to Home, whatever onboardingSeen says`() {
        assertEquals(Destination.HOME, LaunchFlow.start(LaunchState(hasSession = true, onboardingSeen = true)))
        assertEquals(Destination.HOME, LaunchFlow.start(LaunchState(hasSession = true, onboardingSeen = false)))
    }

    @Test fun `case 2 - first launch ever goes to Welcome, and the story comes once after sign-up`() {
        val fresh = LaunchState(hasSession = false, onboardingSeen = false)
        assertEquals(Destination.WELCOME, LaunchFlow.start(fresh))
        assertEquals(Destination.FIND_FRIENDS, LaunchFlow.afterAccountCreated())
        assertEquals(Destination.STORY, LaunchFlow.afterFindFriends(fresh.copy(hasSession = true)))
        assertEquals(Destination.HOME, LaunchFlow.afterStory())
    }

    @Test fun `case 3 - logged out after onboarding shows Welcome with Log in only, no story`() {
        assertEquals(Destination.WELCOME_LOG_IN_ONLY, LaunchFlow.start(LaunchState(hasSession = false, onboardingSeen = true)))
    }

    @Test fun `case 4 - logging in never shows the story`() {
        assertEquals(Destination.HOME, LaunchFlow.afterLogIn())
    }

    @Test fun `case 5 - log out and delete account return to Welcome, never the story`() {
        val signedIn = LaunchState(hasSession = true, onboardingSeen = true)
        val out = LaunchFlow.afterSignOut(signedIn)
        assertEquals(Destination.WELCOME_LOG_IN_ONLY, out)
        // Even if the story was somehow never finished, signing out is never the story.
        assertNotEquals(Destination.STORY, LaunchFlow.afterSignOut(signedIn.copy(onboardingSeen = false)))
    }

    @Test fun `walk-through - create account, story once, log out, log back in`() = runTest {
        val store = InMemorySessionStore()
        assertEquals(Destination.WELCOME, LaunchFlow.start(store.current))

        store.accountCreated()
        assertEquals(Destination.STORY, LaunchFlow.afterFindFriends(store.current))
        store.onboardingFinished()
        assertEquals(Destination.HOME, LaunchFlow.start(store.current)) // reopen: straight to Home

        store.loggedOut()
        assertEquals(Destination.WELCOME_LOG_IN_ONLY, LaunchFlow.start(store.current))

        store.loggedIn()
        assertEquals(Destination.HOME, LaunchFlow.start(store.current))
        // A log-in on a brand-new device also marks the story as seen, so it never replays.
        val newPhone = InMemorySessionStore()
        newPhone.loggedIn()
        newPhone.loggedOut()
        assertEquals(Destination.WELCOME_LOG_IN_ONLY, LaunchFlow.start(newPhone.current))
    }
}
