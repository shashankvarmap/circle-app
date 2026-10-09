package com.circle.app.data.repo

import com.circle.app.data.CircleStore
import com.circle.app.data.ME
import com.circle.app.data.access.DenyReason
import com.circle.app.data.access.EventVisibility
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant

/** Phase F4: groups and events. */
class GroupsAndEventsTest {
    private lateinit var repo: FakeCircleRepository

    @Before fun setUp() {
        CircleStore.reset()
        repo = FakeCircleRepository()
    }

    private suspend fun created(): String =
        (repo.createGroup("Night Swims", "Outdoors", "Lake at dusk", isPublic = true) as Outcome.Success).value

    @Test fun `creating a group makes you host and member, with its own chat in Groups`() = runTest {
        val id = created()
        val g = repo.group(id).first()!!
        assertTrue(g.isMember && g.iAmHost)
        assertTrue(repo.explore().first().groups.any { it.ref.id == id })
        assertTrue(repo.chats().first().any { it.kind == ChatKind.GROUP && it.group?.id == id })
    }

    @Test fun `only hosts approve or decline, and approving makes a member`() = runTest {
        // You're an ordinary member of the photo walk: no host powers.
        assertEquals(Outcome.Denied(DenyReason.NOT_FOUND), repo.approveJoinRequest("photo-walk", "LB"))
        assertFalse(repo.group("photo-walk").first()!!.iAmHost)

        val id = created()
        CircleStore.group(id)!!.joinRequests.add("LB" to Instant.now())
        CircleStore.group(id)!!.joinRequests.add("NK" to Instant.now())
        assertTrue(repo.approveJoinRequest(id, "LB").isSuccess)
        val g = repo.group(id).first()!!
        assertTrue(g.people!!.members.any { it.id == "LB" })
        assertEquals(2, g.memberCount)
        repo.declineJoinRequest(id, "NK")
        assertTrue(repo.group(id).first()!!.people!!.requests.isEmpty())
    }

    @Test fun `hosts edit the group and remove members, never other hosts`() = runTest {
        val id = created()
        CircleStore.group(id)!!.memberIds.add("PK")
        assertTrue(repo.editGroup(id, "Dusk Swims", "Lake", isPublic = false).isSuccess)
        assertEquals("Dusk Swims", repo.group(id).first()!!.ref.name)
        assertFalse(repo.explore().first().groups.any { it.ref.id == id }) // private now
        assertTrue(repo.removeMember(id, "PK").isSuccess)
        assertTrue(repo.removeMember(id, ME) is Outcome.Error)
        assertEquals(Outcome.Denied(DenyReason.NOT_FOUND), repo.editGroup("photo-walk", "Hijack", "", true))
    }

    @Test fun `leaving a group removes its chat and posts from your side`() = runTest {
        repo.leaveGroup("photo-walk")
        assertTrue(repo.groupPosts("photo-walk").first().isEmpty())
        assertEquals(null, repo.chat("g-photo-walk").first())
    }

    @Test fun `invites go to friends only`() = runTest {
        assertEquals(2, (repo.inviteToGroup("photo-walk", listOf("PK", "JL", "EV")) as Outcome.Success).value)
        assertEquals(Outcome.Denied(DenyReason.GROUP_NOT_MEMBER), repo.inviteToGroup("board-game-club", listOf("PK")))
        assertEquals(1, (repo.inviteToEvent("trail-brunch", listOf("AO")) as Outcome.Success).value)
        assertEquals(3, CircleStore.invites.size)
    }

    @Test fun `a new event this week sits on the map at its topic, a later one only in the list`() = runTest {
        val soon = (repo.createEvent("Picnic", EventVisibility.FRIENDS, "", 8, Instant.now().plus(Duration.ofDays(2)), "OUTDOORS", "Park") as Outcome.Success).value
        val later = (repo.createEvent("Concert", EventVisibility.FRIENDS, "", 8, Instant.now().plus(Duration.ofDays(20)), "MUSIC", "Hall") as Outcome.Success).value
        val events = repo.explore().first().events
        val deg = events.first { it.id == soon }.compassDeg!!
        assertTrue(deg in 0f..90f) // the outdoors quarter
        assertEquals(null, events.first { it.id == later }.compassDeg)
        assertTrue(repo.createEvent("Yesterday", EventVisibility.FRIENDS, "", 8, Instant.now().minusSeconds(60)) is Outcome.Error)
    }

    @Test fun `hosts edit and cancel their events, and seated people are told`() = runTest {
        val id = (repo.createEvent("Picnic", EventVisibility.FRIENDS, "", 8) as Outcome.Success).value
        assertTrue(repo.editEvent(id, "Big picnic", "Lousberg", "Bring a blanket").isSuccess)
        assertEquals("Big picnic", repo.event(id).first()!!.summary.title)
        assertEquals(Outcome.Denied(DenyReason.NOT_FOUND), repo.cancelEvent("board-game-night")) // not yours
        assertTrue(repo.cancelEvent(id).isSuccess)
        assertEquals(null, repo.event(id).first())
    }

    @Test fun `leaving a seat locks the thread and quietly tells the host`() = runTest {
        repo.takeSeat("board-game-night")
        assertTrue(repo.event("board-game-night").first()!!.thread is EventThreadView.Open)
        repo.leaveSeat("board-game-night")
        assertTrue(repo.event("board-game-night").first()!!.thread is EventThreadView.Locked)
        assertEquals("PK", CircleStore.sentNotices.single().toPersonId)
    }

    @Test fun `the event thread is a real thread - replies, likes, delete your own`() = runTest {
        repo.takeSeat("board-game-night")
        val first = (repo.event("board-game-night").first()!!.thread as EventThreadView.Open).comments.first()
        repo.addEventComment("board-game-night", "Count me in", replyToCommentId = first.id)
        repo.toggleEventCommentLike("board-game-night", first.id)
        val open = repo.event("board-game-night").first()!!.thread as EventThreadView.Open
        val updated = open.comments.first { it.id == first.id }
        assertTrue(updated.likedByMe)
        val mine = updated.replies.single()
        assertNotNull(mine)
        assertTrue(repo.deleteEventComment("board-game-night", mine.id).isSuccess)
        assertTrue(repo.deleteEventComment("board-game-night", first.id) is Outcome.Error)
    }
}
