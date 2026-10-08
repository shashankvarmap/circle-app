package com.circle.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CircleStoreTest {

    @Before
    fun setUp() = CircleStore.reset()

    // ---- Friend graph ----

    @Test
    fun seedHas128FriendsAnd5InInnerCircle() {
        assertEquals(128, CircleStore.friendCount)
        assertEquals(5, CircleStore.innerCount)
    }

    @Test
    fun acceptingARequestIncrementsFriendCountAndKeepsTheRow() {
        CircleStore.acceptRequest("RS")
        assertEquals(129, CircleStore.friendCount)
        assertTrue(CircleStore.isFriend("RS"))
        assertEquals(RequestStatus.ACCEPTED, CircleStore.statusOfRequest("RS"))
        assertTrue(CircleStore.visibleRequests.any { it.personId == "RS" })
    }

    @Test
    fun acceptingTwiceCountsOnce() {
        CircleStore.acceptRequest("RS")
        CircleStore.acceptRequest("RS")
        assertEquals(129, CircleStore.friendCount)
    }

    @Test
    fun decliningRemovesTheRowAndKeepsTheCount() {
        CircleStore.declineRequest("MD")
        assertEquals(128, CircleStore.friendCount)
        assertFalse(CircleStore.visibleRequests.any { it.personId == "MD" })
        CircleStore.acceptRequest("MD") // a declined request can't be accepted later
        assertFalse(CircleStore.isFriend("MD"))
    }

    @Test
    fun outgoingRequestStaysRequestedUntilCancelled() {
        CircleStore.sendFriendRequest("Lukas Brandt")
        assertEquals(FriendStatus.REQUESTED, CircleStore.statusOf("Lukas Brandt"))
        CircleStore.cancelFriendRequest("Lukas Brandt")
        assertEquals(FriendStatus.NONE, CircleStore.statusOf("Lukas Brandt"))
    }

    // ---- Inner circle ----

    @Test
    fun toggleInnerMovesAFriendBothWays() {
        assertFalse(CircleStore.isInner("EK"))
        CircleStore.toggleInner("EK")
        assertTrue(CircleStore.isInner("EK"))
        assertEquals(6, CircleStore.innerCount)
        CircleStore.toggleInner("EK")
        assertFalse(CircleStore.isInner("EK"))
        assertEquals(5, CircleStore.innerCount)
    }

    @Test
    fun nonFriendsCannotBeInTheInnerCircle() {
        CircleStore.toggleInner("RS")
        assertFalse(CircleStore.isInner("RS"))
    }

    // ---- Likes & posts ----

    @Test
    fun likeIsAPrivateToggle() {
        assertFalse(CircleStore.isLiked("amara-puzzle"))
        CircleStore.toggleLike("amara-puzzle")
        assertTrue(CircleStore.isLiked("amara-puzzle"))
        CircleStore.toggleLike("amara-puzzle")
        assertFalse(CircleStore.isLiked("amara-puzzle"))
    }

    @Test
    fun addPostGoesToTheTopWithItsAudience() {
        val p = CircleStore.addPost("  Inner thoughts  ", Audience.INNER_CIRCLE)!!
        assertEquals(p, CircleStore.posts.first())
        assertEquals("Inner thoughts", p.text)
        assertEquals(Audience.INNER_CIRCLE, p.audience)
        assertTrue(p in CircleStore.myPosts)
    }

    @Test
    fun blankPostIsIgnored() {
        val before = CircleStore.posts.size
        assertNull(CircleStore.addPost("   ", Audience.FRIENDS))
        assertEquals(before, CircleStore.posts.size)
    }

    // ---- Events ----

    @Test
    fun takingASeatCountsYouAndLeavingUndoesIt() {
        val e = CircleStore.event("trail-brunch")
        assertEquals(4, e.going)
        assertTrue(CircleStore.joinEvent(e))
        assertEquals(5, e.going)
        CircleStore.leaveEvent(e)
        assertEquals(4, e.going)
    }

    @Test
    fun cannotJoinPastCapacity() {
        val full = CircleStore.createEvent("Tiny dinner", "", "Friends", "", capacity = 1) // the host fills it
        assertTrue(full.isFull)
        assertTrue(CircleStore.hasSeat(full)) // the host always has a seat
        val e = EventInfo(
            "full", "Full", "SEP", "1", "SOCIAL", 0f, "", "", "FRIENDS", "PK", "",
            capacity = 3, goingBase = 3, joined = false
        )
        assertFalse(CircleStore.joinEvent(e))
        assertFalse(e.joined)
        assertEquals(3, e.going)
    }

    @Test
    fun eventThreadIsLockedUntilYouTakeASeat() {
        val e = CircleStore.event("board-game-night")
        val before = e.comments.size
        CircleStore.addEventComment(e, "hello")
        assertEquals(before, e.comments.size)
        CircleStore.joinEvent(e)
        CircleStore.addEventComment(e, "hello")
        assertEquals(before + 1, e.comments.size)
    }

    // ---- Groups ----

    @Test
    fun requestToJoinIsNeverInstant() {
        val g = CircleStore.group("board-game-club")
        CircleStore.requestToJoin(g)
        assertTrue(g.joinRequested)
        assertFalse(g.isMember)
    }

    @Test
    fun onlyPhotoWalkStartsAsMember() {
        assertEquals(listOf("photo-walk"), CircleStore.groups.filter { it.isMember }.map { it.id })
    }

    // ---- Repost & quote ----

    @Test
    fun repostAddsAYouRepostedCardAtTheTopAndUndoRemovesIt() {
        assertTrue(CircleStore.repost("amara-puzzle"))
        val top = CircleStore.homeFeed.first()
        assertEquals("amara-puzzle", top.repostOfId)
        assertTrue(top in CircleStore.myPosts)
        assertTrue(CircleStore.isReposted("amara-puzzle"))
        assertFalse(CircleStore.repost("amara-puzzle")) // only once
        CircleStore.undoRepost("amara-puzzle")
        assertFalse(CircleStore.isReposted("amara-puzzle"))
        assertFalse(CircleStore.posts.any { it.repostOfId == "amara-puzzle" })
    }

    @Test
    fun repostingARepostCardRepostsTheOriginal() {
        CircleStore.undoRepost("priya-seaglass")
        CircleStore.repost("amara-puzzle")
        val card = CircleStore.homeFeed.first()
        assertEquals("amara-puzzle", CircleStore.original(card).id)
    }

    @Test
    fun innerCircleNotesCannotBeRepostedOrQuoted() {
        val inner = CircleStore.addPost("just for you five", Audience.INNER_CIRCLE)!!
        assertEquals("Inner circle notes can't be reposted", CircleStore.repostBlockReason(inner))
        assertFalse(CircleStore.repost(inner.id))
        assertNull(CircleStore.addPost("look", Audience.FRIENDS, quotedPostId = inner.id))
    }

    @Test
    fun groupPostsCannotBeRepostedOrQuoted() {
        assertEquals("Group posts stay in the group", CircleStore.repostBlockReason(CircleStore.post("group-golden-hour")!!))
        assertFalse(CircleStore.repost("group-golden-hour"))
        assertNull(CircleStore.addPost("look", Audience.FRIENDS, quotedPostId = "group-golden-hour"))
    }

    @Test
    fun quoteCreatesAQuoteCardAtTheTopOfHomeAndProfile() {
        val q = CircleStore.addPost("So true", Audience.FRIENDS, quotedPostId = "tomas-tomatoes")!!
        assertEquals(q, CircleStore.homeFeed.first())
        assertEquals("tomas-tomatoes", q.quotedPostId)
        assertTrue(q in CircleStore.myPosts)
    }

    @Test
    fun quotingARepostCardQuotesTheOriginal() {
        val q = CircleStore.addPost("Agreed", Audience.FRIENDS, quotedPostId = "you-repost-seaglass")!!
        assertEquals("priya-seaglass", q.quotedPostId)
    }

    @Test
    fun homeGroupsTabOnlyShowsGroupsYouAreIn() {
        assertTrue(CircleStore.groupFeed.all { it.groupId == "photo-walk" })
        assertFalse(CircleStore.groupFeed.any { it.id == "group-catan" })
        assertFalse(CircleStore.homeFeed.any { it.groupId != null })
    }

    @Test
    fun youCanOnlyPostToGroupsYouAreIn() {
        assertNull(CircleStore.addPost("hi", Audience.FRIENDS, groupId = "board-game-club"))
        val p = CircleStore.addPost("hi", Audience.FRIENDS, groupId = "photo-walk")!!
        assertEquals(p, CircleStore.postsInGroup("photo-walk").first())
        assertFalse(p in CircleStore.homeFeed)
    }

    // ---- Profile, settings & safety ----

    @Test
    fun saveProfileValidatesTheHandleAndKeepsThePinnedThoughtAsTheBio() {
        assertEquals("Lowercase letters, numbers and _ only", CircleStore.saveProfile("Sam", "sam smith", "hi", null))
        assertEquals("At least 3 characters", CircleStore.saveProfile("Sam", "sa", "hi", null))
        assertEquals("Add a display name", CircleStore.saveProfile("  ", "sam", "hi", null))
        assertNull(CircleStore.saveProfile(" Sam ", "@Sam_1", " Out walking. ", null))
        assertEquals("Sam", CircleStore.profile.displayName)
        assertEquals("sam_1", CircleStore.profile.handle)
        assertEquals("Out walking.", CircleStore.profile.pinnedThought)
        assertEquals("S", CircleStore.profile.initial)
    }

    @Test
    fun blockingRemovesTheFriendshipAndInnerCircle() {
        CircleStore.block("AO")
        assertFalse(CircleStore.isFriend("AO"))
        assertFalse(CircleStore.isInner("AO"))
        assertEquals(127, CircleStore.friendCount)
        assertTrue("AO" in CircleStore.blockedIds)
        CircleStore.unblock("AO")
        assertFalse("AO" in CircleStore.blockedIds)
    }

    @Test
    fun onlyFriendsCanBeMuted() {
        CircleStore.mute("RS")
        assertFalse("RS" in CircleStore.mutedIds)
        CircleStore.mute("PK")
        assertTrue("PK" in CircleStore.mutedIds)
    }

    @Test
    fun resetRestoresDefaults() {
        CircleStore.theme = ThemeMode.DAY
        CircleStore.saveProfile("Sam", "sam", "", null)
        CircleStore.reset()
        assertEquals(ThemeMode.SYSTEM, CircleStore.theme)
        assertEquals("shashank", CircleStore.profile.handle)
    }
}
