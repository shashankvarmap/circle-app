package com.circle.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.circle.app.R
import com.circle.app.data.repo.ChatKind
import com.circle.app.data.repo.ChatRowView
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.Outcome
import com.circle.app.navigation.Routes
import com.circle.app.ui.a11y.rememberReduceMotion
import com.circle.app.ui.components.*
import com.circle.app.ui.format.clockTime
import com.circle.app.ui.format.relative
import com.circle.app.ui.format.relativeTime
import com.circle.app.ui.theme.*
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class MTab { DIRECT, GROUP }

class MessagesViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    val chats = repo.chats().load()
    fun pin(id: String, pinned: Boolean) = run({ setChatPinned(id, pinned) })
    fun mute(id: String, muted: Boolean) = run({ setChatMuted(id, muted) })
    fun delete(id: String) = run({ deleteChat(id) })
}

/**
 * Messages (MESSAGES_AND_SETTINGS_V2 1): New chat, search, Messages | Groups, Pinned and All chats,
 * unread tint and dot, "You:" prefix. Swipe a row left (or long-press) for Pin / Mute / Delete.
 * No presence and no typing indicators.
 */
@Composable
fun MessagesScreen(nav: NavHostController) {
    val colors = CircleTheme.colors
    var tab by rememberSaveable { mutableStateOf(MTab.DIRECT) }
    var query by rememberSaveable { mutableStateOf("") }
    var confirmDelete by rememberSaveable { mutableStateOf<String?>(null) }
    val vm = circleViewModel { MessagesViewModel(it) }
    val chatsLoad by vm.chats.collectAsStateWithLifecycle()
    val chats = (chatsLoad as? com.circle.app.ui.vm.Load.Ready)?.value.orEmpty()
    val listState = rememberLazyListState()
    val header = rememberCollapsingHeaderState { listState.canScrollForward || listState.canScrollBackward }

    val inTab = chats.filter { if (tab == MTab.GROUP) it.kind == ChatKind.GROUP else it.kind != ChatKind.GROUP }
    val q = query.trim()
    val shown = inTab.filter { q.isEmpty() || it.title.contains(q, ignoreCase = true) || it.members.any { m -> m.name.contains(q, ignoreCase = true) } }
    val pinned = shown.filter { it.pinned }
    val rest = shown.filter { !it.pinned }

    confirmDelete?.let { id ->
        ConfirmDialog(
            stringResource(R.string.messages_delete_title), stringResource(R.string.messages_delete_body), stringResource(R.string.note_delete),
            destructive = true, onDismiss = { confirmDelete = null }
        ) { vm.delete(id) }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        OfflineBanner()
        CollapsingHeaderLayout(
            state = header,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            header = {
                Column(Modifier.fillMaxWidth().background(colors.background)) {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.messages_title), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 26.sp,
                            color = colors.text, modifier = Modifier.weight(1f).semantics { heading() }
                        )
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button) { nav.navigate(Routes.NEW_CHAT) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier.size(42.dp).clip(CircleShape).background(colors.accent.copy(alpha = 0.08f))
                                    .border(1.5.dp, colors.accent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.messages_new_chat), tint = colors.accent) }
                        }
                    }
                    SearchField(query, { query = it }, placeholder = stringResource(R.string.messages_search), modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp))
                    Box(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)) {
                        UnderlineTabs(listOf(stringResource(R.string.messages_tab_messages), stringResource(R.string.messages_tab_groups)), tab.ordinal) {
                            tab = MTab.entries[it]
                        }
                    }
                }
            }
        ) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                if (tab == MTab.GROUP) item(key = "group-note") {
                    Text(stringResource(R.string.messages_groups_note), color = colors.muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
                if (chatsLoad is com.circle.app.ui.vm.Load.Loading) {
                    item(key = "loading") { SkeletonList() }
                } else if (chatsLoad is com.circle.app.ui.vm.Load.Failed) {
                    item(key = "error") { ErrorState(onRetry = vm::retry) }
                } else if (inTab.isEmpty()) {
                    item(key = "empty") {
                        if (tab == MTab.DIRECT) EmptyState(EmptyKind.MESSAGES, action = stringResource(R.string.messages_start_chat), onAction = { nav.navigate(Routes.NEW_CHAT) })
                        else EmptyState(EmptyKind.GROUPS, action = stringResource(R.string.empty_groups_action), onAction = { nav.navigate(Routes.EXPLORE) })
                    }
                } else if (shown.isEmpty()) {
                    item(key = "nomatch") {
                        Text(stringResource(R.string.messages_no_match, q), color = colors.muted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
                    }
                }
                if (pinned.isNotEmpty()) {
                    item(key = "pinned-h") { CapsHeading(stringResource(R.string.messages_pinned_caps)) }
                    items(pinned, key = { it.id }) { c -> SwipeChatRow(c, vm, nav, onDelete = { confirmDelete = c.id }) }
                }
                if (rest.isNotEmpty()) {
                    if (pinned.isNotEmpty()) item(key = "all-h") { CapsHeading(stringResource(R.string.messages_all_caps)) }
                    items(rest, key = { it.id }) { c -> SwipeChatRow(c, vm, nav, onDelete = { confirmDelete = c.id }) }
                }
                if (tab == MTab.DIRECT && inTab.isNotEmpty()) item(key = "footer") {
                    Text(
                        stringResource(R.string.messages_footer), color = colors.muted, fontSize = 12.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)
                    )
                }
            }
        }
        CircleBottomNav(nav, NavTab.MESSAGES)
    }
}

@Composable
private fun CapsHeading(text: String) {
    Text(
        text, color = CircleTheme.colors.muted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.47.sp,
        modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 4.dp).semantics { heading() }
    )
}

private val ActionWidth = 64.dp

/** A row that slides left to reveal Pin / Mute / Delete; long-press opens the same as a menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeChatRow(chat: ChatRowView, vm: MessagesViewModel, nav: NavHostController, onDelete: () -> Unit) {
    val colors = CircleTheme.colors
    val density = LocalDensity.current
    val reduceMotion = rememberReduceMotion()
    val scope = rememberCoroutineScope()
    val maxPx = with(density) { (ActionWidth * 3).toPx() }
    val offset = remember { Animatable(0f) }
    var menu by remember { mutableStateOf(false) }
    fun close() = scope.launch { if (reduceMotion) offset.snapTo(0f) else offset.animateTo(0f) }

    val pinLabel = stringResource(if (chat.pinned) R.string.messages_unpin else R.string.messages_pin)
    val muteLabel = stringResource(if (chat.muted) R.string.messages_unmute else R.string.messages_mute)
    val deleteLabel = stringResource(R.string.note_delete)
    val actions = listOf(
        Triple(pinLabel, Icons.Outlined.PushPin) { vm.pin(chat.id, !chat.pinned) },
        Triple(muteLabel, Icons.Outlined.NotificationsOff) { vm.mute(chat.id, !chat.muted) },
        Triple(deleteLabel, Icons.Outlined.DeleteOutline, onDelete),
    )

    Box(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // The actions are drawn only while the row is slid open, so nothing shows through it.
        if (offset.value < 0f) Row(Modifier.align(Alignment.CenterEnd).fillMaxHeight()) {
            actions.forEachIndexed { i, (label, icon, act) ->
                Column(
                    Modifier.width(ActionWidth).fillMaxHeight()
                        .background(if (i == 2) colors.alertDot else if (i == 0) colors.accent else colors.track)
                        .clickable { act(); close() },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    val fg = if (i == 1) colors.text else colors.onAccent
                    Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                    Text(label, color = fg, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { d -> scope.launch { offset.snapTo((offset.value + d).coerceIn(-maxPx, 0f)) } },
                    onDragStopped = {
                        val open = offset.value < -maxPx / 2
                        if (reduceMotion) offset.snapTo(if (open) -maxPx else 0f) else offset.animateTo(if (open) -maxPx else 0f)
                    }
                )
                .semantics { customActions = actions.map { (label, _, act) -> CustomAccessibilityAction(label) { act(); true } } }
        ) {
            ChatRow(chat, onOpen = {
                if (offset.value != 0f) close() else nav.navigate(Routes.chat(chat.id))
            }, onLongPress = { menu = true })
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                actions.forEach { (label, _, act) -> DropdownMenuItem(text = { Text(label) }, onClick = { menu = false; act() }) }
            }
        }
    }
}

/** A chat in the list: 52dp avatar (circle for people, rounded square for groups), name, preview, time, unread dot. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRow(chat: ChatRowView, onOpen: () -> Unit, onLongPress: () -> Unit) {
    val colors = CircleTheme.colors
    val unreadLabel = stringResource(R.string.messages_unread)
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(colors.background)
            .background(if (chat.unread) colors.accent.copy(alpha = 0.08f) else colors.background)
            .combinedClickable(onClickLabel = stringResource(R.string.messages_open_chat), onClick = onOpen, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            chat.group != null -> GroupTile(chat.group.initial, chat.group.color, size = 52)
            chat.person != null -> Avatar(chat.person.initials, chat.person.color, size = 52)
            else -> Box(modifier = Modifier.size(52.dp)) {
                chat.members.getOrNull(0)?.let { Avatar(it.initials, it.color, size = 36) }
                chat.members.getOrNull(1)?.let { Box(Modifier.offset(x = 16.dp, y = 16.dp)) { Avatar(it.initials, it.color, size = 36) } }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    chat.title, color = colors.text, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = if (chat.unread) FontWeight.ExtraBold else FontWeight.Bold, modifier = Modifier.weight(1f, fill = false)
                )
                if (chat.pinned) {
                    Spacer(Modifier.width(5.dp))
                    Icon(Icons.Outlined.PushPin, contentDescription = stringResource(R.string.messages_pinned), tint = colors.muted, modifier = Modifier.size(13.dp))
                }
                if (chat.muted) {
                    Spacer(Modifier.width(5.dp))
                    Icon(Icons.Outlined.NotificationsOff, contentDescription = stringResource(R.string.messages_muted), tint = colors.muted, modifier = Modifier.size(13.dp))
                }
            }
            val p = chat.preview
            val preview = when {
                p == null -> stringResource(R.string.messages_no_messages)
                p.mine -> stringResource(R.string.messages_preview_you, summary(p.kind, p.text))
                p.senderFirstName != null -> stringResource(R.string.messages_preview_named, p.senderFirstName, summary(p.kind, p.text))
                else -> summary(p.kind, p.text)
            }
            Text(
                preview, color = if (chat.unread) colors.text else colors.muted,
                fontWeight = if (chat.unread) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            val time = chat.lastAt?.let { at -> if (relative(at) is com.circle.app.ui.format.Relative.Hours || relative(at) is com.circle.app.ui.format.Relative.Minutes) clockTime(at) else relativeTime(at) }
            Text(time.orEmpty(), color = if (chat.unread) colors.accent else colors.muted, fontSize = 11.5.sp)
            if (chat.unread) {
                Spacer(Modifier.height(5.dp))
                Box(Modifier.size(10.dp).background(colors.accent, CircleShape).semantics { contentDescription = unreadLabel })
            }
        }
    }
}

/** One-line summary of a message, for list previews and reply snippets. */
@Composable
fun summary(kind: com.circle.app.data.MessageKind, text: String): String = when (kind) {
    com.circle.app.data.MessageKind.TEXT -> text
    com.circle.app.data.MessageKind.SHARED_NOTE -> stringResource(R.string.chat_shared_a_note)
    com.circle.app.data.MessageKind.EVENT_INVITE -> stringResource(R.string.chat_event_invite)
    com.circle.app.data.MessageKind.PHOTO -> stringResource(R.string.chat_photo)
}
