package com.circle.app.data.access

import com.circle.app.data.access.TimedAction.*
import com.circle.app.data.lifetime.LifeState
import com.circle.app.data.lifetime.LifeState.*
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * TIMED_GROUPS 4, every cell. Written out by hand from the spec, not computed from the policy:
 * a cell not listed as allowed must be denied.
 */
class TimedAccessTableTest {

    private enum class Who(val member: Boolean, val host: Boolean) { OUTSIDER(false, false), MEMBER(true, false), HOST(true, true) }

    private val write = setOf(POST, COMMENT, SEND, INVITE)
    private val join = setOf(JOIN, REQUEST_TO_JOIN)
    private val hostOnly = setOf(EXTEND, MAKE_PERMANENT, END_NOW)

    /** (state, who) -> allowed actions. Anything missing: none. */
    private val allowed: Map<Pair<LifeState, Who>, Set<TimedAction>> = mapOf(
        // Permanent: normal life; nothing to save or change about a lifetime.
        (PERMANENT to Who.OUTSIDER) to join,
        (PERMANENT to Who.MEMBER) to write + join,
        (PERMANENT to Who.HOST) to write + join,
        // Active and ending soon: as permanent, plus saving a copy; the host can extend, end now or make permanent.
        (ACTIVE to Who.OUTSIDER) to join,
        (ACTIVE to Who.MEMBER) to write + join + EXPORT,
        (ACTIVE to Who.HOST) to write + join + EXPORT + hostOnly,
        (ENDING_SOON to Who.OUTSIDER) to join,
        (ENDING_SOON to Who.MEMBER) to write + join + EXPORT,
        (ENDING_SOON to Who.HOST) to write + join + EXPORT + hostOnly,
        // Wrap-up: read-only. No writes, joins or invites. Members save a copy; the host may still extend or keep it.
        (WRAP_UP to Who.MEMBER) to setOf(EXPORT),
        (WRAP_UP to Who.HOST) to setOf(EXPORT) + hostOnly,
        // DELETED: nothing, for anyone.
    )

    @Test fun `every cell`() {
        var cells = 0
        for (state in LifeState.entries) for (who in Who.entries) for (action in TimedAction.entries) {
            val expected = action in allowed[state to who].orEmpty()
            val d = AccessPolicy.timed(state, action, who.member, who.host)
            assertEquals("$state / $who / $action", expected, d.allowed)
            cells++
        }
        assertEquals(5 * 3 * 10, cells)
    }

    @Test fun `denials carry the right reason`() {
        for (who in Who.entries) for (action in TimedAction.entries) {
            assertEquals(Decision.Deny(DenyReason.NOT_FOUND), AccessPolicy.timed(DELETED, action, who.member, who.host))
        }
        for (action in write) assertEquals(Decision.Deny(DenyReason.ENDED), AccessPolicy.timed(WRAP_UP, action, true, true))
        for (action in join) assertEquals(Decision.Deny(DenyReason.ENDED), AccessPolicy.timed(WRAP_UP, action, false, false))
        for (action in hostOnly) {
            assertEquals(Decision.Deny(DenyReason.NOT_HOST), AccessPolicy.timed(ACTIVE, action, true, false))
            assertEquals(Decision.Deny(DenyReason.GROUP_NOT_MEMBER), AccessPolicy.timed(ACTIVE, action, false, false))
            assertEquals(Decision.Deny(DenyReason.NOT_TEMPORARY), AccessPolicy.timed(PERMANENT, action, true, true))
        }
        assertEquals(Decision.Deny(DenyReason.GROUP_NOT_MEMBER), AccessPolicy.timed(WRAP_UP, EXPORT, false, false))
        assertEquals(Decision.Deny(DenyReason.NOT_TEMPORARY), AccessPolicy.timed(PERMANENT, EXPORT, true, false))
    }

    @Test fun `reads follow membership until DELETED, then NotFound`() {
        for (state in LifeState.entries) {
            assertEquals("$state", state != DELETED, AccessPolicy.timedRead(state).allowed)
        }
        assertEquals(Decision.Deny(DenyReason.NOT_FOUND), AccessPolicy.timedRead(DELETED))
    }

    @Test fun `group posts in each state`() {
        val read = setOf(NoteAction.VIEW, NoteAction.OPEN_THREAD, NoteAction.READ_COMMENTS)
        val interact = setOf(NoteAction.COMMENT, NoteAction.LIKE, NoteAction.SHARE)
        for (state in LifeState.entries) for (member in listOf(false, true)) for (action in NoteAction.entries) {
            val expected = when {
                state == DELETED || !member -> false
                action in read -> true
                action in interact -> state.isOpen
                else -> false // repost, quote: never for group posts
            }
            val facts = NoteFacts(Audience.FRIENDS, false, groupId = "g", viewerIsGroupMember = member, groupState = state)
            assertEquals("$state / member=$member / $action", expected, AccessPolicy.note(Relationship.FRIEND, facts, action).allowed)
        }
        val ended = NoteFacts(Audience.FRIENDS, false, "g", true, groupState = WRAP_UP)
        assertEquals(Decision.Deny(DenyReason.ENDED), AccessPolicy.note(Relationship.FRIEND, ended, NoteAction.COMMENT))
        val gone = NoteFacts(Audience.FRIENDS, false, "g", true, groupState = DELETED)
        assertEquals(Decision.Deny(DenyReason.NOT_FOUND), AccessPolicy.note(Relationship.FRIEND, gone, NoteAction.VIEW))
    }
}
