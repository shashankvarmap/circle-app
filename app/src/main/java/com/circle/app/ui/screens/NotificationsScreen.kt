package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.data.CircleStore
import com.circle.app.data.Person
import com.circle.app.data.RequestStatus
import com.circle.app.navigation.openPerson
import com.circle.app.ui.components.*
import com.circle.app.ui.theme.*

private enum class NTab { NOTIFICATIONS, ACTIVITY }

@Composable
fun NotificationsScreen(nav: NavHostController) {
    var tab by rememberSaveable { mutableStateOf(NTab.NOTIFICATIONS) }
    val listState = rememberLazyListState()
    val header = rememberCollapsingHeaderState { listState.canScrollForward || listState.canScrollBackward }

    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
        CollapsingHeaderLayout(
            state = header,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            header = {
                TabbedScreenHeader(
                    title = "Alerts",
                    tabs = listOf("Notifications", "Activity"),
                    selected = tab.ordinal,
                    onSelect = { tab = NTab.entries[it] }
                )
            }
        ) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (tab == NTab.NOTIFICATIONS) {
                    item { SectionLabel("FRIEND REQUESTS") }
                    val requests = CircleStore.visibleRequests
                    if (requests.isEmpty()) {
                        item { Text("No friend requests right now.", color = CircleTheme.colors.muted, fontSize = 12.5.sp, modifier = Modifier.padding(vertical = 8.dp)) }
                    }
                    items(requests, key = { it.personId }) { request ->
                        val person = CircleStore.person(request.personId) ?: return@items
                        RequestRow(
                            person = person,
                            onOpen = { nav.openPerson(person.id) },
                            mutualText = request.mutualText,
                            accepted = CircleStore.statusOfRequest(request.personId) == RequestStatus.ACCEPTED,
                            onAccept = { CircleStore.acceptRequest(request.personId) },
                            onDecline = { CircleStore.declineRequest(request.personId) }
                        )
                    }
                    item { Spacer(Modifier.height(4.dp)); SectionLabel("RECENT") }
                    item { ActivityRow("Priya Kapoor", "liked your post", "PK", AvatarSage, "10m") { nav.openPerson("PK") } }
                    item { ActivityRow("Jonah Lee", "commented: \"Where was this?\"", "JL", AvatarBlue, "25m") { nav.openPerson("JL") } }
                    item { ActivityRow("Amara Osei", "reposted your thought", "AO", AvatarOrange, "1h") { nav.openPerson("AO") } }
                    item { ActivityRow("Tomas Novak", "quoted your post: \"so real\"", "TN", AvatarSlate, "3h") { nav.openPerson("TN") } }
                } else {
                    item { ActivityRow("Priya", "liked Jonah's post", "PK", AvatarSage, "20m") { nav.openPerson("PK") } }
                    item { ActivityRow("Amara", "commented on Tomas's thought: \"ha, same\"", "AO", AvatarOrange, "45m") { nav.openPerson("AO") } }
                    item { ActivityRow("Jonah", "reposted a thought from Riya Sharma", "JL", AvatarBlue, "2h") { nav.openPerson("JL") } }
                    item { ActivityRow("Marco and Priya", "are now friends", "MD", AvatarTeal, "5h") { nav.openPerson("MD") } }
                }
            }
        }
        CircleBottomNav(nav, NavTab.NOTIFICATIONS)
    }
}

@Composable
private fun RequestRow(person: Person, mutualText: String, accepted: Boolean, onOpen: () -> Unit, onAccept: () -> Unit, onDecline: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable(onClickLabel = "Open ${person.name}'s profile", onClick = onOpen)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Avatar(person.initials, person.color, size = 40)
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(person.name, color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            Text(if (accepted) "Now friends" else mutualText, color = CircleTheme.colors.muted, fontSize = 11.5.sp)
        }
        }
        if (accepted) {
            Text("✓ Friends", color = CircleTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        } else {
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
}

@Composable
private fun ActivityRow(name: String, action: String, initials: String, color: androidx.compose.ui.graphics.Color, time: String, onOpen: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .clickable(onClickLabel = "Open $name's profile", onClick = onOpen).padding(vertical = 10.dp)
    ) {
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
