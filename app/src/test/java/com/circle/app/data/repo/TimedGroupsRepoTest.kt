package com.circle.app.data.repo

import com.circle.app.data.Alert
import com.circle.app.data.AlertKind
import com.circle.app.data.CircleStore
import com.circle.app.data.access.Audience
import com.circle.app.data.access.DenyReason
import com.circle.app.data.lifetime.Term
import com.circle.app.data.persist.capture
import com.circle.app.data.persist.restore
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** TIMED_GROUPS T1: the fake repository's rules for temporary groups and chats, driven by a movable clock. */
class TimedGroupsRepoTest {

    private class MovableClock(var t: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = t
    }

    private val start = Instant.parse("2026-11-01T12:00:00Z")
    private val endsAt = start.plus(Duration.ofDays(10))
    private lateinit var clock: MovableClock
    private lateinit var repo: FakeCircleRepository

    private val group = "photo-walk"
    private val groupChat = "g-photo-walk"
    private val groupPost = "group-golden-hour"

    @Before fun setUp() {
        CircleStore.reset()
        clock = MovableClock(start)
        repo = FakeCircleRepository(clock = clock)
        CircleStore.group(group)!!.term = Term.Temporary(endsAt)
    }

    private fun goTo(i: Instant) { clock.t = i }
    private fun wrapUp() = goTo(endsAt.plus(Duration.ofDays(1)))
    private fun deleted() = goTo(endsAt.plus(Duration.ofDays(7)))
    private fun <T> Outcome<T>.reason() = (this as Outcome.Denied).reason

    @Test fun `active and ending soon work as usual`() = runTest {
        assertTrue(repo.post("Sunday?", Audience.FRIENDS, null, group, emptyList()) is Outcome.Success)
        goTo(endsAt.minus(Duration.ofDays(1)))
        assertTrue(repo.sendMessage(groupChat, "See you there", null) is Outcome.Success)
        assertTrue(repo.addComment(groupPost, "Count me in", null) is Outcome.Success)
    }

    @Test fun `wrap-up is read-only`() = runTest {
        wrapUp()
        assertEquals(DenyReason.ENDED, repo.post("late", Audience.FRIENDS, null, group, emptyList()).reason())
        assertEquals(DenyReason.ENDED, repo.addComment(groupPost, "late", null).reason())
        assertEquals(DenyReason.ENDED, repo.toggleLike(groupPost).reason())
        assertEquals(DenyReason.ENDED, repo.sendMessage(groupChat, "late", null).reason())
        assertEquals(DenyReason.ENDED, repo.sendMedia(groupChat, listOf(com.circle.app.data.media.MediaRef("m", "u", com.circle.app.data.media.MediaKind.PHOTO))).reason())
        assertEquals(DenyReason.ENDED, repo.shareNote(groupChat, groupPost).reason())
        assertEquals(DenyReason.ENDED, repo.inviteToGroup(group, listOf("AO")).reason())
        // Reads still follow membership.
        assertNotNull(repo.group(group).first())
        assertTrue(repo.groupPosts(group).first().any { it.id == groupPost })
        assertNotNull(repo.chat(groupChat).first())
    }

    @Test fun `no joins once ended, and open join requests are declined`() = runTest {
        val other = CircleStore.group("sunday-trail")!!
        other.term = Term.Temporary(endsAt)
        assertTrue(repo.requestToJoin("sunday-trail") is Outcome.Success)
        assertTrue(other.joinRequested)
        assertTrue(CircleStore.group(group)!!.joinRequests.isNotEmpty()) // Lukas and Clara asked
        wrapUp()
        repo.markAllAlertsRead() // any action lets the fake catch up with time
        assertFalse(other.joinRequested)
        assertTrue(CircleStore.group(group)!!.joinRequests.isEmpty())
        assertEquals(DenyReason.ENDED, repo.requestToJoin("sunday-trail").reason())
        assertFalse(other.joinRequested)
    }

    @Test fun `host-side join approval is refused once ended`() = runTest {
        val g = CircleStore.group(group)!!
        g.hostIds.add(com.circle.app.data.ME)
        CircleStore.groups.first { it.id == "sunday-trail" }.apply { term = Term.Temporary(endsAt); hostIds.add(com.circle.app.data.ME); isMember = true; joinRequests.add("NK" to start) }
        wrapUp()
        assertEquals(DenyReason.ENDED, repo.approveJoinRequest("sunday-trail", "NK").reason())
    }

    @Test fun `at deleteAt everything is gone for everyone, in memory and in the saved copy`() = runTest {
        CircleStore.alerts.add(Alert("ax", "PK", AlertKind.LIKED_YOUR_NOTE, start, postId = groupPost))
        CircleStore.invites.add(com.circle.app.data.Invite("AO", group, null, start))
        repo.note(groupPost).first() // opened: now cached
        repo.thread(groupPost).first()
        assertTrue(repo.cache.keysFor("PK").any { it.endsWith(groupPost) })
        assertTrue("group-long-exposure" in CircleStore.likedPostIds)

        deleted()
        // Reads are NotFound at once, before the fake sweeps.
        assertNull(repo.group(group).first())
        assertNull(repo.chat(groupChat).first())
        assertTrue(repo.groupPosts(group).first().isEmpty())
        assertTrue(repo.chats().first().none { it.id == groupChat })
        assertTrue(repo.explore().first().groups.none { it.ref.id == group })
        assertTrue(repo.memberGroups().first().none { it.id == group })

        repo.markAllAlertsRead()
        assertNull(CircleStore.group(group))
        assertNull(CircleStore.chat(groupChat))
        assertTrue(CircleStore.posts.none { it.groupId == group })
        assertTrue(CircleStore.alerts.none { it.postId == groupPost })
        assertTrue(CircleStore.invites.none { it.groupId == group })
        assertFalse("group-long-exposure" in CircleStore.likedPostIds)
        assertTrue(repo.cache.keysFor("PK").none { it.endsWith(groupPost) })
        val saved = CircleStore.capture()
        assertTrue(saved.groups.none { it.id == group })
        assertTrue(saved.chats.none { it.id == groupChat })
        assertTrue(saved.posts.none { it.groupId == group })
        // Everything else is untouched.
        assertNotNull(CircleStore.group("sunday-trail"))
        assertTrue(CircleStore.posts.any { it.id == "priya-seaglass" })
    }

    @Test fun `just before deleteAt nothing is deleted`() = runTest {
        goTo(endsAt.plus(Duration.ofDays(7)).minusMillis(1))
        repo.markAllAlertsRead()
        assertNotNull(CircleStore.group(group))
        assertNotNull(repo.group(group).first())
    }

    @Test fun `a temporary chat with friends ends and is deleted on its own clock`() = runTest {
        val id = (repo.createChat(listOf("AO", "PK"), "Trip") as Outcome.Success).value
        val chat = CircleStore.chat(id)!!
        assertEquals(com.circle.app.data.ME, chat.startedBy)
        chat.term = Term.Temporary(endsAt)
        assertTrue(repo.sendMessage(id, "Tickets booked", null) is Outcome.Success)
        wrapUp()
        assertEquals(DenyReason.ENDED, repo.sendMessage(id, "late", null).reason())
        assertNotNull(repo.chat(id).first())
        deleted()
        assertNull(repo.chat(id).first())
        repo.markAllAlertsRead()
        assertNull(CircleStore.chat(id))
        // Permanent chats are untouched.
        assertNotNull(CircleStore.chats.firstOrNull { it.directPersonId == "PK" })
    }

    @Test fun `permanent groups and chats never end`() = runTest {
        CircleStore.group(group)!!.term = Term.Permanent
        goTo(start.plus(Duration.ofDays(5000)))
        assertTrue(repo.sendMessage(groupChat, "still here", null) is Outcome.Success)
        assertNotNull(CircleStore.group(group))
    }

    @Test fun `terms survive a save and restore`() {
        val chat = CircleStore.chats.first { it.groupId == null }
        chat.term = Term.Temporary(endsAt)
        val snapshot = CircleStore.capture()
        CircleStore.reset()
        CircleStore.restore(snapshot)
        assertEquals(Term.Temporary(endsAt), CircleStore.group(group)!!.term)
        assertEquals(Term.Temporary(endsAt), CircleStore.chat(chat.id)!!.term)
        assertEquals(Term.Permanent, CircleStore.group("sunday-trail")!!.term)
    }
}
