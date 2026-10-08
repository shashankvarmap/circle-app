package com.circle.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color

/** Id of the signed-in user ("You"). */
const val ME = "S"

data class Person(val id: String, val name: String, val color: Color, val sinceYear: Int) {
    val initials: String
        get() = if (id == ME) ME else name.split(" ").filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }
    val firstName: String get() = name.substringBefore(" ")
}

enum class Audience { FRIENDS, INNER_CIRCLE }

enum class RequestStatus { PENDING, ACCEPTED, DECLINED }

/** Outgoing friend request state, as seen from Contacts Sync and Explore → People. */
enum class FriendStatus { NONE, REQUESTED, ACCEPTED }

/** An incoming friend request. Its status lives in [CircleStore.requestStatus]. */
data class FriendRequest(val personId: String, val mutualText: String)

/** A comment in a thread. Only top-level comments carry [replies]; one layer deep. */
data class Comment(
    val author: String,
    val initials: String,
    val color: Color,
    val time: String,
    val text: String,
    val replies: List<Comment> = emptyList()
)

class Post(
    val id: String,
    val authorId: String,
    val author: String,
    val initials: String,
    val color: Color,
    val time: String,
    val text: String,
    val hasMedia: Boolean = false,
    val audience: Audience = Audience.FRIENDS,
    /** A quote: your text, then this original note embedded under it. */
    val quotedPostId: String? = null,
    /** A repost: a "You reposted" card that shows this original note. */
    val repostOfId: String? = null,
    /** Posted in a group: stays in the group (comment and like only). */
    val groupId: String? = null,
    comments: List<Comment> = emptyList()
) {
    val comments: SnapshotStateList<Comment> = mutableStateListOf<Comment>().apply { addAll(comments) }
    val isRepost: Boolean get() = repostOfId != null
}

class GroupInfo(
    val id: String,
    val name: String,
    val category: String,
    /** Explore kicker topic, e.g. "PHOTOGRAPHY". */
    val topic: String,
    val description: String,
    val color: Color,
    val isPublic: Boolean,
    /** Compass angle on the Explore map; null = not placed on the map (list only). */
    val compassDeg: Float?,
    memberCount: Int,
    isMember: Boolean
) {
    val initial: String get() = name.first().uppercase()
    var memberCount by mutableIntStateOf(memberCount)
    var isMember by mutableStateOf(isMember)
    /** Pending join request — visible to every member, never auto-approved. */
    var joinRequested by mutableStateOf(false)
}

class EventInfo(
    val id: String,
    val title: String,
    val month: String,
    val day: String,
    /** Explore kicker topic, e.g. "OUTDOORS". */
    val topic: String,
    /** Compass angle on the Explore map (inner ring); null = list only. */
    val compassDeg: Float?,
    val whenLabel: String,
    val venue: String,
    val visibility: String,
    val hostId: String,
    val description: String,
    val capacity: Int,
    /** People going other than me. The host is always among them. */
    val goingBase: Int,
    joined: Boolean,
    comments: List<Comment> = emptyList()
) {
    /** I have a seat. */
    var joined by mutableStateOf(joined)
    val going: Int get() = goingBase + if (joined) 1 else 0
    val seatsLeft: Int get() = (capacity - going).coerceAtLeast(0)
    val isFull: Boolean get() = going >= capacity
    val comments: SnapshotStateList<Comment> = mutableStateListOf<Comment>().apply { addAll(comments) }
}
