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
import com.circle.app.R
import com.circle.app.data.repo.Outcome
import com.circle.app.ui.components.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

class EventViewModel(repo: CircleRepository, private val eventId: String) : RepoViewModel(repo) {
    /** Null while loading or when the event isn't visible to you. */
    val event = repo.event(eventId).state(null)
    fun takeSeat() = run({ takeSeat(eventId) })
    fun leaveSeat() = run({ leaveSeat(eventId) })
    fun comment(text: String, replyTo: String?) = run({ addEventComment(eventId, text, replyTo) })
    fun likeComment(id: String) = run({ toggleEventCommentLike(eventId, id) })
    fun deleteComment(id: String) = run({ deleteEventComment(eventId, id) })
    fun invite(ids: List<String>, then: (Outcome<Int>) -> Unit) = run({ inviteToEvent(eventId, ids) }, then)
    fun edit(title: String, venue: String, description: String) = run({ editEvent(eventId, title, venue, description) })
    fun cancel(then: () -> Unit) = run({ cancelEvent(eventId) }) { if (it is Outcome.Success) then() }
    fun blockHost(hostId: String, then: () -> Unit) = run({ block(hostId) }) { if (it is Outcome.Success) then() }
}

private enum class EventDialog { EDIT, CANCEL, INVITE, REPORT, BLOCK }

@Composable
fun EventScreen(nav: NavHostController, eventId: String?) {
    val vm = circleViewModel(key = "event-$eventId") { EventViewModel(it, eventId.orEmpty()) }
    val loaded by vm.event.collectAsStateWithLifecycle()
    val view = loaded ?: run {
        Column(Modifier.fillMaxSize().background(CircleTheme.colors.background)) { DetailHeader(androidx.compose.ui.res.stringResource(com.circle.app.R.string.event_title)) { nav.popBackStack() } }
        return
    }
    val event = view.summary
    val host = view.host
    val isJoined = view.hasSeat
    val screenReader = com.circle.app.ui.a11y.rememberScreenReaderOn()
    val fits = com.circle.app.ui.orbit.rememberOrbitFits()
    var showMapChoice by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(!screenReader) }
    val showMap = showMapChoice && fits
    var dialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<EventDialog?>(null) }
    var menu by remember { mutableStateOf(false) }
    var replyTo by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var expanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(setOf<String>()) }
    var confirmDelete by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var reportingComment by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    reportingComment?.let { ReportSheet(com.circle.app.data.ReportKind.COMMENT, it, onDone = { reportingComment = null }) }
    confirmDelete?.let { id ->
        ConfirmDialog(stringResource(R.string.comment_delete_title), "", stringResource(R.string.comment_delete), destructive = true, onDismiss = { confirmDelete = null }) {
            vm.deleteComment(id)
        }
    }
    when (dialog) {
        EventDialog.EDIT -> {
            val title = remember { mutableStateOf(event.title) }
            val venue = remember { mutableStateOf(event.venue) }
            val description = remember { mutableStateOf(view.description) }
            EditFieldsDialog(
                stringResource(R.string.event_edit),
                listOf(stringResource(R.string.event_field_title) to title, stringResource(R.string.event_field_venue) to venue, stringResource(R.string.event_field_description) to description),
                onDismiss = { dialog = null }
            ) { vm.edit(title.value, venue.value, description.value) }
        }
        EventDialog.CANCEL -> ConfirmDialog(
            stringResource(R.string.event_cancel_title), stringResource(R.string.event_cancel_body), stringResource(R.string.event_cancel),
            destructive = true, onDismiss = { dialog = null }
        ) { vm.cancel { nav.popBackStack() } }
        EventDialog.INVITE -> FriendPickerSheet(stringResource(R.string.group_invite), stringResource(R.string.group_invite_send), onDismiss = { dialog = null }) { ids ->
            vm.invite(ids) { if (it is Outcome.Success) android.widget.Toast.makeText(context, context.resources.getQuantityString(R.plurals.invites_sent, it.value, it.value), android.widget.Toast.LENGTH_SHORT).show() }
        }
        EventDialog.REPORT -> ReportSheet(com.circle.app.data.ReportKind.EVENT, event.id, onDone = { dialog = null })
        EventDialog.BLOCK -> ConfirmDialog(
            stringResource(R.string.profile_block_title, host.firstName), stringResource(R.string.profile_block_body), stringResource(R.string.profile_block),
            destructive = true, onDismiss = { dialog = null }
        ) { vm.blockHost(host.id) { nav.popBackStack() } }
        null -> Unit
    }

    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background).imePadding()) {
        Row(modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.popBackStack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back), tint = CircleTheme.colors.body)
            }
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.event_title), color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Box {
                Box(Modifier.size(44.dp).clip(CircleShape).clickable { menu = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.event_options), tint = CircleTheme.colors.text)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (view.iAmHost) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.event_edit)) }, onClick = { menu = false; dialog = EventDialog.EDIT })
                        DropdownMenuItem(text = { Text(stringResource(R.string.event_cancel)) }, onClick = { menu = false; dialog = EventDialog.CANCEL })
                    }
                    DropdownMenuItem(text = { Text(stringResource(R.string.group_invite)) }, onClick = { menu = false; dialog = EventDialog.INVITE })
                    if (!view.iAmHost) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.event_report)) }, onClick = { menu = false; dialog = EventDialog.REPORT })
                        DropdownMenuItem(text = { Text(stringResource(R.string.event_block_host)) }, onClick = { menu = false; dialog = EventDialog.BLOCK })
                    }
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(
                            when (view.visibility) {
                                com.circle.app.data.access.EventVisibility.FRIENDS -> R.string.visibility_friends
                                com.circle.app.data.access.EventVisibility.FRIENDS_OF_FRIENDS -> R.string.visibility_fof
                                com.circle.app.data.access.EventVisibility.OPEN_NEARBY -> R.string.visibility_open
                            }
                        ).uppercase(),
                        color = CircleTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp
                    )
                    Text(event.title, color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                        Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.event_going_of, event.going, event.capacity), color = CircleTheme.colors.muted, fontSize = 11.5.sp)
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { com.circle.app.ui.components.SectionLabel(androidx.compose.ui.res.stringResource(com.circle.app.R.string.event_at_the_table)) }
                    if (fits) com.circle.app.ui.orbit.MapListToggle(showMap, { showMapChoice = it })
                }
            }
            item { EventTable(view, showMap) { nav.openPerson(it) } }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .clickable(onClickLabel = androidx.compose.ui.res.stringResource(com.circle.app.R.string.open_profile_of, host.name)) { nav.openPerson(host.id) }
                        .heightIn(min = 48.dp).padding(end = 8.dp)
                ) {
                    Avatar(host.initials, host.color, size = 28)
                    Spacer(Modifier.width(9.dp))
                    Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.event_hosted_by, host.name, androidx.compose.ui.res.pluralStringResource(com.circle.app.R.plurals.event_seats_left, view.seatsLeft, view.seatsLeft)), color = CircleTheme.colors.body, fontSize = 12.5.sp)
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
                            ) { Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.event_you_re_going), color = CircleTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.5.sp) }
                            Spacer(Modifier.width(10.dp))
                            if (!view.iAmHost) {
                                Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.event_leave_seat), color = CircleTheme.colors.muted, fontSize = 12.sp, modifier = Modifier.clickable { vm.leaveSeat() })
                            }
                        }
                        Text(if (view.iAmHost) androidx.compose.ui.res.stringResource(com.circle.app.R.string.event_hosting) else androidx.compose.ui.res.stringResource(com.circle.app.R.string.event_leave_note, host.firstName), color = CircleTheme.colors.muted, fontSize = 11.sp)
                    }
                } else {
                    if (view.isFull) {
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp)).border(1.dp, CircleTheme.colors.border, RoundedCornerShape(999.dp)).padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.error_event_full), color = CircleTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    } else {
                        com.circle.app.ui.components.PrimaryButton(androidx.compose.ui.res.stringResource(com.circle.app.R.string.event_take_a_seat), modifier = Modifier.fillMaxWidth()) { vm.takeSeat() }
                    }
                }
            }
            item {
                when (val thread = view.thread) {
                    is EventThreadView.Open -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val n = thread.comments.size
                        com.circle.app.ui.components.SectionLabel(if (n == 0) androidx.compose.ui.res.stringResource(com.circle.app.R.string.event_thread_empty_caps) else androidx.compose.ui.res.stringResource(com.circle.app.R.string.event_thread_caps, androidx.compose.ui.res.pluralStringResource(com.circle.app.R.plurals.thread_replies_caps, n, n)))
                        val actions = object : CommentActions {
                            override fun like(comment: com.circle.app.data.repo.CommentView) = vm.likeComment(comment.id)
                            override fun reply(comment: com.circle.app.data.repo.CommentView) { replyTo = comment.id }
                            override fun delete(comment: com.circle.app.data.repo.CommentView) { confirmDelete = comment.id }
                            override fun report(comment: com.circle.app.data.repo.CommentView) { reportingComment = comment.id }
                        }
                        thread.comments.forEach { c ->
                            ThreadComment(
                                c, expanded = c.id in expanded,
                                onToggleReplies = { expanded = if (c.id in expanded) expanded - c.id else expanded + c.id },
                                onPerson = { id -> nav.openPerson(id) }, actions = actions
                            )
                        }
                        val all = thread.comments + thread.comments.flatMap { it.replies }
                        val target = replyTo?.let { id -> all.firstOrNull { it.id == id } }
                        ReplyComposer(above = target?.let { t -> { ReplyingToChip(t.author.firstName) { replyTo = null } } }) { text ->
                            val top = target?.let { t -> thread.comments.firstOrNull { it.id == t.id || it.replies.any { r -> r.id == t.id } } }
                            vm.comment(text, target?.id)
                            top?.let { expanded = expanded + it.id }
                            replyTo = null
                        }
                    }
                    is EventThreadView.Locked -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LockedPanel(androidx.compose.ui.res.stringResource(com.circle.app.R.string.event_the_conversation_opens_when_you), "${thread.replyCount} replies so far")
                        Text(
                            androidx.compose.ui.res.stringResource(com.circle.app.R.string.event_only_the_people_actually_coming),
                            color = CircleTheme.colors.muted, fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
