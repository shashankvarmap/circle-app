package com.circle.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.R
import com.circle.app.data.access.Audience
import com.circle.app.data.repo.Note
import com.circle.app.data.repo.NoteView
import com.circle.app.navigation.Routes
import com.circle.app.navigation.openPerson
import com.circle.app.ui.format.relativeTime
import com.circle.app.ui.theme.*

private const val AVATAR = 34
private val TextInset = (AVATAR + 10).dp

/**
 * A note on Home or Profile: a plain note, a photo post, a quote (your text, then the original
 * embedded under it) or a "You reposted" card. Actions always act on the original note.
 */
@Composable
fun FeedCard(nav: NavHostController, note: Note) {
    val repostOf = note.repostOf
    if (repostOf != null) {
        CircleCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Repeat, contentDescription = null, tint = CircleTheme.colors.muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.note_you_reposted), color = CircleTheme.colors.muted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                Text(" " + androidx.compose.ui.res.stringResource(com.circle.app.R.string.dot_time, relativeTime(note.createdAt)), color = CircleTheme.colors.muted, fontSize = 12.sp)
            }
            when (repostOf) {
                is NoteView.Full -> {
                    val original = repostOf.note
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(11.dp))
                            .background(CircleTheme.colors.background)
                            .clickable(onClickLabel = androidx.compose.ui.res.stringResource(com.circle.app.R.string.common_open_thread)) { nav.navigate(Routes.thread(original.id)) }
                            .padding(12.dp)
                    ) { NoteBody(nav, original, embedQuote = true) }
                    Box(Modifier.padding(start = 2.dp)) { PostActions(nav, note) }
                }
                else -> EmbeddedPost(repostOf) { nav.navigate(Routes.thread(note.actionTargetId)) }
            }
        }
    } else {
        CircleCard {
            NoteBody(nav, note, embedQuote = true)
            Box(Modifier.padding(start = TextInset)) { PostActions(nav, note) }
        }
    }
}

/** Avatar, name, time (and the private "INNER CIRCLE ONLY" tag on your own notes), then the content. */
@Composable
private fun NoteBody(nav: NavHostController, note: Note, embedQuote: Boolean) {
    val author = note.author
    Row {
        Box(
            Modifier.clip(CircleShape)
                .clickable(onClickLabel = androidx.compose.ui.res.stringResource(com.circle.app.R.string.open_profile_of, author.name)) { nav.openPerson(author.id) }
        ) { if (author.isMe) MeAvatar(AVATAR) else Avatar(author.initials, author.color, size = AVATAR) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
                        .clickable(onClickLabel = androidx.compose.ui.res.stringResource(com.circle.app.R.string.open_profile_of, author.name)) { nav.openPerson(author.id) }
                ) { NoteHeader(note) }
                Box(Modifier.offset(x = 8.dp, y = (-10).dp)) { NoteMenuButton(nav, note) }
            }
            if (note.text.isNotEmpty()) {
                Text(note.text, color = CircleTheme.colors.body, style = CircleTheme.postText, modifier = Modifier.padding(top = 2.dp))
            }
            if (note.hasMedia) NoteMedia(note.media, Modifier.padding(top = 8.dp)) { i -> nav.navigate(Routes.viewer(note.id, i)) }
            if (embedQuote) note.quoted?.let { quoted ->
                EmbeddedPost(quoted, Modifier.padding(top = 10.dp)) {
                    val id = when (quoted) {
                        is NoteView.Full -> quoted.note.id
                        is NoteView.Snippet -> quoted.noteId
                        NoteView.Hidden -> null
                    }
                    id?.let { nav.navigate(Routes.thread(it)) }
                }
            }
        }
    }
}

@Composable
private fun NoteHeader(note: Note) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(note.author.name, color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1)
        Spacer(Modifier.width(6.dp))
        Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.dot_time, relativeTime(note.createdAt)), color = CircleTheme.colors.muted, fontSize = 12.sp, maxLines = 1)
        if (note.isMine && note.audience == Audience.INNER_CIRCLE) {
            Spacer(Modifier.width(8.dp))
            InnerCircleTag()
        }
    }
}

@Composable
fun InnerCircleTag() {
    Text(
        stringResource(R.string.note_inner_circle_tag), color = CircleTheme.colors.accent, fontSize = 10.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, maxLines = 1
    )
}

/**
 * A note shown small inside another (a quote card, a repost of something you can no longer see,
 * Compose while quoting). A note you may not open shows only its author and snippet, with a lock.
 */
@Composable
fun EmbeddedPost(view: NoteView, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val colors = CircleTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(colors.background)
            .border(1.dp, colors.border, RoundedCornerShape(11.dp))
            .then(if (onClick != null && view !is NoteView.Hidden) Modifier.clickable(onClickLabel = androidx.compose.ui.res.stringResource(com.circle.app.R.string.postcard_open_the_original_note), onClick = onClick) else Modifier)
            .padding(12.dp)
    ) {
        when (view) {
            NoteView.Hidden -> Text(stringResource(R.string.note_unavailable), color = colors.muted, fontSize = 13.sp)
            is NoteView.Snippet -> {
                Text(view.authorName, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(view.text, color = colors.body, fontSize = 13.5.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.muted, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.not_friends_yet), color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            is NoteView.Full -> {
                val post = view.note
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (post.author.isMe) MeAvatar(24) else Avatar(post.author.initials, post.author.color, size = 24)
                    Spacer(Modifier.width(8.dp))
                    Text(post.author.name, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(" " + androidx.compose.ui.res.stringResource(com.circle.app.R.string.dot_time, relativeTime(post.createdAt)), color = colors.muted, fontSize = 12.sp)
                }
                if (post.text.isNotEmpty()) {
                    Text(
                        post.text, color = colors.body, fontSize = 13.5.sp, lineHeight = 19.sp,
                        maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp)
                    )
                }
                if (post.hasMedia) NoteMedia(post.media, Modifier.padding(top = 8.dp), height = 90, onOpen = null)
            }
        }
    }
}

/** A post in a group: optional group line above the author, comment and like only. */
@Composable
fun GroupPostCard(nav: NavHostController, note: Note, showGroup: Boolean) {
    CircleCard {
        if (showGroup) {
            note.group?.let { g ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClickLabel = androidx.compose.ui.res.stringResource(com.circle.app.R.string.chat_open_group)) { nav.navigate(Routes.groupDetail(g.id)) }
                ) {
                    GroupTile(g.initial, g.color, size = 22)
                    Spacer(Modifier.width(8.dp))
                    Text(g.name, color = CircleTheme.colors.body, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
            }
        }
        NoteBody(nav, note, embedQuote = false)
        // Group posts stay in the group: comment and like only, no repost or quote.
        Box(Modifier.padding(start = TextInset)) { PostActions(nav, note) }
    }
}
