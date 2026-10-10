package com.circle.app.data.repo

import com.circle.app.data.CircleStore
import com.circle.app.data.Seed
import com.circle.app.data.access.Relationship
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Owner fix 2: enough non-friends to ask, Add friend -> Requested -> Cancel, and the demo auto-accept. */
@OptIn(ExperimentalCoroutinesApi::class)
class FriendRequestsTest {

    @Before fun setUp() = CircleStore.reset()

    @Test fun `at least five non-friends appear in contacts, search and a group's members`() = runTest {
        val repo = FakeCircleRepository()
        val strangers = Seed.others.map { it.id }
        assertTrue(strangers.size >= 5)
        strangers.forEach { assertEquals(Relationship.NONE, repo.relationshipTo(it).first()) }
        val contacts = repo.contactMatches().first()
        assertTrue(contacts.map { it.person.id }.containsAll(strangers))
        // Friends in the same list say so (no Add button is drawn for FRIEND).
        assertEquals(Relationship.FRIEND, contacts.first { it.person.id == "PK" }.relationship)
        val search = repo.searchPeople("").first().map { it.person.id }
        assertTrue(search.containsAll(strangers))
        val members = repo.group("photo-walk").first()!!.people!!.members.map { it.id }
        assertTrue(strangers.count { it in members } >= 5)
    }

    @Test fun `Add friend, Requested, Cancel request`() = runTest {
        val repo = FakeCircleRepository()
        repo.sendFriendRequest("HS")
        assertEquals(Relationship.REQUEST_SENT, repo.relationshipTo("HS").first())
        repo.cancelFriendRequest("HS")
        assertEquals(Relationship.NONE, repo.relationshipTo("HS").first())
    }

    @Test fun `a new account can ask Priya, and the request shows as sent`() = runTest {
        val repo = FakeCircleRepository()
        repo.createAccount("Sam", "sam_new", "", null)
        assertEquals(Relationship.NONE, repo.relationshipTo("PK").first())
        repo.sendFriendRequest("PK")
        assertEquals(Relationship.REQUEST_SENT, repo.relationshipTo("PK").first())
    }

    @Test fun `with the demo switch on, they accept after 5 seconds and the friend count rises`() = runTest {
        val repo = FakeCircleRepository(autoAccept = true, demoDispatcher = StandardTestDispatcher(testScheduler))
        val before = repo.circleSummary().first().friendCount
        repo.sendFriendRequest("DW")
        advanceTimeBy(4_900); runCurrent()
        assertEquals(Relationship.REQUEST_SENT, repo.relationshipTo("DW").first())
        advanceTimeBy(200); runCurrent()
        assertEquals(Relationship.FRIEND, repo.relationshipTo("DW").first())
        assertEquals(before + 1, repo.circleSummary().first().friendCount)
    }

    @Test fun `cancelling before they accept keeps them a non-friend`() = runTest {
        val repo = FakeCircleRepository(autoAccept = true, demoDispatcher = StandardTestDispatcher(testScheduler))
        repo.sendFriendRequest("FB")
        repo.cancelFriendRequest("FB")
        advanceTimeBy(6_000); runCurrent()
        assertEquals(Relationship.NONE, repo.relationshipTo("FB").first())
    }

    @Test fun `with the switch off nobody accepts`() = runTest {
        val repo = FakeCircleRepository(demoDispatcher = StandardTestDispatcher(testScheduler))
        repo.sendFriendRequest("NK")
        advanceTimeBy(60_000); runCurrent()
        assertEquals(Relationship.REQUEST_SENT, repo.relationshipTo("NK").first())
    }
}
