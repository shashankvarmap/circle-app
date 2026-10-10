package com.circle.app.data.repo

import com.circle.app.data.CircleStore
import com.circle.app.data.ME
import com.circle.app.data.MessageKind
import com.circle.app.data.ThemeMode
import com.circle.app.data.access.AccessPolicy
import com.circle.app.data.access.Audience
import com.circle.app.data.access.DenyReason
import com.circle.app.data.access.EventVisibility
import com.circle.app.data.access.Relationship
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The repository: every read and action goes through AccessPolicy and returns redacted types.
 * First the MASTER_PROMPT 2.4 repository cases, then the store behaviour carried over from the
 * old CircleStoreTest (now checked through the repository, the only door the UI has).
 */
class FakeCircleRepositoryTest {

    private lateinit var repo: FakeCircleRepository

    @Before fun setUp() {
        CircleStore.reset()
        repo = FakeCircleRepository()
    }

    private fun <T> Outcome<T>.value(): T = (this as Outcome.Success).value

    // ======================================================================== MASTER_PROMPT 2.4

    @Test fun `getProfile returns Limited for NONE and never Full`() = runTest {
        assertEquals(Relationship.NONE, repo.relationshipTo("EV").first())
        val p = repo.profile("EV").first()
        assertTrue(p is ProfileView.Limited)
        p as ProfileView.Limited
        assertEquals("Elena Voss", p.name)
        assertEquals("Museums on rainy days, the Lousberg on sunny ones.", p.pinnedThought)
        // Pending either way is still Limited.
        repo.sendFriendRequest("EV")
        assertTrue(repo.profile("EV").first() is ProfileView.Limited)
        assertTrue(repo.profile("RS").first() is ProfileView.Limited) // REQUEST_RECEIVED
        // Friends are Full; blocked people don't exist.
        assertTrue(repo.profile("PK").first() is ProfileView.Full)
        repo.block("NK")
        assertEquals(ProfileView.NotFound, repo.profile("NK").first())
    }

    @Test fun `a non-friend's notes are never returned`() = runTest {
        assertTrue(repo.notesBy("EV").first().isEmpty())
        assertTrue(repo.notesBy("RS").first().isEmpty())
        assertFalse(repo.homeFeed().first().any { it.author.id == "EV" })
        assertTrue(repo.notesBy("PK").first().isNotEmpty())
    }

    @Test fun `openThread returns Denied NOT_FRIENDS for a non-friend's note`() = runTest {
        val r = repo.thread("elena-forum").first()
        assertTrue(r is ThreadResult.Denied)
        r as ThreadResult.Denied
        assertEquals(DenyReason.NOT_FRIENDS, r.reason)
        assertEquals("EV", r.author?.id) // who posted it, for the gate; nothing else
    }

    @Test fun `comment, like, repost and quote are Denied for non-friends`() = runTest {
        assertEquals(Outcome.Denied(DenyReason.NOT_FRIENDS), repo.addComment("elena-forum", "hello"))
        assertEquals(Outcome.Denied(DenyReason.NOT_FRIENDS), repo.toggleLike("elena-forum"))
        assertEquals(Outcome.Denied(DenyReason.NOT_FRIENDS), repo.repost("elena-forum"))
        assertEquals(Outcome.Denied(DenyReason.NOT_FRIENDS), repo.post("look", Audience.FRIENDS, quotedNoteId = "elena-forum"))
        val elena = CircleStore.post("elena-forum")!!
        assertTrue(elena.comments.isEmpty())
        assertFalse("elena-forum" in CircleStore.likedPostIds)
        assertFalse(CircleStore.posts.any { it.repostOfId == "elena-forum" || it.quotedPostId == "elena-forum" })
    }

    @Test fun `a shared note from a non-friend is a Snippet of at most 90 characters that can't be opened`() = runTest {
        val chat = repo.chat("pk").first()!!
        val shared = chat.messages.first { it.id == "pk2c" }.sharedNote
        assertTrue(shared is NoteView.Snippet)
        shared as NoteView.Snippet
        assertFalse(shared.canOpen)
        assertTrue(shared.text.length <= AccessPolicy.SNIPPET_MAX)
        assertEquals("Elena Voss", shared.authorName)
        // The friend's shared note opens in full.
        assertTrue(chat.messages.first { it.id == "pk2" }.sharedNote is NoteView.Full)
    }

    @Test fun `unfriend purges cached content and the open thread turns into the gate`() = runTest {
        val opened = repo.thread("amara-puzzle").first()
        assertTrue(opened is ThreadResult.Opened)
        repo.chat("pk").first() // shows Amara's shared note in full
        assertTrue(repo.cache.holdsAnythingBy("AO"))

        repo.unfriend("AO")

        assertFalse(repo.cache.holdsAnythingBy("AO"))
        val after = repo.thread("amara-puzzle").first()
        assertEquals(DenyReason.NOT_FRIENDS, (after as ThreadResult.Denied).reason)
        // The shared card in Priya's chat is now only a snippet.
        assertTrue(repo.chat("pk").first()!!.messages.first { it.id == "pk2" }.sharedNote is NoteView.Snippet)
    }

    @Test fun `block purges cached content and hides the person everywhere`() = runTest {
        repo.chat("pk").first() // snippet of Elena's note is cached
        assertTrue(repo.cache.holdsAnythingBy("EV"))
        repo.block("EV")
        assertFalse(repo.cache.holdsAnythingBy("EV"))
        assertEquals(NoteView.Hidden, repo.chat("pk").first()!!.messages.first { it.id == "pk2c" }.sharedNote)
        val photoWalk = repo.group("photo-walk").first()!!
        assertFalse(photoWalk.people!!.members.any { it.id == "EV" })
        assertFalse(repo.chat("g-photo-walk").first()!!.messages.any { it.sender.id == "EV" })
        assertFalse(repo.searchPeople("Elena").first().any { it.person.id == "EV" })
        val amara = repo.thread("amara-puzzle").first() as ThreadResult.Opened
        assertFalse(amara.thread.comments.any { it.author.id == "EV" })
    }

    @Test fun `someone who blocked you is invisible and can't be found or reached`() = runTest {
        repo.blockedByThem("PK")
        assertEquals(Relationship.BLOCKED_ME, repo.relationshipTo("PK").first())
        assertEquals(ProfileView.NotFound, repo.profile("PK").first())
        assertFalse(repo.searchPeople("Priya").first().any { it.person.id == "PK" })
        assertFalse(repo.homeFeed().first().any { it.author.id == "PK" })
        assertTrue(repo.openDirectChat("PK") is Outcome.Denied)
        assertEquals(Outcome.Denied(DenyReason.BLOCKED), repo.sendFriendRequest("PK"))
    }

    @Test fun `accepting the request opens the same card and thread with no other change`() = runTest {
        assertTrue(repo.chat("pk").first()!!.messages.first { it.id == "pk2c" }.sharedNote is NoteView.Snippet)
        repo.sendFriendRequest("EV")
        assertEquals(Relationship.REQUEST_SENT, repo.relationshipTo("EV").first())
        repo.acceptedByThem("EV")
        assertTrue(repo.chat("pk").first()!!.messages.first { it.id == "pk2c" }.sharedNote is NoteView.Full)
        assertTrue(repo.thread("elena-forum").first() is ThreadResult.Opened)
        assertTrue(repo.homeFeed().first().any { it.id == "elena-forum" })
    }

    @Test fun `friend graph and counts are only ever returned to the owner`() = runTest {
        // No API returns another person's friends or counts; a friend's Full profile has no such field.
        val full = repo.profile("PK").first() as ProfileView.Full
        assertEquals(Relationship.FRIEND, full.relationship)
        assertEquals(CircleSummary(128, 5), repo.circleSummary().first())
    }

    @Test fun `deep link to a group post you're not a member of is denied`() = runTest {
        assertEquals(DenyReason.GROUP_NOT_MEMBER, (repo.thread("group-catan").first() as ThreadResult.Denied).reason)
        assertTrue(repo.thread("group-golden-hour").first() is ThreadResult.Opened)
    }

    @Test fun `alerts never carry a comment's text to someone who can't read the thread`() = runTest {
        assertTrue(repo.alerts().first().any { it.quote == "Where was this?" })
        assertTrue(repo.activity().first().all { it.actor.id in CircleStore.friendIds })
    }

    // ======================================================================== friend graph

    @Test fun `seed has 128 friends, 5 inner, and Elena is not a friend`() = runTest {
        assertEquals(Relationship.NONE, repo.relationshipTo("EV").first())
        assertEquals(CircleSummary(128, 5), repo.circleSummary().first())
        assertEquals(128, repo.friends().first().size)
    }

    @Test fun `relationship covers every case`() = runTest {
        assertEquals(Relationship.SELF, repo.relationshipTo(ME).first())
        assertEquals(Relationship.FRIEND, repo.relationshipTo("PK").first())
        assertEquals(Relationship.REQUEST_RECEIVED, repo.relationshipTo("RS").first())
        assertEquals(Relationship.NONE, repo.relationshipTo("EV").first())
        repo.sendFriendRequest("LB")
        assertEquals(Relationship.REQUEST_SENT, repo.relationshipTo("LB").first())
        repo.block("NK")
        assertEquals(Relationship.BLOCKED_BY_ME, repo.relationshipTo("NK").first())
        repo.blockedByThem("ML")
        assertEquals(Relationship.BLOCKED_ME, repo.relationshipTo("ML").first())
    }

    @Test fun `accepting a request increments the count and keeps the row`() = runTest {
        repo.acceptRequest("RS")
        assertEquals(129, repo.circleSummary().first().friendCount)
        val row = repo.incomingRequests().first().first { it.person.id == "RS" }
        assertTrue(row.accepted)
        repo.acceptRequest("RS") // twice counts once
        assertEquals(129, repo.circleSummary().first().friendCount)
    }

    @Test fun `declining removes the row and a declined request can't be accepted later`() = runTest {
        repo.declineRequest("MD")
        assertFalse(repo.incomingRequests().first().any { it.person.id == "MD" })
        assertTrue(repo.acceptRequest("MD") is Outcome.Error)
        assertEquals(128, repo.circleSummary().first().friendCount)
    }

    @Test fun `sending a request moves NONE to REQUEST_SENT and cancelling returns it`() = runTest {
        assertTrue(repo.sendFriendRequest("EV").isSuccess)
        assertEquals(Relationship.REQUEST_SENT, repo.relationshipTo("EV").first()) // never instant
        repo.cancelFriendRequest("EV")
        assertEquals(Relationship.NONE, repo.relationshipTo("EV").first())
    }

    @Test fun `only friends can be messaged`() = runTest {
        assertEquals(Outcome.Denied(DenyReason.NOT_FRIENDS), repo.openDirectChat("EV"))
        assertEquals("pk", repo.openDirectChat("PK").value())
        val id = repo.openDirectChat("EK").value()
        assertEquals("EK", repo.chat(id).first()!!.person!!.id)
    }

    @Test fun `inner-circle notes can't be shared to non-inner friends or non-friends`() = runTest {
        val inner = repo.post("for the five of you", Audience.INNER_CIRCLE).value()
        val toFriend = repo.openDirectChat("EK").value() // Elin is a friend, not inner
        assertTrue(repo.shareNote("jl", inner).isSuccess) // Jonah is in your inner circle
        assertEquals(Outcome.Denied(DenyReason.INNER_CIRCLE), repo.shareNote(toFriend, inner))
        assertEquals(Outcome.Denied(DenyReason.INNER_CIRCLE), repo.shareNote("g-photo-walk", inner))
    }

    @Test fun `group posts stay in their group and a friend's note can be shared`() = runTest {
        assertEquals(Outcome.Denied(DenyReason.GROUP_POST), repo.shareNote("pk", "group-golden-hour"))
        assertTrue(repo.shareNote("g-photo-walk", "group-golden-hour").isSuccess)
        assertTrue(repo.shareNote("ao", "tomas-tomatoes").isSuccess)
        assertEquals(MessageKind.SHARED_NOTE, repo.chat("ao").first()!!.messages.last().kind)
    }

    @Test fun `friends-only events need the host as a friend`() = runTest {
        assertNotNull(repo.event("board-game-night").first()) // friends of friends
        repo.unfriend("JL")
        assertEquals(null, repo.event("open-mic").first()) // Jonah's friends-only event
        assertFalse(repo.explore().first().events.any { it.id == "open-mic" })
    }

    // ======================================================================== inner circle

    @Test fun `toggle inner moves a friend both ways and non-friends can't be inner`() = runTest {
        assertEquals(true, repo.toggleInnerCircle("EK").value())
        assertEquals(6, repo.circleSummary().first().innerCount)
        assertEquals(false, repo.toggleInnerCircle("EK").value())
        assertEquals(Outcome.Denied(DenyReason.NOT_FRIENDS), repo.toggleInnerCircle("RS"))
    }

    // ======================================================================== notes

    @Test fun `like is a private toggle`() = runTest {
        assertEquals(true, repo.toggleLike("amara-puzzle").value())
        assertTrue(repo.homeFeed().first().first { it.id == "amara-puzzle" }.likedByMe)
        assertEquals(false, repo.toggleLike("amara-puzzle").value())
    }

    @Test fun `posting goes to the top with its audience, blank is refused`() = runTest {
        val id = repo.post("  Inner thoughts  ", Audience.INNER_CIRCLE).value()
        val top = repo.myNotes().first().first()
        assertEquals(id, top.id)
        assertEquals("Inner thoughts", top.text)
        assertEquals(Audience.INNER_CIRCLE, top.audience)
        assertTrue(repo.post("   ", Audience.FRIENDS) is Outcome.Error)
    }

    @Test fun `repost adds a card at the top, once, and undo removes it`() = runTest {
        repo.repost("amara-puzzle")
        val top = repo.homeFeed().first().first()
        assertEquals("amara-puzzle", top.actionTargetId)
        assertEquals(RepostState.Reposted, top.repost)
        repo.repost("amara-puzzle")
        assertEquals(1, CircleStore.posts.count { it.repostOfId == "amara-puzzle" })
        repo.undoRepost("amara-puzzle")
        assertFalse(CircleStore.posts.any { it.repostOfId == "amara-puzzle" })
    }

    @Test fun `quoting or reposting a repost card acts on the original`() = runTest {
        val q = repo.post("Agreed", Audience.FRIENDS, quotedNoteId = "you-repost-seaglass").value()
        assertEquals("priya-seaglass", CircleStore.post(q)!!.quotedPostId)
    }

    @Test fun `inner-circle and group notes can't be reposted or quoted`() = runTest {
        val inner = repo.post("just for you five", Audience.INNER_CIRCLE).value()
        assertEquals(Outcome.Denied(DenyReason.INNER_CIRCLE), repo.repost(inner))
        assertEquals(Outcome.Denied(DenyReason.INNER_CIRCLE), repo.post("look", Audience.FRIENDS, quotedNoteId = inner))
        assertEquals(RepostState.Unavailable(DenyReason.INNER_CIRCLE), repo.myNotes().first().first { it.id == inner }.repost)
        assertEquals(Outcome.Denied(DenyReason.GROUP_POST), repo.repost("group-golden-hour"))
        assertEquals(Outcome.Denied(DenyReason.GROUP_POST), repo.post("look", Audience.FRIENDS, quotedNoteId = "group-golden-hour"))
    }

    @Test fun `quote card sits at the top of Home and Profile`() = runTest {
        val q = repo.post("So true", Audience.FRIENDS, quotedNoteId = "tomas-tomatoes").value()
        assertEquals(q, repo.homeFeed().first().first().id)
        assertEquals(q, repo.myNotes().first().first().id)
        assertTrue(repo.homeFeed().first().first().quoted is NoteView.Full)
    }

    @Test fun `home Groups tab only shows groups you're in, and you can only post to those`() = runTest {
        val feed = repo.groupFeed().first()
        assertTrue(feed.all { it.group?.id in setOf("photo-walk", "project-alps") })
        assertFalse(repo.homeFeed().first().any { it.group != null })
        assertEquals(Outcome.Denied(DenyReason.GROUP_NOT_MEMBER), repo.post("hi", Audience.FRIENDS, groupId = "board-game-club"))
        val id = repo.post("hi", Audience.FRIENDS, groupId = "photo-walk").value()
        assertEquals(id, repo.groupPosts("photo-walk").first().first().id)
        assertTrue(repo.groupPosts("board-game-club").first().isEmpty())
    }

    // ======================================================================== events & groups

    @Test fun `taking a seat counts you, leaving undoes it, and you can't join past capacity`() = runTest {
        assertEquals(4, repo.event("trail-brunch").first()!!.summary.going)
        repo.takeSeat("trail-brunch")
        assertEquals(5, repo.event("trail-brunch").first()!!.summary.going)
        repo.leaveSeat("trail-brunch")
        assertEquals(4, repo.event("trail-brunch").first()!!.summary.going)

        val tiny = repo.createEvent("Tiny dinner", EventVisibility.FRIENDS, "", capacity = 1).value()
        val v = repo.event(tiny).first()!!
        assertTrue(v.isFull && v.hasSeat) // the host fills it and always has a seat

        CircleStore.events.add(
            com.circle.app.data.EventInfo(
                "full", "Full", "SOCIAL", 0f, java.time.Instant.now(), "", EventVisibility.OPEN_NEARBY, "PK", "",
                capacity = 3, goingBase = 3, joined = false
            )
        )
        assertEquals(Outcome.Error("full"), repo.takeSeat("full"))
        assertFalse(repo.event("full").first()!!.hasSeat)
        assertEquals(3, repo.event("full").first()!!.summary.going)
    }

    @Test fun `event thread is locked until you take a seat`() = runTest {
        assertTrue(repo.event("board-game-night").first()!!.thread is EventThreadView.Locked)
        assertEquals(Outcome.Denied(DenyReason.EVENT_LOCKED), repo.addEventComment("board-game-night", "hello"))
        repo.takeSeat("board-game-night")
        assertTrue(repo.addEventComment("board-game-night", "hello").isSuccess)
        val open = repo.event("board-game-night").first()!!.thread as EventThreadView.Open
        assertEquals("hello", open.comments.last().text)
    }

    @Test fun `request to join is never instant and members-only content stays closed`() = runTest {
        repo.requestToJoin("board-game-club")
        val g = repo.group("board-game-club").first()!!
        assertTrue(g.joinRequested)
        assertFalse(g.isMember)
        assertEquals(null, g.people) // people are for members only
        assertEquals(listOf("photo-walk", "project-alps"), repo.memberGroups().first().map { it.id })
    }

    // ======================================================================== profile & settings

    @Test fun `saveProfile validates the handle and keeps the pinned thought as the bio`() = runTest {
        assertEquals(Outcome.Error("Lowercase letters, numbers and _ only"), repo.saveProfile("Sam", "sam smith", "hi", null))
        assertEquals(Outcome.Error("At least 3 characters"), repo.saveProfile("Sam", "sa", "hi", null))
        assertEquals(Outcome.Error("Add a display name"), repo.saveProfile("  ", "sam", "hi", null))
        assertTrue(repo.saveProfile(" Sam ", "@Sam_1", " Out walking. ", null).isSuccess)
        val me = repo.me().first()
        assertEquals("Sam", me.displayName)
        assertEquals("sam_1", me.handle)
        assertEquals("Out walking.", me.pinnedThought)
    }

    @Test fun `blocking ends the friendship and inner circle, unblock lifts it`() = runTest {
        repo.block("AO")
        assertEquals(Relationship.BLOCKED_BY_ME, repo.relationshipTo("AO").first())
        assertEquals(CircleSummary(127, 4), repo.circleSummary().first())
        assertTrue(repo.blocked().first().any { it.id == "AO" })
        repo.unblock("AO")
        assertEquals(Relationship.NONE, repo.relationshipTo("AO").first()) // the friendship doesn't come back
    }

    @Test fun `only friends can be muted`() = runTest {
        assertEquals(Outcome.Denied(DenyReason.NOT_FRIENDS), repo.mute("RS"))
        repo.mute("PK")
        assertTrue(repo.muted().first().any { it.id == "PK" })
        assertFalse(repo.homeFeed().first().any { it.author.id == "PK" })
    }

    @Test fun `reset restores defaults`() = runTest {
        repo.setTheme(ThemeMode.DAY)
        repo.saveProfile("Sam", "sam", "", null)
        repo.deleteAccount()
        assertEquals(ThemeMode.SYSTEM, repo.theme().first())
        assertEquals("shashank", repo.me().first().handle)
    }

    // ======================================================================== chats

    @Test fun `delete for me hides it only on your side, unsend only your own`() = runTest {
        repo.deleteForMe("pk", "pk5")
        assertFalse(repo.chat("pk").first()!!.messages.any { it.id == "pk5" })
        assertTrue(CircleStore.chat("pk")!!.messages.any { it.id == "pk5" }) // the other person still has it
        assertTrue(repo.unsend("pk", "pk7") is Outcome.Error) // Priya's
        assertTrue(repo.unsend("pk", "pk6").isSuccess)
        assertFalse(CircleStore.chat("pk")!!.messages.any { it.id == "pk6" })
    }

    @Test fun `sending a reply keeps the quoted message and is added at once`() = runTest {
        repo.sendMessage("jl", " See you there ", replyToId = "jl2")
        val last = repo.chat("jl").first()!!.messages.last()
        assertEquals("See you there", last.text)
        assertEquals("jl2", last.replyTo?.messageId)
        assertTrue(repo.sendMessage("jl", "   ") is Outcome.Error)
    }

    @Test fun `previews mark your own and group senders`() = runTest {
        repo.sendMessage("jl", "On my way")
        val rows = repo.chats().first()
        assertEquals(ChatPreview(true, null, MessageKind.TEXT, "On my way"), rows.first { it.id == "jl" }.preview)
        assertEquals("Tomas", rows.first { it.id == "g-photo-walk" }.preview!!.senderFirstName)
        assertEquals(null, rows.first { it.id == "pk" }.preview!!.senderFirstName)
    }

    @Test fun `group chats only for groups you're in`() = runTest {
        val groupChats = repo.chats().first().filter { it.kind == ChatKind.GROUP }
        assertEquals(setOf("g-photo-walk", "g-project-alps"), groupChats.map { it.id }.toSet())
        repo.leaveGroup("photo-walk")
        assertTrue(repo.chats().first().none { it.id == "g-photo-walk" })
        assertEquals(null, repo.chat("g-photo-walk").first())
    }

    @Test fun `seen shows only while your read receipts are on`() = runTest {
        assertNotNull(repo.chat("jl").first()!!.messages.first { it.id == "jl1" }.seenAt)
        repo.setPrivacy(CircleStore.privacy.copy(readReceipts = false))
        assertEquals(null, repo.chat("jl").first()!!.messages.first { it.id == "jl1" }.seenAt)
    }
}
