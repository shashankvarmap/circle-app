package com.circle.app.data.repo

import com.circle.app.data.Clock
import com.circle.app.data.CircleStore
import com.circle.app.data.access.DenyReason
import com.circle.app.data.lifetime.LifeState
import com.circle.app.data.persist.capture
import com.circle.app.data.persist.restore
import java.time.Duration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** TIMED_GROUPS T2: the demo seed's temporary groups and chats, and the time-travel control. */
class TimeTravelTest {

    private lateinit var repo: FakeCircleRepository

    @Before fun setUp() {
        CircleStore.reset()
        repo = FakeCircleRepository()
    }

    @After fun tearDown() { Clock.shift = Duration.ZERO }

    private suspend fun states() = repo.timedItems().first().associate { it.id to it.state }

    @Test fun `seed has every stage`() = runTest {
        assertEquals(
            mapOf(
                "project-alps" to LifeState.ACTIVE, "alps-hiking-crew" to LifeState.ACTIVE, "exam-prep-circle" to LifeState.ACTIVE,
                "alps-trip" to LifeState.ACTIVE, "study-group" to LifeState.ENDING_SOON, "exam-prep" to LifeState.WRAP_UP,
            ),
            states()
        )
        val explore = repo.explore().first().groups.map { it.ref.id }
        assertTrue(explore.containsAll(listOf("alps-hiking-crew", "exam-prep-circle")))
    }

    @Test fun `travelling forward moves stages and deletes at the end of wrap-up`() = runTest {
        repo.travel(Duration.ofDays(3))
        assertEquals(LifeState.ENDING_SOON, states()["alps-hiking-crew"])
        assertEquals(LifeState.WRAP_UP, states()["study-group"])
        repo.travel(Duration.ofDays(3))
        // Exam prep ended two days before today: deleted five days after today, gone from the store.
        assertNull(states()["exam-prep"])
        assertNull(CircleStore.chat("exam-prep"))
        assertEquals(Duration.ofDays(6), repo.timeShift().first())
    }

    @Test fun `jump to wrap-up makes the group chat read-only, jump to deleted removes the group`() = runTest {
        repo.jumpTo("project-alps", LifeState.WRAP_UP)
        assertEquals(LifeState.WRAP_UP, states()["project-alps"])
        assertEquals(DenyReason.ENDED, (repo.sendMessage("g-project-alps", "Still on?", null) as Outcome.Denied).reason)
        repo.jumpTo("project-alps", LifeState.ENDING_SOON)
        assertEquals(LifeState.ENDING_SOON, states()["project-alps"])
        repo.jumpTo("project-alps", LifeState.DELETED)
        assertNull(CircleStore.group("project-alps"))
        assertNull(CircleStore.chat("g-project-alps"))
        assertTrue(CircleStore.posts.none { it.groupId == "project-alps" })
    }

    @Test fun `back to real time keeps deletions, reset brings the seed back`() = runTest {
        repo.jumpTo("exam-prep", LifeState.DELETED)
        repo.backToToday()
        assertEquals(Duration.ZERO, Clock.shift)
        assertNull(CircleStore.chat("exam-prep"))
        repo.resetDemoData()
        assertEquals(LifeState.WRAP_UP, states()["exam-prep"])
    }

    @Test fun `the shift is saved with the demo data`() = runTest {
        repo.travel(Duration.ofDays(1))
        val saved = CircleStore.capture()
        Clock.shift = Duration.ZERO
        CircleStore.restore(saved)
        assertEquals(Duration.ofDays(1), Clock.shift)
    }
}
