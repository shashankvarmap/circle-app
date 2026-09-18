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
import com.circle.app.ui.components.Avatar
import com.circle.app.ui.theme.*

@Composable
fun EventScreen(nav: NavHostController) {
    var isJoined by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        PreviewAsStrip(listOf("Joined" to true, "Not joined" to false), isJoined) { isJoined = it }

        Row(modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = TextBody, modifier = Modifier.clickable { nav.popBackStack() })
            Spacer(Modifier.width(12.dp))
            Text("Event", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Board Game Night", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("FRIENDS OF FRIENDS", color = AvatarRose, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sep 18, 6:00 PM", color = TextBody, fontSize = 12.5.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Kaffeehaus Zeitlos, Aachen", color = TextBody, fontSize = 12.5.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { 0.5f },
                            modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(999.dp)),
                            color = AccentDefault, trackColor = Border
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("6 of 12 confirmed", color = TextMuted, fontSize = 11.5.sp)
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar("PK", AvatarSage, size = 28)
                    Spacer(Modifier.width(9.dp))
                    Text("Hosted by Priya Kapoor", color = TextBody, fontSize = 12.5.sp)
                }
            }
            item {
                Text(
                    "Monthly board game night — bringing Catan and Wingspan, but open to requests. Snacks provided, BYO drink.",
                    color = TextBody, fontSize = 13.sp
                )
            }
            item {
                if (isJoined) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).border(1.dp, Border, RoundedCornerShape(999.dp)).padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("✓ You're going", color = Success, fontWeight = FontWeight.Bold, fontSize = 12.5.sp) }
                            Spacer(Modifier.width(10.dp))
                            Text("Unjoin", color = TextMuted, fontSize = 12.sp)
                        }
                        Text("Unjoining locks the conversation back and quietly lets Priya know.", color = TextDim, fontSize = 11.sp)
                    }
                } else {
                    com.circle.app.ui.components.PrimaryButton("Join", modifier = Modifier.fillMaxWidth()) {}
                }
            }
            item {
                if (isJoined) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        com.circle.app.ui.components.SectionLabel("THREAD · 8 REPLIES")
                        ThreadReply("Jonah Lee", AvatarBlue, "JL", "2h", "Bringing Wingspan too if anyone wants a break from Catan.")
                        ThreadReply("Amara Osei", AvatarOrange, "AO", "1h", "Count me in, I'll bring the good snacks this time.")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar("S", AvatarSlate, size = 28)
                            Spacer(Modifier.width(9.dp))
                            OutlinedTextField(
                                value = "", onValueChange = {}, placeholder = { Text("Add a reply…", fontSize = 12.5.sp) },
                                modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(999.dp),
                                trailingIcon = { Icon(Icons.Outlined.Send, contentDescription = "Send", tint = AccentDefault) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Surface, unfocusedContainerColor = Surface,
                                    focusedIndicatorColor = Border, unfocusedIndicatorColor = Border,
                                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                                )
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LockedPanel("The conversation unlocks once you join.", "8 replies so far")
                        Text(
                            "Only the people actually coming can see or post here — not a public comment section.",
                            color = TextDim, fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadReply(name: String, color: androidx.compose.ui.graphics.Color, initials: String, time: String, text: String) {
    Row {
        Avatar(initials, color, size = 30)
        Spacer(Modifier.width(10.dp))
        Column {
            Row {
                Text(name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text("· $time", color = TextMuted, fontSize = 11.5.sp)
            }
            Text(text, color = TextBody, fontSize = 13.sp)
        }
    }
}
