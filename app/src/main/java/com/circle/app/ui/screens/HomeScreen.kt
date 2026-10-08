package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.circle.app.data.CircleStore
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

    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(52.dp).clickable { doRefresh() },
            contentAlignment = Alignment.Center
        ) {
            Text("Circle.", fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = CircleTheme.colors.text)
        }

        Box(modifier = Modifier.fillMaxWidth().padding(top = 9.dp), contentAlignment = Alignment.Center) {
            UnderlineTabs(listOf("Friends", "Groups"), if (feed == Feed.FRIENDS) 0 else 1) {
                feed = if (it == 0) Feed.FRIENDS else Feed.GROUPS
            }
        }
        Text(
            "Tip: press & hold the Home icon below to flip feeds too",
            color = CircleTheme.colors.muted, fontSize = 9.5.sp,
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
                        Refresh.CHECKING -> RefreshRow("Checking for new posts…", CircleTheme.colors.muted)
                        Refresh.DONE -> RefreshRow("Nothing to refresh — you're caught up", CircleTheme.colors.accent, showCheck = true)
                        Refresh.IDLE -> {}
                    }
                }
                item(key = "compose") { ComposeRow(nav) }
                items(CircleStore.homeFeed, key = { it.id }) { post -> FeedCard(nav, post) }
                item(key = "caught-up") {
                    Text(
                        "You're all caught up", color = CircleTheme.colors.muted, fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                val memberGroups = CircleStore.groups.filter { it.isMember }
                if (memberGroups.isNotEmpty()) item(key = "chips") { GroupsChipRow(nav) }
                item(key = "note") {
                    Text(
                        "Group posts stay in the group. No repost, no quote.",
                        color = CircleTheme.colors.muted, fontSize = 11.sp
                    )
                }
                val groupPosts = CircleStore.groupFeed
                if (groupPosts.isEmpty()) {
                    item(key = "empty") {
                        Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (memberGroups.isEmpty()) "You're not in any groups yet." else "Nothing new in your groups.",
                                color = CircleTheme.colors.muted, fontSize = 13.sp
                            )
                            Text(
                                "Find a group in Explore", color = CircleTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                modifier = Modifier.clickable { nav.navigate(Routes.EXPLORE) }.padding(8.dp)
                            )
                        }
                    }
                }
                items(groupPosts, key = { it.id }) { post -> GroupPostCard(nav, post, showGroup = true) }
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
            .background(CircleTheme.colors.surface)
            .clickable { nav.navigate(Routes.COMPOSE_POST) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar("S", AvatarSlate, size = 32)
        Spacer(Modifier.width(10.dp))
        Text("What's on your mind?", color = CircleTheme.colors.muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.Image, contentDescription = "Attach media", tint = CircleTheme.colors.muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun GroupsChipRow(nav: NavHostController) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(CircleStore.groups.filter { it.isMember }, key = { it.id }) { g ->
            GroupChip(g.initial, g.color) { nav.navigate(Routes.groupDetail(g.id)) }
        }
    }
}

@Composable
private fun GroupChip(initial: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(modifier = Modifier.clickable(onClick = onClick)) { GroupTile(initial, color, size = 52) }
}

