package com.circle.app.data.export

import com.circle.app.data.CircleStore
import com.circle.app.data.ChatMessage
import com.circle.app.data.Clock
import com.circle.app.data.access.DenyReason
import com.circle.app.data.lifetime.LifeState
import com.circle.app.data.media.MediaKind
import com.circle.app.data.media.MediaRef
import com.circle.app.data.repo.FakeCircleRepository
import com.circle.app.data.repo.Outcome
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.zip.ZipInputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** TIMED_GROUPS T4: what a saved copy contains, and who may save one. */
class SaveCopyTest {

    private lateinit var repo: FakeCircleRepository

    @Before fun setUp() {
        CircleStore.reset()
        repo = FakeCircleRepository()
    }

    @After fun tearDown() { Clock.shift = Duration.ZERO }

    private fun unzip(bytes: ByteArray): Map<String, String> {
        val out = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { z ->
            while (true) { val e = z.nextEntry ?: break; out[e.name] = z.readBytes().toString(Charsets.UTF_8) }
        }
        return out
    }

    private suspend fun zip(id: String, withMedia: Boolean = false, media: (MediaRef) -> java.io.InputStream? = { null }): Map<String, String> {
        val b = (repo.exportBundle(id, withMedia) as Outcome.Success).value
        val out = ByteArrayOutputStream()
        CopyWriter.write(b, out, "0.1", ZoneOffset.UTC, media)
        return unzip(out.toByteArray())
    }

    @Test fun `a group copy has the manifest, the page, messages and posts, names only`() = runTest {
        val files = zip("project-alps")
        assertEquals(setOf("manifest.json", "index.html", "messages.json", "posts.json"), files.keys)
        assertTrue(files.getValue("posts.json").contains("Draft of the route plan is up."))
        assertTrue(files.getValue("messages.json").contains("Priya Kapoor"))
        assertTrue(files.getValue("index.html").contains("Booked the hut for the 21st."))
        assertTrue(files.getValue("manifest.json").contains("\"kind\": \"group\""))
        // Display names only: no handles, no ids, no phone numbers.
        files.values.forEach { body ->
            assertFalse(body.contains("@"))
            assertFalse(Regex("\\+?\\d[\\d ]{8,}\\d").containsMatchIn(body.replace(Regex("\\d{4}-\\d{2}-\\d{2}T[0-9:.]+Z"), "")))
            assertFalse(body.contains("\"PK\""))
        }
    }

    @Test fun `a chat copy has no posts file`() = runTest {
        val files = zip("alps-trip")
        assertFalse("posts.json" in files)
        assertTrue(files.getValue("messages.json").contains("Booked. Sixteen beds."))
    }

    @Test fun `content from people you blocked is left out`() = runTest {
        repo.block("JL")
        val files = zip("alps-trip")
        assertFalse(files.values.any { it.contains("Sixteen beds") || it.contains("Jonah") })
        assertTrue(files.getValue("messages.json").contains("Route plan is in the group"))
    }

    @Test fun `only members, only temporary, only until deletion`() = runTest {
        assertTrue(repo.exportBundle("pk", false) is Outcome.Denied) // permanent 1:1 chat
        assertEquals(DenyReason.GROUP_NOT_MEMBER, (repo.exportBundle("alps-hiking-crew", false) as Outcome.Denied).reason)
        assertTrue(repo.exportBundle("exam-prep", false) is Outcome.Success) // wrap-up: still allowed
        repo.jumpTo("exam-prep", LifeState.DELETED)
        assertEquals(DenyReason.NOT_FOUND, (repo.exportBundle("exam-prep", false) as Outcome.Denied).reason)
        assertNull(repo.exportPlan("exam-prep").first())
    }

    @Test fun `everything adds media, text only does not, unreadable media is counted not fatal`() = runTest {
        val photo = MediaRef("m1", "content://photo", MediaKind.PHOTO)
        val gone = MediaRef("m2", "content://gone", MediaKind.PHOTO)
        CircleStore.chat("alps-trip")!!.messages.add(ChatMessage("at4", "PK", Clock.now(), media = listOf(photo, gone)))
        val plan = repo.exportPlan("alps-trip").first()!!
        assertEquals(2, plan.mediaCount)
        assertTrue(plan.everythingBytes > plan.textBytes)
        val all = zip("alps-trip", withMedia = true) { if (it.uri == "content://photo") ByteArrayInputStream(byteArrayOf(1, 2, 3)) else null }
        assertTrue("media/001-photo.jpg" in all)
        assertTrue(all.getValue("manifest.json").contains("\"mediaMissing\": 1"))
        assertTrue(zip("alps-trip").keys.none { it.startsWith("media/") })
    }

    @Test fun `offline fails, a full disk throws, the file name is safe`() = runTest {
        repo.setFailing(true)
        assertTrue(repo.exportBundle("alps-trip", false) is Outcome.Error)
        repo.setFailing(false)
        val b = (repo.exportBundle("alps-trip", false) as Outcome.Success).value
        val full = object : OutputStream() { override fun write(b: Int) = throw IOException("ENOSPC") }
        assertTrue(runCatching { CopyWriter.write(b, full, "0.1", ZoneOffset.UTC) }.exceptionOrNull() is IOException)
        assertEquals("Circle-Project-Alps-2026-10-22.zip", CopyWriter.fileName("Project Alps", LocalDate.of(2026, 10, 22), false))
        assertEquals("Circle-Exam-prep-text-2026-10-22.zip", CopyWriter.fileName("Exam prep / ✓", LocalDate.of(2026, 10, 22), true))
    }

    @Test fun `the page escapes what people wrote`() = runTest {
        CircleStore.chat("alps-trip")!!.messages.add(ChatMessage("at5", "PK", Clock.now(), text = "<script>alert(1)</script>"))
        val html = zip("alps-trip").getValue("index.html")
        assertFalse(html.contains("<script>"))
        assertTrue(html.contains("&lt;script&gt;"))
    }
}
