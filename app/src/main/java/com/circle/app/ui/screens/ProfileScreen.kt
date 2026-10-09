package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.CircleSummary
import com.circle.app.ui.vm.LocalMe
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import com.circle.app.navigation.Routes
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.circle.app.ui.components.*
import com.circle.app.ui.theme.*

private enum class ProfileTab { ALL, THOUGHTS, POSTS }

class MyProfileViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    val notes = repo.myNotes().state(emptyList())
    val circle = repo.circleSummary().state(CircleSummary(0, 0))
}

@Composable
fun ProfileScreen(nav: NavHostController) {
    var tab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(ProfileTab.ALL) }
    val vm = circleViewModel { MyProfileViewModel(it) }
    val mine by vm.notes.collectAsStateWithLifecycle()
    val circle by vm.circle.collectAsStateWithLifecycle()
    val me = LocalMe.current
    // All: everything of yours, reposts and quotes included. Thoughts: text notes. Posts: photo posts.
    val visiblePosts = when (tab) {
        ProfileTab.ALL -> mine
        ProfileTab.THOUGHTS -> mine.filter { !it.isRepost && it.quoted == null && !it.hasMedia }
        ProfileTab.POSTS -> mine.filter { !it.isRepost && it.hasMedia }
    }

    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MeAvatar(68)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            me?.displayName.orEmpty(), fontFamily = Display, fontWeight = FontWeight.SemiBold,
                            fontSize = 24.sp, letterSpacing = (-0.36).sp, color = CircleTheme.colors.text
                        )
                        Text("@${me?.handle.orEmpty()}", color = CircleTheme.colors.muted, fontSize = 13.sp)
                    }
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).clickable { nav.navigate(Routes.SETTINGS) },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = CircleTheme.colors.text) }
                }
            }
            item {
                CircleCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.PushPin, contentDescription = null, tint = CircleTheme.colors.muted, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("PINNED", color = CircleTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Text(
                        me?.pinnedThought.orEmpty().ifBlank { "No pinned thought yet. Add one in Settings → Edit profile." },
                        color = CircleTheme.colors.body, fontSize = 14.5.sp
                    )
                }
            }
            item { FriendsRow(circle, onOpen = { nav.navigate(Routes.YOUR_CIRCLE) }) }
            item {
                UnderlineTabs(listOf("All", "Thoughts", "Posts"), tab.ordinal) { tab = ProfileTab.entries[it] }
            }
            if (visiblePosts.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Nothing here yet.", color = CircleTheme.colors.muted, fontSize = 13.sp)
                        Text(
                            "Write a note", color = CircleTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            modifier = Modifier.clickable { nav.navigate(Routes.COMPOSE_POST) }.padding(8.dp)
                        )
                    }
                }
            }
            items(visiblePosts, key = { it.id }) { post -> FeedCard(nav, post) }
            // Your photo notes, newest first (no grid at all when there are none).
            val photoNotes = mine.filter { it.hasMedia && !it.isRepost }
            if (photoNotes.isNotEmpty()) item(key = "photos") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("PHOTOS")
                    photoNotes.take(9).chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { n ->
                                Box(
                                    Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(12.dp)).background(CircleTheme.colors.track)
                                        .clickable(onClickLabel = "Open photo") { nav.navigate(Routes.viewer(n.id)) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val first = n.media.firstOrNull()
                                    if (first != null) MediaThumb(first)
                                    else Icon(Icons.Outlined.Image, contentDescription = null, tint = CircleTheme.colors.muted)
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            item {
                CircleCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = CircleTheme.colors.muted, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Private by default", color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(
                        "If someone who isn't a friend finds their way here, all they see is your avatar and your pinned thought — nothing else on this page is visible until you're connected.",
                        color = CircleTheme.colors.muted, fontSize = 12.sp
                    )
                }
            }
        }
        CircleBottomNav(nav, NavTab.PROFILE)
    }
}

/** The highlighted entry to Your circle. Counts come from the store and are private to you. */
@Composable
private fun FriendsRow(circle: CircleSummary, onOpen: () -> Unit) {
    val colors = CircleTheme.colors
    val friends = circle.friendCount
    val inner = circle.innerCount
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(colors.surface)
                .border(1.5.dp, colors.accent, RoundedCornerShape(13.dp))
                .clickable(onClickLabel = "Open your circle", onClick = onOpen)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$friends friends, $inner inner circle. See your circle"
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("$friends", color = colors.text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text("friends", color = colors.muted, fontSize = 13.sp)
            Text("·", color = colors.muted, fontSize = 13.sp)
            Text("$inner", color = colors.text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text("inner circle", color = colors.muted, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text("See your circle", color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
        }
        Row(Modifier.padding(start = 2.dp, top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.muted, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(5.dp))
            Text("Only you see this. Tap it to open your circle.", color = colors.muted, fontSize = 11.sp)
        }
    }
}

