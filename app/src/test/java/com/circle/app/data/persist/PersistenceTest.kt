package com.circle.app.data.persist

import com.circle.app.data.CircleStore
import com.circle.app.data.ThemeMode
import com.circle.app.data.repo.FakeCircleRepository
import com.circle.app.data.repo.Outcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Phase 11: the demo survives a restart, Reset demo data, and the debug fail switch. */
class PersistenceTest {

    @get:Rule val tmp = TemporaryFolder()
    private lateinit var repo: FakeCircleRepository
    private lateinit var file: File

    // The seed's times are relative to now; a fixed clock makes two seeds equal.
    private val realNow = com.circle.app.data.Clock.now

    @Before fun setUp() {
        val fixed = java.time.Instant.parse("2026-10-10T08:00:00Z")
        com.circle.app.data.Clock.now = { fixed }
        CircleStore.reset()
        repo = FakeCircleRepository()
        file = File(tmp.root, "store.json")
    }

    @org.junit.After fun tearDown() { com.circle.app.data.Clock.now = realNow }

    @Test fun `the whole store survives a save and load`() = runTest {
        val note = repo.homeFeed().first().first { !it.likedByMe && it.repost !is com.circle.app.data.repo.RepostState.Unavailable }
        repo.toggleLike(note.id)
        repo.post("Saved across restarts", com.circle.app.data.access.Audience.INNER_CIRCLE, null, null, emptyList())
        repo.setTheme(ThemeMode.DAY)
        repo.unfriend(CircleStore.friendIds.first())
        val before = CircleStore.capture()

        StorePersistence(file).save(before)
        CircleStore.reset()
        assertNotEquals(before, CircleStore.capture())

        assertTrue(StorePersistence(file).load())
        assertEquals(before, CircleStore.capture())
        assertEquals(ThemeMode.DAY, repo.theme().first())
        assertTrue(repo.myNotes().first().any { it.text == "Saved across restarts" })
    }

    @Test fun `no file, a broken file or an old version keeps the seed`() {
        val seed = CircleStore.capture()
        assertFalse(StorePersistence(file).load())
        file.writeText("{ not json")
        assertFalse(StorePersistence(file).load())
        StorePersistence(file).save(seed.copy(version = StoreSnapshot.VERSION - 1))
        assertFalse(StorePersistence(file).load())
        assertEquals(seed, CircleStore.capture())
    }

    @Test fun `reset demo data puts the seed back`() = runTest {
        val seed = CircleStore.capture()
        repo.setTheme(ThemeMode.NIGHT)
        repo.unfriend(CircleStore.friendIds.first())
        repo.resetDemoData()
        assertEquals(seed, CircleStore.capture())
    }

    @Test fun `fail switch makes actions fail and loaded lists throw`() = runTest {
        repo.setFailing(true)
        assertTrue(repo.failing().first())
        val chatId = CircleStore.chats.first().id
        val result = repo.sendMessage(chatId, "hello", null)
        assertTrue(result is Outcome.Error)
        assertFalse(CircleStore.chat(chatId)!!.messages.any { it.text == "hello" })
        for (flow in listOf(repo.homeFeed(), repo.chats(), repo.alerts())) {
            try { flow.first(); fail("expected the list to fail") } catch (e: java.io.IOException) { /* the error state */ }
        }

        repo.setFailing(false)
        assertTrue(repo.sendMessage(chatId, "hello", null) is Outcome.Success)
        assertTrue(repo.homeFeed().first().isNotEmpty())
    }
}
