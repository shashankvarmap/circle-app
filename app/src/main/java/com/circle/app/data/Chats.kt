package com.circle.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

enum class MessageKind { TEXT, SHARED_NOTE, EVENT_INVITE }

/**
 * One message in a chat. Rich messages point at a post ([postId], a shared note) or an event
 * ([eventId], an invite). [replyToId] is the message this one replies to (shown as a snippet).
 */
data class ChatMessage(
    val id: String,
    val senderId: String,
    /** Day separator label, e.g. "YESTERDAY" or "TODAY". */
    val day: String,
    val time: String,
    val kind: MessageKind = MessageKind.TEXT,
    val text: String = "",
    val postId: String? = null,
    val eventId: String? = null,
    val replyToId: String? = null,
    /** When the other person read it (only shown if they have read receipts on). */
    val seenAt: String? = null,
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
) {
    val messages: SnapshotStateList<ChatMessage> = mutableStateListOf<ChatMessage>().apply { addAll(messages) }
    var unread by mutableStateOf(unread)
    val isGroupChat: Boolean get() = groupId != null
    /** The other person in a 1:1 chat. */
    val directPersonId: String? get() = if (groupId == null && name == null) memberIds.singleOrNull() else null
}

internal object ChatSeed {
    fun chats() = listOf(
        Chat(
            "pk", listOf("PK"), unread = true, messages = listOf(
                ChatMessage("pk1", "PK", "YESTERDAY", "21:40", text = "Did you see? Amara finally finished the puzzle."),
                ChatMessage("pk2", "PK", "YESTERDAY", "21:40", MessageKind.SHARED_NOTE, postId = "amara-puzzle"),
                ChatMessage("pk3", ME, "YESTERDAY", "21:55", text = "Three months! Mom must be thrilled.", seenAt = "21:58"),
                ChatMessage("pk4", "PK", "TODAY", "09:05", MessageKind.EVENT_INVITE, eventId = "board-game-night"),
                ChatMessage("pk5", "PK", "TODAY", "09:06", text = "Friday? Bring the good snacks."),
                ChatMessage("pk6", ME, "TODAY", "09:12", text = "I'm in. I'll bring the good snacks.", replyToId = "pk5", seenAt = "09:20"),
                ChatMessage("pk7", "PK", "TODAY", "09:31", text = "Sounds good, see you then"),
            )
        ),
        Chat(
            "jl", listOf("JL"), messages = listOf(
                ChatMessage("jl1", ME, "TODAY", "09:50", text = "Can you bring the tripod on Sunday?", seenAt = "09:58"),
                ChatMessage("jl2", "JL", "TODAY", "10:02", text = "Bringing the tripod"),
            )
        ),
        Chat(
            "trip", listOf("PK", "JL"), name = "Weekend Trip", messages = listOf(
                ChatMessage("tr1", "JL", "TODAY", "08:30", text = "Cabin or tents?"),
                ChatMessage("tr2", "PK", "TODAY", "08:41", text = "I'll book the cabin"),
            )
        ),
        Chat(
            "ao", listOf("AO"), messages = listOf(
                ChatMessage("ao1", ME, "YESTERDAY", "18:20", text = "Here's the recipe. It's my mum's, don't tell her.", seenAt = "18:25"),
                ChatMessage("ao2", "AO", "YESTERDAY", "18:26", text = "Thanks for the recipe"),
            )
        ),
        Chat(
            "g-photo-walk", listOf("PK", "TN", "AO"), groupId = "photo-walk", unread = true, messages = listOf(
                ChatMessage("pw1", "PK", "TODAY", "10:10", text = "Meet at the cathedral steps, 6:30"),
                ChatMessage("pw2", "PK", "TODAY", "10:10", text = "Golden hour is short this week"),
                ChatMessage("pw3", "TN", "TODAY", "10:24", text = "I'll bring a tripod to share"),
            )
        ),
        Chat(
            "g-board-game-club", listOf("JL"), groupId = "board-game-club", messages = listOf(
                ChatMessage("bg1", "JL", "TODAY", "08:00", text = "Anyone up for Catan tonight?"),
            )
        ),
    )
}
