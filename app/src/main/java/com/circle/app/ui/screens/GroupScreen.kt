package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.circle.app.ui.components.*
import com.circle.app.ui.theme.*

/**
 * Group detail. `isMember` here is a demo-only toggle in the top strip so you
 * can preview both the member view and the outsider (locked) view without
 * needing a second account — see chat history, this mirrors the HTML mockup.
 */
@Composable
fun GroupScreen(nav: NavHostController) {
    var isMember by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        PreviewAsStrip(listOf("Member" to true, "Not joined" to false), isMember) { isMember = it }

        Row(modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = TextBody, modifier = Modifier.clickable { nav.popBackStack() })
            Spacer(Modifier.width(12.dp))
            Text("Group", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GroupTile("A", AvatarOrange, size = 64)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Aachen Photo Walk", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Photography · Aachen", color = TextMuted, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("PUBLIC", color = Success, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Spacer(Modifier.width(7.dp))
                            Text("128 members", color = TextMuted, fontSize = 11.5.sp)
                        }
                    }
                }
            }
            item {
                Text(
                    "Weekend photo walks around Aachen — the cathedral, the Elisenbrunnen, wherever the light looks good. All levels welcome.",
                    color = TextBody, fontSize = 13.sp
                )
            }
            item {
                if (isMember) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).border(1.dp, Border, RoundedCornerShape(999.dp)).padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("✓ Member", color = Success, fontWeight = FontWeight.Bold, fontSize = 12.5.sp) }
                        Spacer(Modifier.width(10.dp))
                        Text("Leave", color = TextMuted, fontSize = 12.sp)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton("Request to join", modifier = Modifier.fillMaxWidth()) {}
                        Text(
                            "Groups default to private. This one is public — your request will be visible to every member.",
                            color = TextDim, fontSize = 11.sp
                        )
                    }
                }
            }
            if (isMember) {
                item { ComposeToGroupRow(nav) }
                item { GroupPost("Priya Kapoor", AvatarSage, "PK", "40m", "Golden hour shoot this Sunday — meet at the cathedral steps, 6:30 sharp.") }
                item { GroupPost("Tomas Novak", AvatarBlue, "TN", "3h", "Finally got the long-exposure shot of the fountain right.", liked = true) }
            } else {
                item { LockedPanel("Join to see what's being posted here.") }
            }
        }
    }
}

@Composable
fun PreviewAsStrip(options: List<Pair<String, Boolean>>, current: Boolean, onSelect: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color(0xFF0D0F13)).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text("PREVIEW AS", color = TextDim, fontWeight = FontWeight.Bold, fontSize = 9.sp, modifier = Modifier.padding(end = 8.dp, top = 2.dp))
        options.forEach { (label, value) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (current == value) AccentDefault else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(label, color = if (current == value) OnAccent else TextMuted, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
            Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
fun LockedPanel(text: String, footnote: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, BorderStrong, RoundedCornerShape(14.dp))
            .padding(vertical = 22.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextDim, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(8.dp))
        Text(text, color = TextBody, fontSize = 12.5.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        footnote?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = TextDim, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ComposeToGroupRow(nav: NavHostController) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface)
            .clickable { nav.navigate(com.circle.app.navigation.Routes.COMPOSE_POST) }.padding(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar("S", AvatarSlate, size = 28)
        Spacer(Modifier.width(10.dp))
        Text("Post to the group…", color = TextMuted, fontSize = 13.sp)
    }
}

@Composable
private fun GroupPost(name: String, color: androidx.compose.ui.graphics.Color, initials: String, time: String, text: String, liked: Boolean = false) {
    CircleCard {
        Row {
            Avatar(initials, color, size = 34)
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
        // Group posts: comment + like only — no repost/quote, they never leave the group
        Row(modifier = Modifier.padding(start = 44.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Reply", tint = TextMuted, modifier = Modifier.size(16.dp))
            Icon(
                Icons.Outlined.FavoriteBorder, contentDescription = "Like",
                tint = if (liked) AccentDefault else TextMuted, modifier = Modifier.size(16.dp)
            )
        }
    }
}
