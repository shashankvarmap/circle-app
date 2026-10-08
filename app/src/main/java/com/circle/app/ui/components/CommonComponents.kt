package com.circle.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.data.CircleStore
import com.circle.app.data.Comment
import com.circle.app.data.FriendStatus
import com.circle.app.data.GroupInfo
import com.circle.app.navigation.Routes
import com.circle.app.ui.theme.*

/** Circular initials avatar — the same visual language used for people everywhere. */
@Composable
fun Avatar(initials: String, color: Color, size: Int = 40) {
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = avatarTextColor(color), fontWeight = FontWeight.Bold, fontSize = (size / 3).sp)
    }
}

/** Rounded-square avatar — reserved for Groups, distinct from people's circles. */
@Composable
fun GroupTile(initial: String, color: Color, size: Int = 40) {
    Box(
        modifier = Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.3).dp)).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(initial, color = avatarTextColor(color), fontWeight = FontWeight.Bold, fontSize = (size / 2.5).sp)
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, color = CircleTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp)
}

/** Small underline text tabs — the "Twitter For You / Following" style used
 *  across Home (Friends/Groups), Notifications, Explore search, and Group. */
@Composable
fun UnderlineTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val accent = CircleTheme.colors.accent
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        tabs.forEachIndexed { i, label ->
            val isActive = i == selected
            // Underline is drawn behind the label rather than laid out as a sibling,
            // so each tab is sized purely by its own text — no fillMaxWidth/intrinsics.
            Text(
                label,
                color = if (isActive) CircleTheme.colors.text else CircleTheme.colors.muted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .clickable { onSelect(i) }
                    .drawBehind {
                        if (isActive) {
                            val stroke = 1.5.dp.toPx()
                            drawRect(
                                accent,
                                topLeft = Offset(0f, size.height - stroke),
                                size = Size(size.width, stroke)
                            )
                        }
                    }
                    .padding(bottom = 7.5.dp)
            )
        }
    }
}

/** One comment in a thread. Top-level comments are always visible; their own replies
 *  stay collapsed behind a "View N replies" link that expands just this comment, in place. */
@Composable
fun ThreadComment(comment: Comment) {
    var showReplies by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CommentRow(comment, avatarSize = 32)
        if (comment.replies.isNotEmpty()) {
            val n = comment.replies.size
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 42.dp).clickable { showReplies = !showReplies }
            ) {
                Text(
                    if (showReplies) "Hide replies" else "View $n ${if (n == 1) "reply" else "replies"}",
                    color = CircleTheme.colors.muted, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp
                )
                Icon(
                    if (showReplies) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null, tint = CircleTheme.colors.muted, modifier = Modifier.size(16.dp)
                )
            }
            if (showReplies) {
                Column(
                    modifier = Modifier.padding(start = 42.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) { comment.replies.forEach { CommentRow(it, avatarSize = 26) } }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: Comment, avatarSize: Int) {
    Row {
        Avatar(comment.initials, comment.color, size = avatarSize)
        Spacer(Modifier.width(10.dp))
        Column {
            Row {
                Text(comment.author, color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text("· ${comment.time}", color = CircleTheme.colors.muted, fontSize = 11.5.sp)
            }
            Text(comment.text, color = CircleTheme.colors.body, fontSize = 13.sp)
        }
    }
}

/**
 * Reply / repost / like action row shared by Thought, Post, Repost, Quote and Group cards.
 * Tap the comment icon to open the post's Thread screen; tap Repost for a Repost/Quote menu.
 * The heart is a private like stored in [CircleStore]: no count, no reactions.
 */
@Composable
fun PostActions(
    nav: NavHostController,
    postId: String,
    showRepost: Boolean = true
) {
    val liked = CircleStore.isLiked(postId)
    var reposted by remember { mutableStateOf(false) }
    var showRepostMenu by remember { mutableStateOf(false) }
    val commentCount = CircleStore.post(postId)?.comments?.sumOf { 1 + it.replies.size } ?: 0

    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { nav.navigate(Routes.thread(postId)) }
        ) {
            Icon(
                Icons.Outlined.ChatBubbleOutline, contentDescription = "Reply",
                tint = CircleTheme.colors.muted, modifier = Modifier.size(18.dp)
            )
            if (commentCount > 0) {
                Spacer(Modifier.width(5.dp))
                Text("$commentCount", color = CircleTheme.colors.muted, fontSize = 12.sp)
            }
        }
        if (showRepost) {
            Box {
                Icon(
                    Icons.Outlined.Repeat, contentDescription = "Repost / Quote",
                    tint = if (reposted) CircleTheme.colors.accent else CircleTheme.colors.muted,
                    modifier = Modifier.size(18.dp).clickable { showRepostMenu = true }
                )
                DropdownMenu(expanded = showRepostMenu, onDismissRequest = { showRepostMenu = false }) {
                    DropdownMenuItem(text = { Text(if (reposted) "Undo repost" else "Repost") }, onClick = {
                        reposted = !reposted
                        showRepostMenu = false
                    })
                    DropdownMenuItem(text = { Text("Quote") }, onClick = {
                        showRepostMenu = false
                        nav.navigate(Routes.COMPOSE_POST)
                    })
                }
            }
        }

        Icon(
            if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (liked) "Liked. Only the author sees this" else "Like",
            tint = if (liked) CircleTheme.colors.accent else CircleTheme.colors.muted,
            modifier = Modifier.size(18.dp).clickable { CircleStore.toggleLike(postId) }
        )
    }
}

@Composable
fun CircleCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardShadow(CircleTheme.colors, cornerRadius = 13.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(CircleTheme.colors.surface)
            .border(1.dp, CircleTheme.colors.border, RoundedCornerShape(13.dp))
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) { content() }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(CircleTheme.colors.accent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = CircleTheme.colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

/** Pill-shaped reply input with a send action. Calls [onSend] with trimmed text, then clears. */
@Composable
fun ReplyComposer(placeholder: String = "Add a reply…", onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val canSend = text.isNotBlank()
    fun send() {
        if (canSend) {
            onSend(text.trim())
            text = ""
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar("S", AvatarSlate, size = 28)
        Spacer(Modifier.width(9.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(999.dp))
                .background(CircleTheme.colors.surface)
                .border(1.dp, CircleTheme.colors.border, RoundedCornerShape(999.dp))
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (text.isEmpty()) Text(placeholder, color = CircleTheme.colors.muted, fontSize = 13.sp)
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = androidx.compose.ui.text.TextStyle(color = CircleTheme.colors.text, fontSize = 13.sp),
                    cursorBrush = SolidColor(CircleTheme.colors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (canSend) CircleTheme.colors.accent else CircleTheme.colors.track)
                    .clickable(enabled = canSend) { send() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Send, contentDescription = "Send",
                    tint = if (canSend) CircleTheme.colors.onAccent else CircleTheme.colors.muted, modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/** Outgoing friend request button: none → Requested → ✓ Friends. Shared by Contacts Sync
 *  and Explore → People; status lives in [CircleStore] so both screens stay in sync. */
@Composable
fun FriendRequestButton(name: String, addLabel: String = "Add friend") {
    val incoming = CircleStore.hasPendingRequestFrom(name)
    if (incoming != null) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(CircleTheme.colors.accent)
                .clickable { CircleStore.acceptRequest(incoming.personId) }
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) { Text("Accept", color = CircleTheme.colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        return
    }
    when (CircleStore.statusOf(name)) {
        FriendStatus.NONE -> Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(CircleTheme.colors.accent)
                .clickable { CircleStore.sendFriendRequest(name) }
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) { Text(addLabel, color = CircleTheme.colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp) }

        FriendStatus.REQUESTED -> Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .border(1.dp, CircleTheme.colors.border, RoundedCornerShape(999.dp))
                .clickable { CircleStore.cancelFriendRequest(name) }
                .semantics { contentDescription = "Requested. Tap to cancel the request" }
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text("Requested", color = CircleTheme.colors.muted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }

        FriendStatus.ACCEPTED -> Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)) {
            Text("✓ Friends", color = CircleTheme.colors.accent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }
}

/** Join button for a public group. Tapping sends a pending request (visible to every
 *  member) — it never makes you a member instantly. */
@Composable
fun JoinRequestButton(group: GroupInfo, modifier: Modifier = Modifier) {
    when {
        group.isMember -> Box(
            modifier = modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, CircleTheme.colors.border, RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) { Text("✓ Member", color = CircleTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp) }

        group.joinRequested -> Box(
            modifier = modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, CircleTheme.colors.border, RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) { Text("Requested", color = CircleTheme.colors.muted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }

        else -> Box(
            modifier = modifier.clip(RoundedCornerShape(999.dp)).background(CircleTheme.colors.accent)
                .clickable { CircleStore.requestToJoin(group) }
                .padding(horizontal = 14.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) { Text("Request to join", color = CircleTheme.colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
    }
}
