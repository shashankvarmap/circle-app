package com.circle.app.data.access

import com.circle.app.data.lifetime.LifeState

/**
 * How the viewer relates to a person. Decided by the repository from the friend graph; every
 * access decision starts from this one value.
 */
enum class Relationship {
    SELF, FRIEND, REQUEST_SENT, REQUEST_RECEIVED, NONE, BLOCKED_BY_ME, BLOCKED_ME;

    val isBlocked: Boolean get() = this == BLOCKED_BY_ME || this == BLOCKED_ME
}

/** Who may see a note. */
enum class Audience { FRIENDS, INNER_CIRCLE }

/** Why something was refused. The first five are the thread reasons of MASTER_PROMPT 2.1. */
enum class DenyReason {
    NOT_FRIENDS, BLOCKED, AUDIENCE, GROUP_NOT_MEMBER, EVENT_LOCKED,
    /** Inner-circle notes can't be reposted, quoted or shared outside the inner circle. */
    INNER_CIRCLE,
    /** Group posts stay in their group. */
    GROUP_POST,
    /** The thing doesn't exist (or no longer exists) for this viewer. */
    NOT_FOUND,
    /** A temporary group or chat has ended: it is read-only until it is deleted. */
    ENDED,
    /** Only the host may change a group's or chat's lifetime. */
    NOT_HOST,
    /** Lifetime actions (save a copy, extend, end now, make permanent) need a temporary group or chat. */
    NOT_TEMPORARY,
}

/** What can be done in a group or chat that has a lifetime (TIMED_GROUPS 4). Permanent ones are asked too. */
enum class TimedAction { POST, COMMENT, SEND, JOIN, INVITE, REQUEST_TO_JOIN, EXPORT, EXTEND, MAKE_PERMANENT, END_NOW }

/** What can be done with a note. */
enum class NoteAction { VIEW, OPEN_THREAD, READ_COMMENTS, COMMENT, LIKE, REPOST, QUOTE, SHARE }

/**
 * The facts about one note that the rules depend on, from the viewer's side.
 * [viewerInAuthorsInnerCircle] is decided by the server; the viewer never learns the rest of it.
 */
data class NoteFacts(
    val audience: Audience,
    val viewerInAuthorsInnerCircle: Boolean,
    /** Set for group posts: then membership decides, never friendship. */
    val groupId: String? = null,
    val viewerIsGroupMember: Boolean = false,
    /** For group posts: the group's lifetime state. Ended groups are read-only; deleted ones are gone. */
    val groupState: LifeState = LifeState.PERMANENT,
)

sealed interface Decision {
    data object Allow : Decision
    data class Deny(val reason: DenyReason) : Decision

    val allowed: Boolean get() = this is Allow
}

/** Who an event is for. Friends-of-friends is checked by the server; the fake treats it as visible. */
enum class EventVisibility { FRIENDS, FRIENDS_OF_FRIENDS, OPEN_NEARBY }

enum class ProfileAccess { FULL, LIMITED, NOT_FOUND }

enum class SharedCardAccess { OPEN, SNIPPET, HIDDEN }

/**
 * The one place that decides access (MASTER_PROMPT 2). Pure Kotlin, no Android, no UI.
 * **Deny by default**: every function lists what is allowed and refuses everything else.
 * The repository asks it before returning anything; the server must apply the same table
 * (PRIVACY_RULES.md). The app's checks are for experience; the server's are for security.
 */
object AccessPolicy {

    /** Max characters of a shared note shown to someone who can't open it. */
    const val SNIPPET_MAX = 90

    private val PENDING_OR_NONE = setOf(Relationship.REQUEST_SENT, Relationship.REQUEST_RECEIVED, Relationship.NONE)

    /** Profile, and also what tapping a person anywhere opens. */
    fun profile(rel: Relationship): ProfileAccess = when (rel) {
        Relationship.SELF, Relationship.FRIEND -> ProfileAccess.FULL
        in PENDING_OR_NONE -> ProfileAccess.LIMITED
        else -> ProfileAccess.NOT_FOUND
    }

    /** [rel] is the viewer's relationship to the note's author. */
    fun note(rel: Relationship, facts: NoteFacts, action: NoteAction): Decision {
        if (rel.isBlocked) return Decision.Deny(DenyReason.BLOCKED)
        if (facts.groupId != null) return groupNote(facts, action)
        val visible = when (rel) {
            Relationship.SELF -> true
            Relationship.FRIEND -> facts.audience == Audience.FRIENDS || facts.viewerInAuthorsInnerCircle
            else -> return Decision.Deny(DenyReason.NOT_FRIENDS)
        }
        if (!visible) return Decision.Deny(DenyReason.AUDIENCE)
        return when (action) {
            NoteAction.VIEW, NoteAction.OPEN_THREAD, NoteAction.READ_COMMENTS, NoteAction.COMMENT, NoteAction.LIKE -> Decision.Allow
            NoteAction.REPOST, NoteAction.QUOTE ->
                if (facts.audience == Audience.INNER_CIRCLE) Decision.Deny(DenyReason.INNER_CIRCLE) else Decision.Allow
            // Only the author may share an inner-circle note (and only into their inner circle: see [shareInto]).
            NoteAction.SHARE ->
                if (facts.audience == Audience.INNER_CIRCLE && rel != Relationship.SELF) Decision.Deny(DenyReason.INNER_CIRCLE) else Decision.Allow
        }
    }

    /** Group posts: by membership, never by friendship alone. Comment and like only; shared only inside the group. */
    private fun groupNote(facts: NoteFacts, action: NoteAction): Decision {
        if (facts.groupState == LifeState.DELETED) return Decision.Deny(DenyReason.NOT_FOUND)
        if (!facts.viewerIsGroupMember) return Decision.Deny(DenyReason.GROUP_NOT_MEMBER)
        return when (action) {
            NoteAction.VIEW, NoteAction.OPEN_THREAD, NoteAction.READ_COMMENTS -> Decision.Allow
            // Read-only once ended: no comments, likes or shares.
            NoteAction.COMMENT, NoteAction.LIKE, NoteAction.SHARE ->
                if (facts.groupState.isOpen) Decision.Allow else Decision.Deny(DenyReason.ENDED)
            NoteAction.REPOST, NoteAction.QUOTE -> Decision.Deny(DenyReason.GROUP_POST)
        }
    }

    /**
     * Reading a group or chat with a lifetime: normal membership rules apply in every state except
     * DELETED, when every read is NotFound (the server has hard-deleted it).
     */
    fun timedRead(state: LifeState): Decision =
        if (state == LifeState.DELETED) Decision.Deny(DenyReason.NOT_FOUND) else Decision.Allow

    /**
     * Writing to, joining or changing a group or chat with a lifetime (TIMED_GROUPS 4).
     * [isMember] is membership now (for EXPORT, "members at the time": nobody joins after the end);
     * [isHost] is the group's host or whoever started the chat.
     */
    fun timed(state: LifeState, action: TimedAction, isMember: Boolean, isHost: Boolean): Decision {
        if (state == LifeState.DELETED) return Decision.Deny(DenyReason.NOT_FOUND)
        val temporary = state != LifeState.PERMANENT
        return when (action) {
            TimedAction.POST, TimedAction.COMMENT, TimedAction.SEND, TimedAction.INVITE -> when {
                !isMember -> Decision.Deny(DenyReason.GROUP_NOT_MEMBER)
                !state.isOpen -> Decision.Deny(DenyReason.ENDED)
                else -> Decision.Allow
            }
            TimedAction.JOIN, TimedAction.REQUEST_TO_JOIN ->
                if (state.isOpen) Decision.Allow else Decision.Deny(DenyReason.ENDED)
            TimedAction.EXPORT -> when {
                !isMember -> Decision.Deny(DenyReason.GROUP_NOT_MEMBER)
                !temporary -> Decision.Deny(DenyReason.NOT_TEMPORARY)
                else -> Decision.Allow
            }
            TimedAction.EXTEND, TimedAction.MAKE_PERMANENT, TimedAction.END_NOW -> when {
                !isMember -> Decision.Deny(DenyReason.GROUP_NOT_MEMBER)
                !isHost -> Decision.Deny(DenyReason.NOT_HOST)
                !temporary -> Decision.Deny(DenyReason.NOT_TEMPORARY)
                else -> Decision.Allow
            }
        }
    }

    /**
     * Whether the sharer may send this note into a chat. [chatGroupId] is the chat's group, if any;
     * [allRecipientsInAuthorsInnerCircle] is decided by the server for inner-circle notes.
     */
    fun shareInto(
        sharerRel: Relationship,
        facts: NoteFacts,
        chatGroupId: String?,
        allRecipientsInAuthorsInnerCircle: Boolean,
    ): Decision {
        val base = note(sharerRel, facts, NoteAction.SHARE)
        if (!base.allowed) return base
        if (facts.groupId != null) return if (chatGroupId == facts.groupId) Decision.Allow else Decision.Deny(DenyReason.GROUP_POST)
        if (facts.audience == Audience.INNER_CIRCLE && !allRecipientsInAuthorsInnerCircle) return Decision.Deny(DenyReason.INNER_CIRCLE)
        return Decision.Allow
    }

    /** A note shared into a chat, as one recipient sees it. [rel] is the recipient's relationship to the author. */
    fun sharedCard(rel: Relationship, facts: NoteFacts): SharedCardAccess = when {
        rel.isBlocked -> SharedCardAccess.HIDDEN
        note(rel, facts, NoteAction.OPEN_THREAD).allowed -> SharedCardAccess.OPEN
        // Group and inner-circle notes never travel as snippets.
        facts.groupId != null || facts.audience == Audience.INNER_CIRCLE -> SharedCardAccess.HIDDEN
        rel in PENDING_OR_NONE -> SharedCardAccess.SNIPPET
        else -> SharedCardAccess.HIDDEN
    }

    /** Friends list, friend and inner counts, groups, inner circle, activity: the owner only. */
    fun friendGraph(rel: Relationship): Decision =
        if (rel == Relationship.SELF) Decision.Allow else Decision.Deny(if (rel.isBlocked) DenyReason.BLOCKED else DenyReason.NOT_FRIENDS)

    /** Direct messages: friends only. */
    fun directMessage(rel: Relationship): Decision = when {
        rel == Relationship.FRIEND -> Decision.Allow
        rel.isBlocked -> Decision.Deny(DenyReason.BLOCKED)
        rel == Relationship.SELF -> Decision.Deny(DenyReason.NOT_FOUND)
        else -> Decision.Deny(DenyReason.NOT_FRIENDS)
    }

    /** A group's posts and people: members only. */
    fun groupContent(isMember: Boolean): Decision =
        if (isMember) Decision.Allow else Decision.Deny(DenyReason.GROUP_NOT_MEMBER)

    /** Seeing an event at all. [relToHost] is the viewer's relationship to the host. */
    fun event(visibility: EventVisibility, relToHost: Relationship): Decision = when {
        relToHost.isBlocked -> Decision.Deny(DenyReason.BLOCKED)
        visibility != EventVisibility.FRIENDS -> Decision.Allow
        relToHost == Relationship.SELF || relToHost == Relationship.FRIEND -> Decision.Allow
        else -> Decision.Deny(DenyReason.NOT_FRIENDS)
    }

    /** Seeing a group's page at all: public groups, or private ones you belong to. Its content is [groupContent]. */
    fun group(isPublic: Boolean, isMember: Boolean): Decision =
        if (isPublic || isMember) Decision.Allow else Decision.Deny(DenyReason.NOT_FOUND)

    /** An event's thread: only people with a seat (the host always has one). */
    fun eventThread(hasSeat: Boolean): Decision =
        if (hasSeat) Decision.Allow else Decision.Deny(DenyReason.EVENT_LOCKED)

    /** Cuts a note's text to at most [SNIPPET_MAX] characters (ellipsis included). */
    fun snippet(text: String): String {
        if (text.length <= SNIPPET_MAX) return text
        val cut = text.take(SNIPPET_MAX - 1).trimEnd().trimEnd(',', '.', ';', ':')
        return cut + "…"
    }
}
