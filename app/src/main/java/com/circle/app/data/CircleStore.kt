package com.circle.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The fake "server": every record in the demo, in Compose snapshot state so changes propagate.
 * It holds data and raw mutations only. **It decides nothing about access**: the repository
 * (data/repo/FakeCircleRepository) asks AccessPolicy and returns redacted views. Nothing in ui/
 * may import this (a unit test enforces it). [reset] restores the seed.
 */
object CircleStore {

    // ---- People & the friend graph ----

    val people = mutableStateMapOf<String, Person>()

    /** Your friends, in a stable order (seed order, then newly accepted). */
    val friendIds = mutableStateListOf<String>()

    /** Your inner circle. Private: nobody is told who is in it, and there is no badge. */
    val innerCircle = mutableStateListOf<String>()

    /** Authors whose inner circle includes you (server-side knowledge, never shown). */
    val innerCirclesIAmIn = mutableStateListOf<String>()

    /** Incoming requests, in arrival order. Declined ones stay here but are hidden. */
    val incomingRequests = mutableStateListOf<FriendRequest>()
    val requestStatus = mutableStateMapOf<String, RequestStatus>()

    /** People you sent a friend request to. Their answer is theirs to give: never instant. */
    val outgoingRequestIds = mutableStateListOf<String>()

    /** People you blocked: hidden everywhere on your side. */
    val blockedIds = mutableStateListOf<String>()

    /** People who blocked you (the server knows; you only notice they're gone). */
    val blockedMeIds = mutableStateListOf<String>()

    /** Friends you muted: their notes are hidden from your Home, nobody is told. */
    val mutedIds = mutableStateListOf<String>()

    /** Reports you filed. Moderation comes with the backend. */
    val reports = mutableStateListOf<Report>()

    // ---- Content ----

    val posts = mutableStateListOf<Post>()

    /** Post ids you liked. Private: never counted or shown to anyone but the author. */
    val likedPostIds = mutableStateListOf<String>()

    /** Comment ids you liked. Private in the same way. */
    val likedCommentIds = mutableStateListOf<String>()

    val groups = mutableStateListOf<GroupInfo>()
    val events = mutableStateListOf<EventInfo>()
    val chats = mutableStateListOf<Chat>()

    /** Messages you deleted for yourself only: the other people still have them. */
    val hiddenMessageIds = mutableStateListOf<String>()

    val alerts = mutableStateListOf<Alert>()

    /** Your data-export audit log, and how many invite links you've shared (shown to you only). */
    val exportLog = mutableStateListOf<ExportEvent>()
    var invitesShared by mutableStateOf(0)

    /** When you last opened Alerts or tapped "Mark all read"; anything newer is unread. */
    var alertsReadAt by mutableStateOf<java.time.Instant?>(null)

    /** Invitations you sent, and quiet notices for other people (delivered by a backend). */
    val invites = mutableStateListOf<Invite>()
    val sentNotices = mutableStateListOf<Notice>()

    // ---- You & settings ----

    var profile by mutableStateOf(MyProfile())
    var theme by mutableStateOf(ThemeMode.SYSTEM)
    var privacy by mutableStateOf(PrivacyPrefs())
    var notifications by mutableStateOf(NotificationPrefs())

    init {
        reset()
    }

    /** Restores the seed data. Also used by unit tests. */
    fun reset() {
        people.clear()
        (listOf(Seed.me) + Seed.namedFriends + Seed.generatedFriends + Seed.requesters + Seed.others).forEach { people[it.id] = it }

        friendIds.clear(); friendIds.addAll((Seed.namedFriends + Seed.generatedFriends).map { it.id })
        innerCircle.clear(); innerCircle.addAll(Seed.initialInnerCircle)
        innerCirclesIAmIn.clear(); innerCirclesIAmIn.addAll(Seed.innerCirclesIAmIn)

        val requests = Seed.incomingRequests()
        incomingRequests.clear(); incomingRequests.addAll(requests)
        requestStatus.clear(); requests.forEach { requestStatus[it.personId] = RequestStatus.PENDING }
        outgoingRequestIds.clear(); reports.clear()
        blockedIds.clear(); blockedMeIds.clear(); mutedIds.clear()

        posts.clear(); posts.addAll(Seed.posts())
        likedPostIds.clear(); likedPostIds.addAll(Seed.initialLikes)
        likedCommentIds.clear()
        groups.clear(); groups.addAll(Seed.groups())
        events.clear(); events.addAll(Seed.events())
        chats.clear(); chats.addAll(ChatSeed.chats()); hiddenMessageIds.clear()
        alerts.clear(); alerts.addAll(Seed.alerts())
        invites.clear(); sentNotices.clear(); alertsReadAt = null
        exportLog.clear(); invitesShared = 0

        profile = MyProfile(); theme = ThemeMode.SYSTEM
        privacy = PrivacyPrefs(); notifications = NotificationPrefs()
    }

    /**
     * A brand-new account: the people already on Circle are still there to find, but you have no
     * friends, chats, groups, seats or notes yet, so Home starts empty (ACCESS_AND_LAUNCH 1).
     */
    fun startFresh(p: MyProfile) {
        reset()
        friendIds.clear(); innerCircle.clear(); innerCirclesIAmIn.clear()
        incomingRequests.clear(); requestStatus.clear()
        posts.removeAll { it.authorId == ME }
        likedPostIds.clear()
        groups.forEach { g -> if (g.isMember) g.memberCount -= 1; g.isMember = false; g.joinRequested = false; g.hostIds.remove(ME) }
        events.forEach { it.joined = false }
        chats.clear(); alerts.clear()
        profile = p
    }

    fun person(id: String): Person? = people[id]
    fun post(id: String): Post? = posts.firstOrNull { it.id == id }
    fun group(id: String): GroupInfo? = groups.firstOrNull { it.id == id }
    fun event(id: String): EventInfo? = events.firstOrNull { it.id == id }
    fun chat(id: String): Chat? = chats.firstOrNull { it.id == id }
}
