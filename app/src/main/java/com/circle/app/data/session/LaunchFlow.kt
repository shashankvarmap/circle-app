package com.circle.app.data.session

/** What the device knows at launch: a session (logged in) and whether the story was ever shown. */
data class LaunchState(val hasSession: Boolean, val onboardingSeen: Boolean)

/** Where a step of getting in leads. */
enum class Destination {
    HOME,
    /** Welcome with Create account and Log in. */
    WELCOME,
    /** Welcome with Log in only: the story was seen on this device, so it never replays. */
    WELCOME_LOG_IN_ONLY,
    FIND_FRIENDS,
    /** The 4-slide story. Only ever reached right after creating an account. */
    STORY,
}

/**
 * The rules of ACCESS_AND_LAUNCH 0.1, as pure functions so each case is unit-tested:
 * 1. a session goes straight to Home; 2. no session and never onboarded: Welcome, then sign-up,
 * then the story once; 3. no session but onboarded: Welcome with Log in only; 4. logging in never
 * shows the story; 5. log out and delete account return to Welcome, never to the story.
 */
object LaunchFlow {

    fun start(state: LaunchState): Destination = when {
        state.hasSession -> Destination.HOME
        !state.onboardingSeen -> Destination.WELCOME
        else -> Destination.WELCOME_LOG_IN_ONLY
    }

    /** After the account is created: find friends, then the story. */
    fun afterAccountCreated(): Destination = Destination.FIND_FRIENDS

    /** After find friends (connected or skipped): the story the first time, else Home. */
    fun afterFindFriends(state: LaunchState): Destination =
        if (state.onboardingSeen) Destination.HOME else Destination.STORY

    fun afterStory(): Destination = Destination.HOME

    /** Logging in (an existing account, a new phone, after reinstalling) never shows the story. */
    fun afterLogIn(): Destination = Destination.HOME

    /** Log out and delete account: back to Welcome, never the story. */
    fun afterSignOut(state: LaunchState): Destination = start(state.copy(hasSession = false))
}
