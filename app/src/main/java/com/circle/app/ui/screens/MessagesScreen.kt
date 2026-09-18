package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.navigation.Routes
import com.circle.app.ui.components.*
import com.circle.app.ui.theme.*

private enum class MTab { DIRECT, GROUP }

@Composable
fun MessagesScreen(nav: NavHostController) {
    var tab by remember { mutableStateOf(MTab.DIRECT) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Messages", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Icon(Icons.Outlined.Add, contentDescription = "New chat", tint = AccentDefault)
        }
        Box(modifier = Modifier.padding(top = 12.dp, start = 16.dp)) {
            UnderlineTabs(listOf("Messages", "Group Messages"), tab.ordinal) { tab = MTab.entries[it] }
        }

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
            if (tab == MTab.DIRECT) {
                item { DmRow("Priya Kapoor", "Sounds good, see you then", "PK", AvatarSage, "10m", unread = true) }
                item { DmRow("Jonah Lee", "Bringing the tripod", "JL", AvatarBlue, "2h") }
                item { GroupChatRow("Weekend Trip", "Priya: I'll book the cabin", "5h") }
                item { DmRow("Amara Osei", "Thanks for the recipe", "AO", AvatarOrange, "1d") }
            } else {
                item {
                    Text(
                        "Only chats tied to a group you've joined show up here. Chats you start with friends live under Messages instead.",
                        color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { nav.navigate(Routes.GROUP_DETAIL) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GroupTile("A", AvatarOrange, size = 44)
                        Spacer(Modifier.width(11.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Aachen Photo Walk", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Text("Tomas: I'll bring a tripod to share", color = TextBody, fontSize = 12.5.sp, maxLines = 1)
                        }
                        Text("12m", color = TextMuted, fontSize = 11.sp)
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        GroupTile("R", AvatarBlue, size = 44)
                        Spacer(Modifier.width(11.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("RWTH Board Game Club", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Text("Jonah: Anyone up for Catan tonight?", color = TextBody, fontSize = 12.5.sp, maxLines = 1)
                        }
                        Text("2h", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }
        CircleBottomNav(nav, NavTab.MESSAGES)
    }
}

@Composable
private fun DmRow(name: String, preview: String, initials: String, color: Color, time: String, unread: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(initials, color, size = 44)
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            Text(preview, color = TextBody, fontSize = 12.5.sp, maxLines = 1)
        }
        Text(time, color = TextMuted, fontSize = 11.sp)
        if (unread) {
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.size(8.dp).background(AccentDefault, androidx.compose.foundation.shape.CircleShape))
        }
    }
}

@Composable
private fun GroupChatRow(name: String, preview: String, time: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(44.dp)) {
            Avatar("PK", AvatarSage, size = 32)
            Box(modifier = Modifier.offset(x = 20.dp)) { Avatar("JL", AvatarBlue, size = 32) }
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            Text(preview, color = TextBody, fontSize = 12.5.sp, maxLines = 1)
        }
        Text(time, color = TextMuted, fontSize = 11.sp)
    }
}
