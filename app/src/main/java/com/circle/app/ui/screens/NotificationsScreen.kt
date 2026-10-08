package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.data.CircleStore
import com.circle.app.data.Person
import com.circle.app.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.circle.app.ui.theme.*

private enum class NTab { NOTIFICATIONS, ACTIVITY }

@Composable
fun NotificationsScreen(nav: NavHostController) {
    var tab by remember { mutableStateOf(NTab.NOTIFICATIONS) }
    var confirmation by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
        Box(modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
            Text("Notifications", color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
        Box(modifier = Modifier.padding(top = 12.dp, start = 16.dp)) {
            UnderlineTabs(listOf("Notifications", "Activity"), tab.ordinal) { tab = NTab.entries[it] }
        }

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (tab == NTab.NOTIFICATIONS) {
                item { SectionLabel("FRIEND REQUESTS") }
                confirmation?.let { msg ->
                    item(key = "confirmation") {
                        Text(msg, color = CircleTheme.colors.accent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
                    }
                }
                if (CircleStore.incomingRequests.isEmpty()) {
                    item { Text("No pending requests", color = CircleTheme.colors.muted, fontSize = 12.5.sp, modifier = Modifier.padding(vertical = 8.dp)) }
                }
                items(CircleStore.incomingRequests, key = { it.name }) { person ->
                    RequestRow(
                        person,
                        onAccept = {
                            CircleStore.acceptRequest(person)
                            val msg = "✓ You and ${person.name.substringBefore(" ")} are now friends"
                            confirmation = msg
                            scope.launch {
                                delay(2500)
                                if (confirmation == msg) confirmation = null
                            }
                        },
                        onDecline = { CircleStore.declineRequest(person) }
                    )
                }
                item { Spacer(Modifier.height(4.dp)); SectionLabel("RECENT") }
                item { ActivityRow("Priya Kapoor", "liked your post", "PK", AvatarSage, "10m") }
                item { ActivityRow("Jonah Lee", "commented: \"Where was this?\"", "JL", AvatarBlue, "25m") }
                item { ActivityRow("Amara Osei", "reposted your thought", "AO", AvatarOrange, "1h") }
                item { ActivityRow("Tomas Novak", "quoted your post: \"so real\"", "TN", AvatarSlate, "3h") }
            } else {
                item { ActivityRow("Priya", "liked Jonah's post", "PK", AvatarSage, "20m") }
                item { ActivityRow("Amara", "commented on Tomas's thought: \"ha, same\"", "AO", AvatarOrange, "45m") }
                item { ActivityRow("Jonah", "reposted a thought from Riya Sharma", "JL", AvatarBlue, "2h") }
                item { ActivityRow("Marco and Priya", "are now friends", "MD", AvatarTeal, "5h") }
            }
        }
        CircleBottomNav(nav, NavTab.NOTIFICATIONS)
    }
}

@Composable
private fun RequestRow(person: Person, onAccept: () -> Unit, onDecline: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(person.initials, person.color, size = 40)
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(person.name, color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            Text(person.subtitle, color = CircleTheme.colors.muted, fontSize = 11.5.sp)
        }
        Box(
            modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(CircleTheme.colors.accent)
                .clickable(onClick = onAccept).padding(horizontal = 13.dp, vertical = 7.dp)
        ) {
            Text("Accept", color = CircleTheme.colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, CircleTheme.colors.border, RoundedCornerShape(999.dp))
                .clickable(onClick = onDecline).padding(horizontal = 13.dp, vertical = 7.dp)
        ) {
            Text("Decline", color = CircleTheme.colors.body, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ActivityRow(name: String, action: String, initials: String, color: androidx.compose.ui.graphics.Color, time: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Avatar(initials, color, size = 38)
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                buildAnnotatedString2(name, action),
                fontSize = 13.5.sp
            )
            Text(time, color = CircleTheme.colors.muted, fontSize = 11.5.sp)
        }
    }
}

@Composable
private fun buildAnnotatedString2(name: String, action: String) = androidx.compose.ui.text.buildAnnotatedString {
    withStyle(androidx.compose.ui.text.SpanStyle(color = CircleTheme.colors.text, fontWeight = FontWeight.Bold)) { append(name) }
    withStyle(androidx.compose.ui.text.SpanStyle(color = CircleTheme.colors.body)) { append(" $action") }
}
