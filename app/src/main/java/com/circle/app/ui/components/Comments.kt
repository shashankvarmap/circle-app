package com.circle.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circle.app.R
import com.circle.app.data.repo.CommentView
import com.circle.app.ui.format.relativeTime
import com.circle.app.ui.theme.CircleTheme

/** What can be done to a comment. Null on read-only threads. */
interface CommentActions {
    fun like(comment: CommentView)
    fun reply(comment: CommentView)
    fun delete(comment: CommentView)
    fun report(comment: CommentView)
}

/**
 * One comment in a thread. Top-level comments are always visible; their replies stay collapsed
 * behind "View N replies" (accent), which expands just this comment, indented with a left rule.
 */
@Composable
fun ThreadComment(
    comment: CommentView,
    expanded: Boolean,
    onToggleReplies: () -> Unit,
    onPerson: (String) -> Unit,
    actions: CommentActions?,
) {
    val colors = CircleTheme.colors
    Column {
        CommentRow(comment, avatarSize = 36, onPerson = onPerson, actions = actions)
        if (comment.replies.isNotEmpty()) {
            val n = comment.replies.size
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 47.dp, top = 4.dp).clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggleReplies).heightIn(min = 36.dp).padding(end = 6.dp)
            ) {
                Text(
                    if (expanded) stringResource(R.string.thread_hide_replies) else pluralStringResource(R.plurals.thread_view_replies, n, n),
                    color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.5.sp
                )
                Spacer(Modifier.width(5.dp))
                Icon(
                    if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp)
                )
            }
            if (expanded) {
                val rule = colors.border
                Column(
                    modifier = Modifier
                        .padding(start = 47.dp, top = 6.dp)
                        .drawBehind { drawRect(rule, topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height)) }
                        .padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) { comment.replies.forEach { CommentRow(it, avatarSize = 28, onPerson = onPerson, actions = actions) } }
            }
        }
    }
}

/** A comment: avatar and name open the commenter's profile (they may not be your friend). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommentRow(comment: CommentView, avatarSize: Int, onPerson: (String) -> Unit, actions: CommentActions?) {
    val colors = CircleTheme.colors
    val author = comment.author
    val haptics = LocalHapticFeedback.current
    var menu by remember { mutableStateOf(false) }
    val openProfile = stringResource(R.string.open_profile_of, author.name)
    val deleteLabel = stringResource(R.string.comment_delete)
    val reportLabel = stringResource(R.string.comment_report)
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (actions != null) Modifier
                    .combinedClickable(onClick = {}, onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); menu = true })
                    .semantics {
                        customActions = listOf(
                            if (comment.isMine) CustomAccessibilityAction(deleteLabel) { actions.delete(comment); true }
                            else CustomAccessibilityAction(reportLabel) { actions.report(comment); true }
                        )
                    }
                else Modifier
            )
    ) {
        Box(
            Modifier.size(maxOf(avatarSize, 40).dp).clip(CircleShape).clickable(onClickLabel = openProfile) { onPerson(author.id) },
            contentAlignment = Alignment.TopCenter
        ) { if (author.isMe) MeAvatar(avatarSize) else Avatar(author.initials, author.color, size = avatarSize) }
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Row(
                Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClickLabel = openProfile) { onPerson(author.id) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(author.name, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                Spacer(Modifier.width(6.dp))
                Text("· ${relativeTime(comment.createdAt)}", color = colors.muted, fontSize = 12.sp)
            }
            Text(comment.text, color = colors.body, fontSize = 13.5.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 2.dp))
            if (actions != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.offset(x = (-10).dp)) {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape)
                            .clickable(onClickLabel = stringResource(if (comment.likedByMe) R.string.comment_unlike else R.string.comment_like)) { actions.like(comment) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (comment.likedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (comment.likedByMe) stringResource(R.string.comment_liked) else null,
                            tint = if (comment.likedByMe) colors.accent else colors.muted, modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        stringResource(R.string.comment_reply), color = colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { actions.reply(comment) }.padding(horizontal = 8.dp, vertical = 10.dp)
                    )
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (comment.isMine) DropdownMenuItem(text = { Text(deleteLabel) }, onClick = { menu = false; actions.delete(comment) })
                    else DropdownMenuItem(text = { Text(reportLabel) }, onClick = { menu = false; actions.report(comment) })
                }
            }
        }
    }
}

/** The "Replying to Priya" chip above the composer, with a close button. */
@Composable
fun ReplyingToChip(name: String, onClose: () -> Unit) {
    val colors = CircleTheme.colors
    Row(
        Modifier.padding(bottom = 6.dp).clip(RoundedCornerShape(999.dp)).background(colors.accent.copy(alpha = 0.10f))
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.thread_replying_to, name), color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Box(Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.chat_cancel_reply), tint = colors.accent, modifier = Modifier.size(14.dp))
        }
    }
}
