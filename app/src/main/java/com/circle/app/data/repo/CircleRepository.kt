package com.circle.app.data.repo

import com.circle.app.data.MyProfile
import com.circle.app.data.NotificationPrefs
import com.circle.app.data.PrivacyPrefs
import com.circle.app.data.ThemeMode
import com.circle.app.data.access.Audience
import com.circle.app.data.access.EventVisibility
import com.circle.app.data.access.Relationship
import kotlinx.coroutines.flow.Flow

/**
 * Everything the UI may know or do, from the signed-in user's side. The only code that touches
 * real data. Every read asks AccessPolicy first and returns redacted views; every action is
 * checked the same way and returns [Outcome.Denied] when the rules say no. Observations are
 * [Flow]s that re-evaluate when anything they depend on changes (a relationship included).
 * A backend implementation replaces [FakeCircleRepository]; the server applies the same rules.
 */
interface CircleRepository {

    // ---- Getting in (the fake accepts any 6-digit code) ----
    /**
     * Whether [requestCode] really sends a text message. False for the fake (no SMS provider yet);
     * a backend with an SMS provider returns true and the preview hint on the code screen goes away.
     */
    val sendsTextMessages: Boolean
    suspend fun requestCode(e164: String): Outcome<Unit>
    suspend fun verifyCode(e164: String, code: String): Outcome<Unit>
    /** Whether a handle is free (yours counts as free). */
    suspend fun isHandleFree(handle: String): Boolean
    /** A new account: starts empty (no demo feed). */
    suspend fun createAccount(name: String, handle: String, pinnedThought: String, avatarUri: String?): Outcome<Unit>
    /** An existing account (the demo one). */
    suspend fun logIn(e164: String): Outcome<Unit>
    /**
     * Contacts matching: only SHA-256 hashes of E.164 numbers are sent; the server matches them,
     * returns the people found (Limited information) and deletes the hashes at once.
     */
    suspend fun matchContacts(hashes: List<String>): Outcome<List<PersonResult>>
    /** Your invite link (placeholder domain until the real one is chosen). */
    fun inviteLink(): Flow<String>
    /** How many invites you've shared: shown to you only. */
    fun invitesShared(): Flow<Int>
    suspend fun recordInviteShared()
    /** The person behind an invite link's handle, for the App Link (Limited profile + Add friend). */
    suspend fun personByHandle(handle: String): Outcome<String>
    fun exportLog(): Flow<List<com.circle.app.data.ExportEvent>>
    suspend fun requestDataExport(): Outcome<Unit>

    // ---- You & settings ----
    fun me(): Flow<MeView>
    fun theme(): Flow<ThemeMode>
    fun privacy(): Flow<PrivacyPrefs>
    fun notificationPrefs(): Flow<NotificationPrefs>
    suspend fun setTheme(mode: ThemeMode)
    suspend fun setPrivacy(prefs: PrivacyPrefs)
    suspend fun setNotificationPrefs(prefs: NotificationPrefs)
    /** Saves the edit-profile form; [Outcome.Error] carries why not. */
    suspend fun saveProfile(displayName: String, handle: String, pinnedThought: String, avatarUri: String?): Outcome<MyProfile>
    suspend fun deleteAccount(): Outcome<Unit>

    // ---- Your circle (owner only) ----
    fun circleSummary(): Flow<CircleSummary>
    fun friends(): Flow<List<FriendView>>
    suspend fun toggleInnerCircle(personId: String): Outcome<Boolean>

    // ---- People ----
    fun relationshipTo(personId: String): Flow<Relationship>
    fun profile(personId: String): Flow<ProfileView>
    /** A person's notes you may see (empty for anyone but you and your friends). */
    fun notesBy(personId: String): Flow<List<Note>>
    fun searchPeople(query: String): Flow<List<PersonResult>>
    fun contactMatches(): Flow<List<PersonResult>>
    fun incomingRequests(): Flow<List<RequestView>>
    fun blocked(): Flow<List<PersonRef>>
    fun muted(): Flow<List<PersonRef>>
    suspend fun sendFriendRequest(personId: String): Outcome<Unit>
    suspend fun cancelFriendRequest(personId: String): Outcome<Unit>
    suspend fun acceptRequest(personId: String): Outcome<Unit>
    suspend fun declineRequest(personId: String): Outcome<Unit>
    suspend fun unfriend(personId: String): Outcome<Unit>
    suspend fun block(personId: String): Outcome<Unit>
    suspend fun unblock(personId: String): Outcome<Unit>
    suspend fun mute(personId: String): Outcome<Unit>
    suspend fun unmute(personId: String): Outcome<Unit>
    /** Files a report (reason and an optional note). The reported person isn't told who reported them. */
    suspend fun report(
        kind: com.circle.app.data.ReportKind,
        targetId: String,
        reason: com.circle.app.data.ReportReason,
        note: String = "",
    ): Outcome<Unit>

    // ---- Notes & threads ----
    fun homeFeed(): Flow<List<Note>>
    fun groupFeed(): Flow<List<Note>>
    fun myNotes(): Flow<List<Note>>
    fun note(noteId: String): Flow<NoteView>
    fun thread(noteId: String): Flow<ThreadResult>
    suspend fun toggleLike(noteId: String): Outcome<Boolean>
    /** A comment, or (with [replyToCommentId]) a reply nested under that comment's top-level comment. */
    suspend fun addComment(noteId: String, text: String, replyToCommentId: String? = null): Outcome<Unit>
    suspend fun toggleCommentLike(noteId: String, commentId: String): Outcome<Boolean>
    /** Deletes your own comment (and, for a top-level one, its replies). */
    suspend fun deleteComment(noteId: String, commentId: String): Outcome<Unit>
    suspend fun repost(noteId: String): Outcome<Unit>
    suspend fun undoRepost(noteId: String): Outcome<Unit>
    /** Posts a note (or a quote, or into a group). Returns the new note's id. Text or media is required. */
    suspend fun post(
        text: String,
        audience: Audience,
        quotedNoteId: String? = null,
        groupId: String? = null,
        media: List<com.circle.app.data.media.MediaRef> = emptyList(),
    ): Outcome<String>
    /** Your own notes only. */
    suspend fun editNote(noteId: String, text: String): Outcome<Unit>
    suspend fun setAudience(noteId: String, audience: Audience): Outcome<Unit>
    suspend fun deleteNote(noteId: String): Outcome<Unit>
    /** Friends this note may be sent to in a direct message (inner-circle notes: your inner circle only). */
    fun shareCandidates(noteId: String): Flow<List<PersonRef>>
    /** Sends the note into your 1:1 chat with [friendId]; returns the chat id. */
    suspend fun shareNoteWithFriend(noteId: String, friendId: String): Outcome<String>

    // ---- Chats ----
    fun chats(): Flow<List<ChatRowView>>
    fun chat(chatId: String): Flow<ChatView?>
    suspend fun sendMessage(chatId: String, text: String, replyToId: String? = null): Outcome<Unit>
    suspend fun shareNote(chatId: String, noteId: String): Outcome<Unit>
    suspend fun deleteForMe(chatId: String, messageId: String): Outcome<Unit>
    suspend fun unsend(chatId: String, messageId: String): Outcome<Unit>
    suspend fun markRead(chatId: String)
    /** Opens (or creates) the 1:1 chat with a friend; returns the chat id. Friends only. */
    suspend fun openDirectChat(personId: String): Outcome<String>
    /** New chat: one friend opens the 1:1 chat; several make a chat (with an optional name). Friends only. */
    suspend fun createChat(friendIds: List<String>, name: String?, lifetimeDays: Int? = null): Outcome<String>
    suspend fun sendMedia(chatId: String, media: List<com.circle.app.data.media.MediaRef>): Outcome<Unit>
    suspend fun sendEventInvite(chatId: String, eventId: String): Outcome<Unit>
    suspend fun setChatPinned(chatId: String, pinned: Boolean): Outcome<Unit>
    suspend fun setChatMuted(chatId: String, muted: Boolean): Outcome<Unit>
    /** Removes the chat from your list only. */
    suspend fun deleteChat(chatId: String): Outcome<Unit>
    /** Clears the messages on your side only. */
    suspend fun clearChat(chatId: String): Outcome<Unit>
    /** Events you host or have a seat at (what you can invite friends to). */
    fun myEvents(): Flow<List<EventSummary>>

    // ---- Groups, events, explore ----
    fun explore(): Flow<ExploreView>
    fun memberGroups(): Flow<List<GroupRef>>
    fun group(groupId: String): Flow<GroupView?>
    fun groupPosts(groupId: String): Flow<List<Note>>
    fun event(eventId: String): Flow<EventView?>
    suspend fun requestToJoin(groupId: String): Outcome<Unit>
    suspend fun leaveGroup(groupId: String): Outcome<Unit>
    suspend fun createGroup(name: String, category: String, description: String, isPublic: Boolean, lifetimeDays: Int? = null): Outcome<String>

    // ---- Temporary groups and chats (TIMED_GROUPS). [id] is a group, a chat with friends, or a group's chat (= its group). ----
    /** When a temporary group or chat made today for [days] days would end and be deleted. */
    fun lifetimePreview(days: Int): LifetimePreview
    /** Host only. Moves the end [days] days later (from today when it has already ended); members are told. */
    suspend fun extendLifetime(id: String, days: Int): Outcome<Unit>
    /** Host only. Removes the end; members are told. */
    suspend fun makePermanent(id: String): Outcome<Unit>
    /** Host only. Ends it now: read-only for 7 days, then deleted. */
    suspend fun endNow(id: String): Outcome<Unit>
    /** True once [id] was a temporary group or chat the server has deleted for everyone. */
    fun wasDeleted(id: String): Flow<Boolean>
    /** Save a copy: counts and sizes for the sheet; null when you may not save one (not a member, permanent, deleted). */
    fun exportPlan(id: String): Flow<com.circle.app.data.export.ExportPlan?>
    /**
     * Save a copy: what you may take home, as one bundle. Members only, from Active to Wrap-up; only
     * what you were allowed to see; people you blocked are left out; display names only.
     */
    suspend fun exportBundle(id: String, withMedia: Boolean): Outcome<com.circle.app.data.export.ExportBundle>
    /** Hosts only. Approving makes the person a member. */
    suspend fun approveJoinRequest(groupId: String, personId: String): Outcome<Unit>
    suspend fun declineJoinRequest(groupId: String, personId: String): Outcome<Unit>
    suspend fun editGroup(groupId: String, name: String, description: String, isPublic: Boolean): Outcome<Unit>
    suspend fun removeMember(groupId: String, personId: String): Outcome<Unit>
    /** Sends invites to friends (they land in the friends' Alerts with a backend). */
    suspend fun inviteToGroup(groupId: String, friendIds: List<String>): Outcome<Int>
    suspend fun inviteToEvent(eventId: String, friendIds: List<String>): Outcome<Int>
    suspend fun takeSeat(eventId: String): Outcome<Unit>
    suspend fun leaveSeat(eventId: String): Outcome<Unit>
    suspend fun addEventComment(eventId: String, text: String, replyToCommentId: String? = null): Outcome<Unit>
    suspend fun toggleEventCommentLike(eventId: String, commentId: String): Outcome<Boolean>
    suspend fun deleteEventComment(eventId: String, commentId: String): Outcome<Unit>
    suspend fun createEvent(
        title: String,
        visibility: EventVisibility,
        description: String,
        capacity: Int,
        startsAt: java.time.Instant = java.time.Instant.now().plusSeconds(7 * 24 * 3600L),
        topic: String = "SOCIAL",
        venue: String = "",
    ): Outcome<String>
    /** Hosts only. */
    suspend fun editEvent(eventId: String, title: String, venue: String, description: String): Outcome<Unit>
    /** Hosts only: the event is removed and everyone with a seat is told. */
    suspend fun cancelEvent(eventId: String): Outcome<Unit>

    // ---- Alerts ----
    fun alerts(): Flow<List<AlertView>>
    fun activity(): Flow<List<AlertView>>
    /** True when anything in Alerts is unread (the dot on the bottom bar). */
    fun hasUnreadAlerts(): Flow<Boolean>
    suspend fun markAllAlertsRead(): Outcome<Unit>
}
