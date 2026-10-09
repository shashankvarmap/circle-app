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
    suspend fun report(kind: String, targetId: String): Outcome<Unit>

    // ---- Notes & threads ----
    fun homeFeed(): Flow<List<Note>>
    fun groupFeed(): Flow<List<Note>>
    fun myNotes(): Flow<List<Note>>
    fun note(noteId: String): Flow<NoteView>
    fun thread(noteId: String): Flow<ThreadResult>
    suspend fun toggleLike(noteId: String): Outcome<Boolean>
    suspend fun addComment(noteId: String, text: String): Outcome<Unit>
    suspend fun repost(noteId: String): Outcome<Unit>
    suspend fun undoRepost(noteId: String): Outcome<Unit>
    /** Posts a note (or a quote, or into a group). Returns the new note's id. */
    suspend fun post(text: String, audience: Audience, quotedNoteId: String? = null, groupId: String? = null): Outcome<String>

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

    // ---- Groups, events, explore ----
    fun explore(): Flow<ExploreView>
    fun memberGroups(): Flow<List<GroupRef>>
    fun group(groupId: String): Flow<GroupView?>
    fun groupPosts(groupId: String): Flow<List<Note>>
    fun event(eventId: String): Flow<EventView?>
    suspend fun requestToJoin(groupId: String): Outcome<Unit>
    suspend fun leaveGroup(groupId: String): Outcome<Unit>
    suspend fun createGroup(name: String, category: String, description: String, isPublic: Boolean): Outcome<String>
    suspend fun takeSeat(eventId: String): Outcome<Unit>
    suspend fun leaveSeat(eventId: String): Outcome<Unit>
    suspend fun addEventComment(eventId: String, text: String): Outcome<Unit>
    suspend fun createEvent(title: String, visibility: EventVisibility, description: String, capacity: Int): Outcome<String>

    // ---- Alerts ----
    fun alerts(): Flow<List<AlertView>>
    fun activity(): Flow<List<AlertView>>
}
