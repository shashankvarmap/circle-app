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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.EventThreadView
import com.circle.app.ui.format.whenLabel
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import com.circle.app.navigation.openPerson
import com.circle.app.ui.components.Avatar
import com.circle.app.ui.components.ReplyComposer
import com.circle.app.ui.components.ThreadComment
import com.circle.app.ui.theme.*

class EventViewModel(repo: CircleRepository, private val eventId: String) : RepoViewModel(repo) {
    /** Null while loading or when the event isn't visible to you. */
    val event = repo.event(eventId).state(null)
    fun takeSeat() = run({ takeSeat(eventId) })
    fun leaveSeat() = run({ leaveSeat(eventId) })
    fun comment(text: String) = run({ addEventComment(eventId, text) })
}

@Composable
fun EventScreen(nav: NavHostController, eventId: String?) {
    val vm = circleViewModel(key = "event-$eventId") { EventViewModel(it, eventId.orEmpty()) }
    val loaded by vm.event.collectAsStateWithLifecycle()
    val view = loaded ?: run {
        Column(Modifier.fillMaxSize().background(CircleTheme.colors.background)) { DetailHeader("Event") { nav.popBackStack() } }
        return
    }
    val event = view.summary
    val host = view.host
    val isJoined = view.hasSeat
    val screenReader = com.circle.app.ui.a11y.rememberScreenReaderOn()
    var showMap by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(!screenReader) }

    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
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
                        Text(view.visibility.name.replace('_', ' '), color = CircleTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 10.sp)
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
                        Text("${event.going} of ${event.capacity} going", color = CircleTheme.colors.muted, fontSize = 11.5.sp)
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { com.circle.app.ui.components.SectionLabel("AT THE TABLE") }
                    com.circle.app.ui.orbit.MapListToggle(showMap, { showMap = it })
                }
            }
            item { EventTable(view, showMap) { nav.openPerson(it) } }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .clickable(onClickLabel = "Open ${host.name}'s profile") { nav.openPerson(host.id) }
                        .heightIn(min = 48.dp).padding(end = 8.dp)
                ) {
                    Avatar(host.initials, host.color, size = 28)
                    Spacer(Modifier.width(9.dp))
                    Text("Hosted by ${host.name} · ${view.seatsLeft} ${if (view.seatsLeft == 1) "seat" else "seats"} left", color = CircleTheme.colors.body, fontSize = 12.5.sp)
                }
            }
            item {
                if (view.description.isNotEmpty()) Text(view.description, color = CircleTheme.colors.body, fontSize = 13.sp)
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
                            if (!view.iAmHost) {
                                Text("Leave seat", color = CircleTheme.colors.muted, fontSize = 12.sp, modifier = Modifier.clickable { vm.leaveSeat() })
                            }
                        }
                        Text(if (view.iAmHost) "You're hosting this one." else "Leaving your seat locks the conversation again and quietly lets ${host.firstName} know.", color = CircleTheme.colors.muted, fontSize = 11.sp)
                    }
                } else {
                    if (view.isFull) {
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp)).border(1.dp, CircleTheme.colors.border, RoundedCornerShape(999.dp)).padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("Event full", color = CircleTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    } else {
                        com.circle.app.ui.components.PrimaryButton("Take a seat", modifier = Modifier.fillMaxWidth()) { vm.takeSeat() }
                    }
                }
            }
            item {
                when (val thread = view.thread) {
                    is EventThreadView.Open -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val n = thread.comments.size
                        com.circle.app.ui.components.SectionLabel(if (n == 0) "THREAD · NO REPLIES YET" else "THREAD · $n ${if (n == 1) "REPLY" else "REPLIES"}")
                        thread.comments.forEach { c -> ThreadComment(c, expanded = true, onToggleReplies = {}, onPerson = { id -> nav.openPerson(id) }, actions = null) }
                        ReplyComposer { text -> vm.comment(text) }
                    }
                    is EventThreadView.Locked -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LockedPanel("The conversation opens when you take a seat.", "${thread.replyCount} replies so far")
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
