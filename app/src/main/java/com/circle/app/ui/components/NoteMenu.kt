package com.circle.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.circle.app.R
import com.circle.app.data.access.Audience
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.Note
import com.circle.app.data.repo.Outcome
import com.circle.app.data.repo.PersonRef
import com.circle.app.navigation.Routes
import com.circle.app.ui.screens.SearchField
import com.circle.app.ui.theme.CircleTheme
import com.circle.app.ui.theme.Display
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import kotlinx.coroutines.flow.Flow

/** The ⋯ menu's actions, all through the repository (which refuses what the rules don't allow). */
class NoteMenuViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    fun candidates(noteId: String): Flow<List<PersonRef>> = repo.shareCandidates(noteId)
    fun share(noteId: String, friendId: String, then: (Outcome<String>) -> Unit) = run({ shareNoteWithFriend(noteId, friendId) }, then)
    fun mute(personId: String, then: (Outcome<Unit>) -> Unit) = run({ mute(personId) }, then)
    fun block(personId: String, then: (Outcome<Unit>) -> Unit) = run({ block(personId) }, then)

    fun delete(noteId: String) = run({ deleteNote(noteId) })
    fun setAudience(noteId: String, audience: Audience) = run({ setAudience(noteId, audience) })
}

private enum class NoteDialog { DELETE, AUDIENCE, REPORT, BLOCK, SHARE }

/**
 * The ⋯ button on a note. Your own notes: Edit, Change audience (not in groups), Delete.
 * Others': Send in a message (when sharing is allowed), Mute this friend, Report, Block.
 */
@Composable
fun NoteMenuButton(nav: NavHostController, note: Note) {
    val colors = CircleTheme.colors
    val context = LocalContext.current
    val vm = circleViewModel { NoteMenuViewModel(it) }
    var open by remember { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf<NoteDialog?>(null) }
    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    val first = note.author.firstName

    Box {
        Box(
            Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button) { open = true },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.MoreHoriz, contentDescription = stringResource(R.string.note_more_options), tint = colors.muted, modifier = Modifier.size(18.dp)) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val items = buildList<Pair<Int, () -> Unit>> {
                if (note.isMine) {
                    add(R.string.note_edit to { nav.navigate(Routes.compose(edit = note.id)) })
                    if (note.group == null) add(R.string.note_change_audience to { dialog = NoteDialog.AUDIENCE })
                    add(R.string.note_delete to { dialog = NoteDialog.DELETE })
                } else {
                    if (note.canShare && note.group == null) add(R.string.note_send_in_message to { dialog = NoteDialog.SHARE })
                    if (note.group == null) add(R.string.note_mute_friend to {
                        val muted = context.getString(R.string.toast_muted)
                        vm.mute(note.author.id) { if (it is Outcome.Success) toast(muted) }
                    })
                    add(R.string.profile_report to { dialog = NoteDialog.REPORT })
                    add(R.string.profile_block to { dialog = NoteDialog.BLOCK })
                }
            }
            items.forEach { (label, action) ->
                DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { open = false; action() })
            }
        }
    }

    when (dialog) {
        NoteDialog.DELETE -> ConfirmDialog(
            stringResource(R.string.note_delete_title), stringResource(R.string.note_delete_body), stringResource(R.string.note_delete),
            destructive = true, onDismiss = { dialog = null }
        ) { vm.delete(note.id) }
        NoteDialog.REPORT -> ReportSheet(com.circle.app.data.ReportKind.NOTE, note.id, onDone = { dialog = null })
        NoteDialog.BLOCK -> ConfirmDialog(
            stringResource(R.string.profile_block_title, first), stringResource(R.string.profile_block_body), stringResource(R.string.profile_block),
            destructive = true, onDismiss = { dialog = null }
        ) {
            val blocked = context.getString(R.string.toast_blocked)
            vm.block(note.author.id) { if (it is Outcome.Success) toast(blocked) }
        }
        NoteDialog.AUDIENCE -> AudienceDialog(note.audience, onDismiss = { dialog = null }) { vm.setAudience(note.id, it) }
        NoteDialog.SHARE -> ShareSheet(vm, note, onDismiss = { dialog = null }) { friend ->
            val sent = context.getString(R.string.toast_sent_to, friend.firstName)
            vm.share(note.id, friend.id) { if (it is Outcome.Success) toast(sent) }
        }
        null -> Unit
    }
}

@Composable
fun ConfirmDialog(title: String, body: String, action: String, destructive: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val colors = CircleTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm() }) { Text(action, color = if (destructive) colors.alertDot else colors.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = colors.text) } }
    )
}

@Composable
private fun AudienceDialog(current: Audience, onDismiss: () -> Unit, onPick: (Audience) -> Unit) {
    val colors = CircleTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.note_change_audience)) },
        text = {
            Column {
                listOf(Audience.FRIENDS to R.string.compose_audience_friends, Audience.INNER_CIRCLE to R.string.compose_audience_inner).forEach { (a, label) ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .selectable(selected = a == current, role = Role.RadioButton) { onDismiss(); onPick(a) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = a == current, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = colors.accent))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(label), color = colors.text, fontSize = 15.sp)
                    }
                }
                Text(stringResource(R.string.note_audience_note), color = colors.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = colors.text) } }
    )
}

/** "Send in a message": pick a friend (only those the note may go to); it lands in your 1:1 chat. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareSheet(vm: NoteMenuViewModel, note: Note, onDismiss: () -> Unit, onPick: (PersonRef) -> Unit) {
    val colors = CircleTheme.colors
    val candidates by remember(note.actionTargetId) { vm.candidates(note.actionTargetId) }.collectAsStateWithLifecycle(emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    val shown = candidates.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), containerColor = colors.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                stringResource(R.string.note_send_in_message), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 21.sp,
                color = colors.text, modifier = Modifier.semantics { heading() }
            )
            if (note.audience == Audience.INNER_CIRCLE) {
                Text(stringResource(R.string.note_share_inner_only), color = colors.muted, fontSize = 12.5.sp, modifier = Modifier.padding(top = 4.dp))
            }
            SearchField(query, { query = it }, placeholder = stringResource(R.string.note_share_search), modifier = Modifier.padding(vertical = 12.dp))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                items(shown, key = { it.id }) { p ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(10.dp))
                            .clickable { onDismiss(); onPick(p) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(p.initials, p.color, size = 36)
                        Spacer(Modifier.width(12.dp))
                        Text(p.name, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
