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
import com.circle.app.ui.components.*
import com.circle.app.ui.theme.*

private enum class ProfileTab { ALL, THOUGHTS, POSTS }

private data class ProfilePost(val time: String, val text: String, val isMedia: Boolean)

private val profilePosts = listOf(
    ProfilePost("1d", "Repotted every plant on the balcony before the first frost. Overdue by a month.", isMedia = false),
    ProfilePost("3d", "Finally cleared the laptop repair backlog sitting around since spring. Small victories.", isMedia = false),
    ProfilePost("5d", "Sunset over the river walk — worth the detour.", isMedia = true),
    ProfilePost("9d", "Workshop table is finally starting to look like a table.", isMedia = true)
)

@Composable
fun ProfileScreen(nav: NavHostController) {
    var tab by remember { mutableStateOf(ProfileTab.ALL) }
    val visiblePosts = when (tab) {
        ProfileTab.ALL -> profilePosts
        ProfileTab.THOUGHTS -> profilePosts.filter { !it.isMedia }
        ProfileTab.POSTS -> profilePosts.filter { it.isMedia }
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar("S", AvatarSlate, size = 72)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Shashank", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("@shashank", color = TextMuted, fontSize = 13.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .border(1.dp, Border, RoundedCornerShape(999.dp))
                            .clickable { }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Edit profile", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
            item {
                CircleCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.PushPin, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("PINNED", color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Text(
                        "Slowly rebuilding the workshop table. Say hi if you're nearby — always up for coffee and bad puns.",
                        color = TextBody, fontSize = 14.5.sp
                    )
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                        StatItem("128", "friends")
                        StatItem("6", "inner circle")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextDim, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Only you see this", color = TextDim, fontSize = 10.5.sp)
                    }
                }
            }
            item {
                UnderlineTabs(listOf("All", "Thoughts", "Posts"), tab.ordinal) { tab = ProfileTab.entries[it] }
            }
            items(visiblePosts) { post -> ProfilePostCard(post) }
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
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceAlt))
                        }
                    }
                }
            }
            item {
                CircleCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Private by default", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(
                        "If someone who isn't a friend finds their way here, all they see is your avatar and your pinned thought — nothing else on this page is visible until you're connected.",
                        color = TextMuted, fontSize = 12.sp
                    )
                }
            }
        }
        CircleBottomNav(nav, NavTab.PROFILE)
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.width(5.dp))
        Text(label, color = TextMuted, fontSize = 13.sp)
    }
}

@Composable
private fun ProfilePostCard(post: ProfilePost) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row {
            Text("Shashank", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Text("· ${post.time}", color = TextMuted, fontSize = 11.5.sp)
        }
        Text(post.text, color = TextBody, fontSize = 13.5.sp)
        if (post.isMedia) {
            Box(
                modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceAlt),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.Image, contentDescription = null, tint = TextDim) }
        }
    }
}
