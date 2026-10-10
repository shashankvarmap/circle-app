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
}
