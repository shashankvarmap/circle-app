package com.circle.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.data.CircleStore
import com.circle.app.data.Person
import com.circle.app.data.Relationship
import com.circle.app.navigation.Routes
import com.circle.app.ui.components.Avatar
import com.circle.app.ui.components.CircleCard
import com.circle.app.ui.components.FeedCard
import com.circle.app.ui.theme.*

/**
 * Someone else's profile, rendered by relationship. Everyone sees the photo, name, handle,
 * mutual-friend count (a number only) and pinned thought. Notes, friends, counts and the Message
 * button only exist between friends.
 */
@Composable
fun PersonProfileScreen(nav: NavHostController, personId: String) {
    val colors = CircleTheme.colors
    val context = LocalContext.current
    val person = CircleStore.person(personId)
    val rel = CircleStore.relationshipTo(personId)

    if (person == null || rel == Relationship.BLOCKED || rel == Relationship.SELF) {
        LaunchedEffect(rel) { if (rel == Relationship.SELF) nav.popBackStack() }
        Column(Modifier.fillMaxSize().background(colors.background)) {
            DetailHeader("Profile") { nav.popBackStack() }
            Text("This profile isn't available.", color = colors.muted, fontSize = 14.sp, modifier = Modifier.padding(24.dp))
        }
        return
    }

    var menuOpen by remember { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf<String?>(null) } // "unfriend" | "block" | "report"
    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    confirm?.let { kind ->
        val (title, body, action) = when (kind) {
            "unfriend" -> Triple(
                "Unfriend ${person.firstName}?",
                "${person.firstName} leaves your circle and your friend count. They aren't told, but you'll need to send a new request to be friends again.",
                "Unfriend"
            )
            "block" -> Triple(
                "Block ${person.firstName}?",
                "They won't be able to see you or reach you, and they disappear from your feeds, search, groups, chats and event seats. Any friendship or request ends.",
                "Block"
            )
            else -> Triple(
                "Report ${person.firstName}?",
                "We'll look at this profile. ${person.firstName} won't know who reported them.",
                "Report"
            )
        }
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(title) },
            text = { Text(body) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    when (kind) {
                        "unfriend" -> { CircleStore.unfriend(person.id); toast("Unfriended") }
                        "block" -> { CircleStore.block(person.id); toast("Blocked"); nav.popBackStack() }
                        else -> { CircleStore.report("person", person.id); toast("Thanks. We'll look at this.") }
                    }
                }) { Text(action, color = if (kind == "report") colors.accent else colors.alertDot) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel", color = colors.text) } }
        )
    }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        // Header: back, and the ⋯ menu (Mute / Unfriend / Block / Report for friends; Report / Block otherwise)
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.popBackStack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.text)
            }
            Spacer(Modifier.weight(1f))
            Box {
                Box(Modifier.size(44.dp).clip(CircleShape).clickable { menuOpen = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "More options for ${person.name}", tint = colors.text)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (rel == Relationship.FRIEND) {
                        val muted = person.id in CircleStore.mutedIds
                        DropdownMenuItem(text = { Text(if (muted) "Unmute" else "Mute") }, onClick = {
                            menuOpen = false
                            if (muted) { CircleStore.unmute(person.id); toast("Unmuted") }
                            else { CircleStore.mute(person.id); toast("Muted. ${person.firstName} isn't told.") }
                        })
                        DropdownMenuItem(text = { Text("Unfriend") }, onClick = { menuOpen = false; confirm = "unfriend" })
                    }
                    DropdownMenuItem(text = { Text("Report") }, onClick = { menuOpen = false; confirm = "report" })
                    DropdownMenuItem(text = { Text("Block") }, onClick = { menuOpen = false; confirm = "block" })
                }
            }
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            item { ProfileHead(person) }
            item { Spacer(Modifier.height(16.dp)); PinnedCard(person) }
            item { Spacer(Modifier.height(14.dp)); RelationshipActions(nav, person, rel, ::toast) }

            if (rel == Relationship.FRIEND) {
                val notes = CircleStore.posts.filter { it.authorId == person.id && it.groupId == null && CircleStore.canOpenPost(it) }
                item { Spacer(Modifier.height(18.dp)); com.circle.app.ui.components.SectionLabel("NOTES") ; Spacer(Modifier.height(10.dp)) }
                if (notes.isEmpty()) item { Text("${person.firstName} hasn't posted yet.", color = colors.muted, fontSize = 13.sp) }
                items(notes, key = { it.id }) { p -> Box(Modifier.padding(bottom = 12.dp)) { FeedCard(nav, p) } }
            } else {
                item { Spacer(Modifier.height(16.dp)); LockedNotFriendsPanel() }
            }
        }
    }
}

@Composable
private fun ProfileHead(person: Person) {
    val colors = CircleTheme.colors
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Avatar(person.initials, person.color, size = 92)
        Text(
            person.name, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, letterSpacing = (-0.39).sp,
            color = colors.text, modifier = Modifier.padding(top = 12.dp).semantics { heading() }
        )
        Text("@${person.handle}", color = colors.muted, fontSize = 13.sp)
        val n = person.mutualFriends
        Text(
            if (n == 1) "1 mutual friend" else "$n mutual friends",
            color = colors.muted, fontSize = 12.5.sp, modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun PinnedCard(person: Person) {
    val colors = CircleTheme.colors
    CircleCard {
        Text("PINNED", color = colors.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Text(
            person.pinned.ifBlank { "Nothing pinned yet." },
            color = if (person.pinned.isBlank()) colors.muted else colors.body, style = CircleTheme.postText
        )
    }
}

@Composable
private fun RelationshipActions(nav: NavHostController, person: Person, rel: Relationship, toast: (String) -> Unit) {
    val colors = CircleTheme.colors
    when (rel) {
        Relationship.FRIEND -> Row(verticalAlignment = Alignment.CenterVertically) {
            Pill("Message", filled = true, modifier = Modifier.weight(1f)) {
                CircleStore.openDirectChat(person.id)?.let { nav.navigate(Routes.chat(it.id)) }
            }
            Spacer(Modifier.width(10.dp))
            Text("✓ Friends", color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp))
        }
        Relationship.NONE -> Pill("Add friend", filled = true, modifier = Modifier.fillMaxWidth()) {
            if (CircleStore.sendFriendRequest(person.id)) toast("Friend request sent")
        }
        Relationship.REQUEST_SENT -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Pill("Requested", filled = false, enabled = false, modifier = Modifier.fillMaxWidth()) {}
            Text(
                "Cancel request", color = colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.5.sp,
                modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(8.dp))
                    .clickable { CircleStore.cancelFriendRequest(person.id); toast("Request cancelled") }
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            )
        }
        Relationship.REQUEST_RECEIVED -> Row {
            Pill("Accept", filled = true, modifier = Modifier.weight(1f)) { CircleStore.acceptRequest(person.id); toast("You're friends now") }
            Spacer(Modifier.width(10.dp))
            Pill("Decline", filled = false, modifier = Modifier.weight(1f)) { CircleStore.declineRequest(person.id); toast("Request declined") }
        }
        Relationship.SELF, Relationship.BLOCKED -> Unit
    }
}

@Composable
private fun Pill(text: String, filled: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = CircleTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(if (filled) colors.accent else colors.background)
            .border(1.5.dp, if (filled) colors.accent else colors.border, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = if (filled) colors.onAccent else colors.text, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
}

/** The dashed panel non-friends see in place of notes, friends and counts. */
@Composable
fun LockedNotFriendsPanel() {
    val colors = CircleTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    colors.border, cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())))
                )
            }
            .padding(horizontal = 18.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.muted, modifier = Modifier.size(22.dp))
        Text(
            "You're not friends yet. You'll see their notes and friends once they accept. Until then you see only their photo and pinned thought.",
            color = colors.body, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp)
        )
    }
}
