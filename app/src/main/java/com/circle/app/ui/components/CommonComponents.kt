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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.data.access.DenyReason
import com.circle.app.data.access.Relationship
import com.circle.app.data.repo.CommentView
import com.circle.app.data.repo.GroupView
import com.circle.app.data.repo.Note
import com.circle.app.data.repo.RepostState
import com.circle.app.navigation.Routes
import com.circle.app.ui.format.denyText
import com.circle.app.ui.format.relativeTime
import com.circle.app.ui.theme.*
import com.circle.app.ui.vm.ActionsViewModel
import com.circle.app.ui.vm.circleViewModel

/** Circular initials avatar: the same visual language used for people everywhere. */
@Composable
fun Avatar(initials: String, color: Color, size: Int = 40) {
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = avatarTextColor(color), fontWeight = FontWeight.Bold, fontSize = (size / 3).sp)
    }
}

/** Rounded-square avatar: reserved for Groups, distinct from people's circles. */
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

/** Small underline text tabs used across Home (Friends/Groups), Alerts, Explore search and Group. */
@Composable
fun UnderlineTabs(tabs: List<String>, selected: Int, spacing: Dp = 22.dp, onSelect: (Int) -> Unit) {
    val accent = CircleTheme.colors.accent
    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
        tabs.forEachIndexed { i, label ->
            val isActive = i == selected
            // Underline is drawn behind the label rather than laid out as a sibling,
            // so each tab is sized purely by its own text: no fillMaxWidth/intrinsics.
            Text(
                label,
                color = if (isActive) CircleTheme.colors.text else CircleTheme.colors.muted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .clickable { onSelect(i) }
                    .drawBehind {
                        if (isActive) {
                            val stroke = 1.5.dp.toPx()
                            drawRect(accent, topLeft = Offset(0f, size.height - stroke), size = Size(size.width, stroke))
                        }
                    }
                    .padding(start = 1.dp, end = 1.dp, top = 6.dp, bottom = 7.dp)
            )
        }
    }
}

/** One comment in a thread. Top-level comments are always visible; their own replies
 *  stay collapsed behind a "View N replies" link that expands just this comment, in place. */
@Composable
fun ThreadComment(comment: CommentView, onPerson: (String) -> Unit = {}) {
    var showReplies by rememberSaveable(comment.id) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CommentRow(comment, avatarSize = 32, onPerson = onPerson)
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
                ) { comment.replies.forEach { CommentRow(it, avatarSize = 26, onPerson = onPerson) } }
            }
        }
    }
}

/** A comment. The commenter may not be your friend: avatar and name open their profile by relationship. */
@Composable
private fun CommentRow(comment: CommentView, avatarSize: Int, onPerson: (String) -> Unit) {
    val author = comment.author
    Row {
        Box(
            Modifier.size(maxOf(avatarSize, 40).dp).clip(CircleShape)
                .clickable(onClickLabel = "Open ${author.name}'s profile") { onPerson(author.id) },
            contentAlignment = Alignment.TopCenter
        ) { Avatar(author.initials, author.color, size = avatarSize) }
        Spacer(Modifier.width(6.dp))
        Column {
            Row(
                Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClickLabel = "Open ${author.name}'s profile") { onPerson(author.id) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(author.name, color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text("· ${relativeTime(comment.createdAt)}", color = CircleTheme.colors.muted, fontSize = 11.5.sp)
            }
            Text(comment.text, color = CircleTheme.colors.body, fontSize = 13.sp)
        }
    }
}

/**
 * Comment / repost / like row on every note. All three act on the original note (so a
 * "You reposted" card likes and opens the original). The heart is a private like: no count.
 * Group posts get no repost at all; otherwise the menu says why it can't be (from the repository).
 */
@Composable
fun PostActions(nav: NavHostController, note: Note, showComment: Boolean = true) {
    val colors = CircleTheme.colors
    val actions = circleViewModel { ActionsViewModel(it) }
    val target = note.actionTargetId
    val liked = note.likedByMe
    val repost = note.repost
    val reposted = repost == RepostState.Reposted

    // Each action has a 40dp touch area; the row is pulled left so the first icon lines up with the text.
    Row(
        modifier = Modifier.offset(x = (-11).dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showComment) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClickLabel = "Open thread") { nav.navigate(Routes.thread(target)) }
                    .padding(horizontal = 11.dp)
            ) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comments", tint = colors.muted, modifier = Modifier.size(18.dp))
                if (note.commentCount > 0) {
                    Spacer(Modifier.width(5.dp))
                    Text("${note.commentCount}", color = colors.muted, fontSize = 12.sp)
                }
            }
        }
        // Group posts stay in the group: no repost or quote at all.
        val isGroupPost = repost is RepostState.Unavailable && repost.reason == DenyReason.GROUP_POST
        if (!isGroupPost) {
            RepostButton(
                reposted = reposted,
                items = repostMenuItems(
                    blockReason = (repost as? RepostState.Unavailable)?.let { denyText(it.reason) },
                    reposted = reposted,
                    onRepost = { actions.repost(target) },
                    onUndo = { actions.undoRepost(target) },
                    onQuote = { nav.navigate(Routes.compose(quote = target)) }
                )
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClickLabel = if (liked) "Remove like" else "Like") { actions.toggleLike(target) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (liked) "Liked. Only the author sees this" else "Like",
                tint = if (liked) colors.accent else colors.muted,
                modifier = Modifier.size(18.dp)
            )
        }
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
    var text by rememberSaveable { mutableStateOf("") }
    val canSend = text.isNotBlank()
    fun send() {
        if (canSend) {
            onSend(text.trim())
            text = ""
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        MeAvatar(28)
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
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = placeholder }
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

/**
 * Add friend / Requested (tap to cancel) / Accept / ✓ Friends, by relationship. Requests are
 * never instant. Shared by Contacts, Explore search and lists of people.
 */
@Composable
fun FriendRequestButton(personId: String, relationship: Relationship, addLabel: String = "Add friend") {
    val colors = CircleTheme.colors
    val actions = circleViewModel { ActionsViewModel(it) }
    val shape = RoundedCornerShape(999.dp)
    when (relationship) {
        Relationship.NONE -> Box(
            Modifier.clip(shape).background(colors.accent).clickable { actions.sendFriendRequest(personId) }
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) { Text(addLabel, color = colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        Relationship.REQUEST_SENT -> Box(
            Modifier.clip(shape).border(1.dp, colors.border, shape)
                .clickable { actions.cancelFriendRequest(personId) }
                .semantics { contentDescription = "Requested. Tap to cancel the request" }
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) { Text("Requested", color = colors.muted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
        Relationship.REQUEST_RECEIVED -> Box(
            Modifier.clip(shape).background(colors.accent).clickable { actions.acceptRequest(personId) }
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) { Text("Accept", color = colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        Relationship.FRIEND -> Box(Modifier.padding(horizontal = 14.dp, vertical = 7.dp)) {
            Text("✓ Friends", color = colors.accent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
        else -> Unit
    }
}

/** Join button for a public group. Tapping sends a pending request (visible to every
 *  member): it never makes you a member instantly. */
@Composable
fun JoinRequestButton(group: GroupView, modifier: Modifier = Modifier) {
    val actions = circleViewModel { ActionsViewModel(it) }
    val colors = CircleTheme.colors
    val shape = RoundedCornerShape(999.dp)
    when {
        group.isMember -> Box(
            modifier = modifier.clip(shape).border(1.dp, colors.border, shape).padding(horizontal = 14.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) { Text("✓ Member", color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp) }

        group.joinRequested -> Box(
            modifier = modifier.clip(shape).border(1.dp, colors.border, shape).padding(horizontal = 14.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) { Text("Requested", color = colors.muted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }

        else -> Box(
            modifier = modifier.clip(shape).background(colors.accent)
                .clickable { actions.requestToJoin(group.ref.id) }
                .padding(horizontal = 14.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) { Text("Request to join", color = colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
    }
}
