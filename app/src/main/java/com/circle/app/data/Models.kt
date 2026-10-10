package com.circle.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import com.circle.app.data.access.Audience
import com.circle.app.data.access.EventVisibility
import java.time.Duration
import java.time.Instant
import java.util.UUID

/*
 * Records held by the store: the fake "server". Nothing in ui/ sees these; the repository turns
 * them into redacted views (data/repo/Views.kt) after asking AccessPolicy.
 */

/** Id of the signed-in user ("You"). */
const val ME = "S"

/** The time source for the fake. Tests pin it. */
object Clock {
    /**
     * Demo time travel (Settings > Demo, preview builds): how far the fake server's clock is moved
     * from real time. Snapshot state, so every list, chip and banner that reads the time updates live.
     */
    var shift by mutableStateOf(Duration.ZERO)
    var now: () -> Instant = { Instant.now().plus(shift) }
    fun ago(d: Duration): Instant = now().minus(d)
}

/** [Clock] as a java.time.Clock, for [com.circle.app.data.lifetime.Lifetime]. */
object AppClock : java.time.Clock() {
    override fun getZone(): java.time.ZoneId = java.time.ZoneId.systemDefault()
    override fun withZone(zone: java.time.ZoneId): java.time.Clock = this
    override fun instant(): Instant = Clock.now()
}

/** New ids: UUID-style, never list indexes. */
fun newId(prefix: String): String = "$prefix-${UUID.randomUUID()}"

/** Anyone who can appear in the app. */
data class Person(
    val id: String,
    val name: String,
    val color: Color,
    val sinceYear: Int,
    /** The pinned thought: their bio, visible to anyone who finds them. */
    val pinned: String = "",
) {
    val initials: String
        get() = if (id == ME) ME else name.split(" ").filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }
    val firstName: String get() = name.substringBefore(" ")
    val handle: String get() = name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
}

enum class RequestStatus { PENDING, ACCEPTED, DECLINED }

/** What a report is about. */
enum class ReportKind { PERSON, NOTE, COMMENT, GROUP, EVENT, MESSAGE }

enum class ReportReason { SPAM, HARASSMENT, HATE, SEXUAL, VIOLENCE, SELF_HARM, IMPERSONATION, OTHER }

/** A report you filed. Moderation (who reviews it, how fast) comes with the backend. */
data class Report(val kind: ReportKind, val targetId: String, val reason: ReportReason, val note: String, val at: java.time.Instant)

/**
 * Reported content from a temporary group or chat, held aside for moderation when everything else
 * was deleted (TIMED_GROUPS 9). NEEDS LAWYER: whether this is required and for how long
 * ([REVIEW_HOLD_DAYS] is a placeholder). Never shown to members; only moderation would read it.
 */
data class HeldForReview(val kind: ReportKind, val targetId: String, val authorId: String, val text: String, val heldAt: Instant) {
    companion object { const val REVIEW_HOLD_DAYS = 30L /* placeholder: needs lawyer */ }
}

/** An incoming friend request. Its status lives in [CircleStore.requestStatus]. */
data class FriendRequest(val personId: String, val receivedAt: Instant)

/** A comment in a thread. Only top-level comments carry [replies]; one layer deep. */
data class Comment(
    val id: String,
    val authorId: String,
    val createdAt: Instant,
    val text: String,
    val replies: List<Comment> = emptyList(),
)

class Post(
    val id: String,
    val authorId: String,
    val createdAt: Instant,
    val text: String,
    val hasMedia: Boolean = false,
    val audience: Audience = Audience.FRIENDS,
    /** A quote: your text, then this original note embedded under it. */
    val quotedPostId: String? = null,
    /** A repost: a "You reposted" card that shows this original note. */
    val repostOfId: String? = null,
    /** Posted in a group: stays in the group (comment and like only). */
    val groupId: String? = null,
    comments: List<Comment> = emptyList(),
    /** Photos and videos picked in Compose (seed photo posts have [hasMedia] and no items). */
    val media: List<com.circle.app.data.media.MediaRef> = emptyList(),
) {
    val comments: SnapshotStateList<Comment> = mutableStateListOf<Comment>().apply { addAll(comments) }
    val isRepost: Boolean get() = repostOfId != null

    /** Copy with new text or audience (editing your own note); comments are carried over. */
    fun edited(text: String = this.text, audience: Audience = this.audience) = Post(
        id, authorId, createdAt, text, hasMedia, audience, quotedPostId, repostOfId, groupId, comments.toList(), media
    )
}

class GroupInfo(
    val id: String,
    name: String,
    val category: String,
    /** Explore kicker topic, e.g. "PHOTOGRAPHY". */
    val topic: String,
    description: String,
    val color: Color,
    isPublic: Boolean,
    /** Compass angle on the Explore map; null = not placed on the map (list only). */
    val compassDeg: Float?,
    memberCount: Int,
    isMember: Boolean
) {
    var name by mutableStateOf(name)
    var description by mutableStateOf(description)
    var isPublic by mutableStateOf(isPublic)
    val initial: String get() = name.first().uppercase()
    var memberCount by mutableIntStateOf(memberCount)
    var isMember by mutableStateOf(isMember)
    /** Hosts, shown on the inner ring of the People tab. */
    val hostIds: SnapshotStateList<String> = mutableStateListOf()
    /** Members shown on the outer ring (the rest are counted in the "+N" chip). */
    val memberIds: SnapshotStateList<String> = mutableStateListOf()
    /** People who asked to join, with when. Visible to every member. */
    val joinRequests: SnapshotStateList<Pair<String, Instant>> = mutableStateListOf()
    /** Your pending join request: visible to every member, never auto-approved. */
    var joinRequested by mutableStateOf(false)
    /** Permanent, or temporary until an end (TIMED_GROUPS). The group's chat shares this one clock. */
    var term by mutableStateOf<com.circle.app.data.lifetime.Term>(com.circle.app.data.lifetime.Term.Permanent)
}

class EventInfo(
    val id: String,
    title: String,
    /** Explore kicker topic, e.g. "OUTDOORS". */
    val topic: String,
    /** Compass angle on the Explore map (inner ring); null = list only. */
    val compassDeg: Float?,
    val startsAt: Instant,
    venue: String,
    val visibility: EventVisibility,
    val hostId: String,
    description: String,
    val capacity: Int,
    /** People going other than me. The host is always among them. */
    val goingBase: Int,
    joined: Boolean,
    comments: List<Comment> = emptyList()
) {
    var title by mutableStateOf(title)
    var venue by mutableStateOf(venue)
    var description by mutableStateOf(description)
    /** I have a seat. */
    var joined by mutableStateOf(joined)
    val going: Int get() = goingBase + if (joined) 1 else 0
    val seatsLeft: Int get() = (capacity - going).coerceAtLeast(0)
    val isFull: Boolean get() = going >= capacity
    val comments: SnapshotStateList<Comment> = mutableStateListOf<Comment>().apply { addAll(comments) }
}

/** One step of a data export, shown to you as an audit log ("Requested 9 Oct"). */
data class ExportEvent(val kind: ExportStep, val at: Instant)

enum class ExportStep { REQUESTED, READY, DOWNLOADED }

/** An invitation you sent to a friend (to a group or an event). With a backend it lands in their Alerts. */
data class Invite(val toPersonId: String, val groupId: String?, val eventId: String?, val at: Instant)

/** A quiet note the server would deliver to someone else (e.g. the host when you leave a seat). */
data class Notice(val toPersonId: String, val text: String, val at: Instant)

/** A quiet alert. [actorId] did [kind]; [postId] is what it refers to, if anything. */
data class Alert(
    val id: String,
    val actorId: String,
    val kind: AlertKind,
    val createdAt: Instant,
    /** A short quote (a comment's text), shown only to someone who may read the thread. */
    val quote: String? = null,
    val postId: String? = null,
    /** For activity rows: whose note it was about. */
    val aboutId: String? = null,
    /** Timed reminders and changes: the temporary group or chat with friends it is about. */
    val timedId: String? = null,
    /** The date the alert names: the new end (extended), or when it is deleted (ended, deleted tomorrow). */
    val timedAt: Instant? = null,
)

enum class AlertKind {
    // Aimed at you (Notifications tab)
    LIKED_YOUR_NOTE, COMMENTED_ON_YOUR_NOTE, REPOSTED_YOUR_NOTE, QUOTED_YOUR_NOTE,
    // What friends did (Activity tab)
    FRIEND_COMMENTED, FRIEND_REPOSTED,
    // Temporary groups and chats with friends (TIMED_GROUPS 7): the server sends these to members
    TIMED_ENDS_SOON, TIMED_ENDS_TOMORROW, TIMED_ENDED, TIMED_DELETED_TOMORROW, TIMED_EXTENDED, TIMED_MADE_PERMANENT,
}
