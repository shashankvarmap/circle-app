package com.circle.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.data.Audience
import com.circle.app.data.CircleStore
import com.circle.app.data.ME
import com.circle.app.data.Post
import com.circle.app.navigation.Routes
import com.circle.app.ui.theme.*

private const val AVATAR = 34
private val TextInset = (AVATAR + 10).dp

/**
 * A note on Home or Profile: a plain note, a photo post, a quote (your text, then the original
 * embedded under it) or a "You reposted" card. Actions always act on the original note.
 */
@Composable
fun FeedCard(nav: NavHostController, post: Post) {
    if (post.isRepost) {
        val original = post.repostOfId?.let { CircleStore.post(it) }
        CircleCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Repeat, contentDescription = null, tint = CircleTheme.colors.muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("You reposted", color = CircleTheme.colors.muted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                Text(" · ${post.time}", color = CircleTheme.colors.muted, fontSize = 12.sp)
            }
            if (original == null) {
                Text("This note is no longer available.", color = CircleTheme.colors.muted, fontSize = 13.sp)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(11.dp))
                        .background(CircleTheme.colors.background)
                        .clickable(onClickLabel = "Open thread") { nav.navigate(Routes.thread(original.id)) }
                        .padding(12.dp)
                ) { NoteBody(nav, original, embedQuote = true) }
                Box(Modifier.padding(start = 2.dp)) { PostActions(nav, post) }
            }
        }
    } else {
        CircleCard {
            NoteBody(nav, post, embedQuote = true)
            Box(Modifier.padding(start = TextInset)) { PostActions(nav, post) }
        }
    }
}

/** Avatar, name, time (and the private "INNER CIRCLE ONLY" tag on your own notes), then the content. */
@Composable
private fun NoteBody(nav: NavHostController, post: Post, embedQuote: Boolean) {
    Row {
        Avatar(post.initials, post.color, size = AVATAR)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            NoteHeader(post)
            if (post.text.isNotEmpty()) {
                Text(post.text, color = CircleTheme.colors.body, style = CircleTheme.postText, modifier = Modifier.padding(top = 2.dp))
            }
            if (post.hasMedia) MediaBlock(Modifier.padding(top = 8.dp))
            if (embedQuote) post.quotedPostId?.let { id ->
                EmbeddedPost(CircleStore.post(id), Modifier.padding(top = 10.dp)) { nav.navigate(Routes.thread(id)) }
            }
        }
    }
}

@Composable
private fun NoteHeader(post: Post) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(post.author, color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
        Spacer(Modifier.width(6.dp))
        Text("· ${post.time}", color = CircleTheme.colors.muted, fontSize = 12.sp)
        if (post.authorId == ME && post.audience == Audience.INNER_CIRCLE) {
            Spacer(Modifier.width(8.dp))
            InnerCircleTag()
        }
    }
}

@Composable
fun InnerCircleTag() {
    Text(
        "INNER CIRCLE ONLY", color = CircleTheme.colors.accent, fontSize = 10.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, maxLines = 1
    )
}

@Composable
fun MediaBlock(modifier: Modifier = Modifier, height: Int = 180) {
    Box(
        modifier = modifier.fillMaxWidth().height(height.dp).clip(RoundedCornerShape(11.dp)).background(CircleTheme.colors.track),
        contentAlignment = Alignment.Center
    ) { Icon(Icons.Outlined.Image, contentDescription = "Photo", tint = CircleTheme.colors.muted) }
}

/**
 * The original note shown small inside a quote card (and in Compose while quoting).
 * Tapping it opens the original's thread.
 */
@Composable
fun EmbeddedPost(post: Post?, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val colors = CircleTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(colors.background)
            .border(1.dp, colors.border, RoundedCornerShape(11.dp))
            .then(if (onClick != null && post != null) Modifier.clickable(onClickLabel = "Open the original note", onClick = onClick) else Modifier)
            .padding(12.dp)
    ) {
        if (post == null) {
            Text("This note is no longer available.", color = colors.muted, fontSize = 13.sp)
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(post.initials, post.color, size = 24)
            Spacer(Modifier.width(8.dp))
            Text(post.author, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(" · ${post.time}", color = colors.muted, fontSize = 12.sp)
        }
        if (post.text.isNotEmpty()) {
            Text(
                post.text, color = colors.body, fontSize = 13.5.sp, lineHeight = 19.sp,
                maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp)
            )
        }
        if (post.hasMedia) MediaBlock(Modifier.padding(top = 8.dp), height = 90)
    }
}

/** A post in a group: optional group line above the author, comment and like only. */
@Composable
fun GroupPostCard(nav: NavHostController, post: Post, showGroup: Boolean) {
    CircleCard {
        if (showGroup) {
            post.groupId?.let { CircleStore.groupOrNull(it) }?.let { g ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClickLabel = "Open group") { nav.navigate(Routes.groupDetail(g.id)) }
                ) {
                    GroupTile(g.initial, g.color, size = 22)
                    Spacer(Modifier.width(8.dp))
                    Text(g.name, color = CircleTheme.colors.body, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
            }
        }
        NoteBody(nav, post, embedQuote = false)
        // Group posts stay in the group: comment and like only, no repost or quote.
        Box(Modifier.padding(start = TextInset)) { PostActions(nav, post) }
    }
}
