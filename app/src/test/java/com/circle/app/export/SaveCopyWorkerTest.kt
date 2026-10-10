package com.circle.app.export

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.circle.app.AppGraph
import com.circle.app.data.CircleStore
import com.circle.app.data.Clock
import com.circle.app.data.lifetime.LifeState
import com.circle.app.data.repo.FakeCircleRepository
import java.time.Duration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** TIMED_GROUPS T4: the Save a copy job: saves to Downloads, remembers it locally, and fails clearly. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SaveCopyWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var repo: FakeCircleRepository

    @Before fun setUp() {
        CircleStore.reset()
        repo = FakeCircleRepository()
        AppGraph.install(repository = repo)
    }

    @After fun tearDown() { Clock.shift = Duration.ZERO }

    private fun run(id: String, media: Boolean = false): ListenableWorker.Result = runBlocking {
        TestListenableWorkerBuilder<SaveCopyWorker>(context)
            .setInputData(workDataOf(SaveCopyWorker.KEY_ID to id, SaveCopyWorker.KEY_MEDIA to media))
            .build().doWork()
    }

    @Test fun `saves the ZIP to Downloads and remembers it on this phone only`() {
        val r = run("project-alps")
        assertTrue("$r", r is ListenableWorker.Result.Success)
        val out = (r as ListenableWorker.Result.Success).outputData
        assertTrue(out.getString(SaveCopyWorker.KEY_NAME)!!.matches(Regex("Circle-Project-Alps-text-\\d{4}-\\d{2}-\\d{2}\\.zip")))
        assertEquals(SaveCopyWorker.WHERE_DOWNLOADS, out.getString(SaveCopyWorker.KEY_WHERE))
        assertTrue(out.getLong(SaveCopyWorker.KEY_SIZE, 0) > 0)
        assertTrue("project-alps" in runBlocking { AppGraph.savedCopies.saved().first() })
        // Nothing about who saved reaches the "server".
        assertTrue(CircleStore.sentNotices.isEmpty())
    }

    @Test fun `a deleted group, no network or a non-member fail with a reason`() {
        runBlocking { repo.jumpTo("exam-prep", LifeState.DELETED) }
        assertEquals(SaveCopyWorker.FAIL_GONE, (run("exam-prep") as ListenableWorker.Result.Failure).outputData.getString(SaveCopyWorker.KEY_FAIL))
        runBlocking { repo.setFailing(true) }
        assertEquals(SaveCopyWorker.FAIL_OFFLINE, (run("project-alps") as ListenableWorker.Result.Failure).outputData.getString(SaveCopyWorker.KEY_FAIL))
        runBlocking { repo.setFailing(false) }
        assertEquals(SaveCopyWorker.FAIL_OTHER, (run("alps-hiking-crew") as ListenableWorker.Result.Failure).outputData.getString(SaveCopyWorker.KEY_FAIL))
    }
}
