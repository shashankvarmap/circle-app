package com.circle.app.data.repo

import com.circle.app.data.CircleStore
import com.circle.app.data.MessageKind
import com.circle.app.data.access.DenyReason
import com.circle.app.data.media.MediaKind
import com.circle.app.data.media.MediaRef
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** MESSAGES_AND_SETTINGS_V2: the list, the chat and new chats. */
class ChatsTest {
    private lateinit var repo: FakeCircleRepository

    @Before fun setUp() {
        CircleStore.reset()
        repo = FakeCircleRepository()
    }

    @Test fun `new chat - one friend opens the 1-1, several make a named chat in Messages`() = runTest {
        assertEquals("pk", (repo.createChat(listOf("PK"), null) as Outcome.Success).value)
        val id = (repo.createChat(listOf("PK", "EK"), "Book club") as Outcome.Success).value
        val row = repo.chats().first().first { it.id == id }
        assertEquals(ChatKind.MULTI, row.kind)
        assertEquals("Book club", row.title)
        val unnamed = (repo.createChat(listOf("PK", "JL"), "  ") as Outcome.Success).value
        assertEquals("Priya, Jonah", repo.chats().first().first { it.id == unnamed }.title)
    }

    @Test fun `new chats are friends only`() = runTest {
        assertEquals(Outcome.Denied(DenyReason.NOT_FRIENDS), repo.createChat(listOf("PK", "EV"), null))
        assertEquals(Outcome.Denied(DenyReason.NOT_FRIENDS), repo.createChat(listOf("RS"), null))
    }

    @Test fun `pin, mute, clear and delete act on your side only`() = runTest {
        repo.setChatPinned("jl", true)
        repo.setChatMuted("jl", true)
        val row = repo.chats().first().first { it.id == "jl" }
        assertTrue(row.pinned && row.muted)
        repo.clearChat("jl")
        assertTrue(repo.chat("jl").first()!!.messages.isEmpty())
        assertEquals(2, CircleStore.chat("jl")!!.messages.size) // the other person still has them
        repo.deleteChat("ao")
        assertFalse(repo.chats().first().any { it.id == "ao" })
    }

    @Test fun `photos and event invites are sent at once`() = runTest {
        repo.sendMedia("jl", listOf(MediaRef("m", "content://p", MediaKind.PHOTO)))
        assertEquals(MessageKind.PHOTO, repo.chat("jl").first()!!.messages.last().kind)
        repo.takeSeat("trail-brunch")
        assertEquals(listOf("trail-brunch"), repo.myEvents().first().map { it.id })
        repo.sendEventInvite("jl", "trail-brunch")
        assertEquals("trail-brunch", repo.chat("jl").first()!!.messages.last().event!!.id)
    }

    @Test fun `the new-messages marker points at the first unread message and clears on read`() = runTest {
        assertEquals("pk7", repo.chat("pk").first()!!.firstUnreadId)
        repo.markRead("pk")
        assertEquals(null, repo.chat("pk").first()!!.firstUnreadId)
        assertFalse(repo.chats().first().first { it.id == "pk" }.unread)
    }

    @Test fun `seed pins Priya and lists chats newest first`() = runTest {
        val rows = repo.chats().first()
        assertTrue(rows.first { it.id == "pk" }.pinned)
        val times = rows.mapNotNull { it.lastAt }
        assertEquals(times.sortedDescending(), times)
        assertNotNull(rows.first().preview)
    }
}
