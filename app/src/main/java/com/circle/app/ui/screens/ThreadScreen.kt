package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.data.CircleStore
import com.circle.app.data.Post
import com.circle.app.navigation.Routes
import com.circle.app.navigation.openPerson
import com.circle.app.ui.components.*
import com.circle.app.ui.theme.*

/** A post's thread: the root post, then its top-level comments (always visible). */
@Composable
fun ThreadScreen(nav: NavHostController, postId: String) {
    // A "You reposted" card opens the original note's thread.
    val post = CircleStore.post(postId)?.let { CircleStore.original(it) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
        Row(modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = CircleTheme.colors.body, modifier = Modifier.clickable { nav.popBackStack() })
            Spacer(Modifier.width(12.dp))
            Text("Thread", color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        if (post == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("This post isn't available.", color = CircleTheme.colors.muted, fontSize = 13.sp)
            }
            return@Column
        }

        // A note from someone you aren't friends with stays closed, wherever the link came from.
        if (!CircleStore.canOpenPost(post)) {
            Column(Modifier.weight(1f).fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                LockedNotFriendsPanel()
                Text(
                    "View ${post.author}'s profile", color = CircleTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    modifier = Modifier.padding(top = 12.dp).clickable { nav.openPerson(post.authorId) }.padding(12.dp)
                )
            }
            return@Column
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { RootPost(nav, post) }
            item {
                val n = post.comments.size
                SectionLabel(if (n == 0) "NO REPLIES YET" else "$n ${if (n == 1) "REPLY" else "REPLIES"}")
            }
            items(post.comments) { comment -> ThreadComment(comment) { nav.openPerson(it) } }
        }

        // New replies land as top-level comments at the end of this thread's list.
        Box(
            modifier = Modifier.fillMaxWidth().background(CircleTheme.colors.background).padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            ReplyComposer { text ->
                CircleStore.addComment(post.id, text)
                scope.launch { listState.animateScrollToItem(post.comments.size + 1) }
            }
        }
    }
}

@Composable
private fun RootPost(nav: NavHostController, post: Post) {
    val colors = CircleTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardShadow(colors, 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(post.initials, post.color, size = 44)
            Spacer(Modifier.width(11.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(post.author, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    if (post.authorId == com.circle.app.data.ME && post.audience == com.circle.app.data.Audience.INNER_CIRCLE) {
                        Spacer(Modifier.width(8.dp)); InnerCircleTag()
                    }
                }
                Text(post.time, color = colors.muted, fontSize = 12.sp)
            }
        }
        if (post.text.isNotEmpty()) {
            Text(
                post.text, color = colors.text,
                style = CircleTheme.postText.copy(fontSize = 17.sp, lineHeight = 24.6.sp),
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        if (post.hasMedia) MediaBlock(Modifier.padding(top = 12.dp), height = 200)
        post.quotedPostId?.let { id ->
            EmbeddedPost(CircleStore.post(id), Modifier.padding(top = 12.dp)) { nav.navigate(Routes.thread(id)) }
        }
        Box(Modifier.padding(top = 14.dp)) { PostActions(nav, post, showComment = false) }
    }
}
