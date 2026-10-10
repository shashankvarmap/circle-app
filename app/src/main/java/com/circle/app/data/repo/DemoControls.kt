package com.circle.app.data.repo

import kotlinx.coroutines.flow.Flow

/**
 * Controls that exist only for the demo (the fake repository). A backend repository does not
 * implement this, so Settings hides the rows. Phase 11 (F6).
 */
interface DemoControls {
    /** Debug switch: while on, actions fail and loaded lists show their error state. */
    fun failing(): Flow<Boolean>
    suspend fun setFailing(on: Boolean)

    /** Puts the demo back to its seed data (the saved copy follows). */
    suspend fun resetDemoData()

    /** Preview builds: sample people accept your friend requests a few seconds after you send them. */
    fun autoAccept(): Flow<Boolean>
    suspend fun setAutoAccept(on: Boolean)

    /**
     * Time travel (TIMED_GROUPS T2): how far the fake server's clock is ahead of real time. Moving it
     * runs what the server does as time passes (wrap-up, reminders, deletion) at once, and every
     * list, chip and banner follows live.
     */
    fun timeShift(): Flow<java.time.Duration>
    suspend fun travel(by: java.time.Duration)
    /** Back to real time. Anything already deleted stays deleted; Reset demo data brings it back. */
    suspend fun backToToday()

    /** Temporary groups and chats that still exist, for "Jump to a stage". */
    fun timedItems(): Flow<List<TimedItem>>
    /** Moves the clock so [id] is just inside [stage] (ACTIVE, ENDING_SOON, WRAP_UP or DELETED). */
    suspend fun jumpTo(id: String, stage: com.circle.app.data.lifetime.LifeState)
}

/** A temporary group or chat as the time-travel screen lists it. */
data class TimedItem(
    val id: String,
    val name: String,
    val isGroup: Boolean,
    val state: com.circle.app.data.lifetime.LifeState,
    val endsAt: java.time.Instant,
)
