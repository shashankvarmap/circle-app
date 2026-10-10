package com.circle.app.data.repo

import com.circle.app.data.AlertKind
import com.circle.app.data.CircleStore
import com.circle.app.data.Clock
import com.circle.app.data.access.DenyReason
import com.circle.app.data.lifetime.LifeState
import com.circle.app.data.lifetime.Term
import java.time.Duration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** TIMED_GROUPS T3: what the screens are given, and the host's changes. */
class TimedDisplayTest {

    private lateinit var repo: FakeCircleRepository

    @Before fun setUp() {
        CircleStore.reset()
        repo = FakeCircleRepository()
    }

    @After fun tearDown() { Clock.shift = Duration.ZERO }

    private suspend fun row(id: String) = repo.chats().first().first { it.id == id }

    @Test fun `chat rows carry the lifetime, counted in whole days`() = runTest {
        assertEquals(12, row("alps-trip").lifetime!!.daysLeft)
        assertEquals(LifeState.ENDING_SOON, row("study-group").lifetime!!.state)
        assertEquals(3, row("study-group").lifetime!!.daysLeft)
        val ended = row("exam-prep").lifetime!!
        assertEquals(LifeState.WRAP_UP, ended.state)
        assertEquals(5, ended.daysToDelete)
        // A group's chat shows its group's one clock; permanent chats have none.
        assertEquals(LifeState.ACTIVE, row("g-project-alps").lifetime!!.state)
        assertNull(row("pk").lifetime)
    }

    @Test fun `only the host may change it, and an ended chat is read-only`() = runTest {
        // You started Alps trip and Exam prep; Elin started Study group; Priya hosts Project Alps.
        assertTrue(row("alps-trip").lifetime!!.canChange)
        assertFalse(row("study-group").lifetime!!.canChange)
        assertFalse(repo.group("project-alps").first()!!.lifetime!!.canChange)
        assertEquals(DenyReason.NOT_HOST, (repo.extendLifetime("study-group", 7) as Outcome.Denied).reason)
        assertEquals(DenyReason.NOT_HOST, (repo.endNow("g-project-alps") as Outcome.Denied).reason)
        val chat = repo.chat("exam-prep").first()!!
        assertFalse(chat.canSend)
        assertTrue(chat.lifetime!!.canSaveCopy)
    }

    @Test fun `extend from wrap-up reopens it, then end now and make permanent`() = runTest {
        assertTrue(repo.extendLifetime("exam-prep", 7) is Outcome.Success)
        val reopened = repo.chat("exam-prep").first()!!
        assertTrue(reopened.canSend)
        // From wrap-up, +7 counts from today: the end of the 7th day from now (time left rounds up: 8).
        assertEquals(8, reopened.lifetime!!.daysLeft)
        assertTrue(repo.endNow("alps-trip") is Outcome.Success)
        assertEquals(LifeState.WRAP_UP, row("alps-trip").lifetime!!.state)
        assertTrue(repo.makePermanent("alps-trip") is Outcome.Success)
        assertNull(row("alps-trip").lifetime)
        // Members are told (by the server).
        assertTrue(CircleStore.sentNotices.any { it.toPersonId == "PK" })
    }

    @Test fun `reminders are sent as stages are reached, and vanish with the deleted chat`() = runTest {
        val kinds = { id: String -> CircleStore.alerts.filter { it.timedId == id }.map { it.kind }.toSet() }
        assertEquals(setOf(AlertKind.TIMED_ENDS_SOON), kinds("study-group"))
        assertEquals(setOf(AlertKind.TIMED_ENDED), kinds("exam-prep"))
        assertTrue(kinds("alps-trip").isEmpty())
        repo.travel(Duration.ofDays(4))
        assertTrue(AlertKind.TIMED_DELETED_TOMORROW in kinds("exam-prep"))
        val shown = repo.alerts().first().mapNotNull { it.timed?.id }
        assertTrue("exam-prep" in shown)
        repo.travel(Duration.ofDays(2))
        assertTrue(kinds("exam-prep").isEmpty())
        assertTrue(repo.wasDeleted("exam-prep").first())
    }

    @Test fun `temporary group and chat from the forms`() = runTest {
        val g = (repo.createGroup("Wedding", "Family", "", isPublic = false, lifetimeDays = 14) as Outcome.Success).value
        val view = repo.group(g).first()!!
        assertEquals(LifeState.ACTIVE, view.lifetime!!.state)
        assertTrue(view.lifetime!!.canChange)
        assertNotNull(view.chatId)
        val c = (repo.createChat(listOf("PK"), "Trip", lifetimeDays = 3) as Outcome.Success).value
        val chat = repo.chats().first().first { it.id == c }
        assertEquals(ChatKind.MULTI, chat.kind) // in Messages, never in Groups
        assertEquals(LifeState.ACTIVE, chat.lifetime!!.state)
        assertTrue(CircleStore.chat(c)!!.term is Term.Temporary)
        assertTrue(repo.createGroup("Too long", "", "", false, lifetimeDays = 91) is Outcome.Error)
    }
}
