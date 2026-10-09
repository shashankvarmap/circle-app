package com.circle.app.data.repo

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.circle.app.data.MessageKind
import com.circle.app.data.access.Audience
import com.circle.app.data.access.DenyReason
import com.circle.app.data.access.EventVisibility
import com.circle.app.data.access.Relationship
import java.time.Instant

/*
 * What the UI receives. Every type here is already redacted by the repository after asking
 * AccessPolicy: a screen can only show what is in these fields, so a screen bug cannot leak.
 */

/** The result of any network-shaped call. */
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Denied(val reason: DenyReason) : Outcome<Nothing>
    data class Error(val message: String) : Outcome<Nothing>
}

val Outcome<*>.isSuccess: Boolean get() = this is Outcome.Success

/** Who someone is: photo (initials + colour) and name. Anyone may see this, except across a block. */
@Immutable
data class PersonRef(val id: String, val name: String, val initials: String, val color: Color, val isMe: Boolean = false) {
    val firstName: String get() = name.substringBefore(" ")
}

/** A group's tile and name. */
@Immutable
data class GroupRef(val id: String, val name: String, val initial: String, val color: Color)

// ---- Profiles ------------------------------------------------------------------------------

sealed interface ProfileView {
    /** You, or a friend. Still no friends list or counts for a friend: those are the owner's only. */
    data class Full(
        val person: PersonRef,
        val handle: String,
        val pinnedThought: String,
        val relationship: Relationship,
        val friendsSinceYear: Int?,
        val isMuted: Boolean,
    ) : ProfileView

    /**
     * Someone who isn't your friend (requested either way, or nobody yet): photo, name, handle and
     * pinned thought. **There is no field for anything else**, so nothing else can be shown.
     */
    data class Limited(
        val id: String,
        val name: String,
        val initials: String,
        val color: Color,
        val handle: String,
        val pinnedThought: String,
    ) : ProfileView {
        val firstName: String get() = name.substringBefore(" ")
    }

    /** Blocked either way, or no such person. */
    data object NotFound : ProfileView
}

/** Your own profile (the editable fields). */
data class MeView(
    val person: PersonRef,
    val displayName: String,
    val handle: String,
    val pinnedThought: String,
    val avatarUri: String?,
)

/** Your friend and inner-circle counts. Only ever returned to you. */
data class CircleSummary(val friendCount: Int, val innerCount: Int)

data class FriendView(val person: PersonRef, val isInner: Boolean, val sinceYear: Int)

/** A search or contacts result: Limited information plus the button state. */
data class PersonResult(val person: PersonRef, val handle: String, val relationship: Relationship)

data class RequestView(val person: PersonRef, val accepted: Boolean, val receivedAt: Instant)

// ---- Notes and threads ----------------------------------------------------------------------

/** Repost / quote availability on a note, from the viewer's side. */
sealed interface RepostState {
    data object Available : RepostState
    data object Reposted : RepostState
    data class Unavailable(val reason: DenyReason) : RepostState
}

/**
 * A note the viewer may see in full. For a "You reposted" card, [repostOf] is the original and the
 * action fields ([actionTargetId], [likedByMe], [commentCount], [repost]) refer to the original.
 */
@Immutable
data class Note(
    val id: String,
    val author: PersonRef,
    val createdAt: Instant,
    val text: String,
    val hasMedia: Boolean,
    val audience: Audience,
    val group: GroupRef?,
    val quoted: NoteView?,
    val repostOf: NoteView?,
    val actionTargetId: String,
    val likedByMe: Boolean,
    val commentCount: Int,
    val repost: RepostState,
    val canShare: Boolean,
) {
    val isMine: Boolean get() = author.isMe
    val isRepost: Boolean get() = repostOf != null
}

sealed interface NoteView {
    data class Full(val note: Note) : NoteView

    /**
     * A shared note whose author isn't the viewer's friend: who posted it and at most 90
     * characters. No photos, no replies, nothing else, and it can't be opened.
     */
    data class Snippet(val noteId: String, val authorId: String, val authorName: String, val text: String) : NoteView {
        val canOpen: Boolean get() = false
    }

    data object Hidden : NoteView
}

@Immutable
data class CommentView(
    val id: String,
    val author: PersonRef,
    val createdAt: Instant,
    val text: String,
    val replies: List<CommentView>,
)

data class Thread(val root: Note, val comments: List<CommentView>)

sealed interface ThreadResult {
    data class Opened(val thread: Thread) : ThreadResult

    /** [author] is who posted it (for the gate), or null when blocked or gone. */
    data class Denied(val reason: DenyReason, val author: PersonRef?) : ThreadResult
}

// ---- Chats ------------------------------------------------------------------------------------

enum class ChatKind { DIRECT, MULTI, GROUP }

data class ChatPreview(val mine: Boolean, val senderFirstName: String?, val kind: MessageKind, val text: String)

data class ChatRowView(
    val id: String,
    val kind: ChatKind,
    val title: String,
    val person: PersonRef?,
    val group: GroupRef?,
    val members: List<PersonRef>,
    val preview: ChatPreview?,
    val lastAt: Instant?,
    val unread: Boolean,
)

/** An event as it appears in a chat card. */
data class EventCardView(val id: String, val title: String, val startsAt: Instant, val canOpen: Boolean, val host: PersonRef?)

data class ReplySnippet(val messageId: String, val senderName: String, val mine: Boolean, val kind: MessageKind, val text: String)

data class MessageView(
    val id: String,
    val sender: PersonRef,
    val createdAt: Instant,
    val kind: MessageKind,
    val text: String,
    val sharedNote: NoteView?,
    val event: EventCardView?,
    val replyTo: ReplySnippet?,
    /** Shown only when the other person has read receipts on. */
    val seenAt: Instant?,
) {
    val mine: Boolean get() = sender.isMe
}

data class ChatView(
    val id: String,
    val kind: ChatKind,
    val title: String,
    val person: PersonRef?,
    val group: GroupRef?,
    val memberCount: Int,
    val friendsSinceYear: Int?,
    val messages: List<MessageView>,
)

// ---- Groups, events, explore --------------------------------------------------------------------

data class JoinRequestView(val person: PersonRef, val askedAt: Instant)

/** Hosts, members and join requests: members only. */
data class GroupPeopleView(
    val hosts: List<PersonRef>,
    val members: List<PersonRef>,
    val requests: List<JoinRequestView>,
    /** Members not shown on the ring (counted in the "+N" chip). */
    val hiddenCount: Int,
)

data class GroupView(
    val ref: GroupRef,
    val category: String,
    val topic: String,
    val description: String,
    val isPublic: Boolean,
    val memberCount: Int,
    val isMember: Boolean,
    val joinRequested: Boolean,
    val compassDeg: Float?,
    /** Null unless you're a member. */
    val people: GroupPeopleView?,
)

data class EventSummary(
    val id: String,
    val title: String,
    val topic: String,
    val compassDeg: Float?,
    val startsAt: Instant,
    val venue: String,
    val capacity: Int,
    val going: Int,
)

enum class SeatKind { PERSON, ME, EMPTY, MORE, TAKEN }

/** One place at the table. [TAKEN] is a seat held by someone hidden from you (a block). */
data class SeatView(val kind: SeatKind, val person: PersonRef? = null, val isHost: Boolean = false, val moreCount: Int = 0)

sealed interface EventThreadView {
    data class Open(val comments: List<CommentView>) : EventThreadView
    data class Locked(val replyCount: Int) : EventThreadView
}

data class EventView(
    val summary: EventSummary,
    val visibility: EventVisibility,
    val host: PersonRef,
    val description: String,
    val seatsLeft: Int,
    val isFull: Boolean,
    val hasSeat: Boolean,
    val iAmHost: Boolean,
    val seats: List<SeatView>,
    val thread: EventThreadView,
)

data class ExploreView(val events: List<EventSummary>, val groups: List<GroupView>)

// ---- Alerts -----------------------------------------------------------------------------------

data class AlertView(
    val id: String,
    val actor: PersonRef,
    val kind: com.circle.app.data.AlertKind,
    val createdAt: Instant,
    val quote: String?,
    val postId: String?,
    val about: PersonRef?,
)
