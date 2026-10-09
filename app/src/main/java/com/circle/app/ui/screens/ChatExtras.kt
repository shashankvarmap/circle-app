package com.circle.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.circle.app.AppGraph
import com.circle.app.R
import com.circle.app.data.media.MediaKind
import com.circle.app.data.media.MediaRef
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.EventSummary
import com.circle.app.data.repo.Outcome
import com.circle.app.navigation.Routes
import com.circle.app.ui.components.Avatar
import com.circle.app.ui.theme.CircleTheme
import com.circle.app.ui.theme.Display
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel

/** The chat's "+" sheet: Photo (system picker, several allowed) or Invite to an event (one of yours). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPlusSheet(events: List<EventSummary>, onDismiss: () -> Unit, onMedia: (List<MediaRef>) -> Unit, onInvite: (String) -> Unit) {
    val colors = CircleTheme.colors
    val context = LocalContext.current
    var pickingEvent by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        val refs = uris.mapNotNull { uri ->
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            AppGraph.mediaStore.add(uri.toString(), MediaKind.PHOTO).getOrNull()
        }
        if (refs.isNotEmpty()) onMedia(refs)
        onDismiss()
    }
    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), containerColor = colors.surface) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 28.dp)) {
            if (!pickingEvent) {
                SheetRow(Icons.Outlined.Image, stringResource(R.string.chat_plus_photo)) {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                SheetRow(Icons.Outlined.CalendarToday, stringResource(R.string.chat_plus_event)) { pickingEvent = true }
            } else {
                Text(stringResource(R.string.chat_plus_event), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = colors.text, modifier = Modifier.semantics { heading() })
                if (events.isEmpty()) {
                    Text(stringResource(R.string.chat_plus_no_events), color = colors.muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 16.dp))
                }
                events.forEach { e ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(10.dp))
                            .clickable { onInvite(e.id); onDismiss() }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EventDateTile(e)
                        Spacer(Modifier.width(12.dp))
                        Text(e.title, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val colors = CircleTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(colors.accent.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

/** Full-screen viewer for photos sent in a chat (only chats you can see). */
@Composable
fun ChatMediaViewerScreen(nav: NavHostController, chatId: String, messageId: String) {
    val vm = circleViewModel(key = "chat-$chatId") { ChatViewModel(it, chatId) }
    val chat by vm.chat.collectAsStateWithLifecycle()
    val media = chat?.messages?.firstOrNull { it.id == messageId }?.media
    MediaViewer(media, 0) { nav.popBackStack() }
}

class NewChatViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    val friends = repo.friends().state(emptyList())
    fun create(ids: List<String>, name: String?, then: (Outcome<String>) -> Unit) = run({ createChat(ids, name) }, then)
}

/**
 * New chat (MESSAGES_AND_SETTINGS_V2 3): pick friends (friends only). One opens or creates the 1:1;
 * two or more ask for an optional name and make a chat that shows in Messages (not Groups).
 */
@Composable
fun NewChatScreen(nav: NavHostController) {
    val colors = CircleTheme.colors
    val vm = circleViewModel { NewChatViewModel(it) }
    val friends by vm.friends.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var picked by rememberSaveable { mutableStateOf(listOf<String>()) }
    var naming by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    val shown = friends.filter { query.isBlank() || it.person.name.contains(query.trim(), ignoreCase = true) }

    fun create() = vm.create(picked, name) { if (it is Outcome.Success) nav.navigate(Routes.chat(it.value)) { popUpTo(Routes.NEW_CHAT) { inclusive = true } } }

    if (naming) {
        AlertDialog(
            onDismissRequest = { naming = false },
            title = { Text(stringResource(R.string.newchat_name_title)) },
            text = {
                OutlinedTextField(name, { name = it.take(40) }, placeholder = { Text(stringResource(R.string.newchat_name_hint)) }, singleLine = true)
            },
            confirmButton = { TextButton(onClick = { naming = false; create() }) { Text(stringResource(R.string.newchat_create), color = colors.accent) } },
            dismissButton = { TextButton(onClick = { naming = false }) { Text(stringResource(R.string.cancel), color = colors.text) } }
        )
    }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.cancel), color = colors.body, fontSize = 14.5.sp, modifier = Modifier.clickable { nav.popBackStack() }.padding(vertical = 10.dp))
            Text(
                stringResource(R.string.messages_new_chat), color = colors.text, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier.weight(1f).semantics { heading() }, textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            val enabled = picked.isNotEmpty()
            Box(
                Modifier.alpha(if (enabled) 1f else 0.4f).clip(RoundedCornerShape(999.dp)).background(colors.accent)
                    .clickable(enabled = enabled, role = Role.Button) { if (picked.size == 1) create() else naming = true }
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(stringResource(if (picked.size > 1) R.string.newchat_next else R.string.newchat_chat), color = colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }
        }
        SearchField(query, { query = it }, placeholder = stringResource(R.string.note_share_search), modifier = Modifier.padding(horizontal = 16.dp))
        Text(stringResource(R.string.newchat_note), color = colors.muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(shown, key = { it.person.id }) { f ->
                val checked = f.person.id in picked
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .clickable(role = Role.Checkbox) { picked = if (checked) picked - f.person.id else picked + f.person.id }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(f.person.initials, f.person.color, size = 40)
                    Spacer(Modifier.width(12.dp))
                    Text(f.person.name, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Checkbox(checked = checked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = colors.accent, checkmarkColor = colors.onAccent))
                }
            }
        }
    }
}
