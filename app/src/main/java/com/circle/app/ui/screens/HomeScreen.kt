package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.navigation.Routes
import com.circle.app.ui.components.*
import com.circle.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Feed { FRIENDS, GROUPS }
private enum class Refresh { IDLE, CHECKING, DONE }

@Composable
fun HomeScreen(nav: NavHostController) {
    var feed by remember { mutableStateOf(Feed.FRIENDS) }
    var refresh by remember { mutableStateOf(Refresh.IDLE) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun doRefresh() {
        scope.launch {
            listState.animateScrollToItem(0)
            refresh = Refresh.CHECKING
            delay(700)
            refresh = Refresh.DONE
            delay(2000)
            refresh = Refresh.IDLE
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(52.dp).clickable { doRefresh() },
            contentAlignment = Alignment.Center
        ) {
            Text("Circle.", fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = TextPrimary)
        }

        Box(modifier = Modifier.fillMaxWidth().padding(top = 9.dp), contentAlignment = Alignment.Center) {
            UnderlineTabs(listOf("Friends", "Groups"), if (feed == Feed.FRIENDS) 0 else 1) {
                feed = if (it == 0) Feed.FRIENDS else Feed.GROUPS
            }
        }
        Text(
            "Tip: press & hold the Home icon below to flip feeds too",
            color = androidx.compose.ui.graphics.Color(0xFF454A55), fontSize = 9.5.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (feed == Feed.FRIENDS) {
                item {
                    when (refresh) {
                        Refresh.CHECKING -> RefreshRow("Checking for new posts…", TextMuted)
                        Refresh.DONE -> RefreshRow("Nothing to refresh — you're caught up", Success, showCheck = true)
                        Refresh.IDLE -> {}
                    }
                }
                item { ComposeRow(nav) }
                item { ThoughtCard(nav) }
                item { RepostCard(nav) }
                item { QuoteCard(nav) }
                item { MediaPostCard(nav) }
                item { Text("You're all caught up", color = TextDim, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
            } else {
                item { GroupsChipRow(nav) }
                item {
                    Text(
                        "Group posts stay in the group — no repost, no quote, no share out to Home.",
                        color = TextDim, fontSize = 11.sp
                    )
                }
                item { GroupFeedCard(nav, "Aachen Photo Walk", AvatarOrange, "PK", "Priya Kapoor", "40m", "Golden hour shoot this Sunday — meet at the cathedral steps, 6:30 sharp.") }
                item { GroupFeedCard(nav, "RWTH Board Game Club", AvatarBlue, "JL", "Jonah Lee", "2h", "Anyone up for Catan tonight?", liked = true) }
            }
        }

        CircleBottomNav(nav, NavTab.HOME, onHomeIconHold = {
            feed = if (feed == Feed.FRIENDS) Feed.GROUPS else Feed.FRIENDS
        })
    }
}

@Composable
private fun RefreshRow(text: String, color: androidx.compose.ui.graphics.Color, showCheck: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (showCheck) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = color, fontSize = 11.5.sp)
    }
}

@Composable
private fun ComposeRow(nav: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Surface)
            .clickable { nav.navigate(Routes.COMPOSE_POST) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar("S", AvatarSlate, size = 32)
        Spacer(Modifier.width(10.dp))
        Text("What's on your mind?", color = TextMuted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.Image, contentDescription = "Attach media", tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ThoughtCard(nav: NavHostController) {
    CircleCard {
        Row {
            Avatar("AO", AvatarOrange, size = 38)
            Spacer(Modifier.width(11.dp))
            Column {
                Row {
                    Text("Amara Osei", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("· 2h", color = TextMuted, fontSize = 13.sp)
                }
                Text("Finally finished the puzzle we started at Mom's over the summer.", color = TextBody, fontSize = 14.5.sp)
            }
        }
        Box(Modifier.padding(start = 49.dp)) {
            PostActions(
                nav,
                sampleReplies = listOf(
                    Comment(
                        "Priya", "Aww, that's so wholesome!",
                        replies = listOf(
                            Comment("Amara Osei", "It took us three months 😅"),
                            Comment("Jonah Lee", "Frame it!")
                        )
                    ),
                    Comment("Jonah Lee", "Which puzzle was it?", replies = listOf(Comment("Amara Osei", "The 2000-piece lighthouse one."))),
                    Comment("Elena Voss", "Mom must be thrilled.")
                )
            )
        }
    }
}

@Composable
private fun RepostCard(nav: NavHostController) {
    CircleCard {
        Text("↻  You reposted", color = TextMuted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(11.dp))
                .background(Background)
                .padding(13.dp)
        ) {
            Avatar("PK", AvatarSage, size = 34)
            Spacer(Modifier.width(9.dp))
            Column {
                Row {
                    Text("Priya Kapoor", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("· 5h", color = TextMuted, fontSize = 12.5.sp)
                }
                Text("Sea glass hunting turned into a two hour walk. Worth it.", color = TextBody, fontSize = 14.sp)
            }
        }
        Box(Modifier.padding(start = 43.dp)) { PostActions(nav) }
    }
}

@Composable
private fun QuoteCard(nav: NavHostController) {
    CircleCard {
        Row {
            Avatar("S", AvatarSlate, size = 38)
            Spacer(Modifier.width(11.dp))
            Column {
                Row {
                    Text("You", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("· 1h", color = TextMuted, fontSize = 13.sp)
                }
                Text("This is exactly the energy I needed today.", color = TextBody, fontSize = 14.5.sp)
            }
        }
        Row(
            modifier = Modifier.padding(start = 49.dp).clip(RoundedCornerShape(11.dp)).background(Background).padding(12.dp)
        ) {
            Avatar("JL", AvatarBlue, size = 28)
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Jonah Lee", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Some days the good coffee is the whole plan.", color = TextBody, fontSize = 13.sp)
            }
        }
        Box(Modifier.padding(start = 49.dp)) {
            PostActions(nav, initiallyLiked = true)
        }
    }
}

@Composable
private fun MediaPostCard(nav: NavHostController) {
    CircleCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar("S", AvatarSlate, size = 32)
            Spacer(Modifier.width(9.dp))
            Column {
                Text("You", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                Text("40m", color = TextMuted, fontSize = 11.5.sp)
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(11.dp)).background(SurfaceAlt),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.Image, contentDescription = null, tint = TextDim) }
        PostActions(nav)
    }
}

@Composable
private fun GroupsChipRow(nav: NavHostController) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GroupChip("A", AvatarOrange) { nav.navigate(Routes.GROUP_DETAIL) }
        GroupChip("S", AvatarSage) { nav.navigate(Routes.GROUP_DETAIL) }
        GroupChip("R", AvatarBlue) { nav.navigate(Routes.GROUP_DETAIL) }
    }
}

@Composable
private fun GroupChip(initial: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(modifier = Modifier.clickable(onClick = onClick)) { GroupTile(initial, color, size = 52) }
}

@Composable
private fun GroupFeedCard(nav: NavHostController, groupName: String, groupColor: androidx.compose.ui.graphics.Color, initials: String, name: String, time: String, text: String, liked: Boolean = false) {
    CircleCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GroupTile(groupName.first().toString(), groupColor, size = 22)
            Spacer(Modifier.width(8.dp))
            Text(groupName, color = TextBody, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
        }
        Row {
            Avatar(initials, AvatarBlue, size = 34)
            Spacer(Modifier.width(10.dp))
            Column {
                Row {
                    Text(name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("· $time", color = TextMuted, fontSize = 12.sp)
                }
                Text(text, color = TextBody, fontSize = 13.5.sp)
            }
        }
        // Group posts: comment + like only, no repost/quote
        Box(Modifier.padding(start = 44.dp)) {
            PostActions(nav, showRepost = false, initiallyLiked = liked)
        }
    }
}
