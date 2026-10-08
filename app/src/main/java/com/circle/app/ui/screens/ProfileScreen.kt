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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.data.CircleStore
import com.circle.app.navigation.Routes
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.circle.app.ui.components.*
import com.circle.app.ui.theme.*

private enum class ProfileTab { ALL, THOUGHTS, POSTS }

@Composable
fun ProfileScreen(nav: NavHostController) {
    var tab by remember { mutableStateOf(ProfileTab.ALL) }
    // All: everything of yours, reposts and quotes included. Thoughts: text notes. Posts: photo posts.
    val mine = CircleStore.myPosts
    val visiblePosts = when (tab) {
        ProfileTab.ALL -> mine
        ProfileTab.THOUGHTS -> mine.filter { !it.isRepost && it.quotedPostId == null && !it.hasMedia }
        ProfileTab.POSTS -> mine.filter { !it.isRepost && it.hasMedia }
    }

    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar("S", AvatarSlate, size = 72)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Shashank", color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("@shashank", color = CircleTheme.colors.muted, fontSize = 13.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .border(1.dp, CircleTheme.colors.border, RoundedCornerShape(999.dp))
                            .clickable { }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Edit profile", color = CircleTheme.colors.text, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
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
                        "Slowly rebuilding the workshop table. Say hi if you're nearby — always up for coffee and bad puns.",
                        color = CircleTheme.colors.body, fontSize = 14.5.sp
                    )
                }
            }
            item { FriendsRow(onOpen = { nav.navigate(Routes.YOUR_CIRCLE) }) }
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
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("PHOTOS")
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.height(210.dp),
                        userScrollEnabled = false,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        gridItems(List(6) { it }) {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(12.dp)).background(CircleTheme.colors.track))
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
private fun FriendsRow(onOpen: () -> Unit) {
    val colors = CircleTheme.colors
    val friends = CircleStore.friendCount
    val inner = CircleStore.innerCount
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

