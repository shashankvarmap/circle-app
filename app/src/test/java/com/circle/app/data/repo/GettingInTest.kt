package com.circle.app.data.repo

import com.circle.app.data.CircleStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** ACCESS_AND_LAUNCH 1-4 through the repository. */
class GettingInTest {
    private lateinit var repo: FakeCircleRepository

    @Before fun setUp() {
        CircleStore.reset()
        repo = FakeCircleRepository()
    }

    @Test fun `any 6 digits verify in the fake, anything else doesn't`() = runTest {
        assertTrue(repo.verifyCode("+919876543210", "123456").isSuccess)
        assertTrue(repo.verifyCode("+919876543210", "12345") is Outcome.Error)
        assertTrue(repo.verifyCode("+919876543210", "12a456") is Outcome.Error)
    }

    @Test fun `taken handles are refused`() = runTest {
        assertFalse(repo.isHandleFree("priya"))
        assertFalse(repo.isHandleFree("elena_voss")) // someone's handle
        assertTrue(repo.isHandleFree("sam.lee"))
    }

    @Test fun `a new account starts empty - no friends, chats, groups or demo feed`() = runTest {
        assertTrue(repo.createAccount("Sam", "sam.lee", "Hello", null).isSuccess)
        assertTrue(repo.homeFeed().first().isEmpty())
        assertEquals(CircleSummary(0, 0), repo.circleSummary().first())
        assertTrue(repo.chats().first().isEmpty())
        assertTrue(repo.memberGroups().first().isEmpty())
        assertEquals("sam.lee", repo.me().first().handle)
        // People already on Circle can still be found and asked.
        assertTrue(repo.searchPeople("Priya").first().isNotEmpty())
        assertTrue(repo.sendFriendRequest("PK").isSuccess)
    }

    @Test fun `logging in opens the existing (demo) account`() = runTest {
        repo.createAccount("Sam", "sam.lee", "", null)
        repo.logIn("+491511234567")
        assertEquals(128, repo.circleSummary().first().friendCount)
    }

    @Test fun `an invite link resolves to that person, never to a friendship`() = runTest {
        val id = (repo.personByHandle("elena_voss") as Outcome.Success).value
        assertEquals("EV", id)
        assertTrue(repo.profile(id).first() is ProfileView.Limited)
        assertEquals(Outcome.Denied(com.circle.app.data.access.DenyReason.NOT_FOUND), repo.personByHandle("nobody_here"))
        assertEquals("circle.app/i/shashank", repo.inviteLink().first())
    }

    @Test fun `contact matching takes only hashes and returns Limited results`() = runTest {
        val r = repo.matchContacts(listOf("a".repeat(64))) as Outcome.Success
        assertTrue(r.value.all { it.handle.isNotEmpty() })
    }

    @Test fun `a data export request is recorded once in the audit log`() = runTest {
        repo.requestDataExport(); repo.requestDataExport()
        assertEquals(1, repo.exportLog().first().size)
    }
}
