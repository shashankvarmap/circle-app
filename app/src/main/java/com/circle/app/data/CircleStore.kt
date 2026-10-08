package com.circle.app.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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

    /** Outgoing requests, keyed by name. Shared by Contacts Sync and Explore → People. */
    val friendStatus = mutableStateMapOf<String, FriendStatus>()

    // ---- Content ----

    val posts = mutableStateListOf<Post>()

    /** Post ids you liked. Private: never counted or shown to anyone but the author. */
    val likedPostIds = mutableStateListOf<String>()

    val groups = mutableStateListOf<GroupInfo>()
    val events = mutableStateListOf<EventInfo>()

    init {
        reset()
    }

    /** Restores the seed data. Also used by unit tests. */
    fun reset() {
        people.clear()
        (listOf(Seed.me) + Seed.namedFriends + Seed.generatedFriends + Seed.requesters).forEach { people[it.id] = it }

        friendIds.clear(); friendIds.addAll((Seed.namedFriends + Seed.generatedFriends).map { it.id })
        innerCircle.clear(); innerCircle.addAll(Seed.initialInnerCircle)

        incomingRequests.clear(); incomingRequests.addAll(Seed.incomingRequests)
        requestStatus.clear(); Seed.incomingRequests.forEach { requestStatus[it.personId] = RequestStatus.PENDING }
        friendStatus.clear()

        posts.clear(); posts.addAll(Seed.posts())
        likedPostIds.clear(); likedPostIds.addAll(Seed.initialLikes)
        groups.clear(); groups.addAll(Seed.groups())
        events.clear(); events.addAll(Seed.events())
    }

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

    fun hasPendingRequestFrom(name: String): FriendRequest? =
        incomingRequests.firstOrNull { people[it.personId]?.name == name && requestStatus[it.personId] == RequestStatus.PENDING }

    fun acceptRequest(personId: String) {
        if (requestStatus[personId] != RequestStatus.PENDING) return
        requestStatus[personId] = RequestStatus.ACCEPTED
        if (!isFriend(personId)) friendIds.add(personId)
    }

    fun declineRequest(personId: String) {
        if (requestStatus[personId] != RequestStatus.PENDING) return
        requestStatus[personId] = RequestStatus.DECLINED
    }

    fun statusOf(name: String): FriendStatus {
        if (friends.any { it.name == name }) return FriendStatus.ACCEPTED
        return friendStatus[name] ?: FriendStatus.NONE
    }

    /** none → Requested. Acceptance is the other person's decision, so it never happens on its own here. */
    fun sendFriendRequest(name: String) {
        if (statusOf(name) == FriendStatus.NONE) friendStatus[name] = FriendStatus.REQUESTED
    }

    fun cancelFriendRequest(name: String) {
        if (friendStatus[name] == FriendStatus.REQUESTED) friendStatus.remove(name)
    }

    // ---- Posts, likes & threads ----

    fun post(id: String): Post? = posts.firstOrNull { it.id == id }

    val myPosts: List<Post> get() = posts.filter { it.authorId == ME }

    fun isLiked(postId: String) = postId in likedPostIds

    fun toggleLike(postId: String) {
        if (!likedPostIds.remove(postId)) likedPostIds.add(postId)
    }

    /** Adds your note to the top of Home and Profile. Blank text is ignored. */
    fun addPost(text: String, audience: Audience): Post? {
        val t = text.trim()
        if (t.isEmpty()) return null
        val p = Post("me-${System.nanoTime()}", ME, "You", ME, me.color, "now", t, audience = audience)
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
