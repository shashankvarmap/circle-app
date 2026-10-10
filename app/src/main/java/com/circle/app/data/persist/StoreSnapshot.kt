package com.circle.app.data.persist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.circle.app.data.Alert
import com.circle.app.data.AlertKind
import com.circle.app.data.Chat
import com.circle.app.data.ChatMessage
import com.circle.app.data.CircleStore
import com.circle.app.data.Comment
import com.circle.app.data.EventInfo
import com.circle.app.data.ExportEvent
import com.circle.app.data.ExportStep
import com.circle.app.data.FriendRequest
import com.circle.app.data.GroupInfo
import com.circle.app.data.InvitesFrom
import com.circle.app.data.Invite
import com.circle.app.data.MessageKind
import com.circle.app.data.MyProfile
import com.circle.app.data.NotificationPrefs
import com.circle.app.data.Notice
import com.circle.app.data.PauseAll
import com.circle.app.data.Person
import com.circle.app.data.Post
import com.circle.app.data.PrivacyPrefs
import com.circle.app.data.Report
import com.circle.app.data.ReportKind
import com.circle.app.data.ReportReason
import com.circle.app.data.RequestStatus
import com.circle.app.data.RequestsFrom
import com.circle.app.data.ThemeMode
import com.circle.app.data.access.Audience
import com.circle.app.data.access.EventVisibility
import com.circle.app.data.media.MediaKind
import com.circle.app.data.media.MediaRef
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Everything the fake "server" (CircleStore) holds, as plain serializable data, so the demo
 * survives restarts (F6). Times are epoch milliseconds; colours ARGB ints; enums by name.
 * Bump [VERSION] when the shape changes: an older file is then ignored and the seed loads.
 */
@Serializable
data class StoreSnapshot(
    val version: Int = VERSION,
    val people: List<PersonS>,
    val friendIds: List<String>,
    val innerCircle: List<String>,
    val innerCirclesIAmIn: List<String>,
    val incomingRequests: List<RequestS>,
    val requestStatus: Map<String, String>,
    val outgoingRequestIds: List<String>,
    val blockedIds: List<String>,
    val blockedMeIds: List<String>,
    val mutedIds: List<String>,
    val reports: List<ReportS>,
    val posts: List<PostS>,
    val likedPostIds: List<String>,
    val likedCommentIds: List<String>,
    val groups: List<GroupS>,
    val events: List<EventS>,
    val chats: List<ChatS>,
    val hiddenMessageIds: List<String>,
    val alerts: List<AlertS>,
    val exportLog: List<ExportS>,
    val invitesShared: Int,
    val alertsReadAt: Long?,
    val invites: List<InviteS>,
    val sentNotices: List<NoticeS>,
    val profile: ProfileS,
    val theme: String,
    val privacy: PrivacyS,
    val notifications: NotifS,
) {
    companion object {
        const val VERSION = 1
    }
}

@Serializable data class PersonS(val id: String, val name: String, val color: Int, val sinceYear: Int, val pinned: String)
@Serializable data class RequestS(val personId: String, val at: Long)
@Serializable data class ReportS(val kind: String, val targetId: String, val reason: String, val note: String, val at: Long)
@Serializable data class CommentS(val id: String, val authorId: String, val at: Long, val text: String, val replies: List<CommentS>)
@Serializable data class MediaS(val id: String, val uri: String, val kind: String, val durationMs: Long?)
@Serializable data class PostS(
    val id: String, val authorId: String, val at: Long, val text: String, val hasMedia: Boolean, val audience: String,
    val quotedPostId: String?, val repostOfId: String?, val groupId: String?, val comments: List<CommentS>, val media: List<MediaS>,
)
@Serializable data class JoinS(val personId: String, val at: Long)
@Serializable data class GroupS(
    val id: String, val name: String, val category: String, val topic: String, val description: String, val color: Int,
    val isPublic: Boolean, val compassDeg: Float?, val memberCount: Int, val isMember: Boolean,
    val hostIds: List<String>, val memberIds: List<String>, val joinRequests: List<JoinS>, val joinRequested: Boolean,
)
@Serializable data class EventS(
    val id: String, val title: String, val topic: String, val compassDeg: Float?, val startsAt: Long, val venue: String,
    val visibility: String, val hostId: String, val description: String, val capacity: Int, val goingBase: Int,
    val joined: Boolean, val comments: List<CommentS>,
)
@Serializable data class MessageS(
    val id: String, val senderId: String, val at: Long, val kind: String, val text: String, val postId: String?,
    val eventId: String?, val replyToId: String?, val seenAt: Long?, val media: List<MediaS>,
)
@Serializable data class ChatS(
    val id: String, val memberIds: List<String>, val name: String?, val groupId: String?, val messages: List<MessageS>,
    val unread: Boolean, val pinned: Boolean, val muted: Boolean, val lastReadAt: Long?, val hiddenForMe: Boolean, val clearedAt: Long?,
)
@Serializable data class AlertS(
    val id: String, val actorId: String, val kind: String, val at: Long, val quote: String?, val postId: String?, val aboutId: String?,
)
@Serializable data class ExportS(val kind: String, val at: Long)
@Serializable data class InviteS(val toPersonId: String, val groupId: String?, val eventId: String?, val at: Long)
@Serializable data class NoticeS(val toPersonId: String, val text: String, val at: Long)
@Serializable data class ProfileS(val displayName: String, val handle: String, val pinnedThought: String, val avatarUri: String?)
@Serializable data class PrivacyS(
    val findableByName: Boolean, val findableFromContacts: Boolean, val requestsFrom: String, val invitesFrom: String, val readReceipts: Boolean,
)
@Serializable data class NotifS(
    val likes: Boolean, val comments: Boolean, val friendRequests: Boolean, val events: Boolean, val groups: Boolean,
    val messages: Boolean, val pause: String, val quietHours: Boolean, val quietStart: Int, val quietEnd: Int,
)

private fun Instant.ms() = toEpochMilli()
private fun Long.instant(): Instant = Instant.ofEpochMilli(this)

private fun Comment.s(): CommentS = CommentS(id, authorId, createdAt.ms(), text, replies.map { it.s() })
private fun CommentS.m(): Comment = Comment(id, authorId, at.instant(), text, replies.map { it.m() })
private fun MediaRef.s() = MediaS(id, uri, kind.name, durationMs)
private fun MediaS.m() = MediaRef(id, uri, MediaKind.valueOf(kind), durationMs)

/** Reads the whole store. Called inside a snapshot read, so every change re-triggers a save. */
fun CircleStore.capture(): StoreSnapshot = StoreSnapshot(
    people = people.values.map { PersonS(it.id, it.name, it.color.toArgb(), it.sinceYear, it.pinned) },
    friendIds = friendIds.toList(),
    innerCircle = innerCircle.toList(),
    innerCirclesIAmIn = innerCirclesIAmIn.toList(),
    incomingRequests = incomingRequests.map { RequestS(it.personId, it.receivedAt.ms()) },
    requestStatus = requestStatus.mapValues { it.value.name },
    outgoingRequestIds = outgoingRequestIds.toList(),
    blockedIds = blockedIds.toList(),
    blockedMeIds = blockedMeIds.toList(),
    mutedIds = mutedIds.toList(),
    reports = reports.map { ReportS(it.kind.name, it.targetId, it.reason.name, it.note, it.at.ms()) },
    posts = posts.map { p ->
        PostS(
            p.id, p.authorId, p.createdAt.ms(), p.text, p.hasMedia, p.audience.name, p.quotedPostId, p.repostOfId, p.groupId,
            p.comments.map { it.s() }, p.media.map { it.s() }
        )
    },
    likedPostIds = likedPostIds.toList(),
    likedCommentIds = likedCommentIds.toList(),
    groups = groups.map { g ->
        GroupS(
            g.id, g.name, g.category, g.topic, g.description, g.color.toArgb(), g.isPublic, g.compassDeg, g.memberCount, g.isMember,
            g.hostIds.toList(), g.memberIds.toList(), g.joinRequests.map { JoinS(it.first, it.second.ms()) }, g.joinRequested
        )
    },
    events = events.map { e ->
        EventS(
            e.id, e.title, e.topic, e.compassDeg, e.startsAt.ms(), e.venue, e.visibility.name, e.hostId, e.description,
            e.capacity, e.goingBase, e.joined, e.comments.map { it.s() }
        )
    },
    chats = chats.map { c ->
        ChatS(
            c.id, c.memberIds, c.name, c.groupId,
            c.messages.map { m ->
                MessageS(m.id, m.senderId, m.createdAt.ms(), m.kind.name, m.text, m.postId, m.eventId, m.replyToId, m.seenAt?.ms(), m.media.map { it.s() })
            },
            c.unread, c.pinned, c.muted, c.lastReadAt?.ms(), c.hiddenForMe, c.clearedAt?.ms()
        )
    },
    hiddenMessageIds = hiddenMessageIds.toList(),
    alerts = alerts.map { AlertS(it.id, it.actorId, it.kind.name, it.createdAt.ms(), it.quote, it.postId, it.aboutId) },
    exportLog = exportLog.map { ExportS(it.kind.name, it.at.ms()) },
    invitesShared = invitesShared,
    alertsReadAt = alertsReadAt?.ms(),
    invites = invites.map { InviteS(it.toPersonId, it.groupId, it.eventId, it.at.ms()) },
    sentNotices = sentNotices.map { NoticeS(it.toPersonId, it.text, it.at.ms()) },
    profile = ProfileS(profile.displayName, profile.handle, profile.pinnedThought, profile.avatarUri),
    theme = theme.name,
    privacy = PrivacyS(privacy.findableByName, privacy.findableFromContacts, privacy.requestsFrom.name, privacy.invitesFrom.name, privacy.readReceipts),
    notifications = notifications.let {
        NotifS(it.likes, it.comments, it.friendRequests, it.events, it.groups, it.messages, it.pause.name, it.quietHours, it.quietStart, it.quietEnd)
    },
)

/** Replaces the store's contents with [s]. */
fun CircleStore.restore(s: StoreSnapshot) {
    people.clear(); s.people.forEach { people[it.id] = Person(it.id, it.name, Color(it.color), it.sinceYear, it.pinned) }
    friendIds.replaceWith(s.friendIds); innerCircle.replaceWith(s.innerCircle); innerCirclesIAmIn.replaceWith(s.innerCirclesIAmIn)
    incomingRequests.replaceWith(s.incomingRequests.map { FriendRequest(it.personId, it.at.instant()) })
    requestStatus.clear(); s.requestStatus.forEach { (k, v) -> requestStatus[k] = RequestStatus.valueOf(v) }
    outgoingRequestIds.replaceWith(s.outgoingRequestIds)
    blockedIds.replaceWith(s.blockedIds); blockedMeIds.replaceWith(s.blockedMeIds); mutedIds.replaceWith(s.mutedIds)
    reports.replaceWith(s.reports.map { Report(ReportKind.valueOf(it.kind), it.targetId, ReportReason.valueOf(it.reason), it.note, it.at.instant()) })
    posts.replaceWith(s.posts.map { p ->
        Post(
            p.id, p.authorId, p.at.instant(), p.text, p.hasMedia, Audience.valueOf(p.audience), p.quotedPostId, p.repostOfId, p.groupId,
            p.comments.map { it.m() }, p.media.map { it.m() }
        )
    })
    likedPostIds.replaceWith(s.likedPostIds); likedCommentIds.replaceWith(s.likedCommentIds)
    groups.replaceWith(s.groups.map { g ->
        GroupInfo(g.id, g.name, g.category, g.topic, g.description, Color(g.color), g.isPublic, g.compassDeg, g.memberCount, g.isMember).apply {
            hostIds.addAll(g.hostIds); memberIds.addAll(g.memberIds)
            joinRequests.addAll(g.joinRequests.map { it.personId to it.at.instant() }); joinRequested = g.joinRequested
        }
    })
    events.replaceWith(s.events.map { e ->
        EventInfo(
            e.id, e.title, e.topic, e.compassDeg, e.startsAt.instant(), e.venue, EventVisibility.valueOf(e.visibility), e.hostId,
            e.description, e.capacity, e.goingBase, e.joined, e.comments.map { it.m() }
        )
    })
    chats.replaceWith(s.chats.map { c ->
        Chat(
            c.id, c.memberIds, c.name, c.groupId,
            c.messages.map { m ->
                ChatMessage(m.id, m.senderId, m.at.instant(), MessageKind.valueOf(m.kind), m.text, m.postId, m.eventId, m.replyToId, m.seenAt?.instant(), m.media.map { it.m() })
            },
            c.unread, c.pinned, c.lastReadAt?.instant()
        ).apply { muted = c.muted; hiddenForMe = c.hiddenForMe; clearedAt = c.clearedAt?.instant() }
    })
    hiddenMessageIds.replaceWith(s.hiddenMessageIds)
    alerts.replaceWith(s.alerts.map { Alert(it.id, it.actorId, AlertKind.valueOf(it.kind), it.at.instant(), it.quote, it.postId, it.aboutId) })
    exportLog.replaceWith(s.exportLog.map { ExportEvent(ExportStep.valueOf(it.kind), it.at.instant()) })
    invitesShared = s.invitesShared
    alertsReadAt = s.alertsReadAt?.instant()
    invites.replaceWith(s.invites.map { Invite(it.toPersonId, it.groupId, it.eventId, it.at.instant()) })
    sentNotices.replaceWith(s.sentNotices.map { Notice(it.toPersonId, it.text, it.at.instant()) })
    profile = s.profile.let { MyProfile(it.displayName, it.handle, it.pinnedThought, it.avatarUri) }
    theme = ThemeMode.valueOf(s.theme)
    privacy = s.privacy.let { PrivacyPrefs(it.findableByName, it.findableFromContacts, RequestsFrom.valueOf(it.requestsFrom), InvitesFrom.valueOf(it.invitesFrom), it.readReceipts) }
    notifications = s.notifications.let {
        NotificationPrefs(it.likes, it.comments, it.friendRequests, it.events, it.groups, it.messages, PauseAll.valueOf(it.pause), it.quietHours, it.quietStart, it.quietEnd)
    }
}

private fun <T> MutableList<T>.replaceWith(items: List<T>) { clear(); addAll(items) }
