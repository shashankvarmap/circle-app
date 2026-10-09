package com.circle.app.data.repo

import com.circle.app.data.CircleStore
import com.circle.app.data.ME
import com.circle.app.data.access.Audience
import com.circle.app.data.access.DenyReason
import com.circle.app.data.media.MediaKind
import com.circle.app.data.media.MediaRef
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Phase F1: threads (reply to a reply, comment likes, delete), the ⋯ menu actions and media. */
class PostsAndThreadsTest {
    private lateinit var repo: FakeCircleRepository

    @Before fun setUp() {
        CircleStore.reset()
        repo = FakeCircleRepository()
    }

    private suspend fun opened(id: String) = (repo.thread(id).first() as ThreadResult.Opened).thread

    @Test fun `a reply nests under the comment, and a reply to a reply joins the same comment`() = runTest {
        val priya = opened("amara-puzzle").comments.first { it.author.id == "PK" }
        assertEquals(2, priya.replies.size)
        assertTrue(repo.addComment("amara-puzzle", "Frame it twice", replyToCommentId = priya.id).isSuccess)
        val reply = opened("amara-puzzle").comments.first { it.id == priya.id }.replies
        assertEquals(3, reply.size)
        assertEquals("Frame it twice", reply.last().text)
        // Replying to a reply stays one level deep, under the same top-level comment.
        repo.addComment("amara-puzzle", "Agreed", replyToCommentId = reply.first().id)
        val after = opened("amara-puzzle")
        assertEquals(4, after.comments.first { it.id == priya.id }.replies.size)
        assertEquals(3, after.comments.size) // no new top-level comment
    }

    @Test fun `comment likes are private toggles, and nobody else's thread takes them`() = runTest {
        val c = opened("amara-puzzle").comments.first()
        assertEquals(true, (repo.toggleCommentLike("amara-puzzle", c.id) as Outcome.Success).value)
        assertTrue(opened("amara-puzzle").comments.first().likedByMe)
        repo.toggleCommentLike("amara-puzzle", c.id)
        assertFalse(opened("amara-puzzle").comments.first().likedByMe)
        assertEquals(Outcome.Denied(DenyReason.NOT_FRIENDS), repo.toggleCommentLike("elena-forum", "anything"))
        assertEquals(Outcome.Denied(DenyReason.NOT_FRIENDS), repo.addComment("elena-forum", "hi", replyToCommentId = "c1"))
    }

    @Test fun `you can delete only your own comments`() = runTest {
        repo.addComment("amara-puzzle", "mine")
        val mine = opened("amara-puzzle").comments.last()
        assertTrue(mine.isMine)
        assertTrue(repo.deleteComment("amara-puzzle", opened("amara-puzzle").comments.first().id) is Outcome.Error)
        assertTrue(repo.deleteComment("amara-puzzle", mine.id).isSuccess)
        assertFalse(opened("amara-puzzle").comments.any { it.id == mine.id })
    }

    @Test fun `edit, change audience and delete work on your notes only`() = runTest {
        assertEquals(Outcome.Denied(DenyReason.NOT_FOUND), repo.editNote("tomas-tomatoes", "hacked"))
        assertEquals(Outcome.Denied(DenyReason.NOT_FOUND), repo.deleteNote("tomas-tomatoes"))
        assertEquals(Outcome.Denied(DenyReason.NOT_FOUND), repo.setAudience("tomas-tomatoes", Audience.INNER_CIRCLE))

        assertTrue(repo.editNote("you-plants", "Repotted everything.").isSuccess)
        assertEquals("Repotted everything.", repo.myNotes().first().first { it.id == "you-plants" }.text)

        assertTrue(repo.setAudience("you-plants", Audience.INNER_CIRCLE).isSuccess)
        assertEquals(Audience.INNER_CIRCLE, repo.myNotes().first().first { it.id == "you-plants" }.audience)

        assertTrue(repo.deleteNote("you-plants").isSuccess)
        assertFalse(repo.myNotes().first().any { it.id == "you-plants" })
    }

    @Test fun `making a note inner-circle removes its reposts`() = runTest {
        repo.repost("you-laptop") // reposting your own friends note
        assertTrue(CircleStore.posts.any { it.repostOfId == "you-laptop" })
        repo.setAudience("you-laptop", Audience.INNER_CIRCLE)
        assertFalse(CircleStore.posts.any { it.repostOfId == "you-laptop" })
    }

    @Test fun `send in a message offers only friends the note may go to`() = runTest {
        assertEquals(128, repo.shareCandidates("tomas-tomatoes").first().size)
        val inner = (repo.post("five of you", Audience.INNER_CIRCLE) as Outcome.Success).value
        val innerCandidates = repo.shareCandidates(inner).first().map { it.id }.toSet()
        assertEquals(CircleStore.innerCircle.toSet(), innerCandidates)
        assertTrue(repo.shareCandidates("elena-forum").first().isEmpty())
        assertTrue(repo.shareCandidates("group-golden-hour").first().isEmpty())
    }

    @Test fun `sending a note to a friend lands in your 1-1 chat, refused ones create no chat`() = runTest {
        val chatId = (repo.shareNoteWithFriend("tomas-tomatoes", "EK") as Outcome.Success).value
        assertTrue(repo.chat(chatId).first()!!.messages.last().sharedNote is NoteView.Full)
        val inner = (repo.post("five of you", Audience.INNER_CIRCLE) as Outcome.Success).value
        val before = CircleStore.chats.size
        assertEquals(Outcome.Denied(DenyReason.INNER_CIRCLE), repo.shareNoteWithFriend(inner, "IM")) // Ines isn't inner
        assertEquals(before, CircleStore.chats.size)
    }

    @Test fun `a photo-only note can be posted and keeps its media`() = runTest {
        val m = MediaRef("m1", "content://p/1", MediaKind.PHOTO)
        val id = (repo.post("", Audience.FRIENDS, media = listOf(m)) as Outcome.Success).value
        val note = repo.myNotes().first().first { it.id == id }
        assertEquals(listOf(m), note.media)
        assertTrue(note.hasMedia)
        assertTrue(repo.post("", Audience.FRIENDS) is Outcome.Error) // nothing at all is refused
    }

    @Test fun `media of a note you can't see never reaches you`() = runTest {
        assertEquals(NoteView.Hidden, repo.note("group-catan").first()) // not a member
        assertTrue(repo.note("elena-forum").first() is NoteView.Snippet) // snippet: no media field at all
        assertEquals(ME, (repo.note("you-media").first() as NoteView.Full).note.author.id)
    }

    @Test fun `a report records the reason and note`() = runTest {
        repo.report(com.circle.app.data.ReportKind.PERSON, "EV", com.circle.app.data.ReportReason.SPAM, "  sells things ")
        val r = CircleStore.reports.single()
        assertEquals(com.circle.app.data.ReportReason.SPAM, r.reason)
        assertEquals("sells things", r.note)
        assertEquals("EV", r.targetId)
    }
}
