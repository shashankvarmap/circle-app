package com.circle.app.data.repo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.circle.app.data.*
import com.circle.app.data.access.AccessPolicy
import com.circle.app.data.access.Audience
import com.circle.app.data.access.Decision
import com.circle.app.data.access.DenyReason
import com.circle.app.data.access.EventVisibility
import com.circle.app.data.access.NoteAction
import com.circle.app.data.access.NoteFacts
import com.circle.app.data.access.ProfileAccess
import com.circle.app.data.access.Relationship
import com.circle.app.data.access.SharedCardAccess
import com.circle.app.data.access.TimedAction
import com.circle.app.data.lifetime.LifeState
import com.circle.app.ui.theme.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * The repository over the in-memory [CircleStore] (the fake server). Every read asks
 * [AccessPolicy] and returns redacted views; every action is checked the same way.
 * Observations are snapshot flows, so they re-evaluate whenever anything they read changes.
 */
class FakeCircleRepository(
    private val store: CircleStore = CircleStore,
    val cache: ContentCache = ContentCache(),
    /** Demo switch "Sample people accept my requests after 5 seconds"; the app turns it on in preview builds. */
    autoAccept: Boolean = false,
    private val autoAcceptDelayMs: Long = 5_000,
    demoDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Main,
    /** Time for lifetimes (TIMED_GROUPS). Stands in for server time; tests pass a fixed or movable clock. */
    clock: java.time.Clock = com.circle.app.data.AppClock,
) : CircleRepository, DemoControls {

    private val lifetime = com.circle.app.data.lifetime.Lifetime(clock)

    private fun <T> observe(block: () -> T): Flow<T> = snapshotFlow(block)

    // =========================================================================== demo controls

    private var failingState by androidx.compose.runtime.mutableStateOf(false)

    override fun failing(): Flow<Boolean> = observe { failingState }
    override suspend fun setFailing(on: Boolean) { failingState = on }

    private var autoAcceptState by androidx.compose.runtime.mutableStateOf(autoAccept)
    private val demoScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + demoDispatcher)

    override fun autoAccept(): Flow<Boolean> = observe { autoAcceptState }
    override suspend fun setAutoAccept(on: Boolean) { autoAcceptState = on }

    /** Stands in for the other person tapping Accept. Does nothing if you cancelled meanwhile. */
    private fun scheduleAutoAccept(personId: String) {
        if (!autoAcceptState) return
        demoScope.launch {
            kotlinx.coroutines.delay(autoAcceptDelayMs)
            if (autoAcceptState) acceptedByThem(personId)
        }
    }

    override fun timeShift(): Flow<java.time.Duration> = observe { com.circle.app.data.Clock.shift }

    override suspend fun travel(by: java.time.Duration) {
        com.circle.app.data.Clock.shift = com.circle.app.data.Clock.shift.plus(by)
        sweepLifetimes()
    }

    override suspend fun backToToday() { com.circle.app.data.Clock.shift = java.time.Duration.ZERO }

    override fun timedItems(): Flow<List<TimedItem>> = observe {
        val groups = store.groups.mapNotNull { g ->
            val t = g.term as? com.circle.app.data.lifetime.Term.Temporary ?: return@mapNotNull null
            TimedItem(g.id, g.name, true, groupState(g), t.endsAt)
        }
        val chats = store.chats.filter { it.groupId == null }.mapNotNull { c ->
            val t = c.term as? com.circle.app.data.lifetime.Term.Temporary ?: return@mapNotNull null
            TimedItem(c.id, c.name ?: c.id, false, chatState(c), t.endsAt)
        }
        (groups + chats).filter { it.state != LifeState.DELETED }
    }

    override suspend fun jumpTo(id: String, stage: LifeState) {
        val term = (store.group(id)?.term ?: store.chat(id)?.term) as? com.circle.app.data.lifetime.Term.Temporary ?: return
        val minute = java.time.Duration.ofMinutes(1)
        val target = when (stage) {
            LifeState.PERMANENT -> return
            // An hour before the last 3 days, so it reads "4 days"; then just inside each later stage.
            LifeState.ACTIVE -> term.endsAt.minus(com.circle.app.data.lifetime.Lifetime.ENDING_SOON).minus(java.time.Duration.ofHours(1))
            LifeState.ENDING_SOON -> term.endsAt.minus(com.circle.app.data.lifetime.Lifetime.ENDING_SOON).plus(minute)
            LifeState.WRAP_UP -> term.endsAt.plus(minute)
            LifeState.DELETED -> com.circle.app.data.lifetime.Lifetime.deleteAt(term.endsAt).plus(minute)
        }
        com.circle.app.data.Clock.shift = java.time.Duration.between(java.time.Instant.now(), target)
        sweepLifetimes()
    }

    override suspend fun resetDemoData() {
        store.reset()
        store.people.keys.forEach { cache.purgeAuthor(it) }
        sweepLifetimes()
    }

    init {
        // Catch up with time at start: seed reminders, and anything that ended while the app was closed.
        sweepLifetimes()
    }

    /**
     * While the debug switch is on, every action fails as if the network were down. Every action calls
     * this first, so it is also where the fake catches up with time (see [sweepLifetimes]).
     */
    private fun offline(): Outcome.Error? {
        sweepLifetimes()
        return if (failingState) Outcome.Error("offline") else null
    }

    /** A list loaded from the "network": throws while failing, so the screen shows its error state. */
    private fun <T> observeLoaded(block: () -> T): Flow<T> = snapshotFlow {
        if (failingState) throw java.io.IOException("offline (debug switch)")
        block()
    }

    // =========================================================================== relationship

    private fun rel(personId: String): Relationship = when {
        personId == ME -> Relationship.SELF
        personId in store.blockedIds -> Relationship.BLOCKED_BY_ME
        personId in store.blockedMeIds -> Relationship.BLOCKED_ME
        personId in store.friendIds -> Relationship.FRIEND
        store.requestStatus[personId] == RequestStatus.PENDING -> Relationship.REQUEST_RECEIVED
        personId in store.outgoingRequestIds -> Relationship.REQUEST_SENT
        else -> Relationship.NONE
    }

    private fun hiddenFromMe(personId: String) = rel(personId).isBlocked

    // =========================================================================== lifetimes (TIMED_GROUPS)

    private fun groupState(g: GroupInfo): LifeState = lifetime.state(g.term)

    /** A group that no longer exists counts as DELETED. */
    private fun groupState(groupId: String): LifeState = store.group(groupId)?.let { groupState(it) } ?: LifeState.DELETED

    /** A group's chat follows its group's one clock; a chat with friends has its own. */
    private fun chatState(c: Chat): LifeState = c.groupId?.let { groupState(it) } ?: lifetime.state(c.term)

    /** The group's host, or whoever started the chat with friends. */
    private fun chatHost(c: Chat): Boolean =
        c.groupId?.let { id -> store.group(id)?.hostIds?.contains(ME) == true } ?: (c.startedBy == ME)

    /**
     * What the server does on its own as time passes. At the end, open join requests are declined.
     * At deleteAt the group and its chat (or a chat with friends) are hard-deleted for everyone in one
     * step: posts, comments, messages, media, alerts and invites about them, likes, and every cached
     * copy. The saved database follows (it mirrors the store). Copies members saved stay on their phones.
     * Reads never wait for this: they treat DELETED as NotFound already.
     */
    internal fun sweepLifetimes() {
        remind()
        store.groups.filter { groupState(it) == LifeState.WRAP_UP }.forEach { g ->
            if (g.joinRequests.isNotEmpty()) g.joinRequests.clear()
            if (g.joinRequested) g.joinRequested = false
        }
        val goneGroups = store.groups.filter { groupState(it) == LifeState.DELETED }.map { it.id }.toSet()
        val goneChats = store.chats.filter { chatState(it) == LifeState.DELETED }
        if (goneGroups.isEmpty() && goneChats.isEmpty()) return
        val gonePosts = store.posts.filter { it.groupId != null && it.groupId in goneGroups }
        val gonePostIds = gonePosts.map { it.id }.toSet()
        val goneCommentIds = gonePosts.flatMap { p -> p.comments.flatMap { c -> listOf(c.id) + c.replies.map { it.id } } }.toSet()
        val goneMessageIds = goneChats.flatMap { c -> c.messages.map { it.id } }.toSet()
        store.posts.removeAll { it.id in gonePostIds }
        store.likedPostIds.removeAll { it in gonePostIds }
        store.likedCommentIds.removeAll { it in goneCommentIds }
        store.alerts.removeAll { it.postId != null && it.postId in gonePostIds }
        val goneIds = goneGroups + goneChats.map { it.id }
        store.alerts.removeAll { it.timedId != null && it.timedId in goneIds }
        store.goneTimedIds.addAll(goneIds.filter { it !in store.goneTimedIds })
        store.invites.removeAll { it.groupId != null && it.groupId in goneGroups }
        store.hiddenMessageIds.removeAll { it in goneMessageIds }
        store.chats.removeAll { c -> goneChats.any { it === c } }
        store.groups.removeAll { it.id in goneGroups }
        cache.forgetKeys(gonePostIds.flatMap { listOf("note:$it", "thread:$it", "snippet:$it") }.toSet())
    }

    /**
     * The reminders the server sends members (TIMED_GROUPS 7): 3 days and 1 day before the end, at the
     * end, and 1 day before deletion. Each is sent once, dated when it was due; after a jump in time only
     * the latest one due is sent. The server doesn't know who saved a copy (the app hides that last one).
     */
    private fun remind() {
        val now = lifetime.now()
        val day = java.time.Duration.ofDays(1)
        val targets = store.groups.filter { it.isMember }.map { it.id to it.term } +
            store.chats.filter { it.groupId == null && !it.hiddenForMe }.map { it.id to it.term }
        targets.forEach { (id, term) ->
            val t = term as? com.circle.app.data.lifetime.Term.Temporary ?: return@forEach
            val deleteAt = com.circle.app.data.lifetime.Lifetime.deleteAt(t.endsAt)
            if (now >= deleteAt) return@forEach
            val due = listOf(
                AlertKind.TIMED_DELETED_TOMORROW to deleteAt.minus(day),
                AlertKind.TIMED_ENDED to t.endsAt,
                AlertKind.TIMED_ENDS_TOMORROW to t.endsAt.minus(day),
                AlertKind.TIMED_ENDS_SOON to t.endsAt.minus(com.circle.app.data.lifetime.Lifetime.ENDING_SOON),
            ).firstOrNull { now >= it.second } ?: return@forEach
            val alertId = "timed-${due.first.name}-$id-${t.endsAt.epochSecond}"
            if (store.alerts.none { it.id == alertId }) {
                store.alerts.add(0, Alert(alertId, ME, due.first, due.second, timedId = id, timedAt = deleteAt))
            }
        }
    }

    /** What the viewer sees of a term; null when permanent. */
    private fun lifetimeView(term: com.circle.app.data.lifetime.Term, isMember: Boolean, isHost: Boolean): LifetimeView? {
        val t = term as? com.circle.app.data.lifetime.Term.Temporary ?: return null
        val state = lifetime.state(t)
        val now = lifetime.now()
        val deleteAt = com.circle.app.data.lifetime.Lifetime.deleteAt(t.endsAt)
        fun daysUntil(i: java.time.Instant): Int {
            val ms = java.time.Duration.between(now, i).toMillis()
            return if (ms <= 0) 0 else ((ms + DAY_MS - 1) / DAY_MS).toInt()
        }
        val left = java.time.Duration.between(now, t.endsAt).toMillis().coerceAtLeast(0)
        return LifetimeView(
            state = state, endsAt = t.endsAt, deleteAt = deleteAt,
            daysLeft = daysUntil(t.endsAt), daysToDelete = daysUntil(deleteAt),
            elapsed = (1f - left.toFloat() / (30 * DAY_MS)).coerceIn(0f, 1f),
            canChange = AccessPolicy.timed(state, TimedAction.EXTEND, isMember, isHost).allowed,
            canSaveCopy = AccessPolicy.timed(state, TimedAction.EXPORT, isMember, isHost).allowed,
        )
    }

    private fun groupLifetime(g: GroupInfo) = lifetimeView(g.term, g.isMember, g.isMember && ME in g.hostIds)

    private fun chatLifetime(c: Chat): LifetimeView? =
        c.groupId?.let { id -> store.group(id)?.let { groupLifetime(it) } } ?: lifetimeView(c.term, true, c.startedBy == ME)

    override fun lifetimePreview(days: Int): LifetimePreview {
        val zone = java.time.ZoneId.systemDefault()
        val end = lifetime.endOfDay(lifetime.today(zone).plusDays(days.coerceIn(1, 90).toLong()), zone)
        return LifetimePreview(end, com.circle.app.data.lifetime.Lifetime.deleteAt(end))
    }

    /** The term an id points at: a group, a chat with friends, or a group's chat (its group). */
    private class Owner(val get: () -> com.circle.app.data.lifetime.Term, val set: (com.circle.app.data.lifetime.Term) -> Unit, val name: String, val isMember: Boolean, val isHost: Boolean, val others: List<String>)

    private fun owner(id: String): Owner? {
        val g = store.group(id) ?: store.chat(id)?.groupId?.let { store.group(it) }
        if (g != null) return Owner({ g.term }, { g.term = it }, g.name, g.isMember, g.isMember && ME in g.hostIds, (g.hostIds + g.memberIds).distinct() - ME)
        val c = store.chat(id)?.takeIf { it.groupId == null && chatVisible(it) } ?: return null
        return Owner({ c.term }, { c.term = it }, chatTitle(c), true, c.startedBy == ME, c.memberIds - ME)
    }

    private suspend fun changeLifetime(id: String, action: TimedAction, change: (com.circle.app.data.lifetime.Term) -> com.circle.app.data.lifetime.Lifetime.Change): Outcome<Unit> {
        offline()?.let { return it }
        val o = owner(id) ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        denied(AccessPolicy.timed(lifetime.state(o.get()), action, o.isMember, o.isHost))?.let { return it }
        return when (val r = change(o.get())) {
            is com.circle.app.data.lifetime.Lifetime.Change.Done -> {
                o.set(r.term)
                // Members are told by the server ("Priya extended Project Alps. It now ends Fri 6 Nov.").
                o.others.forEach { store.sentNotices.add(Notice(it, "${o.name}: ${action.name.lowercase()}", Clock.now())) }
                sweepLifetimes()
                Outcome.Success(Unit)
            }
            is com.circle.app.data.lifetime.Lifetime.Change.Refused -> Outcome.Error(r.why.name.lowercase())
        }
    }

    override suspend fun extendLifetime(id: String, days: Int): Outcome<Unit> = changeLifetime(id, TimedAction.EXTEND) { term ->
        val zone = java.time.ZoneId.systemDefault()
        val end = (term as? com.circle.app.data.lifetime.Term.Temporary)?.endsAt
        val lastDay = end?.minusNanos(1)?.atZone(zone)?.toLocalDate() ?: lifetime.today(zone)
        val from = maxOf(lastDay, lifetime.today(zone))
        lifetime.extend(term, from.plusDays(days.toLong()), zone)
    }

    override suspend fun makePermanent(id: String): Outcome<Unit> = changeLifetime(id, TimedAction.MAKE_PERMANENT) { lifetime.makePermanent(it) }

    override suspend fun endNow(id: String): Outcome<Unit> = changeLifetime(id, TimedAction.END_NOW) { lifetime.endNow(it) }

    override fun wasDeleted(id: String): Flow<Boolean> = observe { id in store.goneTimedIds }

    /** Who someone is. Null across a block, or for nobody. */
    private fun ref(personId: String): PersonRef? {
        if (hiddenFromMe(personId)) return null
        val p = store.person(personId) ?: return null
        return if (personId == ME) {
            PersonRef(ME, p.name, store.profile.initial, p.color, isMe = true)
        } else PersonRef(p.id, p.name, p.initials, p.color)
    }

    // =========================================================================== notes

    private fun facts(p: Post) = NoteFacts(
        audience = p.audience,
        viewerInAuthorsInnerCircle = p.authorId in store.innerCirclesIAmIn,
        groupId = p.groupId,
        viewerIsGroupMember = p.groupId?.let { store.group(it)?.isMember } == true,
        groupState = p.groupId?.let { groupState(it) } ?: LifeState.PERMANENT,
    )

    private fun decide(p: Post, action: NoteAction): Decision = AccessPolicy.note(rel(p.authorId), facts(p), action)

    private fun original(p: Post): Post = p.repostOfId?.let { store.post(it) } ?: p

    private fun commentCount(p: Post) = p.comments
        .filter { !hiddenFromMe(it.authorId) }
        .sumOf { c -> 1 + c.replies.count { !hiddenFromMe(it.authorId) } }

    private fun toNote(p: Post): Note? {
        val author = ref(p.authorId) ?: return null
        val target = original(p)
        val repost = when {
            decide(target, NoteAction.REPOST).let { it is Decision.Deny } ->
                RepostState.Unavailable((decide(target, NoteAction.REPOST) as Decision.Deny).reason)
            isReposted(target.id) -> RepostState.Reposted
            else -> RepostState.Available
        }
        return Note(
            id = p.id,
            author = author,
            createdAt = p.createdAt,
            text = p.text,
            hasMedia = p.hasMedia || p.media.isNotEmpty(),
            media = p.media,
            audience = p.audience,
            group = p.groupId?.let { store.group(it) }?.let { GroupRef(it.id, it.name, it.initial, it.color) },
            quoted = p.quotedPostId?.let { embedded(it) },
            repostOf = p.repostOfId?.let { embedded(it) },
            actionTargetId = target.id,
            likedByMe = target.id in store.likedPostIds,
            commentCount = commentCount(target),
            repost = repost,
            canShare = decide(target, NoteAction.SHARE).allowed,
        )
    }

    /** A note seen in full, if allowed. */
    private fun fullView(p: Post): NoteView =
        if (decide(p, NoteAction.VIEW).allowed) toNote(p)?.let { NoteView.Full(it) } ?: NoteView.Hidden else NoteView.Hidden

    /** A note travelling inside something else (a quote, a repost card, a chat): full, a snippet, or hidden. */
    private fun embedded(noteId: String): NoteView {
        val p = store.post(noteId) ?: return NoteView.Hidden
        return when (AccessPolicy.sharedCard(rel(p.authorId), facts(p))) {
            SharedCardAccess.OPEN -> fullView(p).also { if (it is NoteView.Full && p.authorId != ME) cache.remember(p.authorId, "note:${p.id}") }
            SharedCardAccess.SNIPPET -> {
                val author = store.person(p.authorId) ?: return NoteView.Hidden
                cache.remember(p.authorId, "snippet:${p.id}")
                NoteView.Snippet(p.id, author.id, author.name, AccessPolicy.snippet(p.text))
            }
            SharedCardAccess.HIDDEN -> NoteView.Hidden
        }
    }

    private fun isReposted(postId: String) = store.posts.any { it.authorId == ME && it.repostOfId == postId }

    private fun visibleNotes(posts: List<Post>): List<Note> =
        posts.filter { decide(it, NoteAction.VIEW).allowed }.mapNotNull { toNote(it) }

    override fun homeFeed(): Flow<List<Note>> = observeLoaded {
        visibleNotes(store.posts.filter { it.groupId == null && it.authorId !in store.mutedIds })
    }

    override fun groupFeed(): Flow<List<Note>> = observe { visibleNotes(store.posts.filter { it.groupId != null }) }

    override fun myNotes(): Flow<List<Note>> = observe {
        visibleNotes(store.posts.filter { it.authorId == ME && it.groupId == null })
    }

    override fun notesBy(personId: String): Flow<List<Note>> = observe {
        if (AccessPolicy.profile(rel(personId)) != ProfileAccess.FULL) emptyList()
        else visibleNotes(store.posts.filter { it.authorId == personId && it.groupId == null && !it.isRepost })
    }

    override fun note(noteId: String): Flow<NoteView> = observe { embedded(noteId) }

    override fun thread(noteId: String): Flow<ThreadResult> = observe { threadNow(noteId) }

    private fun threadNow(noteId: String): ThreadResult {
        val p = store.post(noteId)?.let { original(it) } ?: return ThreadResult.Denied(DenyReason.NOT_FOUND, null)
        val decision = decide(p, NoteAction.OPEN_THREAD)
        if (decision is Decision.Deny) {
            cache.forget(p.authorId, "thread:${p.id}")
            return ThreadResult.Denied(decision.reason, ref(p.authorId))
        }
        val root = toNote(p) ?: return ThreadResult.Denied(DenyReason.NOT_FOUND, null)
        if (p.authorId != ME) cache.remember(p.authorId, "thread:${p.id}")
        return ThreadResult.Opened(Thread(root, comments(p.comments)))
    }

    private fun comments(list: List<Comment>): List<CommentView> = list.mapNotNull { c ->
        val author = ref(c.authorId) ?: return@mapNotNull null
        CommentView(c.id, author, c.createdAt, c.text, comments(c.replies), c.id in store.likedCommentIds)
    }

    private fun denied(d: Decision): Outcome.Denied? = (d as? Decision.Deny)?.let { Outcome.Denied(it.reason) }

    override suspend fun toggleLike(noteId: String): Outcome<Boolean> {
        offline()?.let { return it }
        val p = store.post(noteId)?.let { original(it) } ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        denied(decide(p, NoteAction.LIKE))?.let { return it }
        val nowLiked = !store.likedPostIds.remove(p.id)
        if (nowLiked) store.likedPostIds.add(p.id)
        return Outcome.Success(nowLiked)
    }

    override suspend fun addComment(noteId: String, text: String, replyToCommentId: String?): Outcome<Unit> {
        offline()?.let { return it }
        val p = store.post(noteId)?.let { original(it) } ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        denied(decide(p, NoteAction.COMMENT))?.let { return it }
        val t = text.trim()
        if (t.isEmpty()) return Outcome.Error("empty")
        val comment = Comment(newId("c"), ME, Clock.now(), t)
        if (replyToCommentId == null) {
            p.comments.add(comment)
            return Outcome.Success(Unit)
        }
        // One nested level: a reply to a reply joins the same top-level comment.
        val index = p.comments.indexOfFirst { top -> top.id == replyToCommentId || top.replies.any { it.id == replyToCommentId } }
        if (index < 0) return Outcome.Denied(DenyReason.NOT_FOUND)
        val top = p.comments[index]
        if (hiddenFromMe(top.authorId)) return Outcome.Denied(DenyReason.NOT_FOUND)
        p.comments[index] = top.copy(replies = top.replies + comment)
        return Outcome.Success(Unit)
    }

    private fun findComment(p: Post, commentId: String): Comment? =
        p.comments.firstOrNull { it.id == commentId } ?: p.comments.flatMap { it.replies }.firstOrNull { it.id == commentId }

    override suspend fun toggleCommentLike(noteId: String, commentId: String): Outcome<Boolean> {
        offline()?.let { return it }
        val p = store.post(noteId)?.let { original(it) } ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        denied(decide(p, NoteAction.LIKE))?.let { return it }
        val c = findComment(p, commentId)?.takeIf { !hiddenFromMe(it.authorId) } ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        val nowLiked = !store.likedCommentIds.remove(c.id)
        if (nowLiked) store.likedCommentIds.add(c.id)
        return Outcome.Success(nowLiked)
    }

    override suspend fun deleteComment(noteId: String, commentId: String): Outcome<Unit> {
        offline()?.let { return it }
        val p = store.post(noteId)?.let { original(it) } ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        val c = findComment(p, commentId) ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        if (c.authorId != ME) return Outcome.Error("not yours")
        if (!p.comments.removeAll { it.id == commentId }) {
            val index = p.comments.indexOfFirst { top -> top.replies.any { it.id == commentId } }
            val top = p.comments[index]
            p.comments[index] = top.copy(replies = top.replies.filterNot { it.id == commentId })
        }
        return Outcome.Success(Unit)
    }

    override suspend fun repost(noteId: String): Outcome<Unit> {
        offline()?.let { return it }
        val p = store.post(noteId)?.let { original(it) } ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        denied(decide(p, NoteAction.REPOST))?.let { return it }
        if (!isReposted(p.id)) store.posts.add(0, Post(newId("rp"), ME, Clock.now(), "", repostOfId = p.id))
        return Outcome.Success(Unit)
    }

    override suspend fun undoRepost(noteId: String): Outcome<Unit> {
        offline()?.let { return it }
        val target = store.post(noteId)?.let { original(it) }?.id ?: noteId
        store.posts.removeAll { it.authorId == ME && it.repostOfId == target }
        return Outcome.Success(Unit)
    }

    override suspend fun post(
        text: String,
        audience: Audience,
        quotedNoteId: String?,
        groupId: String?,
        media: List<com.circle.app.data.media.MediaRef>,
    ): Outcome<String> {
        offline()?.let { return it }
        val t = text.trim()
        if (t.isEmpty() && media.isEmpty()) return Outcome.Error("empty")
        val quoted = quotedNoteId?.let { id -> store.post(id)?.let { original(it) } ?: return Outcome.Denied(DenyReason.NOT_FOUND) }
        if (quoted != null) {
            if (groupId != null) return Outcome.Denied(DenyReason.GROUP_POST)
            denied(decide(quoted, NoteAction.QUOTE))?.let { return it }
        }
        if (groupId != null) {
            val g = store.group(groupId)
            denied(AccessPolicy.groupContent(g?.isMember == true))?.let { return it }
            denied(AccessPolicy.timed(groupState(groupId), TimedAction.POST, isMember = true, isHost = ME in g!!.hostIds))?.let { return it }
        }
        val p = Post(
            newId("note"), ME, Clock.now(), t,
            audience = if (groupId != null) Audience.FRIENDS else audience,
            quotedPostId = quoted?.id, groupId = groupId, media = media,
        )
        store.posts.add(0, p)
        return Outcome.Success(p.id)
    }

    /** Your own note, or a refusal. */
    private fun ownNote(noteId: String): Pair<Int, Post>? {
        val i = store.posts.indexOfFirst { it.id == noteId && it.authorId == ME && !it.isRepost }
        return if (i < 0) null else i to store.posts[i]
    }

    override suspend fun editNote(noteId: String, text: String): Outcome<Unit> {
        offline()?.let { return it }
        val (i, p) = ownNote(noteId) ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        val t = text.trim()
        if (t.isEmpty() && p.media.isEmpty() && !p.hasMedia) return Outcome.Error("empty")
        store.posts[i] = p.edited(text = t)
        return Outcome.Success(Unit)
    }

    override suspend fun setAudience(noteId: String, audience: Audience): Outcome<Unit> {
        offline()?.let { return it }
        val (i, p) = ownNote(noteId) ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        if (p.groupId != null) return Outcome.Denied(DenyReason.GROUP_POST)
        store.posts[i] = p.edited(audience = audience)
        // An inner-circle note can't stay reposted.
        if (audience == Audience.INNER_CIRCLE) store.posts.removeAll { it.repostOfId == noteId }
        return Outcome.Success(Unit)
    }

    override suspend fun deleteNote(noteId: String): Outcome<Unit> {
        offline()?.let { return it }
        val (_, p) = ownNote(noteId) ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        store.posts.remove(p)
        store.posts.removeAll { it.repostOfId == noteId } // its "You reposted" cards go with it
        store.likedPostIds.remove(noteId)
        return Outcome.Success(Unit)
    }

    override fun shareCandidates(noteId: String): Flow<List<PersonRef>> = observe {
        val p = store.post(noteId)?.let { original(it) }
        if (p == null || p.groupId != null || !decide(p, NoteAction.SHARE).allowed) emptyList()
        else store.friendIds
            .filter { p.audience == Audience.FRIENDS || it in store.innerCircle }
            .mapNotNull { ref(it) }
    }

    override suspend fun shareNoteWithFriend(noteId: String, friendId: String): Outcome<String> {
        offline()?.let { return it }
        val p = store.post(noteId)?.let { original(it) } ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        // Check before creating a chat, as if sharing into a 1:1 chat with this friend.
        val allInner = p.authorId == ME && friendId in store.innerCircle
        denied(AccessPolicy.shareInto(rel(p.authorId), facts(p), null, allInner))?.let { return it }
        val chat = openDirectChat(friendId)
        if (chat !is Outcome.Success) return chat
        return when (val shared = shareNote(chat.value, noteId)) {
            is Outcome.Success -> Outcome.Success(chat.value)
            is Outcome.Denied -> shared
            is Outcome.Error -> shared
        }
    }

    // =========================================================================== people

    override fun relationshipTo(personId: String): Flow<Relationship> = observe { rel(personId) }

    override fun profile(personId: String): Flow<ProfileView> = observe { profileNow(personId) }

    private fun profileNow(personId: String): ProfileView {
        val r = rel(personId)
        return when (AccessPolicy.profile(r)) {
            ProfileAccess.FULL -> {
                val p = store.person(personId) ?: return ProfileView.NotFound
                if (r == Relationship.SELF) {
                    ProfileView.Full(ref(ME)!!, store.profile.handle, store.profile.pinnedThought, r, null, false)
                } else ProfileView.Full(ref(personId)!!, p.handle, p.pinned, r, p.sinceYear, personId in store.mutedIds)
            }
            // Built from the limited fields only: nothing else is read.
            ProfileAccess.LIMITED -> {
                val p = store.person(personId) ?: return ProfileView.NotFound
                ProfileView.Limited(p.id, p.name, p.initials, p.color, p.handle, p.pinned)
            }
            ProfileAccess.NOT_FOUND -> ProfileView.NotFound
        }
    }

    private fun result(p: Person) = PersonResult(ref(p.id)!!, p.handle, rel(p.id))

    override fun searchPeople(query: String): Flow<List<PersonResult>> = observe {
        val q = query.trim()
        store.people.values
            .filter { it.id != ME && !hiddenFromMe(it.id) }
            .filter { if (q.isEmpty()) rel(it.id) != Relationship.FRIEND else it.name.contains(q, ignoreCase = true) || it.handle.contains(q.removePrefix("@"), ignoreCase = true) }
            .sortedBy { it.name }
            .map { result(it) }
    }

    override fun contactMatches(): Flow<List<PersonResult>> = observe {
        Seed.contactMatchIds.mapNotNull { id -> store.person(id)?.takeIf { !hiddenFromMe(id) }?.let { result(it) } }
    }

    override fun incomingRequests(): Flow<List<RequestView>> = observe {
        store.incomingRequests
            .filter { store.requestStatus[it.personId] != RequestStatus.DECLINED && !hiddenFromMe(it.personId) }
            .mapNotNull { r -> ref(r.personId)?.let { RequestView(it, store.requestStatus[r.personId] == RequestStatus.ACCEPTED, r.receivedAt) } }
    }

    override fun blocked(): Flow<List<PersonRef>> = observe {
        // You can see who you blocked (to unblock them); they can't see you.
        store.blockedIds.mapNotNull { id -> store.person(id)?.let { PersonRef(it.id, it.name, it.initials, it.color) } }
    }

    override fun muted(): Flow<List<PersonRef>> = observe { store.mutedIds.mapNotNull { ref(it) } }

    override suspend fun sendFriendRequest(personId: String): Outcome<Unit> {
        offline()?.let { return it }
        if (store.person(personId) == null) return Outcome.Denied(DenyReason.NOT_FOUND)
        return when (rel(personId)) {
            Relationship.NONE -> { store.outgoingRequestIds.add(personId); scheduleAutoAccept(personId); Outcome.Success(Unit) }
            Relationship.REQUEST_SENT -> Outcome.Success(Unit)
            Relationship.BLOCKED_BY_ME, Relationship.BLOCKED_ME -> Outcome.Denied(DenyReason.BLOCKED)
            else -> Outcome.Error("not possible")
        }
    }

    override suspend fun cancelFriendRequest(personId: String): Outcome<Unit> {
        offline()?.let { return it }
        store.outgoingRequestIds.remove(personId)
        return Outcome.Success(Unit)
    }

    override suspend fun acceptRequest(personId: String): Outcome<Unit> {
        offline()?.let { return it }
        if (rel(personId) != Relationship.REQUEST_RECEIVED) return Outcome.Error("no request")
        store.requestStatus[personId] = RequestStatus.ACCEPTED
        if (personId !in store.friendIds) store.friendIds.add(personId)
        return Outcome.Success(Unit)
    }

    override suspend fun declineRequest(personId: String): Outcome<Unit> {
        offline()?.let { return it }
        if (store.requestStatus[personId] == RequestStatus.PENDING) store.requestStatus[personId] = RequestStatus.DECLINED
        return Outcome.Success(Unit)
    }

    /** They accepted your request. With a backend this arrives as an event; tests use it. */
    internal fun acceptedByThem(personId: String) {
        if (store.outgoingRequestIds.remove(personId) && personId !in store.friendIds) store.friendIds.add(personId)
    }

    /** They blocked you. With a backend this arrives as an event; tests use it. */
    internal fun blockedByThem(personId: String) {
        if (personId !in store.blockedMeIds) store.blockedMeIds.add(personId)
        endFriendship(personId)
    }

    private fun endFriendship(personId: String) {
        store.friendIds.remove(personId); store.innerCircle.remove(personId); store.mutedIds.remove(personId)
        store.outgoingRequestIds.remove(personId)
        if (store.requestStatus[personId] == RequestStatus.PENDING) store.requestStatus[personId] = RequestStatus.DECLINED
        cache.purgeAuthor(personId)
    }

    override suspend fun unfriend(personId: String): Outcome<Unit> {
        offline()?.let { return it }
        if (rel(personId) != Relationship.FRIEND) return Outcome.Error("not a friend")
        store.friendIds.remove(personId); store.innerCircle.remove(personId); store.mutedIds.remove(personId)
        cache.purgeAuthor(personId)
        return Outcome.Success(Unit)
    }

    override suspend fun block(personId: String): Outcome<Unit> {
        offline()?.let { return it }
        if (personId == ME || store.person(personId) == null) return Outcome.Denied(DenyReason.NOT_FOUND)
        if (personId !in store.blockedIds) store.blockedIds.add(personId)
        endFriendship(personId)
        return Outcome.Success(Unit)
    }

    override suspend fun unblock(personId: String): Outcome<Unit> {
        offline()?.let { return it }
        store.blockedIds.remove(personId)
        return Outcome.Success(Unit)
    }

    override suspend fun mute(personId: String): Outcome<Unit> {
        offline()?.let { return it }
        if (rel(personId) != Relationship.FRIEND) return Outcome.Denied(DenyReason.NOT_FRIENDS)
        if (personId !in store.mutedIds) store.mutedIds.add(personId)
        return Outcome.Success(Unit)
    }

    override suspend fun unmute(personId: String): Outcome<Unit> {
        offline()?.let { return it }
        store.mutedIds.remove(personId)
        return Outcome.Success(Unit)
    }

    override suspend fun report(kind: ReportKind, targetId: String, reason: ReportReason, note: String): Outcome<Unit> {
        offline()?.let { return it }
        store.reports.add(Report(kind, targetId, reason, note.trim().take(500), Clock.now()))
        return Outcome.Success(Unit)
    }

    // =========================================================================== getting in

    /** Handles already used in the demo (the prototype's list plus everyone in the seed). */
    private val reservedHandles = setOf("amara", "priya", "jonah", "circle", "admin", "tomas", "elena", "support", "help")

    /** No SMS provider: nothing is sent and any 6 digits are accepted. */
    override val sendsTextMessages: Boolean = false

    override suspend fun requestCode(e164: String): Outcome<Unit> =
        if (e164.startsWith("+") && e164.length >= 8) Outcome.Success(Unit) else Outcome.Error("invalid number")

    override suspend fun verifyCode(e164: String, code: String): Outcome<Unit> =
        if (code.length == 6 && code.all(Char::isDigit)) Outcome.Success(Unit) else Outcome.Error("wrong code")

    override suspend fun isHandleFree(handle: String): Boolean {
        val h = handle.lowercase()
        if (h == store.profile.handle) return true
        return h !in reservedHandles && store.people.values.none { it.id != ME && it.handle == h }
    }

    override suspend fun createAccount(name: String, handle: String, pinnedThought: String, avatarUri: String?): Outcome<Unit> {
        offline()?.let { return it }
        val n = name.trim().take(30)
        if (n.isEmpty()) return Outcome.Error("Add your name")
        if (com.circle.app.data.signup.SignupHandle.problem(handle) != null || !isHandleFree(handle)) return Outcome.Error("handle")
        cache.clear()
        store.startFresh(MyProfile(n, handle, pinnedThought.trim().take(140), avatarUri))
        return Outcome.Success(Unit)
    }

    override suspend fun logIn(e164: String): Outcome<Unit> {
        offline()?.let { return it }
        cache.clear()
        store.reset() // the demo account
        return Outcome.Success(Unit)
    }

    override suspend fun matchContacts(hashes: List<String>): Outcome<List<PersonResult>> {
        offline()?.let { return it }
        // The fake has no phone numbers on file, so it can't match; it returns the demo's "people you may know"
        // and keeps nothing. The real server matches the hashes and deletes them at once.
        return Outcome.Success(Seed.contactMatchIds.mapNotNull { id -> store.person(id)?.takeIf { !hiddenFromMe(id) }?.let { result(it) } })
    }

    override fun inviteLink(): Flow<String> = observe { "circle.app/i/${store.profile.handle}" }
    override fun invitesShared(): Flow<Int> = observe { store.invitesShared }
    override suspend fun recordInviteShared() { store.invitesShared += 1 }

    override suspend fun personByHandle(handle: String): Outcome<String> {
        offline()?.let { return it }
        val p = store.people.values.firstOrNull { it.handle == handle.lowercase() && it.id != ME } ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        if (hiddenFromMe(p.id)) return Outcome.Denied(DenyReason.NOT_FOUND)
        return Outcome.Success(p.id)
    }

    override fun exportLog(): Flow<List<ExportEvent>> = observe { store.exportLog.toList() }

    override suspend fun requestDataExport(): Outcome<Unit> {
        offline()?.let { return it }
        // The export is prepared by an automated job on the server; the fake can only record the request.
        if (store.exportLog.none { it.kind == ExportStep.REQUESTED }) store.exportLog.add(ExportEvent(ExportStep.REQUESTED, Clock.now()))
        return Outcome.Success(Unit)
    }

    // =========================================================================== you & circle

    override fun me(): Flow<MeView> = observe {
        val p = store.profile
        MeView(ref(ME)!!, p.displayName, p.handle, p.pinnedThought, p.avatarUri)
    }

    override fun theme(): Flow<ThemeMode> = observe { store.theme }
    override fun privacy(): Flow<PrivacyPrefs> = observe { store.privacy }
    override fun notificationPrefs(): Flow<NotificationPrefs> = observe { store.notifications }
    override suspend fun setTheme(mode: ThemeMode) { store.theme = mode }
    override suspend fun setPrivacy(prefs: PrivacyPrefs) { store.privacy = prefs }
    override suspend fun setNotificationPrefs(prefs: NotificationPrefs) { store.notifications = prefs }

    override suspend fun saveProfile(displayName: String, handle: String, pinnedThought: String, avatarUri: String?): Outcome<MyProfile> {
        offline()?.let { return it }
        val name = displayName.trim()
        val h = handle.trim().removePrefix("@").lowercase()
        if (name.isEmpty()) return Outcome.Error("Add a display name")
        HandleRules.problem(h)?.let { return Outcome.Error(it) }
        store.profile = MyProfile(name, h, pinnedThought.trim(), avatarUri)
        return Outcome.Success(store.profile)
    }

    override suspend fun deleteAccount(): Outcome<Unit> {
        offline()?.let { return it }
        // The demo restores its seed; with a backend this deletes the account.
        store.reset()
        cache.clear()
        return Outcome.Success(Unit)
    }

    override fun circleSummary(): Flow<CircleSummary> = observe {
        check(AccessPolicy.friendGraph(Relationship.SELF).allowed)
        CircleSummary(store.friendIds.size, store.innerCircle.size)
    }

    override fun friends(): Flow<List<FriendView>> = observe {
        store.friendIds.mapNotNull { id ->
            val p = store.person(id) ?: return@mapNotNull null
            ref(id)?.let { FriendView(it, id in store.innerCircle, p.sinceYear) }
        }
    }

    override suspend fun toggleInnerCircle(personId: String): Outcome<Boolean> {
        offline()?.let { return it }
        if (store.innerCircle.remove(personId)) return Outcome.Success(false)
        if (rel(personId) != Relationship.FRIEND) return Outcome.Denied(DenyReason.NOT_FRIENDS)
        store.innerCircle.add(personId)
        return Outcome.Success(true)
    }

    // =========================================================================== chats

    private fun chatVisible(c: Chat): Boolean = !c.hiddenForMe && AccessPolicy.timedRead(chatState(c)).allowed && when {
        c.groupId != null -> AccessPolicy.groupContent(store.group(c.groupId)?.isMember == true).allowed
        c.directPersonId != null -> !hiddenFromMe(c.directPersonId!!)
        else -> true
    }

    private fun visibleMessages(c: Chat) =
        c.messages.filter { m ->
            m.id !in store.hiddenMessageIds && !hiddenFromMe(m.senderId) && c.clearedAt.let { it == null || m.createdAt.isAfter(it) }
        }

    private fun chatKind(c: Chat) = when {
        c.groupId != null -> ChatKind.GROUP
        c.directPersonId != null -> ChatKind.DIRECT
        else -> ChatKind.MULTI
    }

    private fun chatTitle(c: Chat): String = when {
        c.groupId != null -> store.group(c.groupId)?.name.orEmpty()
        c.name != null -> c.name
        else -> c.directPersonId?.let { store.person(it)?.name }.orEmpty()
    }

    private fun groupRef(id: String?) = id?.let { store.group(it) }?.let { GroupRef(it.id, it.name, it.initial, it.color) }

    override fun chats(): Flow<List<ChatRowView>> = observeLoaded {
        store.chats.filter { chatVisible(it) }.map { c ->
            val last = visibleMessages(c).lastOrNull()
            val kind = chatKind(c)
            ChatRowView(
                id = c.id, kind = kind, title = chatTitle(c),
                person = c.directPersonId?.let { ref(it) },
                group = groupRef(c.groupId),
                members = c.memberIds.mapNotNull { ref(it) },
                preview = last?.let {
                    ChatPreview(
                        mine = it.senderId == ME,
                        senderFirstName = if (kind != ChatKind.DIRECT && it.senderId != ME) store.person(it.senderId)?.firstName else null,
                        kind = it.kind, text = it.text,
                    )
                },
                lastAt = last?.createdAt,
                unread = c.unread,
                pinned = c.pinned,
                muted = c.muted,
                lifetime = chatLifetime(c),
            )
        }.sortedByDescending { it.lastAt } // newest first, never ranked
    }

    override fun chat(chatId: String): Flow<ChatView?> = observe {
        val c = store.chat(chatId)?.takeIf { chatVisible(it) } ?: return@observe null
        val messages = visibleMessages(c)
        val byId = messages.associateBy { it.id }
        ChatView(
            id = c.id, kind = chatKind(c), title = chatTitle(c),
            person = c.directPersonId?.let { ref(it) },
            group = groupRef(c.groupId),
            memberCount = c.groupId?.let { store.group(it)?.memberCount } ?: (c.memberIds.size + 1),
            friendsSinceYear = c.directPersonId?.let { store.person(it)?.sinceYear },
            messages = messages.mapNotNull { m -> messageView(m, byId) },
            firstUnreadId = c.lastReadAt?.let { read -> messages.firstOrNull { it.senderId != ME && it.createdAt.isAfter(read) }?.id },
            muted = c.muted,
            lifetime = chatLifetime(c),
            canSend = chatState(c).isOpen,
        )
    }

    private fun messageView(m: ChatMessage, byId: Map<String, ChatMessage>): MessageView? {
        val sender = ref(m.senderId) ?: return null
        val replied = m.replyToId?.let { byId[it] }
        return MessageView(
            id = m.id, sender = sender, createdAt = m.createdAt, kind = m.kind, text = m.text,
            sharedNote = m.postId?.let { embedded(it) },
            event = m.eventId?.let { id -> store.event(id) }?.let { e ->
                EventCardView(e.id, e.title, e.startsAt, AccessPolicy.event(e.visibility, rel(e.hostId)).allowed, ref(e.hostId))
            },
            replyTo = replied?.let { r ->
                ReplySnippet(r.id, if (r.senderId == ME) "" else store.person(r.senderId)?.firstName.orEmpty(), r.senderId == ME, r.kind, r.text)
            },
            // Receipts are mutual: you see theirs only while yours are on.
            seenAt = m.seenAt?.takeIf { m.senderId == ME && store.privacy.readReceipts },
            media = m.media,
        )
    }

    /** A chat you may write in: visible to you, and a 1:1 only with a friend. */
    private fun writableChat(chatId: String): Pair<Chat?, Outcome.Denied?> {
        val c = store.chat(chatId)?.takeIf { chatVisible(it) } ?: return null to Outcome.Denied(DenyReason.NOT_FOUND)
        c.directPersonId?.let { denied(AccessPolicy.directMessage(rel(it)))?.let { d -> return null to d } }
        denied(AccessPolicy.timed(chatState(c), TimedAction.SEND, isMember = true, isHost = chatHost(c)))?.let { return null to it }
        return c to null
    }

    override suspend fun createChat(friendIds: List<String>, name: String?, lifetimeDays: Int?): Outcome<String> {
        offline()?.let { return it }
        val ids = friendIds.distinct().filter { it != ME }
        if (ids.isEmpty()) return Outcome.Error("empty")
        ids.forEach { id -> denied(AccessPolicy.directMessage(rel(id)))?.let { return it } }
        // A temporary chat is always its own chat, even with one friend: the 1:1 chat stays permanent.
        if (ids.size == 1 && lifetimeDays == null) return openDirectChat(ids.single())
        val term = lifetimeDays?.let { days ->
            when (val r = lifetime.create(days, java.time.ZoneId.systemDefault())) {
                is com.circle.app.data.lifetime.Lifetime.Change.Done -> r.term
                is com.circle.app.data.lifetime.Lifetime.Change.Refused -> return Outcome.Error(r.why.name.lowercase())
            }
        } ?: com.circle.app.data.lifetime.Term.Permanent
        val title = name?.trim()?.takeIf { it.isNotEmpty() }
            ?: ids.mapNotNull { store.person(it)?.firstName }.joinToString(", ")
        val c = Chat(newId("chat"), ids, name = title, messages = emptyList(), startedBy = ME).apply { this.term = term }
        store.chats.add(0, c)
        return Outcome.Success(c.id)
    }

    override suspend fun sendMedia(chatId: String, media: List<com.circle.app.data.media.MediaRef>): Outcome<Unit> {
        offline()?.let { return it }
        val (c, d) = writableChat(chatId); d?.let { return it }
        if (media.isEmpty()) return Outcome.Error("empty")
        c!!.messages.add(ChatMessage(newId("m"), ME, Clock.now(), MessageKind.PHOTO, media = media))
        return Outcome.Success(Unit)
    }

    override suspend fun sendEventInvite(chatId: String, eventId: String): Outcome<Unit> {
        offline()?.let { return it }
        val (c, d) = writableChat(chatId); d?.let { return it }
        val e = store.event(eventId)?.takeIf { eventVisible(it) } ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        c!!.messages.add(ChatMessage(newId("m"), ME, Clock.now(), MessageKind.EVENT_INVITE, eventId = e.id))
        return Outcome.Success(Unit)
    }

    private fun mineChat(chatId: String) = store.chat(chatId)?.takeIf { chatVisible(it) }

    override suspend fun setChatPinned(chatId: String, pinned: Boolean): Outcome<Unit> {
        offline()?.let { return it }
        val c = mineChat(chatId) ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        c.pinned = pinned
        return Outcome.Success(Unit)
    }

    override suspend fun setChatMuted(chatId: String, muted: Boolean): Outcome<Unit> {
        offline()?.let { return it }
        val c = mineChat(chatId) ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        c.muted = muted
        return Outcome.Success(Unit)
    }

    override suspend fun deleteChat(chatId: String): Outcome<Unit> {
        offline()?.let { return it }
        val c = mineChat(chatId) ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        c.clearedAt = Clock.now(); c.hiddenForMe = true; c.pinned = false; c.unread = false
        return Outcome.Success(Unit)
    }

    override suspend fun clearChat(chatId: String): Outcome<Unit> {
        offline()?.let { return it }
        val c = mineChat(chatId) ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        c.clearedAt = Clock.now()
        return Outcome.Success(Unit)
    }

    override fun myEvents(): Flow<List<EventSummary>> = observe {
        store.events.filter { (it.joined || it.hostId == ME) && eventVisible(it) }.sortedBy { it.startsAt }.map { summary(it) }
    }

    override suspend fun sendMessage(chatId: String, text: String, replyToId: String?): Outcome<Unit> {
        offline()?.let { return it }
        val (c, d) = writableChat(chatId); d?.let { return it }
        c!!
        val t = text.trim()
        if (t.isEmpty()) return Outcome.Error("empty")
        val reply = replyToId?.takeIf { id -> visibleMessages(c).any { it.id == id } }
        c.messages.add(ChatMessage(newId("m"), ME, Clock.now(), text = t, replyToId = reply))
        return Outcome.Success(Unit)
    }

    override suspend fun shareNote(chatId: String, noteId: String): Outcome<Unit> {
        offline()?.let { return it }
        val c = store.chat(chatId)?.takeIf { chatVisible(it) } ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        val p = store.post(noteId)?.let { original(it) } ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        c.directPersonId?.let { denied(AccessPolicy.directMessage(rel(it)))?.let { d -> return d } }
        denied(AccessPolicy.timed(chatState(c), TimedAction.SEND, isMember = true, isHost = chatHost(c)))?.let { return it }
        val allInner = p.authorId == ME && c.memberIds.all { it in store.innerCircle }
        denied(AccessPolicy.shareInto(rel(p.authorId), facts(p), c.groupId, allInner))?.let { return it }
        c.messages.add(ChatMessage(newId("m"), ME, Clock.now(), MessageKind.SHARED_NOTE, postId = p.id))
        return Outcome.Success(Unit)
    }

    override suspend fun deleteForMe(chatId: String, messageId: String): Outcome<Unit> {
        offline()?.let { return it }
        if (messageId !in store.hiddenMessageIds) store.hiddenMessageIds.add(messageId)
        return Outcome.Success(Unit)
    }

    override suspend fun unsend(chatId: String, messageId: String): Outcome<Unit> {
        offline()?.let { return it }
        val c = store.chat(chatId) ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        val m = c.messages.firstOrNull { it.id == messageId } ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        if (m.senderId != ME) return Outcome.Error("not yours")
        c.messages.remove(m)
        return Outcome.Success(Unit)
    }

    override suspend fun markRead(chatId: String) {
        store.chat(chatId)?.let { it.unread = false; it.lastReadAt = Clock.now() }
    }

    override suspend fun openDirectChat(personId: String): Outcome<String> {
        offline()?.let { return it }
        denied(AccessPolicy.directMessage(rel(personId)))?.let { return it }
        store.chats.firstOrNull { it.directPersonId == personId }?.let { return Outcome.Success(it.id) }
        val c = Chat(newId("dm"), listOf(personId), messages = emptyList())
        store.chats.add(0, c)
        return Outcome.Success(c.id)
    }

    // =========================================================================== groups & events

    private fun eventVisible(e: EventInfo) = AccessPolicy.event(e.visibility, rel(e.hostId)).allowed

    private fun summary(e: EventInfo) = EventSummary(e.id, e.title, e.topic, e.compassDeg, e.startsAt, e.venue, e.capacity, e.going)

    private fun groupView(g: GroupInfo): GroupView {
        val people = if (AccessPolicy.groupContent(g.isMember).allowed) {
            val hosts = g.hostIds.mapNotNull { ref(it) }
            val members = g.memberIds.mapNotNull { ref(it) }
            GroupPeopleView(
                hosts = hosts, members = members,
                requests = g.joinRequests.mapNotNull { (id, at) -> ref(id)?.let { JoinRequestView(it, at) } },
                hiddenCount = (g.memberCount - hosts.size - members.size).coerceAtLeast(0),
            )
        } else null
        return GroupView(
            GroupRef(g.id, g.name, g.initial, g.color), g.category, g.topic, g.description, g.isPublic,
            g.memberCount, g.isMember, g.joinRequested, g.compassDeg, people, iAmHost = g.isMember && ME in g.hostIds,
            lifetime = groupLifetime(g),
            chatId = if (g.isMember) store.chats.firstOrNull { it.groupId == g.id }?.id else null,
            isOpen = groupState(g).isOpen,
        )
    }

    override fun explore(): Flow<ExploreView> = observe {
        ExploreView(
            events = store.events.filter { eventVisible(it) }.map { summary(it) },
            groups = store.groups.filter { it.isPublic && AccessPolicy.timedRead(groupState(it)).allowed }.map { groupView(it) },
        )
    }

    override fun memberGroups(): Flow<List<GroupRef>> = observe {
        store.groups.filter { it.isMember && AccessPolicy.timedRead(groupState(it)).allowed }.map { GroupRef(it.id, it.name, it.initial, it.color) }
    }

    override fun group(groupId: String): Flow<GroupView?> = observe {
        store.group(groupId)
            ?.takeIf { AccessPolicy.timedRead(groupState(it)).allowed && AccessPolicy.group(it.isPublic, it.isMember).allowed }
            ?.let { groupView(it) }
    }

    override fun groupPosts(groupId: String): Flow<List<Note>> = observe {
        visibleNotes(store.posts.filter { it.groupId == groupId })
    }

    override fun event(eventId: String): Flow<EventView?> = observe {
        val e = store.event(eventId)?.takeIf { eventVisible(it) } ?: return@observe null
        val host = ref(e.hostId) ?: return@observe null
        val hasSeat = e.joined || e.hostId == ME
        EventView(
            summary = summary(e), visibility = e.visibility, host = host, description = e.description,
            seatsLeft = e.seatsLeft, isFull = e.isFull, hasSeat = hasSeat, iAmHost = e.hostId == ME,
            seats = eventSeats(e, e.joined).map { s ->
                when (s.kind) {
                    com.circle.app.data.SeatKind.PERSON -> ref(s.personId!!)?.let { SeatView(SeatKind.PERSON, it, s.isHost) }
                        ?: SeatView(SeatKind.TAKEN, isHost = s.isHost)
                    com.circle.app.data.SeatKind.ME -> SeatView(SeatKind.ME, ref(ME), s.isHost)
                    com.circle.app.data.SeatKind.EMPTY -> SeatView(SeatKind.EMPTY)
                    com.circle.app.data.SeatKind.MORE -> SeatView(SeatKind.MORE, moreCount = s.moreCount)
                }
            },
            thread = if (AccessPolicy.eventThread(hasSeat).allowed) EventThreadView.Open(comments(e.comments))
            else EventThreadView.Locked(e.comments.size),
        )
    }

    override suspend fun requestToJoin(groupId: String): Outcome<Unit> {
        offline()?.let { return it }
        val g = store.group(groupId) ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        if (!g.isPublic) return Outcome.Denied(DenyReason.NOT_FOUND)
        denied(AccessPolicy.timed(groupState(g), TimedAction.REQUEST_TO_JOIN, g.isMember, ME in g.hostIds))?.let { return it }
        if (!g.isMember) g.joinRequested = true // never instant: a host approves it
        return Outcome.Success(Unit)
    }

    override suspend fun leaveGroup(groupId: String): Outcome<Unit> {
        offline()?.let { return it }
        val g = store.group(groupId) ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        if (g.isMember) {
            g.isMember = false
            g.hostIds.remove(ME)
            g.memberCount = (g.memberCount - 1).coerceAtLeast(0)
        }
        return Outcome.Success(Unit)
    }

    private fun hostedGroup(groupId: String): GroupInfo? = store.group(groupId)?.takeIf { it.isMember && ME in it.hostIds }

    override suspend fun approveJoinRequest(groupId: String, personId: String): Outcome<Unit> {
        offline()?.let { return it }
        val g = hostedGroup(groupId) ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        denied(AccessPolicy.timed(groupState(g), TimedAction.JOIN, isMember = true, isHost = true))?.let { return it }
        if (!g.joinRequests.removeAll { it.first == personId }) return Outcome.Error("no request")
        if (personId !in g.memberIds) g.memberIds.add(personId)
        g.memberCount += 1
        return Outcome.Success(Unit)
    }

    override suspend fun declineJoinRequest(groupId: String, personId: String): Outcome<Unit> {
        offline()?.let { return it }
        val g = hostedGroup(groupId) ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        g.joinRequests.removeAll { it.first == personId }
        return Outcome.Success(Unit)
    }

    override suspend fun editGroup(groupId: String, name: String, description: String, isPublic: Boolean): Outcome<Unit> {
        offline()?.let { return it }
        val g = hostedGroup(groupId) ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        if (name.isBlank()) return Outcome.Error("Add a name")
        g.name = name.trim(); g.description = description.trim(); g.isPublic = isPublic
        return Outcome.Success(Unit)
    }

    override suspend fun removeMember(groupId: String, personId: String): Outcome<Unit> {
        offline()?.let { return it }
        val g = hostedGroup(groupId) ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        if (personId == ME || personId in g.hostIds) return Outcome.Error("can't remove a host")
        if (g.memberIds.remove(personId)) g.memberCount = (g.memberCount - 1).coerceAtLeast(1)
        return Outcome.Success(Unit)
    }

    private fun invite(friendIds: List<String>, groupId: String?, eventId: String?): Outcome<Int> {
        val friends = friendIds.distinct().filter { rel(it) == Relationship.FRIEND }
        friends.forEach { store.invites.add(Invite(it, groupId, eventId, Clock.now())) }
        return Outcome.Success(friends.size)
    }

    override suspend fun inviteToGroup(groupId: String, friendIds: List<String>): Outcome<Int> {
        offline()?.let { return it }
        val g = store.group(groupId)?.takeIf { it.isMember } ?: return Outcome.Denied(DenyReason.GROUP_NOT_MEMBER)
        denied(AccessPolicy.timed(groupState(g), TimedAction.INVITE, isMember = true, isHost = ME in g.hostIds))?.let { return it }
        return invite(friendIds, g.id, null)
    }

    override suspend fun inviteToEvent(eventId: String, friendIds: List<String>): Outcome<Int> {
        offline()?.let { return it }
        val e = store.event(eventId)?.takeIf { eventVisible(it) } ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        return invite(friendIds, null, e.id)
    }

    private val tileColors = listOf(AvatarOrange, AvatarSage, AvatarBlue, AvatarRose, AvatarTeal, AvatarAmber)

    override suspend fun createGroup(name: String, category: String, description: String, isPublic: Boolean, lifetimeDays: Int?): Outcome<String> {
        offline()?.let { return it }
        if (name.isBlank()) return Outcome.Error("Add a name")
        val term = lifetimeDays?.let { days ->
            when (val r = lifetime.create(days, java.time.ZoneId.systemDefault())) {
                is com.circle.app.data.lifetime.Lifetime.Change.Done -> r.term
                is com.circle.app.data.lifetime.Lifetime.Change.Refused -> return Outcome.Error(r.why.name.lowercase())
            }
        } ?: com.circle.app.data.lifetime.Term.Permanent
        val cat = category.trim().ifEmpty { "General" }
        val g = GroupInfo(
            id = newId("group"), name = name.trim(), category = cat, topic = cat.uppercase(),
            description = description.trim(), color = tileColors[store.groups.size % tileColors.size],
            isPublic = isPublic, compassDeg = null, memberCount = 1, isMember = true, // the creator is a member
        ).apply { hostIds.add(ME); this.term = term }
        store.groups.add(0, g)
        // Every group gets its linked chat, in Messages > Groups.
        store.chats.add(0, Chat("g-${g.id}", emptyList(), groupId = g.id, messages = emptyList()))
        return Outcome.Success(g.id)
    }

    override suspend fun takeSeat(eventId: String): Outcome<Unit> {
        offline()?.let { return it }
        val e = store.event(eventId)?.takeIf { eventVisible(it) } ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        if (e.joined || e.hostId == ME) return Outcome.Success(Unit)
        if (e.isFull) return Outcome.Error("full")
        e.joined = true
        return Outcome.Success(Unit)
    }

    override suspend fun leaveSeat(eventId: String): Outcome<Unit> {
        offline()?.let { return it }
        val e = store.event(eventId) ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        if (e.joined) {
            e.joined = false // the thread locks again
            val me = store.profile.displayName
            store.sentNotices.add(Notice(e.hostId, "$me left ${e.title}", Clock.now())) // the host is quietly told
        }
        return Outcome.Success(Unit)
    }

    /** The event's thread, if you have a seat. */
    private fun seatedThread(eventId: String): Pair<EventInfo?, Outcome.Denied?> {
        val e = store.event(eventId)?.takeIf { eventVisible(it) } ?: return null to Outcome.Denied(DenyReason.NOT_FOUND)
        denied(AccessPolicy.eventThread(e.joined || e.hostId == ME))?.let { return null to it }
        return e to null
    }

    override suspend fun addEventComment(eventId: String, text: String, replyToCommentId: String?): Outcome<Unit> {
        offline()?.let { return it }
        val (e, d) = seatedThread(eventId); d?.let { return it }
        val t = text.trim()
        if (t.isEmpty()) return Outcome.Error("empty")
        return addTo(e!!.comments, Comment(newId("c"), ME, Clock.now(), t), replyToCommentId)
    }

    override suspend fun toggleEventCommentLike(eventId: String, commentId: String): Outcome<Boolean> {
        offline()?.let { return it }
        val (e, d) = seatedThread(eventId); d?.let { return it }
        val c = (e!!.comments + e.comments.flatMap { it.replies }).firstOrNull { it.id == commentId }
            ?.takeIf { !hiddenFromMe(it.authorId) } ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        val nowLiked = !store.likedCommentIds.remove(c.id)
        if (nowLiked) store.likedCommentIds.add(c.id)
        return Outcome.Success(nowLiked)
    }

    override suspend fun deleteEventComment(eventId: String, commentId: String): Outcome<Unit> {
        offline()?.let { return it }
        val (e, d) = seatedThread(eventId); d?.let { return it }
        return deleteFrom(e!!.comments, commentId)
    }

    /** Adds a comment, or a reply under the top-level comment that [replyTo] belongs to (one level deep). */
    private fun addTo(list: androidx.compose.runtime.snapshots.SnapshotStateList<Comment>, comment: Comment, replyTo: String?): Outcome<Unit> {
        if (replyTo == null) { list.add(comment); return Outcome.Success(Unit) }
        val i = list.indexOfFirst { top -> top.id == replyTo || top.replies.any { it.id == replyTo } }
        if (i < 0 || hiddenFromMe(list[i].authorId)) return Outcome.Denied(DenyReason.NOT_FOUND)
        list[i] = list[i].copy(replies = list[i].replies + comment)
        return Outcome.Success(Unit)
    }

    private fun deleteFrom(list: androidx.compose.runtime.snapshots.SnapshotStateList<Comment>, commentId: String): Outcome<Unit> {
        val c = (list + list.flatMap { it.replies }).firstOrNull { it.id == commentId } ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        if (c.authorId != ME) return Outcome.Error("not yours")
        if (!list.removeAll { it.id == commentId }) {
            val i = list.indexOfFirst { top -> top.replies.any { it.id == commentId } }
            list[i] = list[i].copy(replies = list[i].replies.filterNot { it.id == commentId })
        }
        return Outcome.Success(Unit)
    }

    override suspend fun createEvent(
        title: String,
        visibility: EventVisibility,
        description: String,
        capacity: Int,
        startsAt: java.time.Instant,
        topic: String,
        venue: String,
    ): Outcome<String> {
        offline()?.let { return it }
        if (title.isBlank()) return Outcome.Error("Add a title")
        if (startsAt.isBefore(Clock.now())) return Outcome.Error("past")
        // On the map (inner ring, at its topic's angle) only if it's within the next 7 days; else the list.
        val soon = startsAt.isBefore(Clock.now().plusSeconds(7 * 24 * 3600L))
        val e = EventInfo(
            id = newId("event"), title = title.trim(), topic = topic, compassDeg = if (soon) topicAngle(topic) else null,
            startsAt = startsAt, venue = venue.trim(), visibility = visibility, hostId = ME,
            description = description.trim(), capacity = capacity.coerceAtLeast(1), goingBase = 1, joined = false,
        )
        store.events.add(0, e)
        return Outcome.Success(e.id)
    }

    /** A free angle inside the topic's quarter of the map, so new events don't sit on top of others. */
    private fun topicAngle(topic: String): Float {
        val center = when (topic) { "OUTDOORS" -> 45f; "SOCIAL" -> 135f; "MUSIC" -> 225f; else -> 315f }
        val taken = store.events.mapNotNull { it.compassDeg }
        return listOf(0f, -22f, 22f, -36f, 36f, -11f, 11f).map { center + it }.firstOrNull { a -> taken.none { kotlin.math.abs(it - a) < 10f } } ?: center
    }

    override suspend fun editEvent(eventId: String, title: String, venue: String, description: String): Outcome<Unit> {
        offline()?.let { return it }
        val e = store.event(eventId)?.takeIf { it.hostId == ME } ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        if (title.isBlank()) return Outcome.Error("Add a title")
        e.title = title.trim(); e.venue = venue.trim(); e.description = description.trim()
        return Outcome.Success(Unit)
    }

    override suspend fun cancelEvent(eventId: String): Outcome<Unit> {
        offline()?.let { return it }
        val e = store.event(eventId)?.takeIf { it.hostId == ME } ?: return Outcome.Denied(DenyReason.NOT_FOUND)
        store.events.remove(e)
        // Everyone with a seat is told (the seed's going people stand in for them).
        eventSeats(e, false).mapNotNull { it.personId }.filter { it != ME }.forEach {
            store.sentNotices.add(Notice(it, "${e.title} was cancelled", Clock.now()))
        }
        return Outcome.Success(Unit)
    }

    // =========================================================================== alerts

    private fun alertView(a: Alert): AlertView? {
        val actor = ref(a.actorId) ?: return null
        val timed = if (a.kind in timedKinds) timedAlert(a) ?: return null else null
        // A quoted comment only travels to someone who may read that thread.
        val readable = a.postId?.let { store.post(it) }?.let { decide(it, NoteAction.READ_COMMENTS).allowed } ?: false
        val read = store.alertsReadAt
        return AlertView(
            a.id, actor, a.kind, a.createdAt, a.quote?.takeIf { readable }, a.postId, a.aboutId?.let { ref(it) },
            unread = read == null || a.createdAt.isAfter(read),
            timed = timed,
        )
    }

    /** Your notification switches decide which quiet alerts you get. */
    private fun wanted(a: Alert): Boolean {
        val prefs = store.notifications
        return when (a.kind) {
            AlertKind.LIKED_YOUR_NOTE -> prefs.likes
            AlertKind.COMMENTED_ON_YOUR_NOTE, AlertKind.QUOTED_YOUR_NOTE, AlertKind.REPOSTED_YOUR_NOTE -> prefs.comments
            AlertKind.FRIEND_COMMENTED, AlertKind.FRIEND_REPOSTED -> true
            AlertKind.TIMED_ENDS_SOON, AlertKind.TIMED_ENDS_TOMORROW, AlertKind.TIMED_ENDED, AlertKind.TIMED_DELETED_TOMORROW,
            AlertKind.TIMED_EXTENDED, AlertKind.TIMED_MADE_PERMANENT -> a.timedId?.let { store.group(it) } != null && prefs.groups || a.timedId?.let { store.group(it) } == null && prefs.messages
        }
    }

    private val timedKinds = setOf(
        AlertKind.TIMED_ENDS_SOON, AlertKind.TIMED_ENDS_TOMORROW, AlertKind.TIMED_ENDED, AlertKind.TIMED_DELETED_TOMORROW,
        AlertKind.TIMED_EXTENDED, AlertKind.TIMED_MADE_PERMANENT,
    )

    /** What a timed alert is about, if you can still see it (a group you're in, or a chat with friends). */
    private fun timedAlert(a: Alert): TimedAlertView? {
        val id = a.timedId ?: return null
        store.group(id)?.let { g ->
            if (!g.isMember || !AccessPolicy.timedRead(groupState(g)).allowed) return null
            return TimedAlertView(g.id, g.name, true, null, a.timedAt)
        }
        val c = store.chat(id)?.takeIf { chatVisible(it) } ?: return null
        return TimedAlertView(c.id, chatTitle(c), false, c.id, a.timedAt)
    }

    override fun hasUnreadAlerts(): Flow<Boolean> = observe {
        val read = store.alertsReadAt
        val alertUnread = store.alerts.any { it.kind in aimedAtYou && wanted(it) && !hiddenFromMe(it.actorId) && (read == null || it.createdAt.isAfter(read)) }
        val requestUnread = store.notifications.friendRequests && store.incomingRequests.any {
            store.requestStatus[it.personId] == RequestStatus.PENDING && !hiddenFromMe(it.personId) && (read == null || it.receivedAt.isAfter(read))
        }
        alertUnread || requestUnread
    }

    override suspend fun markAllAlertsRead(): Outcome<Unit> {
        offline()?.let { return it }
        store.alertsReadAt = Clock.now()
        return Outcome.Success(Unit)
    }

    private val aimedAtYou = setOf(AlertKind.LIKED_YOUR_NOTE, AlertKind.COMMENTED_ON_YOUR_NOTE, AlertKind.REPOSTED_YOUR_NOTE, AlertKind.QUOTED_YOUR_NOTE) + timedKinds

    override fun alerts(): Flow<List<AlertView>> = observeLoaded {
        store.alerts.filter { it.kind in aimedAtYou && wanted(it) }.mapNotNull { alertView(it) }.sortedByDescending { it.createdAt }
    }

    override fun activity(): Flow<List<AlertView>> = observe {
        // Only what friends did, and only about notes you may see.
        store.alerts.filter { it.kind !in aimedAtYou && rel(it.actorId) == Relationship.FRIEND }
            .filter { a -> a.postId?.let { store.post(it) }?.let { decide(it, NoteAction.VIEW).allowed } ?: false }
            .mapNotNull { alertView(it) }
    }
}
private const val DAY_MS = 24L * 60 * 60 * 1000
