package com.circle.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.navigation.Routes
import com.circle.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Circular initials avatar — the same visual language used for people everywhere. */
@Composable
fun Avatar(initials: String, color: Color, size: Int = 40) {
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = OnAccent, fontWeight = FontWeight.Bold, fontSize = (size / 3).sp)
    }
}

/** Rounded-square avatar — reserved for Groups, distinct from people's circles. */
@Composable
fun GroupTile(initial: String, color: Color, size: Int = 40) {
    Box(
        modifier = Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.3).dp)).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(initial, color = OnAccent, fontWeight = FontWeight.Bold, fontSize = (size / 2.5).sp)
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp)
}

/** Small underline text tabs — the "Twitter For You / Following" style used
 *  across Home (Friends/Groups), Notifications, Explore search, and Group. */
@Composable
fun UnderlineTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        tabs.forEachIndexed { i, label ->
            val isActive = i == selected
            // Underline is drawn behind the label rather than laid out as a sibling,
            // so each tab is sized purely by its own text — no fillMaxWidth/intrinsics.
            Text(
                label,
                color = if (isActive) TextPrimary else TextMuted,
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
                                AccentDefault,
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

/** A comment in a post's thread. Only top-level comments carry [replies]; one layer deep. */
data class Comment(val author: String, val text: String, val replies: List<Comment> = emptyList())

/** Top-level comment, always visible. Its own replies stay collapsed behind a
 *  "View N replies" link that expands just this comment, in place. */
@Composable
private fun ThreadComment(comment: Comment) {
    var showReplies by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CommentText(comment)
        if (comment.replies.isNotEmpty()) {
            val n = comment.replies.size
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showReplies = !showReplies }
            ) {
                Text(
                    if (showReplies) "Hide replies" else "View $n ${if (n == 1) "reply" else "replies"}",
                    color = TextMuted, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp
                )
                Icon(
                    if (showReplies) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp)
                )
            }
            if (showReplies) {
                Column(
                    modifier = Modifier.padding(start = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) { comment.replies.forEach { CommentText(it) } }
            }
        }
    }
}

@Composable
private fun CommentText(comment: Comment) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Bold)) { append(comment.author) }
            append("  ")
            append(comment.text)
        },
        color = TextBody, fontSize = 12.5.sp
    )
}

private enum class Reaction(val icon: ImageVector) {
    LIKE(Icons.Filled.Favorite),
    LAUGH(Icons.Filled.SentimentVerySatisfied),
    CRY(Icons.Filled.SentimentVeryDissatisfied)
}

/**
 * Reply / repost / like action row shared by Thought, Post, Repost, Quote and Group cards.
 * Fully interactive: tap-and-hold the like icon to pick a reaction, tap Repost for a
 * Repost/Quote menu, tap the comment icon to expand an inline reply thread.
 */
@Composable
fun PostActions(
    nav: NavHostController,
    showRepost: Boolean = true,
    initiallyLiked: Boolean = false,
    sampleReplies: List<Comment> = emptyList()
) {
    var reaction by remember { mutableStateOf(if (initiallyLiked) Reaction.LIKE else null) }
    var showReactionPicker by remember { mutableStateOf(false) }
    var reposted by remember { mutableStateOf(false) }
    var showRepostMenu by remember { mutableStateOf(false) }
    var commentExpanded by remember { mutableStateOf(false) }
    var replies by remember { mutableStateOf(sampleReplies) }
    var newReply by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.ChatBubbleOutline, contentDescription = "Reply",
                tint = if (commentExpanded) AccentDefault else TextMuted,
                modifier = Modifier.size(18.dp).clickable { commentExpanded = !commentExpanded }
            )

            if (showRepost) {
                Box {
                    Icon(
                        Icons.Outlined.Repeat, contentDescription = "Repost / Quote",
                        tint = if (reposted) AccentDefault else TextMuted,
                        modifier = Modifier.size(18.dp).clickable { showRepostMenu = true }
                    )
                    DropdownMenu(expanded = showRepostMenu, onDismissRequest = { showRepostMenu = false }) {
                        DropdownMenuItem(text = { Text("Repost") }, onClick = {
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

            Box {
                Icon(
                    reaction?.icon ?: Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (reaction != null) AccentDefault else TextMuted,
                    modifier = Modifier
                        .size(18.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    var held = true
                                    val holdJob = scope.launch {
                                        delay(450)
                                        if (held) showReactionPicker = true
                                    }
                                    val released = tryAwaitRelease()
                                    held = false
                                    if (released) holdJob.cancel()
                                    if (!showReactionPicker) {
                                        reaction = if (reaction == null) Reaction.LIKE else null
                                    }
                                }
                            )
                        }
                )
                if (showReactionPicker) {
                    Row(
                        modifier = Modifier
                            .offset(y = (-38).dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(SurfaceAlt)
                            .border(1.dp, Border, RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        Reaction.values().forEach { r ->
                            Icon(
                                r.icon, contentDescription = r.name, tint = AccentDefault,
                                modifier = Modifier.size(18.dp).clickable {
                                    reaction = r
                                    showReactionPicker = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (commentExpanded) {
            Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                replies.forEach { comment -> ThreadComment(comment) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp))
                        .background(Background)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (newReply.isEmpty()) {
                            Text("Write a reply…", color = TextDim, fontSize = 12.5.sp)
                        }
                        BasicTextField(
                            value = newReply,
                            onValueChange = { newReply = it },
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 12.5.sp),
                            cursorBrush = SolidColor(AccentDefault),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        "Send", color = AccentDefault, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        modifier = Modifier.clickable {
                            if (newReply.isNotBlank()) {
                                replies = replies + Comment("You", newReply.trim())
                                newReply = ""
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CircleCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(13.dp))
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) { content() }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AccentDefault)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = OnAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
