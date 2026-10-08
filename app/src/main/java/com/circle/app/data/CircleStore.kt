package com.circle.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import com.circle.app.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A comment in a thread. Only top-level comments carry [replies]; one layer deep. */
data class Comment(
    val author: String,
    val initials: String,
    val color: Color,
    val time: String,
    val text: String,
    val replies: List<Comment> = emptyList()
)

class Post(
    val id: String,
    val author: String,
    val initials: String,
    val color: Color,
    val time: String,
    val text: String,
    val hasMedia: Boolean = false,
    val quoted: Pair<String, String>? = null, // author to text
    comments: List<Comment> = emptyList()
) {
    val comments: SnapshotStateList<Comment> = mutableStateListOf<Comment>().apply { addAll(comments) }
}

class GroupInfo(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val color: Color,
    val isPublic: Boolean,
    memberCount: Int,
    isMember: Boolean
) {
    val initial: String get() = name.first().uppercase()
    var memberCount by mutableIntStateOf(memberCount)
    var isMember by mutableStateOf(isMember)
    /** Pending join request — visible to every member, never auto-approved. */
    var joinRequested by mutableStateOf(false)
}

class EventInfo(
    val id: String,
    val title: String,
    val month: String,
    val day: String,
    val whenLabel: String,
    val venue: String,
    val visibility: String,
    val hostName: String,
    val hostInitials: String,
    val hostColor: Color,
    val description: String,
    val capacity: Int,
    going: Int,
    joined: Boolean,
    comments: List<Comment> = emptyList()
) {
    var going by mutableIntStateOf(going)
    var joined by mutableStateOf(joined)
    val comments: SnapshotStateList<Comment> = mutableStateListOf<Comment>().apply { addAll(comments) }
}

enum class FriendStatus { NONE, REQUESTED, ACCEPTED }

data class Person(val name: String, val initials: String, val color: Color, val subtitle: String)

/**
 * Prototype-only in-memory store shared by every screen. No backend: state lives
 * for the lifetime of the process. The sample content is just the seed data.
 */
object CircleStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun you(time: String, text: String) = Comment("You", "S", AvatarSlate, time, text)

    // ---- Posts & threads ----
    val posts = mutableStateListOf(
        Post(
            "amara-puzzle", "Amara Osei", "AO", AvatarOrange, "2h",
            "Finally finished the puzzle we started at Mom's over the summer.",
            comments = listOf(
                Comment(
                    "Priya Kapoor", "PK", AvatarSage, "1h", "Aww, that's so wholesome!",
                    replies = listOf(
                        Comment("Amara Osei", "AO", AvatarOrange, "58m", "It took us three months 😅"),
                        Comment("Jonah Lee", "JL", AvatarBlue, "40m", "Frame it!")
                    )
                ),
                Comment(
                    "Jonah Lee", "JL", AvatarBlue, "1h", "Which puzzle was it?",
                    replies = listOf(Comment("Amara Osei", "AO", AvatarOrange, "50m", "The 2000-piece lighthouse one."))
                ),
                Comment("Elena Voss", "EV", AvatarRose, "30m", "Mom must be thrilled.")
            )
        ),
        Post(
            "priya-seaglass", "Priya Kapoor", "PK", AvatarSage, "5h",
            "Sea glass hunting turned into a two hour walk. Worth it.",
            comments = listOf(Comment("Amara Osei", "AO", AvatarOrange, "4h", "Save me a green one!"))
        ),
        Post(
            "you-coffee", "You", "S", AvatarSlate, "1h",
            "This is exactly the energy I needed today.",
            quoted = "Jonah Lee" to "Some days the good coffee is the whole plan."
        ),
        Post("you-media", "You", "S", AvatarSlate, "40m", "", hasMedia = true),
        Post(
            "group-golden-hour", "Priya Kapoor", "PK", AvatarSage, "40m",
            "Golden hour shoot this Sunday — meet at the cathedral steps, 6:30 sharp.",
            comments = listOf(Comment("Tomas Novak", "TN", AvatarBlue, "20m", "I'll bring the spare tripod."))
        ),
        Post("group-catan", "Jonah Lee", "JL", AvatarBlue, "2h", "Anyone up for Catan tonight?"),
        Post("group-long-exposure", "Tomas Novak", "TN", AvatarBlue, "3h", "Finally got the long-exposure shot of the fountain right.")
    )

    fun post(id: String): Post? = posts.firstOrNull { it.id == id }

    // ---- Friends ----
    var friendCount by mutableIntStateOf(128)

    /** Outgoing requests, keyed by name. Shared by Contacts Sync and Explore → People. */
    val friendStatus = mutableStateMapOf("Marco Diaz" to FriendStatus.REQUESTED)

    fun statusOf(name: String) = friendStatus[name] ?: FriendStatus.NONE

    /** none → Requested → (simulated) Accepted. */
    fun sendFriendRequest(name: String) {
        if (statusOf(name) != FriendStatus.NONE) return
        friendStatus[name] = FriendStatus.REQUESTED
        scope.launch {
            delay(1800)
            if (statusOf(name) == FriendStatus.REQUESTED) {
                friendStatus[name] = FriendStatus.ACCEPTED
                friendCount++
            }
        }
    }

    val incomingRequests = mutableStateListOf(
        Person("Riya Sharma", "RS", AvatarRose, "3 mutual friends"),
        Person("Marco Diaz", "MD", AvatarTeal, "1 mutual friend")
    )

    fun acceptRequest(p: Person) {
        incomingRequests.remove(p)
        friendStatus[p.name] = FriendStatus.ACCEPTED
        friendCount++
    }

    fun declineRequest(p: Person) {
        incomingRequests.remove(p)
    }

    // ---- Groups ----
    val groups = mutableStateListOf(
        GroupInfo(
            "photo-walk", "Aachen Photo Walk", "Photography · Aachen",
            "Weekend photo walks around Aachen — the cathedral, the Elisenbrunnen, wherever the light looks good. All levels welcome.",
            AvatarOrange, isPublic = true, memberCount = 128, isMember = true
        ),
        GroupInfo(
            "sunday-trail", "Sunday Trail Runners", "Fitness & Sports",
            "Easy Sunday morning trail runs, followed by a very necessary brunch.",
            AvatarSage, isPublic = false, memberCount = 24, isMember = true
        ),
        GroupInfo(
            "board-game-club", "RWTH Board Game Club", "Gaming · Aachen",
            "Weekly game nights on campus. Newcomers and rules-lawyers equally welcome.",
            AvatarBlue, isPublic = false, memberCount = 57, isMember = true
        ),
        GroupInfo(
            "aachen-trail", "Aachen Trail Runners", "Fitness & Sports",
            "Weekend runs around the Dreiländereck. All paces welcome.",
            AvatarTeal, isPublic = true, memberCount = 128, isMember = false
        ),
        GroupInfo(
            "rwth-photo", "RWTH Photography Club", "Photography",
            "Campus shoots, darkroom nights, the occasional exhibit.",
            AvatarRose, isPublic = true, memberCount = 212, isMember = false
        )
    )

    fun group(id: String?): GroupInfo = groups.firstOrNull { it.id == id } ?: groups.first()

    fun requestToJoin(g: GroupInfo) {
        if (!g.isMember) g.joinRequested = true
    }

    private val tileColors = listOf(AvatarOrange, AvatarSage, AvatarBlue, AvatarRose, AvatarTeal, AvatarAmber)

    fun createGroup(name: String, category: String, description: String, isPublic: Boolean): GroupInfo {
        val g = GroupInfo(
            id = "group-${System.currentTimeMillis()}",
            name = name.trim(),
            category = category.trim().ifEmpty { "General" },
            description = description.trim(),
            color = tileColors[groups.size % tileColors.size],
            isPublic = isPublic,
            memberCount = 1,
            isMember = true // the creator is automatically a member
        )
        groups.add(0, g)
        return g
    }

    // ---- Events ----
    val events = mutableStateListOf(
        EventInfo(
            "board-game-night", "Board Game Night", "SEP", "18", "Sep 18, 6:00 PM",
            "Kaffeehaus Zeitlos, Aachen", "FRIENDS OF FRIENDS", "Priya Kapoor", "PK", AvatarSage,
            "Monthly board game night — bringing Catan and Wingspan, but open to requests. Snacks provided, BYO drink.",
            capacity = 12, going = 6, joined = true,
            comments = listOf(
                Comment("Jonah Lee", "JL", AvatarBlue, "2h", "Bringing Wingspan too if anyone wants a break from Catan."),
                Comment("Amara Osei", "AO", AvatarOrange, "1h", "Count me in, I'll bring the good snacks this time.")
            )
        ),
        EventInfo(
            "trail-brunch", "Trail Run + Brunch", "SEP", "19", "Sep 19, 8:30 AM",
            "Forest Park Trailhead", "FRIENDS", "Jonah Lee", "JL", AvatarBlue,
            "Easy 8k loop, then pancakes. Walkers welcome for the brunch half.",
            capacity = 10, going = 4, joined = false,
            comments = listOf(Comment("Priya Kapoor", "PK", AvatarSage, "3h", "Is the trail muddy after the rain?"))
        )
    )

    fun event(id: String?): EventInfo = events.firstOrNull { it.id == id } ?: events.first()

    fun setJoined(e: EventInfo, joined: Boolean) {
        if (e.joined == joined) return
        e.joined = joined
        e.going += if (joined) 1 else -1
    }

    fun createEvent(title: String, venue: String, visibility: String, description: String, capacity: Int): EventInfo {
        val e = EventInfo(
            id = "event-${System.currentTimeMillis()}",
            title = title.trim(),
            month = "TBD", day = "–", whenLabel = "Date to be decided",
            venue = venue.trim().ifEmpty { "Venue to be decided" },
            visibility = visibility.uppercase(),
            hostName = "You", hostInitials = "S", hostColor = AvatarSlate,
            description = description.trim(),
            capacity = capacity, going = 1, joined = true // the host is going
        )
        events.add(0, e)
        return e
    }
}
