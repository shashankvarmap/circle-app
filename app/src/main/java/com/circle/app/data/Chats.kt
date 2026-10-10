package com.circle.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

enum class MessageKind { TEXT, SHARED_NOTE, EVENT_INVITE, PHOTO }

/**
 * One message in a chat. Rich messages point at a post ([postId], a shared note) or an event
 * ([eventId], an invite). [replyToId] is the message this one replies to (shown as a snippet).
 */
data class ChatMessage(
    val id: String,
    val senderId: String,
    val createdAt: Instant,
    val kind: MessageKind = MessageKind.TEXT,
    val text: String = "",
    val postId: String? = null,
    val eventId: String? = null,
    val replyToId: String? = null,
    /** When the other person read it (shown only if they have read receipts on). */
    val seenAt: Instant? = null,
    /** Photos (and short videos) sent in the chat. */
    val media: List<com.circle.app.data.media.MediaRef> = emptyList(),
)

/**
 * A chat. A 1:1 chat has one other member; a chat you start with several friends has a [name];
 * a group's chat is tied to [groupId] and only appears while you are a member.
 */
class Chat(
    val id: String,
    val memberIds: List<String>,
    val name: String? = null,
    val groupId: String? = null,
    messages: List<ChatMessage>,
    unread: Boolean = false,
    pinned: Boolean = false,
    /** Until when you had read this chat; later messages from others are new. */
    lastReadAt: Instant? = null,
    /** Who started a chat with friends: its host for lifetime changes. Null for 1:1 and group chats. */
    val startedBy: String? = null,
) {
    /** A chat with friends can be temporary. A group's chat ignores this and follows its group's term. */
    var term by mutableStateOf<com.circle.app.data.lifetime.Term>(com.circle.app.data.lifetime.Term.Permanent)
    val messages: SnapshotStateList<ChatMessage> = mutableStateListOf<ChatMessage>().apply { addAll(messages) }
    var unread by mutableStateOf(unread)
    var pinned by mutableStateOf(pinned)
    var muted by mutableStateOf(false)
    var lastReadAt by mutableStateOf(lastReadAt)
    /** Deleted from your list (the others keep it). */
    var hiddenForMe by mutableStateOf(false)
    /** "Clear chat": messages up to here are gone from your side only. */
    var clearedAt by mutableStateOf<Instant?>(null)
    val isGroupChat: Boolean get() = groupId != null
    /** The other person in a 1:1 chat. */
    val directPersonId: String? get() = if (groupId == null && name == null) memberIds.singleOrNull() else null
}

internal object ChatSeed {
    /** Yesterday at a local clock time. */
    private fun y(hhmm: String): Instant {
        val zone = ZoneId.systemDefault()
        val day = Clock.now().atZone(zone).toLocalDate().minusDays(1)
        return day.atTime(LocalTime.parse(hhmm)).atZone(zone).toInstant()
    }

    /** Today, [min] minutes ago (never in the future, whatever the time of day). */
    private fun t(min: Long): Instant = Clock.ago(Duration.ofMinutes(min))

    fun chats() = listOf(
        Chat(
            "pk", listOf("PK"), unread = true, pinned = true, lastReadAt = t(140), messages = listOf(
                ChatMessage("pk1", "PK", y("21:40"), text = "Did you see? Amara finally finished the puzzle."),
                ChatMessage("pk2", "PK", y("21:40"), MessageKind.SHARED_NOTE, postId = "amara-puzzle"),
                ChatMessage("pk2b", "PK", y("21:43"), text = "And my friend Elena's museum tip. You two would get on."),
                ChatMessage("pk2c", "PK", y("21:43"), MessageKind.SHARED_NOTE, postId = "elena-forum"),
                ChatMessage("pk3", ME, y("21:55"), text = "Three months! Mom must be thrilled.", seenAt = y("21:58")),
                ChatMessage("pk4", "PK", t(150), MessageKind.EVENT_INVITE, eventId = "board-game-night"),
                ChatMessage("pk5", "PK", t(149), text = "Friday? Bring the good snacks."),
                ChatMessage("pk6", ME, t(143), text = "I'm in. I'll bring the good snacks.", replyToId = "pk5", seenAt = t(135)),
                ChatMessage("pk7", "PK", t(124), text = "Sounds good, see you then"),
            )
        ),
        Chat(
            "jl", listOf("JL"), messages = listOf(
                ChatMessage("jl1", ME, t(105), text = "Can you bring the tripod on Sunday?", seenAt = t(97)),
                ChatMessage("jl2", "JL", t(93), text = "Bringing the tripod"),
            )
        ),
        Chat(
            "trip", listOf("PK", "JL"), name = "Weekend Trip", messages = listOf(
                ChatMessage("tr1", "JL", t(205), text = "Cabin or tents?"),
                ChatMessage("tr2", "PK", t(194), text = "I'll book the cabin"),
            )
        ),
        Chat(
            "ao", listOf("AO"), messages = listOf(
                ChatMessage("ao1", ME, y("18:20"), text = "Here's the recipe. It's my mum's, don't tell her.", seenAt = y("18:25")),
                ChatMessage("ao2", "AO", y("18:26"), text = "Thanks for the recipe"),
            )
        ),
        Chat(
            "g-photo-walk", listOf("PK", "TN", "AO", "EV"), groupId = "photo-walk", unread = true, lastReadAt = t(80), messages = listOf(
                ChatMessage("pw1", "PK", t(85), text = "Meet at the cathedral steps, 6:30"),
                ChatMessage("pw2", "PK", t(85), text = "Golden hour is short this week"),
                ChatMessage("pw2b", "EV", t(77), text = "Could someone lend me a 50mm for Sunday? Mine is at the repair shop."),
                ChatMessage("pw3", "TN", t(71), text = "I'll bring a tripod to share"),
            )
        ),
        // Temporary chats with friends (TIMED_GROUPS T2): 12 days left (you started it), ending soon
        // (Elin started it: you're a member), and ended with 5 days to deletion (you started it).
        Chat(
            "alps-trip", listOf("PK", "JL", "AO"), name = "Alps trip", startedBy = ME, messages = listOf(
                ChatMessage("at1", "PK", t(150), text = "Route plan is in the group. Thoughts?"),
                ChatMessage("at2", ME, t(138), text = "Looks good. I'll check the hut booking."),
                ChatMessage("at3", "JL", t(120), text = "Booked. Sixteen beds."),
            )
        ).apply { term = Seed.termEnding(11) },
        Chat(
            "study-group", listOf("EK", "TN", "SY"), name = "Study group", startedBy = "EK", messages = listOf(
                ChatMessage("sg1", "EK", Clock.ago(Duration.ofHours(26)), text = "Library at ten tomorrow?"),
                ChatMessage("sg2", "TN", Clock.ago(Duration.ofHours(25)), text = "I'll bring the past papers."),
            )
        ).apply { term = Seed.termEnding(2) },
        Chat(
            "exam-prep", listOf("ML", "LF", "CZ"), name = "Exam prep", startedBy = ME, messages = listOf(
                ChatMessage("ex1", "ML", Clock.ago(Duration.ofHours(70)), text = "Last exam done. We survived."),
                ChatMessage("ex2", ME, Clock.ago(Duration.ofHours(69)), text = "Thanks for all the notes, everyone."),
            )
        ).apply { term = Seed.termEnding(-3) },
        Chat(
            "g-project-alps", listOf("PK", "JL", "AO", "TN", "ML", "IM", "NB"), groupId = "project-alps", messages = listOf(
                ChatMessage("pa1", "JL", t(200), text = "Hut is booked for the 21st."),
                ChatMessage("pa2", "PK", t(40), text = "Draft of the route plan is up."),
            )
        ),
        Chat(
            "g-board-game-club", listOf("JL"), groupId = "board-game-club", messages = listOf(
                ChatMessage("bg1", "JL", t(240), text = "Anyone up for Catan tonight?"),
            )
        ),
    )
}
