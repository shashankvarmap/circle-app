package com.circle.app.data.repo

import androidx.compose.runtime.snapshots.Snapshot
import com.circle.app.data.CircleStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Observations re-evaluate when a relationship changes (MASTER_PROMPT 2.3), without re-subscribing. */
@OptIn(ExperimentalCoroutinesApi::class)
class FlowReactivityTest {
    private lateinit var repo: FakeCircleRepository

    @Before fun setUp() {
        CircleStore.reset()
        repo = FakeCircleRepository()
    }

    @Test fun `a chat's shared card opens as soon as the request is accepted`() = runTest(UnconfinedTestDispatcher()) {
        val seen = mutableListOf<NoteView?>()
        val job = launch { repo.chat("pk").collect { c -> seen += c?.messages?.first { it.id == "pk2c" }?.sharedNote } }
        assertTrue(seen.last() is NoteView.Snippet)
        repo.sendFriendRequest("EV")
        Snapshot.sendApplyNotifications(); advanceUntilIdle()
        repo.acceptedByThem("EV")
        Snapshot.sendApplyNotifications(); advanceUntilIdle()
        assertTrue("emissions: ${seen.map { it?.javaClass?.simpleName }}", seen.last() is NoteView.Full)
        job.cancel()
    }

    @Test fun `an open thread closes when you unfriend`() = runTest(UnconfinedTestDispatcher()) {
        val seen = mutableListOf<ThreadResult>()
        val job = launch { repo.thread("amara-puzzle").collect { seen += it } }
        repo.unfriend("AO")
        Snapshot.sendApplyNotifications(); advanceUntilIdle()
        assertTrue(seen.last() is ThreadResult.Denied)
        job.cancel()
    }
}
