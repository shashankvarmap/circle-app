package com.circle.app.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.circle.app.ui.theme.*

/**
 * Prototype-only in-memory store shared by every screen, so an action on one screen shows up
 * on the others. No backend: state lives for the lifetime of the process (persistence is a
 * later phase). The sample content is the seed data in [Seed]; [reset] restores it.
 */
object CircleStore {

    // ---- People & the friend graph ----

    private val people = mutableStateMapOf<String, Person>()

    /** Your friends, in a stable order (seed order, then newly accepted). */
    val friendIds = mutableStateListOf<String>()

    /** Private. Nobody is told who is in it, and there is no badge. */
    val innerCircle = mutableStateListOf<String>()

    /** Incoming requests, in arrival order. Declined ones stay here but are hidden. */
    val incomingRequests = mutableStateListOf<FriendRequest>()
    val requestStatus = mutableStateMapOf<String, RequestStatus>()

    /** People you sent a friend request to (by id). Their answer is theirs to give: never instant. */
    val outgoingRequestIds = mutableStateListOf<String>()

    /** Reports you filed: (kind, target id). Moderation comes with the backend. */
    val reports = mutableStateListOf<Pair<String, String>>()

    // ---- Content ----

    val posts = mutableStateListOf<Post>()

    /** Post ids you liked. Private: never counted or shown to anyone but the author. */
    val likedPostIds = mutableStateListOf<String>()

    val groups = mutableStateListOf<GroupInfo>()
    val events = mutableStateListOf<EventInfo>()

    // ---- You, settings & safety ----

    var profile by mutableStateOf(MyProfile())
    var theme by mutableStateOf(ThemeMode.SYSTEM)
    var privacy by mutableStateOf(PrivacyPrefs())
    var notifications by mutableStateOf(NotificationPrefs())

    val chats = mutableStateListOf<Chat>()

    /** Messages you deleted for yourself only: the other people still have them. */
    val hiddenMessageIds = mutableStateListOf<String>()

    /** People you blocked: hidden everywhere on your side. */
    val blockedIds = mutableStateListOf<String>()
    /** Friends you muted: their notes are hidden from your Home, nobody is told. */
    val mutedIds = mutableStateListOf<String>()

    init {
        reset()
    }

    /** Restores the seed data. Also used by unit tests. */
    fun reset() {
        people.clear()
        (listOf(Seed.me) + Seed.namedFriends + Seed.generatedFriends + Seed.requesters + Seed.others).forEach { people[it.id] = it }

        friendIds.clear(); friendIds.addAll((Seed.namedFriends + Seed.generatedFriends).map { it.id })
        innerCircle.clear(); innerCircle.addAll(Seed.initialInnerCircle)

        incomingRequests.clear(); incomingRequests.addAll(Seed.incomingRequests)
        requestStatus.clear(); Seed.incomingRequests.forEach { requestStatus[it.personId] = RequestStatus.PENDING }
        outgoingRequestIds.clear(); reports.clear()

        posts.clear(); posts.addAll(Seed.posts())
        likedPostIds.clear(); likedPostIds.addAll(Seed.initialLikes)
        groups.clear(); groups.addAll(Seed.groups())
        events.clear(); events.addAll(Seed.events())

        profile = MyProfile(); theme = ThemeMode.SYSTEM
        privacy = PrivacyPrefs(); notifications = NotificationPrefs()
        blockedIds.clear(); mutedIds.clear()
        chats.clear(); chats.addAll(ChatSeed.chats()); hiddenMessageIds.clear()
    }

    // ---- Chats ----

    fun chat(id: String): Chat? = chats.firstOrNull { it.id == id }

    /** Messages tab: 1:1 chats and chats you started with friends. */
    val directChats: List<Chat> get() = chats.filter { !it.isGroupChat }

    /** Group Messages tab: chats of groups you are a member of (not ones you only asked to join). */
    val groupChats: List<Chat> get() = chats.filter { c -> c.groupId?.let { groupOrNull(it)?.isMember } == true }

    fun chatTitle(c: Chat): String = when {
        c.groupId != null -> groupOrNull(c.groupId)?.name ?: "Group"
        c.name != null -> c.name
        else -> c.directPersonId?.let { person(it)?.name } ?: "Chat"
    }

    /** What you see of a chat: everything except messages you deleted for yourself. */
    fun visibleMessages(c: Chat): List<ChatMessage> =
        c.messages.filter { it.id !in hiddenMessageIds && it.senderId !in blockedIds }

    /** Opens (or creates) your 1:1 chat with a friend. Only friends can message each other. */
    fun openDirectChat(personId: String): Chat? {
        if (relationshipTo(personId) != Relationship.FRIEND) return null
        chats.firstOrNull { it.directPersonId == personId }?.let { return it }
        return Chat("dm-$personId", listOf(personId), messages = emptyList()).also { chats.add(0, it) }
    }

    /**
     * Why a note can't be shared into [c], or null when it can:
     * inner-circle notes only go to your own inner circle (never to non-friends of the author),
     * and group posts never leave their group.
     */
    fun shareBlockReason(postId: String, c: Chat): String? {
        val p = post(postId)?.let { original(it) } ?: return "This note is no longer available"
        if (p.groupId != null) return if (c.groupId == p.groupId) null else "Group posts stay in the group"
        if (p.audience == Audience.INNER_CIRCLE) {
            if (p.authorId != ME) return "Inner circle notes can't be shared"
            if (c.memberIds.any { !isInner(it) }) return "Only your inner circle can see this note"
        }
        if (!canOpenPost(p)) return "You can only share notes you can see"
        return null
    }

    /** Sends a note into a chat as a shared card, if the privacy rules allow it. */
    fun shareNote(c: Chat, postId: String): ChatMessage? {
        if (shareBlockReason(postId, c) != null) return null
        val m = ChatMessage("m-${System.nanoTime()}", ME, "TODAY", "now", MessageKind.SHARED_NOTE, postId = original(post(postId)!!).id)
        c.messages.add(m)
        return m
    }

    fun message(c: Chat, id: String): ChatMessage? = c.messages.firstOrNull { it.id == id }

    /** One-line summary of a message, for list previews and reply snippets. */
    fun messageSummary(m: ChatMessage): String = when (m.kind) {
        MessageKind.TEXT -> m.text
        MessageKind.SHARED_NOTE -> "Shared a note"
        MessageKind.EVENT_INVITE -> "Event invite"
    }

    /** List preview: "You: ..." for your own last message, "Tomas: ..." in group chats. */
    fun chatPreview(c: Chat): String {
        val last = visibleMessages(c).lastOrNull() ?: return "No messages yet"
        val prefix = when {
            last.senderId == ME -> "You: "
            c.directPersonId == null -> (person(last.senderId)?.firstName ?: "") + ": "
            else -> ""
        }
        return prefix + messageSummary(last)
    }

    fun markRead(c: Chat) { c.unread = false }

    /** Sends a text message at once (no fake replies). Blank text is ignored. */
    fun sendMessage(c: Chat, text: String, replyToId: String? = null): ChatMessage? {
        val t = text.trim()
        if (t.isEmpty()) return null
        val reply = replyToId?.takeIf { id -> visibleMessages(c).any { it.id == id } }
        val m = ChatMessage("m-${System.nanoTime()}", ME, "TODAY", "now", text = t, replyToId = reply)
        c.messages.add(m)
        return m
    }

    /** Removes a message from your chat only; the other person still has it. */
    fun deleteForMe(messageId: String) {
        if (messageId !in hiddenMessageIds) hiddenMessageIds.add(messageId)
    }

    /** Removes your own message for everyone in the chat. Refused for other people's messages. */
    fun unsend(c: Chat, messageId: String): Boolean {
        val m = message(c, messageId) ?: return false
        if (m.senderId != ME) return false
        c.messages.remove(m)
        return true
    }

    // ---- Profile & settings ----

    /** Saves the edit-profile form. Returns why it couldn't, or null on success. */
    fun saveProfile(displayName: String, handle: String, pinnedThought: String, avatarUri: String?): String? {
        val name = displayName.trim()
        val h = handle.trim().removePrefix("@").lowercase()
        if (name.isEmpty()) return "Add a display name"
        HandleRules.problem(h)?.let { return it }
        profile = MyProfile(name, h, pinnedThought.trim(), avatarUri)
        return null
    }

    fun block(id: String) {
        if (id == ME || id in blockedIds) return
        blockedIds.add(id)
        friendIds.remove(id); innerCircle.remove(id); mutedIds.remove(id)
        outgoingRequestIds.remove(id)
        if (requestStatus[id] == RequestStatus.PENDING) requestStatus[id] = RequestStatus.DECLINED
    }

    fun unblock(id: String) { blockedIds.remove(id) }

    fun mute(id: String) { if (isFriend(id) && id !in mutedIds) mutedIds.add(id) }

    fun unmute(id: String) { mutedIds.remove(id) }

    fun person(id: String): Person? = people[id]

    val me: Person get() = people.getValue(ME)

    val friends: List<Person> get() = friendIds.mapNotNull { people[it] }

    val friendCount: Int get() = friendIds.size

    fun isFriend(id: String) = id in friendIds

    // ---- Inner circle ----

    val innerCount: Int get() = innerCircle.size

    fun isInner(id: String) = id in innerCircle

    /** Moves a friend between the inner circle and friends. Non-friends can't be inner. */
    fun toggleInner(id: String) {
        if (!innerCircle.remove(id) && isFriend(id)) innerCircle.add(id)
    }

    // ---- Friend requests ----

    /** Requests still shown in Alerts: pending, or accepted ("✓ Friends · Now friends"). */
    val visibleRequests: List<FriendRequest>
        get() = incomingRequests.filter { requestStatus[it.personId] != RequestStatus.DECLINED }

    fun statusOfRequest(personId: String): RequestStatus? = requestStatus[personId]

    fun acceptRequest(personId: String) {
        if (requestStatus[personId] != RequestStatus.PENDING) return
        requestStatus[personId] = RequestStatus.ACCEPTED
        if (!isFriend(personId)) friendIds.add(personId)
    }

    fun declineRequest(personId: String) {
        if (requestStatus[personId] != RequestStatus.PENDING) return
        requestStatus[personId] = RequestStatus.DECLINED
    }

    // ---- Relationship: the one place that decides what you may see of someone ----

    /**
     * How you relate to [personId]. Profiles, threads, chat members, shared cards and search all
     * branch on this; nothing else should check friendship directly. (The backend must enforce
     * the same rule server-side.)
     */
    fun relationshipTo(personId: String): Relationship = when {
        personId == ME -> Relationship.SELF
        personId in blockedIds -> Relationship.BLOCKED
        isFriend(personId) -> Relationship.FRIEND
        requestStatus[personId] == RequestStatus.PENDING -> Relationship.REQUEST_RECEIVED
        personId in outgoingRequestIds -> Relationship.REQUEST_SENT
        else -> Relationship.NONE
    }

    /** Asks to be friends. Never instant: it stays Requested until they answer. */
    fun sendFriendRequest(personId: String): Boolean {
        if (relationshipTo(personId) != Relationship.NONE || person(personId) == null) return false
        outgoingRequestIds.add(personId)
        return true
    }

    fun cancelFriendRequest(personId: String) {
        outgoingRequestIds.remove(personId)
    }

    /** They accepted your request. With a backend this arrives as an event; the demo has no trigger for it. */
    internal fun acceptedByThem(personId: String) {
        if (outgoingRequestIds.remove(personId) && !isFriend(personId)) friendIds.add(personId)
    }

    /** Ends a friendship on your side: out of your circle, your count and your mutes. */
    fun unfriend(personId: String) {
        friendIds.remove(personId); innerCircle.remove(personId); mutedIds.remove(personId)
    }

    fun report(kind: String, targetId: String) {
        reports.add(kind to targetId)
    }

    /** People search: anyone you haven't blocked. With no query, people you might know (not friends yet). */
    fun searchPeople(query: String): List<Person> {
        val q = query.trim()
        return people.values
            .filter { it.id != ME && relationshipTo(it.id) != Relationship.BLOCKED }
            .filter { if (q.isEmpty()) !isFriend(it.id) else it.name.contains(q, ignoreCase = true) }
            .sortedBy { it.name }
    }

    // ---- Posts, likes & threads ----

    fun post(id: String): Post? = posts.firstOrNull { it.id == id }

    /** Home -> Friends: your notes and your friends' (not muted), outside groups, newest first. */
    val homeFeed: List<Post> get() = posts.filter { p ->
        p.groupId == null && p.authorId !in mutedIds && relationshipTo(p.authorId).let { it == Relationship.SELF || it == Relationship.FRIEND }
    }

    /** Home -> Groups: posts from groups you are a member of. */
    val groupFeed: List<Post> get() = posts.filter { p ->
        p.groupId?.let { groupOrNull(it)?.isMember } == true && p.authorId !in blockedIds
    }

    fun postsInGroup(groupId: String): List<Post> = posts.filter { it.groupId == groupId && it.authorId !in blockedIds }

    /** A friend's thread is open to you; anyone else's stays closed until they accept (group posts: members only). */
    fun canOpenPost(p: Post): Boolean = when {
        p.groupId != null -> groupOrNull(p.groupId)?.isMember == true && p.authorId !in blockedIds
        else -> relationshipTo(p.authorId).let { it == Relationship.SELF || it == Relationship.FRIEND }
    }

    /** People shown in a group, without anyone you blocked. */
    fun visibleHosts(g: GroupInfo) = g.hostIds.filter { it !in blockedIds }
    fun visibleMembers(g: GroupInfo) = g.memberIds.filter { it !in blockedIds }
    fun visibleJoinRequests(g: GroupInfo) = g.joinRequests.filter { it.first !in blockedIds }

    /** Your notes, reposts and quotes (not group posts), newest first. */
    val myPosts: List<Post> get() = posts.filter { it.authorId == ME && it.groupId == null }

    /** What a card's like / comment / repost act on: the original for a repost card, else the post itself. */
    fun original(p: Post): Post = p.repostOfId?.let { post(it) } ?: p

    /** Why a note can't be reposted or quoted, or null when it can. */
    fun repostBlockReason(p: Post): String? {
        val o = original(p)
        return when {
            o.groupId != null -> "Group posts stay in the group"
            o.audience == Audience.INNER_CIRCLE -> "Inner circle notes can't be reposted"
            else -> null
        }
    }

    fun isReposted(postId: String) = posts.any { it.authorId == ME && it.repostOfId == postId }

    /** Adds a "You reposted" card at the top of Home and Profile. Refused for inner-circle and group notes. */
    fun repost(postId: String): Boolean {
        val o = post(postId)?.let { original(it) } ?: return false
        if (repostBlockReason(o) != null || isReposted(o.id)) return false
        posts.add(0, Post("rp-${System.nanoTime()}", ME, "You", ME, me.color, "now", "", repostOfId = o.id))
        return true
    }

    fun undoRepost(postId: String) {
        posts.removeAll { it.authorId == ME && it.repostOfId == postId }
    }

    fun isLiked(postId: String) = postId in likedPostIds

    fun toggleLike(postId: String) {
        if (!likedPostIds.remove(postId)) likedPostIds.add(postId)
    }

    /**
     * Adds your note to the top of Home and Profile (or to a group, when [groupId] is set).
     * With [quotedPostId] it becomes a quote card. Blank text, quoting a note that can't be
     * reposted, or posting to a group you aren't in, is refused (returns null).
     */
    fun addPost(text: String, audience: Audience, quotedPostId: String? = null, groupId: String? = null): Post? {
        val t = text.trim()
        if (t.isEmpty()) return null
        val quoted = quotedPostId?.let { id -> post(id)?.let { original(it) } ?: return null }
        if (quoted != null && (repostBlockReason(quoted) != null || groupId != null)) return null
        if (groupId != null && groupOrNull(groupId)?.isMember != true) return null
        val p = Post(
            "me-${System.nanoTime()}", ME, "You", ME, me.color, "now", t,
            audience = if (groupId != null) Audience.FRIENDS else audience,
            quotedPostId = quoted?.id,
            groupId = groupId
        )
        posts.add(0, p)
        return p
    }

    fun you(time: String, text: String) = Comment("You", ME, me.color, time, text)

    fun addComment(postId: String, text: String) {
        val t = text.trim()
        if (t.isNotEmpty()) post(postId)?.comments?.add(you("now", t))
    }

    // ---- Groups ----

    fun group(id: String?): GroupInfo = groups.firstOrNull { it.id == id } ?: groups.first()

    fun groupOrNull(id: String): GroupInfo? = groups.firstOrNull { it.id == id }

    /** Public group: you ask, and every member can see that you asked. Never instant. */
    fun requestToJoin(g: GroupInfo) {
        if (!g.isMember && g.isPublic) g.joinRequested = true
    }

    fun leaveGroup(g: GroupInfo) {
        if (!g.isMember) return
        g.isMember = false
        g.memberCount = (g.memberCount - 1).coerceAtLeast(0)
    }

    private val tileColors = listOf(AvatarOrange, AvatarSage, AvatarBlue, AvatarRose, AvatarTeal, AvatarAmber)

    fun createGroup(name: String, category: String, description: String, isPublic: Boolean): GroupInfo {
        val cat = category.trim().ifEmpty { "General" }
        val g = GroupInfo(
            id = "group-${System.nanoTime()}",
            name = name.trim(),
            category = cat,
            topic = cat.uppercase(),
            description = description.trim(),
            color = tileColors[groups.size % tileColors.size],
            isPublic = isPublic,
            compassDeg = null, // topic angles for new groups come with the Explore "See all" work
            memberCount = 1,
            isMember = true // the creator is automatically a member
        )
        groups.add(0, g)
        return g
    }

    // ---- Events ----

    fun event(id: String?): EventInfo = events.firstOrNull { it.id == id } ?: events.first()

    fun hostOf(e: EventInfo): Person = people[e.hostId] ?: me

    /** Friends-only events are for the host's friends; friends-of-friends and open-nearby events are visible to you. */
    fun canSeeEvent(e: EventInfo): Boolean =
        e.visibility != "FRIENDS" || relationshipTo(e.hostId).let { it == Relationship.SELF || it == Relationship.FRIEND }

    /** You have a seat: you took one, or you're the host. */
    fun hasSeat(e: EventInfo) = e.joined || e.hostId == ME

    /** Takes a seat. Refused when the table is full. Returns whether you now have a seat. */
    fun joinEvent(e: EventInfo): Boolean {
        if (hasSeat(e)) return true
        if (e.isFull) return false
        e.joined = true
        return true
    }

    /** Leaves the seat: the thread locks again and the host is quietly told (with a backend). */
    fun leaveEvent(e: EventInfo) {
        e.joined = false
    }

    fun addEventComment(e: EventInfo, text: String) {
        val t = text.trim()
        if (t.isNotEmpty() && hasSeat(e)) e.comments.add(you("now", t))
    }

    fun createEvent(title: String, venue: String, visibility: String, description: String, capacity: Int): EventInfo {
        val e = EventInfo(
            id = "event-${System.nanoTime()}",
            title = title.trim(),
            month = "TBD", day = "–",
            topic = "EVENT", compassDeg = null,
            whenLabel = "Date to be decided",
            venue = venue.trim().ifEmpty { "Venue to be decided" },
            visibility = visibility.uppercase(),
            hostId = ME,
            description = description.trim(),
            capacity = capacity.coerceAtLeast(1),
            goingBase = 1, // you, the host
            joined = false
        )
        events.add(0, e)
        return e
    }
}
