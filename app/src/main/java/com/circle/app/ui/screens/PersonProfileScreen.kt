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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.circle.app.R
import com.circle.app.data.access.Relationship
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.Outcome
import com.circle.app.data.repo.ProfileView
import com.circle.app.data.repo.isSuccess
import com.circle.app.navigation.Routes
import com.circle.app.ui.components.Avatar
import com.circle.app.ui.components.CircleCard
import com.circle.app.ui.components.FeedCard
import com.circle.app.ui.components.SectionLabel
import com.circle.app.ui.theme.*
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel

class PersonProfileViewModel(repo: CircleRepository, private val personId: String) : RepoViewModel(repo) {
    /** Null while loading. */
    val profile = repo.profile(personId).state(null)
    val relationship = repo.relationshipTo(personId).state(Relationship.NONE)
    /** Empty for anyone who isn't you or your friend: the repository returns nothing else. */
    val notes = repo.notesBy(personId).state(emptyList())

    fun sendRequest(then: (Outcome<Unit>) -> Unit) = run({ sendFriendRequest(personId) }, then)
    fun cancelRequest(then: (Outcome<Unit>) -> Unit) = run({ cancelFriendRequest(personId) }, then)
    fun accept(then: (Outcome<Unit>) -> Unit) = run({ acceptRequest(personId) }, then)
    fun decline(then: (Outcome<Unit>) -> Unit) = run({ declineRequest(personId) }, then)
    fun unfriend(then: (Outcome<Unit>) -> Unit) = run({ unfriend(personId) }, then)
    fun block(then: (Outcome<Unit>) -> Unit) = run({ block(personId) }, then)
    fun mute(then: (Outcome<Unit>) -> Unit) = run({ mute(personId) }, then)
    fun unmute(then: (Outcome<Unit>) -> Unit) = run({ unmute(personId) }, then)

    fun message(then: (Outcome<String>) -> Unit) = run({ openDirectChat(personId) }, then)
}

/**
 * Someone else's profile, rendered from [ProfileView]. Full (a friend): Message, ✓ Friends, their
 * notes, and Mute / Unfriend / Block / Report. Limited (not friends): photo, name, handle and
 * pinned thought only, Add friend / Requested / Accept, and Report / Block. NotFound: nothing.
 */
@Composable
fun PersonProfileScreen(nav: NavHostController, personId: String) {
    val colors = CircleTheme.colors
    val context = LocalContext.current
    val vm = circleViewModel(key = "person-$personId") { PersonProfileViewModel(it, personId) }
    val profile by vm.profile.collectAsStateWithLifecycle()
    val rel by vm.relationship.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()

    var menuOpen by remember { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf<String?>(null) } // "unfriend" | "block" | "report"
    fun toast(res: Int) = Toast.makeText(context, context.getString(res), Toast.LENGTH_SHORT).show()

    val view = profile
    if (view == null) {
        Column(Modifier.fillMaxSize().background(colors.background)) { ProfileTopBar(nav, null, false, {}, {}) }
        return
    }
    if (view is ProfileView.NotFound) {
        Column(Modifier.fillMaxSize().background(colors.background)) {
            DetailHeader(stringResource(R.string.profile_title)) { nav.popBackStack() }
            Text(stringResource(R.string.profile_unavailable), color = colors.muted, fontSize = 14.sp, modifier = Modifier.padding(24.dp))
        }
        return
    }
    if (view is ProfileView.Full && view.relationship == Relationship.SELF) {
        LaunchedEffect(Unit) { nav.popBackStack(); nav.navigate(Routes.PROFILE) }
        return
    }

    val name = when (view) { is ProfileView.Full -> view.person.name; is ProfileView.Limited -> view.name; else -> "" }
    val first = name.substringBefore(" ")
    val isMuted = (view as? ProfileView.Full)?.isMuted == true

    if (confirm == "report") {
        com.circle.app.ui.components.ReportSheet(com.circle.app.data.ReportKind.PERSON, personId, onDone = { confirm = null })
    }
    confirm?.takeIf { it != "report" }?.let { kind ->
        val (title, body, action) = when (kind) {
            "unfriend" -> Triple(stringResource(R.string.profile_unfriend_title, first), stringResource(R.string.profile_unfriend_body, first), stringResource(R.string.profile_unfriend))
            "block" -> Triple(stringResource(R.string.profile_block_title, first), stringResource(R.string.profile_block_body), stringResource(R.string.profile_block))
            else -> Triple(stringResource(R.string.profile_report_title, first), stringResource(R.string.profile_report_body, first), stringResource(R.string.profile_report))
        }
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(title) },
            text = { Text(body) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    when (kind) {
                        "unfriend" -> vm.unfriend { if (it.isSuccess) toast(R.string.toast_unfriended) }
                        else -> vm.block { if (it.isSuccess) { toast(R.string.toast_blocked); nav.popBackStack() } }
                    }
                }) { Text(action, color = if (kind == "report") colors.accent else colors.alertDot) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.cancel), color = colors.text) } }
        )
    }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        ProfileTopBar(nav, name, menuOpen, { menuOpen = it }) {
            if (rel == Relationship.FRIEND) {
                DropdownMenuItem(text = { Text(stringResource(if (isMuted) R.string.profile_unmute else R.string.profile_mute)) }, onClick = {
                    menuOpen = false
                    if (isMuted) vm.unmute { if (it.isSuccess) toast(R.string.toast_unmuted) }
                    else vm.mute { if (it.isSuccess) toast(R.string.toast_muted) }
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.profile_unfriend)) }, onClick = { menuOpen = false; confirm = "unfriend" })
            }
            DropdownMenuItem(text = { Text(stringResource(R.string.profile_report)) }, onClick = { menuOpen = false; confirm = "report" })
            DropdownMenuItem(text = { Text(stringResource(R.string.profile_block)) }, onClick = { menuOpen = false; confirm = "block" })
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            when (view) {
                is ProfileView.Full -> {
                    item { ProfileHead(view.person.initials, view.person.color, view.person.name, view.handle) }
                    item { Spacer(Modifier.height(16.dp)); PinnedCard(view.pinnedThought) }
                    item {
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Pill(stringResource(R.string.profile_message), filled = true, modifier = Modifier.weight(1f)) {
                                vm.message { if (it is Outcome.Success) nav.navigate(Routes.chat(it.value)) }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.profile_friends_check), color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        }
                    }
                    item { Spacer(Modifier.height(18.dp)); SectionLabel(stringResource(R.string.profile_notes_caps)); Spacer(Modifier.height(10.dp)) }
                    if (notes.isEmpty()) item { Text(stringResource(R.string.profile_no_notes, first), color = colors.muted, fontSize = 13.sp) }
                    items(notes, key = { it.id }) { p -> Box(Modifier.padding(bottom = 12.dp)) { FeedCard(nav, p) } }
                }
                is ProfileView.Limited -> {
                    item { ProfileHead(view.initials, view.color, view.name, view.handle) }
                    item { Spacer(Modifier.height(16.dp)); PinnedCard(view.pinnedThought) }
                    item {
                        Spacer(Modifier.height(14.dp))
                        LimitedActions(rel,
                            onAdd = { vm.sendRequest { if (it.isSuccess) toast(R.string.toast_request_sent) } },
                            onCancel = { vm.cancelRequest { if (it.isSuccess) toast(R.string.toast_request_cancelled) } },
                            onAccept = { vm.accept { if (it.isSuccess) toast(R.string.toast_now_friends) } },
                            onDecline = { vm.decline { if (it.isSuccess) toast(R.string.toast_request_declined) } },
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)); LockedNotFriendsPanel() }
                }
                ProfileView.NotFound -> Unit
            }
        }
    }
}

@Composable
private fun ProfileTopBar(
    nav: NavHostController,
    name: String?,
    menuOpen: Boolean,
    setMenu: (Boolean) -> Unit,
    menu: @Composable ColumnScope.() -> Unit,
) {
    val colors = CircleTheme.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.popBackStack() }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.text)
        }
        Spacer(Modifier.weight(1f))
        if (name != null) Box {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { setMenu(true) }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.profile_more_options, name), tint = colors.text)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { setMenu(false) }, content = menu)
        }
    }
}

@Composable
private fun ProfileHead(initials: String, color: Color, name: String, handle: String) {
    val colors = CircleTheme.colors
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Avatar(initials, color, size = 92)
        Text(
            name, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, letterSpacing = (-0.39).sp,
            color = colors.text, modifier = Modifier.padding(top = 12.dp).semantics { heading() }
        )
        Text(stringResource(R.string.handle_format, handle), color = colors.muted, fontSize = 13.sp)
    }
}

@Composable
private fun PinnedCard(pinned: String) {
    val colors = CircleTheme.colors
    CircleCard {
        Text(stringResource(R.string.profile_pinned_caps), color = colors.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Text(
            pinned.ifBlank { stringResource(R.string.profile_nothing_pinned) },
            color = if (pinned.isBlank()) colors.muted else colors.body, style = CircleTheme.postText
        )
    }
}

/** Add friend / Requested + Cancel request / Accept + Decline. Never a Message button. */
@Composable
private fun LimitedActions(rel: Relationship, onAdd: () -> Unit, onCancel: () -> Unit, onAccept: () -> Unit, onDecline: () -> Unit) {
    val colors = CircleTheme.colors
    when (rel) {
        Relationship.REQUEST_SENT -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Pill(stringResource(R.string.profile_requested), filled = false, enabled = false, modifier = Modifier.fillMaxWidth()) {}
            Text(
                stringResource(R.string.profile_cancel_request), color = colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.5.sp,
                modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onCancel)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            )
        }
        Relationship.REQUEST_RECEIVED -> Row {
            Pill(stringResource(R.string.profile_accept), filled = true, modifier = Modifier.weight(1f), onClick = onAccept)
            Spacer(Modifier.width(10.dp))
            Pill(stringResource(R.string.profile_decline), filled = false, modifier = Modifier.weight(1f), onClick = onDecline)
        }
        else -> Pill(stringResource(R.string.profile_add_friend), filled = true, modifier = Modifier.fillMaxWidth(), onClick = onAdd)
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
            stringResource(R.string.profile_locked_panel),
            color = colors.body, fontSize = 13.sp, lineHeight = 19.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp)
        )
    }
}
