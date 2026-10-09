package com.circle.app.data.access

import com.circle.app.data.access.NoteAction.*
import com.circle.app.data.access.Relationship.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MASTER_PROMPT 2.2, every cell. The expectations below are written out by hand from the table,
 * not computed from the policy: a cell that is not explicitly listed as allowed must be denied.
 */
class AccessPolicyTableTest {

    private val everyRelationship = Relationship.entries
    private val everyAction = NoteAction.entries
    private val readAndInteract = setOf(VIEW, OPEN_THREAD, READ_COMMENTS, COMMENT, LIKE)

    // ---- Author's notes (not in a group) ------------------------------------------------------

    /** (relationship, audience, viewer in author's inner circle) -> allowed actions. Anything missing: none. */
    private val personalNoteAllowed: Map<Triple<Relationship, Audience, Boolean>, Set<NoteAction>> = mapOf(
        Triple(SELF, Audience.FRIENDS, false) to NoteAction.entries.toSet(),
        Triple(SELF, Audience.FRIENDS, true) to NoteAction.entries.toSet(),
        // Your own inner-circle note: no repost or quote; sharing is limited to your inner circle by shareInto.
        Triple(SELF, Audience.INNER_CIRCLE, false) to readAndInteract + SHARE,
        Triple(SELF, Audience.INNER_CIRCLE, true) to readAndInteract + SHARE,
        Triple(FRIEND, Audience.FRIENDS, false) to NoteAction.entries.toSet(),
        Triple(FRIEND, Audience.FRIENDS, true) to NoteAction.entries.toSet(),
        // A friend's inner-circle note: only if you're in their inner circle, and never reposted, quoted or shared.
        Triple(FRIEND, Audience.INNER_CIRCLE, true) to readAndInteract,
        // REQUEST_SENT / REQUEST_RECEIVED / NONE / BLOCKED_*: never anything.
    )

    @Test fun `every cell of the author's-notes rows`() {
        var cells = 0
        for (rel in everyRelationship) for (audience in Audience.entries) for (inner in listOf(false, true)) for (action in everyAction) {
            val expected = personalNoteAllowed[Triple(rel, audience, inner)].orEmpty()
            val decision = AccessPolicy.note(rel, NoteFacts(audience, inner), action)
            assertEquals("$rel / $audience / inner=$inner / $action", action in expected, decision.allowed)
            cells++
        }
        assertEquals(7 * 2 * 2 * 8, cells)
    }

    @Test fun `denials carry the right reason`() {
        for (rel in listOf(REQUEST_SENT, REQUEST_RECEIVED, NONE)) for (action in everyAction) {
            assertEquals(Decision.Deny(DenyReason.NOT_FRIENDS), AccessPolicy.note(rel, NoteFacts(Audience.FRIENDS, false), action))
        }
        for (rel in listOf(BLOCKED_BY_ME, BLOCKED_ME)) for (action in everyAction) {
            assertEquals(Decision.Deny(DenyReason.BLOCKED), AccessPolicy.note(rel, NoteFacts(Audience.FRIENDS, true), action))
            assertEquals(Decision.Deny(DenyReason.BLOCKED), AccessPolicy.note(rel, NoteFacts(Audience.FRIENDS, true, "g", true), action))
        }
        assertEquals(Decision.Deny(DenyReason.AUDIENCE), AccessPolicy.note(FRIEND, NoteFacts(Audience.INNER_CIRCLE, false), OPEN_THREAD))
        assertEquals(Decision.Deny(DenyReason.INNER_CIRCLE), AccessPolicy.note(FRIEND, NoteFacts(Audience.INNER_CIRCLE, true), REPOST))
        assertEquals(Decision.Deny(DenyReason.GROUP_POST), AccessPolicy.note(FRIEND, NoteFacts(Audience.FRIENDS, true, "g", true), QUOTE))
        assertEquals(Decision.Deny(DenyReason.GROUP_NOT_MEMBER), AccessPolicy.note(FRIEND, NoteFacts(Audience.FRIENDS, true, "g", false), VIEW))
    }

    // ---- Group posts: by membership, never by friendship alone ---------------------------------

    @Test fun `every cell of the group-posts row`() {
        val memberAllowed = readAndInteract + SHARE // comment and like only; no repost or quote
        for (rel in everyRelationship) for (member in listOf(false, true)) for (action in everyAction) {
            val expected = if (member && !rel.isBlocked) memberAllowed else emptySet()
            val decision = AccessPolicy.note(rel, NoteFacts(Audience.FRIENDS, false, groupId = "g", viewerIsGroupMember = member), action)
            assertEquals("group / $rel / member=$member / $action", action in expected, decision.allowed)
        }
    }

    // ---- Profile, and a person tapped anywhere -------------------------------------------------

    @Test fun `profile row and person-tapped row`() {
        val expected = mapOf(
            SELF to ProfileAccess.FULL, FRIEND to ProfileAccess.FULL,
            REQUEST_SENT to ProfileAccess.LIMITED, REQUEST_RECEIVED to ProfileAccess.LIMITED, NONE to ProfileAccess.LIMITED,
            BLOCKED_BY_ME to ProfileAccess.NOT_FOUND, BLOCKED_ME to ProfileAccess.NOT_FOUND,
        )
        assertEquals(everyRelationship.toSet(), expected.keys)
        for (rel in everyRelationship) assertEquals("$rel", expected.getValue(rel), AccessPolicy.profile(rel))
    }

    // ---- Friends list, counts, groups, inner circle: own only ----------------------------------

    @Test fun `friend graph is the owner's only`() {
        for (rel in everyRelationship) assertEquals("$rel", rel == SELF, AccessPolicy.friendGraph(rel).allowed)
    }

    // ---- Direct message: friends only ----------------------------------------------------------

    @Test fun `direct messages only between friends`() {
        for (rel in everyRelationship) assertEquals("$rel", rel == FRIEND, AccessPolicy.directMessage(rel).allowed)
    }

    // ---- Shared note card in a chat ------------------------------------------------------------

    @Test fun `shared card row`() {
        val friendsNote = NoteFacts(Audience.FRIENDS, false)
        val expected = mapOf(
            SELF to SharedCardAccess.OPEN, FRIEND to SharedCardAccess.OPEN,
            REQUEST_SENT to SharedCardAccess.SNIPPET, REQUEST_RECEIVED to SharedCardAccess.SNIPPET, NONE to SharedCardAccess.SNIPPET,
            BLOCKED_BY_ME to SharedCardAccess.HIDDEN, BLOCKED_ME to SharedCardAccess.HIDDEN,
        )
        for (rel in everyRelationship) assertEquals("$rel", expected.getValue(rel), AccessPolicy.sharedCard(rel, friendsNote))
        // Inner-circle and group notes never travel as snippets.
        for (rel in everyRelationship - SELF) {
            assertTrue("$rel inner", AccessPolicy.sharedCard(rel, NoteFacts(Audience.INNER_CIRCLE, false)) == SharedCardAccess.HIDDEN)
            assertTrue("$rel group", AccessPolicy.sharedCard(rel, NoteFacts(Audience.FRIENDS, false, "g", false)) == SharedCardAccess.HIDDEN)
        }
    }

    @Test fun `sharing into a chat`() {
        val friendsNote = NoteFacts(Audience.FRIENDS, false)
        val innerNote = NoteFacts(Audience.INNER_CIRCLE, true)
        val groupNote = NoteFacts(Audience.FRIENDS, false, "g", true)
        // A friend's ordinary note may be shared into any chat (non-friend recipients see a snippet).
        assertTrue(AccessPolicy.shareInto(FRIEND, friendsNote, null, false).allowed)
        // Only the author shares an inner-circle note, and only when every recipient is in their inner circle.
        assertTrue(AccessPolicy.shareInto(SELF, innerNote, null, true).allowed)
        assertEquals(Decision.Deny(DenyReason.INNER_CIRCLE), AccessPolicy.shareInto(SELF, innerNote, null, false))
        assertEquals(Decision.Deny(DenyReason.INNER_CIRCLE), AccessPolicy.shareInto(FRIEND, innerNote, null, true))
        // Group posts stay in their group's chat.
        assertTrue(AccessPolicy.shareInto(FRIEND, groupNote, "g", false).allowed)
        assertEquals(Decision.Deny(DenyReason.GROUP_POST), AccessPolicy.shareInto(FRIEND, groupNote, null, false))
        assertEquals(Decision.Deny(DenyReason.GROUP_POST), AccessPolicy.shareInto(FRIEND, groupNote, "other", false))
        // Nobody shares what they can't see.
        for (rel in listOf(REQUEST_SENT, REQUEST_RECEIVED, NONE, BLOCKED_BY_ME, BLOCKED_ME)) {
            assertTrue("$rel", !AccessPolicy.shareInto(rel, friendsNote, null, true).allowed)
        }
    }

    // ---- Group content and event threads: membership / seat --------------------------------------

    @Test fun `group content and event threads follow membership and seats, not friendship`() {
        assertTrue(AccessPolicy.groupContent(isMember = true).allowed)
        assertEquals(Decision.Deny(DenyReason.GROUP_NOT_MEMBER), AccessPolicy.groupContent(isMember = false))
        assertTrue(AccessPolicy.eventThread(hasSeat = true).allowed)
        assertEquals(Decision.Deny(DenyReason.EVENT_LOCKED), AccessPolicy.eventThread(hasSeat = false))
    }

    @Test fun `events and groups visibility`() {
        for (rel in everyRelationship) {
            val friendsOnly = AccessPolicy.event(EventVisibility.FRIENDS, rel).allowed
            assertEquals("friends-only event / $rel", rel == SELF || rel == FRIEND, friendsOnly)
            for (v in listOf(EventVisibility.FRIENDS_OF_FRIENDS, EventVisibility.OPEN_NEARBY)) {
                assertEquals("$v / $rel", !rel.isBlocked, AccessPolicy.event(v, rel).allowed)
            }
        }
        assertTrue(AccessPolicy.group(isPublic = true, isMember = false).allowed)
        assertTrue(AccessPolicy.group(isPublic = false, isMember = true).allowed)
        assertEquals(Decision.Deny(DenyReason.NOT_FOUND), AccessPolicy.group(isPublic = false, isMember = false))
    }

    @Test fun `snippets are at most 90 characters`() {
        val long = "x".repeat(300)
        assertTrue(AccessPolicy.snippet(long).length <= AccessPolicy.SNIPPET_MAX)
        assertEquals("short", AccessPolicy.snippet("short"))
        assertEquals(90, AccessPolicy.snippet("y".repeat(90)).length)
        assertTrue(AccessPolicy.snippet("y".repeat(91)).length <= 90)
    }
}
