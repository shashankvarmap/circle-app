package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
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
import com.circle.app.ui.components.*
import com.circle.app.ui.theme.*

/**
 * Group detail. Outsiders see "Request to join", which stays pending — never instant.
 */
@Composable
fun GroupScreen(nav: NavHostController, groupId: String?) {
    val group = CircleStore.group(groupId)
    val isMember = group.isMember

    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
        Row(modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = CircleTheme.colors.body, modifier = Modifier.clickable { nav.popBackStack() })
            Spacer(Modifier.width(12.dp))
            Text("Group", color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GroupTile(group.initial, group.color, size = 64)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(group.name, color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(group.category, color = CircleTheme.colors.muted, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (group.isPublic) {
                                Text("PUBLIC", color = CircleTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            } else {
                                Text("PRIVATE", color = CircleTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                            Spacer(Modifier.width(7.dp))
                            Text("${group.memberCount} ${if (group.memberCount == 1) "member" else "members"}", color = CircleTheme.colors.muted, fontSize = 11.5.sp)
                        }
                    }
                }
            }
            item {
                if (group.description.isNotEmpty()) Text(group.description, color = CircleTheme.colors.body, fontSize = 13.sp)
            }
            item {
                if (isMember) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).border(1.dp, CircleTheme.colors.border, RoundedCornerShape(999.dp)).padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("✓ Member", color = CircleTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.5.sp) }
                        Spacer(Modifier.width(10.dp))
                        Text("Leave", color = CircleTheme.colors.muted, fontSize = 12.sp, modifier = Modifier.clickable { CircleStore.leaveGroup(group) })
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (group.isPublic) {
                            JoinRequestButton(group, modifier = Modifier.fillMaxWidth())
                            Text(
                                if (group.joinRequested) "Request sent — every member can see it. You'll get access once someone approves it."
                                else "Groups default to private. This one is public — your request will be visible to every member.",
                                color = CircleTheme.colors.muted, fontSize = 11.sp
                            )
                        } else {
                            Text("This group is private — members add new people directly.", color = CircleTheme.colors.muted, fontSize = 11.sp)
                        }
                    }
                }
            }
            if (isMember) {
                item { ComposeToGroupRow(nav, group.id) }
                val groupPosts = CircleStore.postsInGroup(group.id)
                if (groupPosts.isEmpty()) {
                    item { Text("Nothing posted here yet. Be the first.", color = CircleTheme.colors.muted, fontSize = 13.sp) }
                }
                items(groupPosts, key = { it.id }) { post -> GroupPostCard(nav, post, showGroup = false) }
            } else {
                item { LockedPanel("Join to see what's being posted here.") }
            }
        }
    }
}

@Composable
fun LockedPanel(text: String, footnote: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CircleTheme.colors.surface)
            .border(1.dp, CircleTheme.colors.border, RoundedCornerShape(14.dp))
            .padding(vertical = 22.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = CircleTheme.colors.muted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(8.dp))
        Text(text, color = CircleTheme.colors.body, fontSize = 12.5.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        footnote?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = CircleTheme.colors.muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ComposeToGroupRow(nav: NavHostController, groupId: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CircleTheme.colors.surface)
            .clickable { nav.navigate(com.circle.app.navigation.Routes.compose(group = groupId)) }.padding(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar("S", AvatarSlate, size = 28)
        Spacer(Modifier.width(10.dp))
        Text("Post to the group…", color = CircleTheme.colors.muted, fontSize = 13.sp)
    }
}

