package com.circle.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.circle.app.R
import com.circle.app.data.MessageKind
import com.circle.app.data.repo.ChatKind
import com.circle.app.data.repo.ChatView
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.EventCardView
import com.circle.app.data.repo.MessageView
import com.circle.app.data.repo.NoteView
import com.circle.app.data.repo.ReplySnippet
import com.circle.app.navigation.Routes
import com.circle.app.navigation.openPerson
import com.circle.app.ui.a11y.rememberReduceMotion
import com.circle.app.ui.components.Avatar
import com.circle.app.ui.components.GateKind
import com.circle.app.ui.components.GroupTile
import com.circle.app.ui.components.NotFriendsSheet
import com.circle.app.ui.format.clockTime
import com.circle.app.ui.format.dayLabel
import com.circle.app.ui.format.eventDay
import com.circle.app.ui.format.eventMonth
import com.circle.app.ui.format.eventWhen
import com.circle.app.ui.theme.*
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import kotlinx.coroutines.launch
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

private val SwipeMax = 72.dp       // the bubble follows the finger up to here
private val SwipeTrigger = 44.dp   // releasing past this starts a reply
private val BubbleMaxWidth = 276.dp

class ChatViewModel(repo: CircleRepository, private val chatId: String) : RepoViewModel(repo) {
    /** Null while loading, or when the chat isn't available to you. */
    val chat = repo.chat(chatId).state(null)
    /** A temporary chat (or its group) the server deleted while you were away, or while this screen was open. */
    val gone = repo.wasDeleted(chatId).state(false)
    fun extend(days: Int, then: (com.circle.app.data.repo.Outcome<Unit>) -> Unit) = run({ extendLifetime(chatId, days) }, then)
    fun makePermanent(then: (com.circle.app.data.repo.Outcome<Unit>) -> Unit) = run({ makePermanent(chatId) }, then)
    fun endNow(then: (com.circle.app.data.repo.Outcome<Unit>) -> Unit) = run({ endNow(chatId) }, then)
    /** Leaving a group's chat leaves the group; a chat with friends leaves your list. */
    fun leave(groupId: String?, then: (com.circle.app.data.repo.Outcome<Unit>) -> Unit) =
        run({ if (groupId != null) leaveGroup(groupId) else deleteChat(chatId) }, then)
    fun markRead() = run({ markRead(chatId); com.circle.app.data.repo.Outcome.Success(Unit) })
    /** Messages on their way: shown as "Sending…", then "Not sent · tap to retry" if the send fails. */
    val pending = kotlinx.coroutines.flow.MutableStateFlow<List<PendingMessage>>(emptyList())

    fun send(text: String, replyToId: String?) {
        val p = PendingMessage(java.util.UUID.randomUUID().toString(), text.trim(), replyToId)
        if (p.text.isEmpty()) return
        pending.value = pending.value + p
        deliver(p)
    }

    fun retry(id: String) {
        val p = pending.value.firstOrNull { it.id == id } ?: return
        pending.value = pending.value.map { if (it.id == id) it.copy(failed = false) else it }
        deliver(p)
    }

    // The inline status says what happened, so no app-wide message here.
    private fun deliver(p: PendingMessage) {
        viewModelScope.launch {
            val ok = repo.sendMessage(chatId, p.text, p.replyToId) is com.circle.app.data.repo.Outcome.Success
            pending.value = if (ok) pending.value.filterNot { it.id == p.id }
            else pending.value.map { if (it.id == p.id) it.copy(failed = true) else it }
        }
    }
    fun deleteForMe(messageId: String) = run({ deleteForMe(chatId, messageId) })
    fun unsend(messageId: String) = run({ unsend(chatId, messageId) })
    val myEvents = repo.myEvents().state(emptyList())
    fun sendMedia(media: List<com.circle.app.data.media.MediaRef>) = run({ sendMedia(chatId, media) })
    fun invite(eventId: String) = run({ sendEventInvite(chatId, eventId) })
    fun setMuted(muted: Boolean) = run({ setChatMuted(chatId, muted) })
    fun clearMessages() = run({ clearChat(chatId) })
    fun block(personId: String, then: () -> Unit) = run({ block(personId) }) { if (it is com.circle.app.data.repo.Outcome.Success) then() }
}

data class PendingMessage(val id: String, val text: String, val replyToId: String?, val failed: Boolean = false)

/** One row of the conversation, oldest first. */
private sealed interface ChatEntry {
    val key: String
    data class Day(val at: java.time.Instant, val date: java.time.LocalDate) : ChatEntry { override val key get() = "day-$date" }
    data class Msg(val m: MessageView, val firstOfRun: Boolean, val lastOfRun: Boolean) : ChatEntry { override val key get() = m.id }
    data object NewMessages : ChatEntry { override val key get() = "new-messages" }
}

private fun entries(messages: List<MessageView>, firstUnreadId: String?, zone: ZoneId = ZoneId.systemDefault()): List<ChatEntry> {
    val out = mutableListOf<ChatEntry>()
    fun day(m: MessageView) = m.createdAt.atZone(zone).toLocalDate()
    messages.forEachIndexed { i, m ->
        val prev = messages.getOrNull(i - 1)
        val next = messages.getOrNull(i + 1)
        if (prev == null || day(prev) != day(m)) out += ChatEntry.Day(m.createdAt, day(m))
        if (m.id == firstUnreadId) out += ChatEntry.NewMessages
        val first = prev == null || prev.sender.id != m.sender.id || day(prev) != day(m)
        val last = next == null || next.sender.id != m.sender.id || day(next) != day(m)
        out += ChatEntry.Msg(m, first, last)
    }
    return out
}

/**
 * A conversation. Newest at the bottom. Tapping a text message does nothing; swipe right on any
 * message to reply; hold (~400ms) for Copy / Delete for me / Unsend (yours only). A shared note
 * opens its thread only if the repository says you may (else the not-friends sheet); an event
 * invite opens the event if it's visible to you (else the same gate for the host).
 */
@Composable
fun ChatScreen(nav: NavHostController, chatId: String) {
    val colors = CircleTheme.colors
    val vm = circleViewModel(key = "chat-$chatId") { ChatViewModel(it, chatId) }
    val chat by vm.chat.collectAsStateWithLifecycle()
    LaunchedEffect(chatId) { vm.markRead() }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var draft by rememberSaveable { mutableStateOf("") }
    var replyToId by rememberSaveable { mutableStateOf<String?>(null) }
    var gate by remember { mutableStateOf<Pair<String, GateKind>?>(null) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    var plusSheet by rememberSaveable { mutableStateOf(false) }
    var reporting by rememberSaveable { mutableStateOf(false) }
    // The "New messages" divider stays where it was when you opened the chat.
    var unreadMarker by rememberSaveable { mutableStateOf<String?>(null) }
    var markerTaken by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(chat?.id) {
        val first = chat ?: return@LaunchedEffect
        if (!markerTaken) { unreadMarker = first.firstUnreadId; markerTaken = true }
    }

    val gone by vm.gone.collectAsStateWithLifecycle()
    val c = chat
    if (c == null) {
        if (gone) {
            com.circle.app.ui.components.DeletedTimedState(onBack = {
                nav.navigate(Routes.MESSAGES) { popUpTo(Routes.MESSAGES) { inclusive = true }; launchSingleTop = true }
            })
            return
        }
        Column(Modifier.fillMaxSize().background(colors.background)) {
            DetailHeader(stringResource(R.string.chat_title)) { nav.popBackStack() }
        }
        return
    }
    var extending by rememberSaveable { mutableStateOf(false) }
    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    val saveSoon = stringResource(R.string.timed_save_copy_soon)
    val life = c.lifetime
    if (extending && life != null) {
        val permanentDone = stringResource(if (c.kind == ChatKind.GROUP) R.string.extend_permanent_done_group else R.string.extend_permanent_done_chat)
        val endDone = stringResource(R.string.extend_end_done)
        val res = context.resources
        com.circle.app.ui.components.ExtendSheet(
            canEndNow = life.state != com.circle.app.data.lifetime.LifeState.WRAP_UP,
            onDismiss = { extending = false },
            onExtend = { d -> extending = false; vm.extend(d) { if (it is com.circle.app.data.repo.Outcome.Success) toast(res.getQuantityString(R.plurals.extend_done, d, d)) } },
            onPermanent = { extending = false; vm.makePermanent { if (it is com.circle.app.data.repo.Outcome.Success) toast(permanentDone) } },
            onEndNow = { extending = false; vm.endNow { if (it is com.circle.app.data.repo.Outcome.Success) toast(endDone) } },
        )
    }

    val messages = c.messages
    val pendingList by vm.pending.collectAsStateWithLifecycle()
    val q = search.trim()
    val shownMessages = if (searching && q.isNotEmpty()) messages.filter { it.text.contains(q, ignoreCase = true) } else messages
    val rows = entries(shownMessages, if (searching) null else unreadMarker).asReversed() // reverse layout: index 0 is the newest
    val replyTo = replyToId?.let { id -> messages.firstOrNull { it.id == id } }
    val isGroupLike = c.kind != ChatKind.DIRECT
    val youName = stringResource(R.string.chat_you)
    fun senderName(m: MessageView) = if (m.mine) youName else m.sender.firstName

    fun send() {
        if (draft.isBlank()) return
        vm.send(draft, replyTo?.id)
        draft = ""; replyToId = null
        scope.launch { listState.animateScrollToItem(0) }
    }

    val baseConfig = LocalViewConfiguration.current
    val chatConfig = remember(baseConfig) {
        object : ViewConfiguration by baseConfig {
            override val longPressTimeoutMillis: Long get() = 400L
        }
    }

    gate?.let { (personId, kind) ->
        NotFriendsSheet(personId = personId, kind = kind, onViewProfile = { nav.openPerson(personId) }, onDismiss = { gate = null })
    }
    if (reporting) {
        com.circle.app.ui.components.ReportSheet(
            if (c.group != null) com.circle.app.data.ReportKind.GROUP else com.circle.app.data.ReportKind.PERSON,
            c.group?.id ?: c.person?.id ?: c.id, onDone = { reporting = false }
        )
    }
    if (plusSheet) {
        val events by vm.myEvents.collectAsStateWithLifecycle()
        ChatPlusSheet(events, onDismiss = { plusSheet = false }, onMedia = { vm.sendMedia(it); scope.launch { listState.animateScrollToItem(0) } }, onInvite = { vm.invite(it) })
    }

    Column(Modifier.fillMaxSize().background(colors.background).imePadding()) {
        if (searching) ChatSearchBar(search, { search = it }, onClose = { searching = false; search = "" })
        else ChatTopBar(
            nav, c, vm, onSearch = { searching = true }, onReport = { reporting = true },
            onSave = { toast(saveSoon) }, onExtend = { extending = true },
        )
        if (life != null && !searching) {
            com.circle.app.ui.components.LifetimeBanner(life, isGroupChat = c.kind == ChatKind.GROUP, onSave = { toast(saveSoon) }, onExtend = { extending = true })
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
        CompositionLocalProvider(LocalViewConfiguration provides chatConfig) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
            ) {
                if (searching && q.isNotEmpty() && shownMessages.isEmpty()) item(key = "nomatch") {
                    Text(stringResource(R.string.chat_search_none, q), color = colors.muted, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                items(pendingList.asReversed(), key = { "pending-" + it.id }) { p ->
                    PendingRow(p) { vm.retry(p.id) }
                }
                items(rows, key = { it.key }) { entry ->
                    when (entry) {
                        ChatEntry.NewMessages -> NewMessagesDivider()
                        is ChatEntry.Day -> DayPill(dayLabel(entry.at))
                        is ChatEntry.Msg -> {
                            val m = entry.m
                            val removed = stringResource(R.string.chat_removed_for_you)
                            val copyLabel = copyText(m)
                            MessageRow(
                                m = m,
                                firstOfRun = entry.firstOfRun,
                                lastOfRun = entry.lastOfRun,
                                senderLabel = if (isGroupLike && !m.mine && entry.firstOfRun) senderName(m) else null,
                                showLeadingAvatar = isGroupLike && !m.mine,
                                onPerson = { nav.openPerson(it) },
                                onReply = { replyToId = m.id },
                                onCopy = { clipboard.setText(AnnotatedString(copyLabel)) },
                                onDeleteForMe = {
                                    vm.deleteForMe(m.id)
                                    if (replyToId == m.id) replyToId = null
                                    Toast.makeText(context, removed, Toast.LENGTH_SHORT).show()
                                },
                                onUnsend = if (m.mine) ({
                                    vm.unsend(m.id)
                                    if (replyToId == m.id) replyToId = null
                                }) else null,
                                onOpen = when (m.kind) {
                                    MessageKind.TEXT -> null
                                    MessageKind.PHOTO -> ({ nav.navigate(Routes.chatViewer(c.id, m.id)) })
                                    MessageKind.SHARED_NOTE -> when (val note = m.sharedNote) {
                                        is NoteView.Full -> ({ nav.navigate(Routes.thread(note.note.id)) })
                                        is NoteView.Snippet -> ({ gate = note.authorId to GateKind.NOTE })
                                        else -> null
                                    }
                                    MessageKind.EVENT_INVITE -> m.event?.let { e ->
                                        {
                                            if (e.canOpen) nav.navigate(Routes.eventDetail(e.id))
                                            else e.host?.let { gate = it.id to GateKind.EVENT }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
            // "Jump to latest" when you've scrolled up.
            val awayFromLatest by remember { derivedStateOf { listState.firstVisibleItemIndex > 3 } }
            if (awayFromLatest) {
                Text(
                    stringResource(R.string.chat_jump_latest), color = colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 12.5.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).clip(RoundedCornerShape(999.dp))
                        .background(colors.accent).clickable { scope.launch { listState.animateScrollToItem(0) } }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        if (!c.canSend) {
            // Wrap-up: read-only for everyone until it is deleted.
            com.circle.app.ui.components.ReadOnlyBar()
        } else {
            if (replyTo != null) ReplyStrip(senderName(replyTo), summary(replyTo.kind, replyTo.text)) { replyToId = null }
            Composer(draft, onChange = { draft = it }, onSend = ::send, onPlus = { plusSheet = true })
        }
    }
}

@Composable
private fun NewMessagesDivider() {
    val colors = CircleTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(colors.accent.copy(alpha = 0.5f)))
        Text(stringResource(R.string.chat_new_messages), color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(colors.accent.copy(alpha = 0.5f)))
    }
}

@Composable
private fun ChatSearchBar(value: String, onChange: (String) -> Unit, onClose: () -> Unit) {
    val colors = CircleTheme.colors
    Column {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.chat_search_close), tint = colors.text)
            }
            SearchField(value, onChange, placeholder = stringResource(R.string.chat_search_hint), modifier = Modifier.weight(1f))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
    }
}

/** What Copy puts on the clipboard: never more than the message shows (a snippet stays a snippet). */
@Composable
private fun copyText(m: MessageView): String = when (m.kind) {
    MessageKind.TEXT -> m.text
    MessageKind.PHOTO -> stringResource(R.string.chat_photo)
    MessageKind.SHARED_NOTE -> when (val n = m.sharedNote) {
        is NoteView.Full -> androidx.compose.ui.res.stringResource(com.circle.app.R.string.chat_copy_note, n.note.author.name, n.note.text)
        is NoteView.Snippet -> androidx.compose.ui.res.stringResource(com.circle.app.R.string.chat_copy_note, n.authorName, n.text)
        else -> stringResource(R.string.chat_shared_a_note)
    }
    MessageKind.EVENT_INVITE -> m.event?.let { "${it.title}, ${eventWhen(it.startsAt)}" } ?: stringResource(R.string.chat_event_invite)
}

@Composable
private fun ChatTopBar(
    nav: NavHostController, chat: ChatView, vm: ChatViewModel, onSearch: () -> Unit, onReport: () -> Unit,
    onSave: () -> Unit, onExtend: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf<String?>(null) }
    when (confirm) {
        "clear" -> com.circle.app.ui.components.ConfirmDialog(
            stringResource(R.string.chat_clear_title), stringResource(R.string.chat_clear_body), stringResource(R.string.chat_clear),
            destructive = true, onDismiss = { confirm = null }
        ) { vm.clearMessages() }
        "leave" -> com.circle.app.ui.components.ConfirmDialog(
            stringResource(R.string.timed_leave_title, chat.title), stringResource(R.string.timed_leave_body), stringResource(R.string.timed_menu_leave),
            destructive = true, onDismiss = { confirm = null }
        ) { vm.leave(chat.group?.id) { if (it is com.circle.app.data.repo.Outcome.Success) nav.popBackStack() } }
        "block" -> chat.person?.let { p ->
            com.circle.app.ui.components.ConfirmDialog(
                stringResource(R.string.profile_block_title, p.firstName), stringResource(R.string.profile_block_body), stringResource(R.string.profile_block),
                destructive = true, onDismiss = { confirm = null }
            ) { vm.block(p.id) { nav.popBackStack() } }
        }
    }
    val colors = CircleTheme.colors
    val life = chat.lifetime
    val friends = (chat.memberCount - 1).coerceAtLeast(1)
    val subline = when {
        life != null && life.state == com.circle.app.data.lifetime.LifeState.WRAP_UP ->
            stringResource(R.string.chat_sub_ended, com.circle.app.ui.components.lastDay(life.endsAt))
        life != null && chat.kind == ChatKind.GROUP ->
            androidx.compose.ui.res.pluralStringResource(R.plurals.chat_sub_members_ends, chat.memberCount, chat.memberCount, com.circle.app.ui.components.lastDay(life.endsAt))
        life != null ->
            androidx.compose.ui.res.pluralStringResource(R.plurals.chat_sub_friends_ends, friends, friends, com.circle.app.ui.components.lastDay(life.endsAt))
        else -> when (chat.kind) {
        ChatKind.GROUP -> androidx.compose.ui.res.pluralStringResource(R.plurals.members_count, chat.memberCount, chat.memberCount)
        ChatKind.DIRECT -> chat.friendsSinceYear?.let { stringResource(R.string.chat_friends_since, it) }.orEmpty()
        ChatKind.MULTI -> stringResource(R.string.chat_you_and_friends, chat.memberCount - 1)
        }
    }
    Column {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.popBackStack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.text)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        when {
                            chat.group != null -> Modifier.clickable(onClickLabel = stringResource(R.string.chat_open_group)) { nav.navigate(Routes.groupDetail(chat.group.id)) }
                            chat.person != null -> Modifier.clickable(onClickLabel = stringResource(R.string.open_profile)) { nav.openPerson(chat.person.id) }
                            else -> Modifier
                        }
                    )
                    .padding(4.dp)
                    .weight(1f)
            ) {
                when {
                    chat.group != null -> GroupTile(chat.group.initial, chat.group.color, size = 40)
                    chat.person != null -> Avatar(chat.person.initials, chat.person.color, size = 40)
                    else -> Avatar(chat.title.take(1).uppercase(), AvatarTeal, size = 40)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(chat.title, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 15.5.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
                    Text(subline, color = colors.muted, fontSize = 11.5.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }

            if (life != null) com.circle.app.ui.components.TimerChip(life, Modifier.padding(start = 6.dp))
            Box {
                Box(Modifier.size(44.dp).clip(CircleShape).clickable { menu = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.chat_more_options), tint = colors.text)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (life?.canSaveCopy == true) DropdownMenuItem(text = { Text(stringResource(R.string.timed_save_copy)) }, onClick = { menu = false; onSave() })
                    if (life?.canChange == true) DropdownMenuItem(text = { Text(stringResource(R.string.timed_menu_extend)) }, onClick = { menu = false; onExtend() })
                    if (life != null) DropdownMenuItem(text = { Text(stringResource(R.string.timed_menu_leave), color = colors.alertDot) }, onClick = { menu = false; confirm = "leave" })
                    DropdownMenuItem(text = { Text(stringResource(if (chat.muted) R.string.messages_unmute else R.string.messages_mute)) }, onClick = { menu = false; vm.setMuted(!chat.muted) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.chat_search)) }, onClick = { menu = false; onSearch() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.chat_clear)) }, onClick = { menu = false; confirm = "clear" })
                    if (chat.person != null) DropdownMenuItem(text = { Text(stringResource(R.string.profile_block)) }, onClick = { menu = false; confirm = "block" })
                    if (chat.kind != ChatKind.MULTI) DropdownMenuItem(text = { Text(stringResource(R.string.profile_report)) }, onClick = { menu = false; onReport() })
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
    }
}

@Composable
private fun DayPill(label: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text(
            label, color = CircleTheme.colors.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp,
            modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(CircleTheme.colors.surface)
                .border(1.dp, CircleTheme.colors.border, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

/**
 * A message with its gestures. Swipe right (mostly horizontal; vertical scroll is left alone)
 * drags the message up to [SwipeMax] with a reply icon behind it; releasing past [SwipeTrigger]
 * starts a reply and the message springs back. Long-press opens the menu. [onOpen] is the tap
 * action of rich cards; plain text has none.
 */
@Composable
private fun MessageRow(
    m: MessageView,
    firstOfRun: Boolean,
    lastOfRun: Boolean,
    senderLabel: String?,
    showLeadingAvatar: Boolean,
    onPerson: (String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onDeleteForMe: () -> Unit,
    onUnsend: (() -> Unit)?,
    onOpen: (() -> Unit)?,
) {
    val colors = CircleTheme.colors
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val reduceMotion = rememberReduceMotion()
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val currentOnReply by rememberUpdatedState(onReply)
    var menuOpen by remember { mutableStateOf(false) }
    val maxPx = with(density) { SwipeMax.toPx() }
    val triggerPx = with(density) { SwipeTrigger.toPx() }
    val mine = m.mine

    val replyLabel = stringResource(R.string.chat_reply)
    val copyLabel = stringResource(R.string.chat_copy)
    val deleteLabel = stringResource(R.string.chat_delete_for_me)
    val unsendLabel = stringResource(R.string.chat_unsend)
    val actions = buildList {
        add(CustomAccessibilityAction(replyLabel) { onReply(); true })
        add(CustomAccessibilityAction(copyLabel) { onCopy(); true })
        add(CustomAccessibilityAction(deleteLabel) { onDeleteForMe(); true })
        if (onUnsend != null) add(CustomAccessibilityAction(unsendLabel) { onUnsend(); true })
    }

    Column(
        Modifier.fillMaxWidth().padding(top = if (firstOfRun) 10.dp else 3.dp),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start
    ) {
        if (senderLabel != null) {
            Text(
                senderLabel, color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = if (showLeadingAvatar) 38.dp else 12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .then(if (showLeadingAvatar) Modifier.clickable(onClickLabel = stringResource(R.string.open_profile)) { onPerson(m.sender.id) } else Modifier)
                    .padding(horizontal = 2.dp, vertical = 3.dp)
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            // Group-like chats: the first message of a run shows the sender's avatar (opens their profile).
            if (showLeadingAvatar) {
                val p = m.sender
                Box(Modifier.width(34.dp), contentAlignment = Alignment.TopStart) {
                    if (firstOfRun) {
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).clickable(onClickLabel = stringResource(R.string.open_profile_of, p.firstName)) { onPerson(p.id) },
                            contentAlignment = Alignment.CenterStart
                        ) { Avatar(p.initials, p.color, size = 26) }
                    }
                }
            }
            Box(Modifier.weight(1f), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
                // Reply icon revealed behind the message as it is dragged.
                val progress = (offset.value / triggerPx).coerceIn(0f, 1f)
                Box(
                    Modifier.align(Alignment.CenterStart).padding(start = 4.dp).size(30.dp).alpha(progress)
                        .clip(CircleShape).background(if (progress >= 1f) colors.accent else colors.track),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Reply, contentDescription = null,
                        tint = if (progress >= 1f) colors.onAccent else colors.muted, modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    Modifier
                        .offset { IntOffset(offset.value.roundToInt(), 0) }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var horizontal = false
                                // Only a mostly-horizontal drag to the right counts; otherwise the list scrolls.
                                val start = awaitTouchSlopOrCancellation(down.id) { change, over ->
                                    if (over.x > 0f && abs(over.x) > abs(over.y) * 1.5f) {
                                        horizontal = true
                                        change.consume()
                                    }
                                }
                                if (start == null || !horizontal) return@awaitEachGesture
                                var armed = false
                                horizontalDrag(start.id) { change ->
                                    val next = (offset.value + change.positionChange().x).coerceIn(0f, maxPx)
                                    scope.launch { offset.snapTo(next) }
                                    if (!armed && next >= triggerPx) {
                                        armed = true
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } else if (armed && next < triggerPx) armed = false
                                    change.consume()
                                }
                                if (offset.value >= triggerPx) currentOnReply()
                                scope.launch {
                                    if (reduceMotion) offset.snapTo(0f)
                                    else offset.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium))
                                }
                            }
                        }
                        .pointerInput(onOpen) {
                            detectTapGestures(
                                onLongPress = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuOpen = true
                                },
                                onTap = onOpen?.let { open -> { _ -> open() } }
                            )
                        }
                        .semantics { customActions = actions }
                ) {
                    when (m.kind) {
                        MessageKind.TEXT -> Bubble(m, lastOfRun)
                        MessageKind.PHOTO -> PhotoMessage(m)
                        MessageKind.SHARED_NOTE -> SharedNoteCard(m.sharedNote, onPerson)
                        MessageKind.EVENT_INVITE -> EventInviteCard(m.event)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(copyLabel) }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                            onClick = { menuOpen = false; onCopy() }
                        )
                        DropdownMenuItem(
                            text = { Text(deleteLabel) }, leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
                            onClick = { menuOpen = false; onDeleteForMe() }
                        )
                        if (onUnsend != null) {
                            DropdownMenuItem(
                                text = { Text(unsendLabel) }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Undo, null) },
                                onClick = { menuOpen = false; onUnsend() }
                            )
                        }
                    }
                }
            }
        }
        if (lastOfRun) {
            val time = clockTime(m.createdAt)
            val status = when {
                !mine -> time
                m.seenAt != null -> stringResource(R.string.chat_seen, clockTime(m.seenAt))
                else -> stringResource(R.string.chat_sent, time)
            }
            Text(
                status, color = colors.muted, fontSize = 10.5.sp,
                modifier = Modifier.padding(start = if (showLeadingAvatar) 40.dp else 6.dp, end = 6.dp, top = 3.dp, bottom = 3.dp)
            )
        }
    }
}

@Composable
private fun Bubble(m: MessageView, lastOfRun: Boolean) {
    val colors = CircleTheme.colors
    val mine = m.mine
    val small = if (lastOfRun) 5.dp else 18.dp
    val shape = if (mine) RoundedCornerShape(18.dp, 18.dp, small, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, small)
    val fg = if (mine) colors.onAccent else colors.text
    Column(
        Modifier
            .widthIn(max = BubbleMaxWidth)
            .clip(shape)
            .background(if (mine) colors.accent else colors.surface)
            .then(if (mine) Modifier else Modifier.border(1.dp, colors.border, shape))
            .padding(horizontal = 13.dp, vertical = 9.dp)
    ) {
        m.replyTo?.let { quoted -> QuotedSnippet(quoted, mine, fg) }
        Text(m.text, color = fg, fontSize = 14.5.sp, lineHeight = 20.3.sp)
    }
}

/** A message of yours still on its way, or one that failed (tap to send it again). */
@Composable
private fun PendingRow(p: PendingMessage, onRetry: () -> Unit) {
    val colors = CircleTheme.colors
    val retryLabel = stringResource(R.string.chat_not_sent)
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp)
            .then(if (p.failed) Modifier.clickable(onClickLabel = retryLabel, onClick = onRetry) else Modifier),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            p.text, color = colors.onAccent, fontSize = 14.5.sp, lineHeight = 20.3.sp,
            modifier = Modifier.widthIn(max = BubbleMaxWidth).clip(RoundedCornerShape(18.dp, 18.dp, 5.dp, 18.dp))
                .background(colors.accent.copy(alpha = if (p.failed) 0.45f else 0.7f)).padding(horizontal = 13.dp, vertical = 9.dp)
        )
        Text(
            if (p.failed) retryLabel else stringResource(R.string.chat_sending),
            color = if (p.failed) colors.alertDot else colors.muted, fontSize = 11.5.sp,
            fontWeight = if (p.failed) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(top = 3.dp, end = 4.dp)
        )
    }
}

/** Small quoted snippet of the message this one replies to. */
@Composable
private fun QuotedSnippet(quoted: ReplySnippet, mine: Boolean, fg: androidx.compose.ui.graphics.Color) {
    val colors = CircleTheme.colors
    Row(
        Modifier.padding(bottom = 6.dp).clip(RoundedCornerShape(8.dp))
            .background((if (mine) colors.onAccent else colors.text).copy(alpha = 0.08f))
            .height(IntrinsicSize.Min)
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(if (mine) colors.onAccent else colors.accent))
        Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
            Text(if (quoted.mine) stringResource(R.string.chat_you) else quoted.senderName, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                summary(quoted.kind, quoted.text),
                color = fg.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * A note shared into the chat, exactly as the repository returned it. Full (you may open it):
 * the text and "Open the thread". Snippet (not friends with the author): the author's name, at
 * most 90 characters, no photo, a lock and "Not friends yet"; tapping opens the gate sheet.
 */
@Composable
private fun SharedNoteCard(view: NoteView?, onPerson: (String) -> Unit) {
    val colors = CircleTheme.colors
    Column(
        Modifier.widthIn(max = BubbleMaxWidth).fillMaxWidth(0.82f)
            .clip(RoundedCornerShape(16.dp)).background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp)).padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.chat_shared_note_caps), color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp))
        }
        when (view) {
            is NoteView.Full -> {
                val note = view.note
                AuthorLine(note.author.name, note.author.id, onPerson) { Avatar(note.author.initials, note.author.color, size = 22) }
                Text(note.text, color = colors.body, fontSize = 13.5.sp, lineHeight = 19.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (note.hasMedia) {
                    Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(70.dp).clip(RoundedCornerShape(10.dp)).background(colors.track))
                }
                Text(stringResource(R.string.chat_open_thread), color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }
            is NoteView.Snippet -> {
                AuthorLine(view.authorName, view.authorId, onPerson, avatar = null)
                Text(view.text, color = colors.body, fontSize = 13.5.sp, lineHeight = 19.sp)
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.muted, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.not_friends_yet), color = colors.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            else -> Text(stringResource(R.string.note_unavailable), color = colors.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun AuthorLine(name: String, personId: String, onPerson: (String) -> Unit, avatar: (@Composable () -> Unit)?) {
    Row(
        Modifier.padding(top = 4.dp).clip(RoundedCornerShape(8.dp))
            .clickable(onClickLabel = stringResource(R.string.open_profile_of, name)) { onPerson(personId) }
            .heightIn(min = 36.dp).padding(end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (avatar != null) { avatar(); Spacer(Modifier.width(7.dp)) }
        Text(name, color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

/** Photos sent in a chat: 210 x 140, rounded 16. Tap opens the full-screen viewer. */
@Composable
private fun PhotoMessage(m: MessageView) {
    val first = m.media.firstOrNull()
    Box(Modifier.size(210.dp, 140.dp).clip(RoundedCornerShape(16.dp)).background(CircleTheme.colors.track)) {
        if (first != null) com.circle.app.ui.components.MediaThumb(first)
        if (m.media.size > 1) {
            Text(
                "+${m.media.size - 1}", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).clip(RoundedCornerShape(999.dp))
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)).padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

/** An event invite. Tap opens the event (Back returns here). */
@Composable
private fun EventInviteCard(event: EventCardView?) {
    val colors = CircleTheme.colors
    Row(
        Modifier.widthIn(max = BubbleMaxWidth).fillMaxWidth(0.82f)
            .clip(RoundedCornerShape(16.dp)).background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (event == null) {
            Text(stringResource(R.string.chat_event_unavailable), color = colors.muted, fontSize = 13.sp)
            return@Row
        }
        Column(
            Modifier.size(44.dp).clip(CircleShape).background(colors.background).border(1.5.dp, colors.accent, CircleShape),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Text(eventDay(event.startsAt), color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 14.sp)
            Text(eventMonth(event.startsAt), color = colors.accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, lineHeight = 9.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.chat_event_invite_caps), color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Text(event.title, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
            Text(eventWhen(event.startsAt), color = colors.muted, fontSize = 12.sp)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ReplyStrip(name: String, snippet: String, onClose: () -> Unit) {
    val colors = CircleTheme.colors
    Row(
        Modifier.fillMaxWidth().background(colors.surface).height(IntrinsicSize.Min).padding(start = 12.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(colors.accent))
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(stringResource(R.string.chat_replying_to, name), color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(snippet, color = colors.muted, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.chat_cancel_reply), tint = colors.muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun Composer(text: String, onChange: (String) -> Unit, onSend: () -> Unit, onPlus: () -> Unit) {
    val colors = CircleTheme.colors
    val canSend = text.isNotBlank()
    val hint = stringResource(R.string.chat_message_hint)
    val sendLabel = stringResource(R.string.chat_send)
    Row(
        Modifier.fillMaxWidth().background(colors.surface).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).border(1.5.dp, colors.border, CircleShape).clickable(onClick = onPlus),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.chat_plus), tint = colors.text) }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.weight(1f).heightIn(min = 42.dp).clip(RoundedCornerShape(21.dp)).background(colors.background)
                .border(1.dp, colors.border, RoundedCornerShape(21.dp)).padding(horizontal = 15.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (text.isEmpty()) Text(hint, color = colors.muted, fontSize = 14.5.sp)
            BasicTextField(
                value = text, onValueChange = onChange, maxLines = 4,
                textStyle = TextStyle(color = colors.text, fontSize = 14.5.sp, lineHeight = 20.sp, fontFamily = Body),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint }
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(if (canSend) colors.accent else colors.track)
                .clickable(enabled = canSend, onClick = onSend)
                .semantics { contentDescription = sendLabel; if (!canSend) disabled() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.Send, contentDescription = null,
                tint = if (canSend) colors.onAccent else colors.muted, modifier = Modifier.size(18.dp)
            )
        }
    }
}
