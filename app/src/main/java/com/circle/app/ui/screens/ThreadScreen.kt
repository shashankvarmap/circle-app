package com.circle.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.circle.app.R
import com.circle.app.data.access.Audience
import com.circle.app.data.access.DenyReason
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.CommentView
import com.circle.app.data.repo.Note
import com.circle.app.data.repo.NoteView
import com.circle.app.data.repo.Outcome
import com.circle.app.data.repo.ThreadResult
import com.circle.app.navigation.Routes
import com.circle.app.navigation.openPerson
import com.circle.app.ui.components.*
import com.circle.app.ui.format.denyText
import com.circle.app.ui.format.relativeTime
import com.circle.app.ui.theme.*
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import kotlinx.coroutines.launch

/**
 * A thread, from the repository only. It re-evaluates on every change: if the author unfriends or
 * blocks you (or you them) while it's open, it turns into the gate and its content is gone.
 */
class ThreadViewModel(repo: CircleRepository, noteId: String) : RepoViewModel(repo) {
    /** Null while loading. */
    val thread = repo.thread(noteId).state(null)
    private val id = noteId
    fun reply(text: String, replyTo: String?, then: (Outcome<Unit>) -> Unit) = run({ addComment(id, text, replyTo) }, then)
    fun likeComment(commentId: String) = run({ toggleCommentLike(id, commentId) })
    fun deleteComment(commentId: String) = run({ deleteComment(id, commentId) })

}

/** A post's thread: the root post, then its top-level comments (always visible). */
@Composable
fun ThreadScreen(nav: NavHostController, postId: String) {
    val vm = circleViewModel(key = "thread-$postId") { ThreadViewModel(it, postId) }
    val result by vm.thread.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = CircleTheme.colors
    // Which comments show their replies, and which comment the composer replies to.
    var expanded by rememberSaveable { mutableStateOf(setOf<String>()) }
    var replyTo by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var reporting by rememberSaveable { mutableStateOf<String?>(null) }
    reporting?.let { id -> ReportSheet(com.circle.app.data.ReportKind.COMMENT, id, onDone = { reporting = null }) }

    confirmDelete?.let { id ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.comment_delete_title)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { vm.deleteComment(id); confirmDelete = null }) {
                    Text(stringResource(R.string.comment_delete), color = colors.alertDot)
                }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.cancel), color = colors.text) } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Row(modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.popBackStack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.body)
            }
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.thread_title), color = colors.text, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.semantics { heading() })
        }

        when (val r = result) {
            null -> Spacer(Modifier.weight(1f))
            is ThreadResult.Denied -> {
                val author = r.author
                if (r.reason == DenyReason.NOT_FRIENDS && author != null) {
                    // The gate, never the thread: no comments, no composer.
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 24.dp)) {
                        GateBody(author.id, GateKind.NOTE, onViewProfile = { nav.openPerson(author.id) }, onDone = { nav.popBackStack() })
                    }
                } else {
                    Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (r.reason == DenyReason.BLOCKED || r.reason == DenyReason.NOT_FOUND) stringResource(R.string.thread_unavailable) else denyText(r.reason),
                            color = colors.muted, fontSize = 13.sp
                        )
                    }
                }
            }
            is ThreadResult.Opened -> {
                val thread = r.thread
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "root") { RootPost(nav, thread.root) }
                    item(key = "label") {
                        val n = thread.comments.size
                        SectionLabel(if (n == 0) stringResource(R.string.thread_no_replies) else pluralStringResourceCaps(n))
                    }
                    val actions = object : CommentActions {
                        override fun like(comment: CommentView) = vm.likeComment(comment.id)
                        override fun reply(comment: CommentView) { replyTo = comment.id }
                        override fun delete(comment: CommentView) { confirmDelete = comment.id }
                        override fun report(comment: CommentView) { reporting = comment.id }
                    }
                    items(thread.comments, key = { it.id }) { comment ->
                        ThreadComment(
                            comment,
                            expanded = comment.id in expanded,
                            onToggleReplies = { expanded = if (comment.id in expanded) expanded - comment.id else expanded + comment.id },
                            onPerson = { nav.openPerson(it) },
                            actions = actions,
                        )
                    }
                }
                // The comment being replied to (a reply to a reply joins the same top-level comment).
                val allComments = thread.comments + thread.comments.flatMap { it.replies }
                val target = replyTo?.let { id -> allComments.firstOrNull { it.id == id } }
                val targetTop = target?.let { t -> thread.comments.firstOrNull { top -> top.id == t.id || top.replies.any { it.id == t.id } } }
                Box(Modifier.fillMaxWidth().background(colors.background).padding(horizontal = 14.dp, vertical = 10.dp)) {
                    ReplyComposer(
                        above = target?.let { t -> { ReplyingToChip(t.author.firstName) { replyTo = null } } }, focusKey = replyTo
                    ) { text ->
                        vm.reply(text, target?.id) { outcome ->
                            if (outcome !is Outcome.Success) return@reply
                            if (targetTop != null) {
                                expanded = expanded + targetTop.id // show the reply where it landed
                                replyTo = null
                            } else scope.launch { listState.animateScrollToItem(thread.comments.size + 1) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun pluralStringResourceCaps(n: Int): String =
    androidx.compose.ui.res.pluralStringResource(R.plurals.thread_replies_caps, n, n)

@Composable
private fun RootPost(nav: NavHostController, post: Note) {
    val colors = CircleTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardShadow(colors, 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable(onClickLabel = androidx.compose.ui.res.stringResource(com.circle.app.R.string.open_profile_of, post.author.name)) { nav.openPerson(post.author.id) }
        ) {
            if (post.isMine) MeAvatar(44) else Avatar(post.author.initials, post.author.color, size = 44)
            Spacer(Modifier.width(11.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(post.author.name, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    if (post.isMine && post.audience == Audience.INNER_CIRCLE) {
                        Spacer(Modifier.width(8.dp)); InnerCircleTag()
                    }
                }
                Text(relativeTime(post.createdAt), color = colors.muted, fontSize = 12.sp)
            }
        }
        NoteMenuButton(nav, post)
        }
        if (post.text.isNotEmpty()) {
            Text(
                post.text, color = colors.text,
                style = CircleTheme.postText.copy(fontSize = 17.sp, lineHeight = 24.6.sp),
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        if (post.hasMedia) NoteMedia(post.media, Modifier.padding(top = 12.dp), height = 200) { i -> nav.navigate(Routes.viewer(post.id, i)) }
        post.quoted?.let { quoted ->
            EmbeddedPost(quoted, Modifier.padding(top = 12.dp)) {
                when (quoted) {
                    is NoteView.Full -> nav.navigate(Routes.thread(quoted.note.id))
                    is NoteView.Snippet -> nav.navigate(Routes.thread(quoted.noteId))
                    NoteView.Hidden -> Unit
                }
            }
        }
        Box(Modifier.padding(top = 14.dp)) { PostActions(nav, post, showComment = false) }
    }
}
