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
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.data.CircleStore
import com.circle.app.ui.components.Avatar
import com.circle.app.ui.components.ReplyComposer
import com.circle.app.ui.components.ThreadComment
import com.circle.app.ui.theme.*

@Composable
fun EventScreen(nav: NavHostController, eventId: String?) {
    val event = CircleStore.event(eventId)
    // Same state for the demo "Preview as" strip and the real Join / Unjoin actions.
    val isJoined = event.joined

    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
        PreviewAsStrip(listOf("Joined" to true, "Not joined" to false), isJoined) { CircleStore.setJoined(event, it) }

        Row(modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = CircleTheme.colors.body, modifier = Modifier.clickable { nav.popBackStack() })
            Spacer(Modifier.width(12.dp))
            Text("Event", color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(event.title, color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                        Text(event.visibility, color = CircleTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = CircleTheme.colors.muted, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(event.whenLabel, color = CircleTheme.colors.body, fontSize = 12.5.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = CircleTheme.colors.muted, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(event.venue, color = CircleTheme.colors.body, fontSize = 12.5.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { event.going.toFloat() / event.capacity },
                            modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(999.dp)),
                            color = CircleTheme.colors.accent, trackColor = CircleTheme.colors.border
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("${event.going} of ${event.capacity} confirmed", color = CircleTheme.colors.muted, fontSize = 11.5.sp)
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(event.hostInitials, event.hostColor, size = 28)
                    Spacer(Modifier.width(9.dp))
                    Text("Hosted by ${event.hostName}", color = CircleTheme.colors.body, fontSize = 12.5.sp)
                }
            }
            item {
                if (event.description.isNotEmpty()) Text(event.description, color = CircleTheme.colors.body, fontSize = 13.sp)
            }
            item {
                if (isJoined) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).border(1.dp, CircleTheme.colors.border, RoundedCornerShape(999.dp)).padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("✓ You're going", color = CircleTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.5.sp) }
                            Spacer(Modifier.width(10.dp))
                            Text("Unjoin", color = CircleTheme.colors.muted, fontSize = 12.sp, modifier = Modifier.clickable { CircleStore.setJoined(event, false) })
                        }
                        Text("Unjoining locks the conversation back and quietly lets ${event.hostName.substringBefore(" ")} know.", color = CircleTheme.colors.muted, fontSize = 11.sp)
                    }
                } else {
                    com.circle.app.ui.components.PrimaryButton("Join", modifier = Modifier.fillMaxWidth()) { CircleStore.setJoined(event, true) }
                }
            }
            item {
                if (isJoined) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val n = event.comments.size
                        com.circle.app.ui.components.SectionLabel(if (n == 0) "THREAD · NO REPLIES YET" else "THREAD · $n ${if (n == 1) "REPLY" else "REPLIES"}")
                        event.comments.forEach { ThreadComment(it) }
                        ReplyComposer { text -> event.comments.add(CircleStore.you("now", text)) }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LockedPanel("The conversation unlocks once you join.", "${event.comments.size} replies so far")
                        Text(
                            "Only the people actually coming can see or post here — not a public comment section.",
                            color = CircleTheme.colors.muted, fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
