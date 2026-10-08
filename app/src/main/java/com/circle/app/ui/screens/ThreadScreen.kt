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
import com.circle.app.ui.components.*
import com.circle.app.ui.theme.*

/** A post's thread: the root post, then its top-level comments (always visible). */
@Composable
fun ThreadScreen(nav: NavHostController, postId: String) {
    val post = CircleStore.post(postId)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Row(modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = TextBody, modifier = Modifier.clickable { nav.popBackStack() })
            Spacer(Modifier.width(12.dp))
            Text("Thread", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        if (post == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("This post isn't available.", color = TextMuted, fontSize = 13.sp)
            }
            return@Column
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { RootPost(post) }
            item {
                val n = post.comments.size
                SectionLabel(if (n == 0) "NO REPLIES YET" else "$n ${if (n == 1) "REPLY" else "REPLIES"}")
            }
            items(post.comments) { comment -> ThreadComment(comment) }
        }

        // New replies land as top-level comments at the end of this thread's list.
        Box(
            modifier = Modifier.fillMaxWidth().background(Background).padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            ReplyComposer { text ->
                post.comments.add(CircleStore.you("now", text))
                scope.launch { listState.animateScrollToItem(post.comments.size + 1) }
            }
        }
    }
}

@Composable
private fun RootPost(post: Post) {
    CircleCard {
        Row {
            Avatar(post.initials, post.color, size = 38)
            Spacer(Modifier.width(11.dp))
            Column {
                Row {
                    Text(post.author, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("· ${post.time}", color = TextMuted, fontSize = 13.sp)
                }
                if (post.text.isNotEmpty()) Text(post.text, color = TextBody, fontSize = 14.5.sp)
            }
        }
        post.quoted?.let { (author, text) ->
            Column(
                modifier = Modifier.padding(start = 49.dp).fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Background).padding(12.dp)
            ) {
                Text(author, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text, color = TextBody, fontSize = 13.sp)
            }
        }
        if (post.hasMedia) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(11.dp)).background(SurfaceAlt),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.Image, contentDescription = null, tint = TextDim) }
        }
    }
}
