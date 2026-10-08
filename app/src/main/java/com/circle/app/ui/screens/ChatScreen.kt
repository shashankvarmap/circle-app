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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Undo
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
import androidx.compose.ui.graphics.Color
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
import androidx.navigation.NavHostController
import com.circle.app.data.*
import com.circle.app.navigation.Routes
import com.circle.app.ui.a11y.rememberReduceMotion
import com.circle.app.ui.components.Avatar
import com.circle.app.ui.components.GroupTile
import com.circle.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val SwipeMax = 72.dp       // the bubble follows the finger up to here
private val SwipeTrigger = 44.dp   // releasing past this starts a reply
private val BubbleMaxWidth = 276.dp

/** One row of the conversation, oldest first. */
private sealed interface ChatEntry {
    val key: String
    data class Day(val label: String) : ChatEntry { override val key get() = "day-$label" }
    data class Msg(val m: ChatMessage, val firstOfRun: Boolean, val lastOfRun: Boolean) : ChatEntry { override val key get() = m.id }
}

private fun entries(messages: List<ChatMessage>): List<ChatEntry> {
    val out = mutableListOf<ChatEntry>()
    messages.forEachIndexed { i, m ->
        val prev = messages.getOrNull(i - 1)
        val next = messages.getOrNull(i + 1)
        if (prev == null || prev.day != m.day) out += ChatEntry.Day(m.day)
        val first = prev == null || prev.senderId != m.senderId || prev.day != m.day
        val last = next == null || next.senderId != m.senderId || next.day != m.day
        out += ChatEntry.Msg(m, first, last)
    }
    return out
}

/**
 * A conversation. Newest at the bottom. Tapping a text message does nothing; swipe right on any
 * message to reply; hold (~400ms) for Copy / Delete for me / Unsend (yours only). Tapping a
 * shared note opens its thread, tapping an event invite opens the event.
 */
@Composable
fun ChatScreen(nav: NavHostController, chatId: String) {
    val colors = CircleTheme.colors
    val chat = CircleStore.chat(chatId)
    if (chat == null) {
        Column(Modifier.fillMaxSize().background(colors.background)) {
            DetailHeader("Chat") { nav.popBackStack() }
            Text("This chat isn't available.", color = colors.muted, modifier = Modifier.padding(24.dp))
        }
        return
    }
    LaunchedEffect(chat.id) { CircleStore.markRead(chat) }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var draft by rememberSaveable { mutableStateOf("") }
    var replyToId by rememberSaveable { mutableStateOf<String?>(null) }

    val messages = CircleStore.visibleMessages(chat)
    val rows = entries(messages).asReversed() // reverse layout: index 0 is the newest
    val replyTo = replyToId?.let { id -> messages.firstOrNull { it.id == id } }
    val isGroupLike = chat.directPersonId == null

    fun senderName(id: String) = if (id == ME) "You" else CircleStore.person(id)?.firstName ?: "Someone"

    fun send() {
        if (CircleStore.sendMessage(chat, draft, replyTo?.id) != null) {
            draft = ""; replyToId = null
            scope.launch { listState.animateScrollToItem(0) }
        }
    }

    val baseConfig = LocalViewConfiguration.current
    val chatConfig = remember(baseConfig) {
        object : ViewConfiguration by baseConfig {
            override val longPressTimeoutMillis: Long get() = 400L
        }
    }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        ChatTopBar(nav, chat)

        CompositionLocalProvider(LocalViewConfiguration provides chatConfig) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
            ) {
                items(rows, key = { it.key }) { entry ->
                    when (entry) {
                        is ChatEntry.Day -> DayPill(entry.label)
                        is ChatEntry.Msg -> {
                            val m = entry.m
                            val quoted = m.replyToId?.let { CircleStore.message(chat, it) }
                            MessageRow(
                                m = m,
                                mine = m.senderId == ME,
                                firstOfRun = entry.firstOfRun,
                                lastOfRun = entry.lastOfRun,
                                senderLabel = if (isGroupLike && m.senderId != ME && entry.firstOfRun) senderName(m.senderId) else null,
                                quoted = quoted?.takeIf { it.id !in CircleStore.hiddenMessageIds },
                                quotedName = quoted?.let { senderName(it.senderId) },
                                onReply = { replyToId = m.id },
                                onCopy = {
                                    clipboard.setText(AnnotatedString(copyText(m)))
                                },
                                onDeleteForMe = {
                                    CircleStore.deleteForMe(m.id)
                                    if (replyToId == m.id) replyToId = null
                                    Toast.makeText(context, "Removed from your chat only", Toast.LENGTH_SHORT).show()
                                },
                                onUnsend = if (m.senderId == ME) ({
                                    CircleStore.unsend(chat, m.id)
                                    if (replyToId == m.id) replyToId = null
                                }) else null,
                                onOpen = when (m.kind) {
                                    MessageKind.TEXT -> null
                                    MessageKind.SHARED_NOTE -> m.postId?.let { id -> { nav.navigate(Routes.thread(id)) } }
                                    MessageKind.EVENT_INVITE -> m.eventId?.let { id -> { nav.navigate(Routes.eventDetail(id)) } }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (replyTo != null) ReplyStrip(senderName(replyTo.senderId), CircleStore.messageSummary(replyTo)) { replyToId = null }
        Composer(draft, onChange = { draft = it }, onSend = ::send)
    }
}

private fun copyText(m: ChatMessage): String = when (m.kind) {
    MessageKind.TEXT -> m.text
    MessageKind.SHARED_NOTE -> m.postId?.let { CircleStore.post(it) }?.let { "${it.author}: ${it.text}" } ?: "Shared a note"
    MessageKind.EVENT_INVITE -> m.eventId?.let { id -> CircleStore.events.firstOrNull { it.id == id } }
        ?.let { "${it.title}, ${it.whenLabel}, ${it.venue}" } ?: "Event invite"
}

@Composable
private fun ChatTopBar(nav: NavHostController, chat: Chat) {
    val colors = CircleTheme.colors
    val title = CircleStore.chatTitle(chat)
    val group = chat.groupId?.let { CircleStore.groupOrNull(it) }
    val person = chat.directPersonId?.let { CircleStore.person(it) }
    val subline = when {
        group != null -> "${group.memberCount} members"
        person != null -> "Friends since ${person.sinceYear}"
        else -> "You and ${chat.memberIds.size} friends"
    }
    Column {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.popBackStack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.text)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .then(if (group != null) Modifier.clickable(onClickLabel = "Open group") { nav.navigate(Routes.groupDetail(group.id)) } else Modifier)
                    .padding(4.dp)
            ) {
                when {
                    group != null -> GroupTile(group.initial, group.color, size = 40)
                    person != null -> Avatar(person.initials, person.color, size = 40)
                    else -> Avatar(title.take(1).uppercase(), AvatarTeal, size = 40)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 15.5.sp, maxLines = 1, modifier = Modifier.semantics { heading() })
                    Text(subline, color = colors.muted, fontSize = 11.5.sp, maxLines = 1)
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
    m: ChatMessage,
    mine: Boolean,
    firstOfRun: Boolean,
    lastOfRun: Boolean,
    senderLabel: String?,
    quoted: ChatMessage?,
    quotedName: String?,
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

    val actions = buildList {
        add(CustomAccessibilityAction("Reply") { onReply(); true })
        add(CustomAccessibilityAction("Copy") { onCopy(); true })
        add(CustomAccessibilityAction("Delete for me") { onDeleteForMe(); true })
        if (onUnsend != null) add(CustomAccessibilityAction("Unsend") { onUnsend(); true })
    }

    Column(
        Modifier.fillMaxWidth().padding(top = if (firstOfRun) 10.dp else 3.dp),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start
    ) {
        if (senderLabel != null) {
            Text(senderLabel, color = colors.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp, bottom = 3.dp))
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
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
                    MessageKind.TEXT -> Bubble(m, mine, lastOfRun, quoted, quotedName)
                    MessageKind.SHARED_NOTE -> SharedNoteCard(m.postId?.let { CircleStore.post(it) })
                    MessageKind.EVENT_INVITE -> EventInviteCard(m.eventId?.let { id -> CircleStore.events.firstOrNull { it.id == id } })
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Copy") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                        onClick = { menuOpen = false; onCopy() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete for me") }, leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
                        onClick = { menuOpen = false; onDeleteForMe() }
                    )
                    if (onUnsend != null) {
                        DropdownMenuItem(
                            text = { Text("Unsend") }, leadingIcon = { Icon(Icons.Outlined.Undo, null) },
                            onClick = { menuOpen = false; onUnsend() }
                        )
                    }
                }
            }
        }
        if (lastOfRun) {
            val status = when {
                !mine -> m.time
                m.seenAt != null -> "Seen ${m.seenAt}"
                else -> "${m.time} · Sent"
            }
            Text(status, color = colors.muted, fontSize = 10.5.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
        }
    }
}

@Composable
private fun Bubble(m: ChatMessage, mine: Boolean, lastOfRun: Boolean, quoted: ChatMessage?, quotedName: String?) {
    val colors = CircleTheme.colors
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
        if (m.replyToId != null) {
            // Small quoted snippet of the message this one replies to.
            Row(
                Modifier.padding(bottom = 6.dp).clip(RoundedCornerShape(8.dp))
                    .background((if (mine) colors.onAccent else colors.text).copy(alpha = 0.08f))
                    .height(IntrinsicSize.Min)
            ) {
                Box(Modifier.width(3.dp).fillMaxHeight().background(if (mine) colors.onAccent else colors.accent))
                Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Text(quotedName ?: "", color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        quoted?.let { CircleStore.messageSummary(it) } ?: "Message unavailable",
                        color = fg.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Text(m.text, color = fg, fontSize = 14.5.sp, lineHeight = 20.3.sp)
    }
}

/** A post shared into the chat. Tap opens its thread (Back returns here). */
@Composable
private fun SharedNoteCard(post: Post?) {
    val colors = CircleTheme.colors
    Column(
        Modifier.widthIn(max = BubbleMaxWidth).fillMaxWidth(0.82f)
            .clip(RoundedCornerShape(16.dp)).background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp)).padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("SHARED NOTE", color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp))
        }
        if (post == null) {
            Text("This note is no longer available.", color = colors.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            return@Column
        }
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(post.initials, post.color, size = 22)
            Spacer(Modifier.width(7.dp))
            Text(post.author, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Text(post.text, color = colors.body, fontSize = 13.5.sp, lineHeight = 19.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        Text("Open the thread", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
    }
}

/** An event invite. Tap opens the event (Back returns here). */
@Composable
private fun EventInviteCard(event: EventInfo?) {
    val colors = CircleTheme.colors
    Row(
        Modifier.widthIn(max = BubbleMaxWidth).fillMaxWidth(0.82f)
            .clip(RoundedCornerShape(16.dp)).background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (event == null) {
            Text("This event is no longer available.", color = colors.muted, fontSize = 13.sp)
            return@Row
        }
        Column(
            Modifier.size(44.dp).clip(CircleShape).background(colors.background).border(1.5.dp, colors.accent, CircleShape),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Text(event.day, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 14.sp)
            Text(event.month, color = colors.accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, lineHeight = 9.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("EVENT INVITE", color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Text(event.title, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
            Text(event.whenLabel, color = colors.muted, fontSize = 12.sp)
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
            Text("Replying to $name", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(snippet, color = colors.muted, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Close, contentDescription = "Cancel reply", tint = colors.muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun Composer(text: String, onChange: (String) -> Unit, onSend: () -> Unit) {
    val colors = CircleTheme.colors
    val canSend = text.isNotBlank()
    Row(
        Modifier.fillMaxWidth().background(colors.surface).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            Modifier.weight(1f).heightIn(min = 42.dp).clip(RoundedCornerShape(21.dp)).background(colors.background)
                .border(1.dp, colors.border, RoundedCornerShape(21.dp)).padding(horizontal = 15.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (text.isEmpty()) Text("Message", color = colors.muted, fontSize = 14.5.sp)
            BasicTextField(
                value = text, onValueChange = onChange, maxLines = 4,
                textStyle = TextStyle(color = colors.text, fontSize = 14.5.sp, lineHeight = 20.sp, fontFamily = Body),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Message" }
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(if (canSend) colors.accent else colors.track)
                .clickable(enabled = canSend, onClick = onSend)
                .semantics { contentDescription = "Send"; if (!canSend) disabled() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.Send, contentDescription = null,
                tint = if (canSend) colors.onAccent else colors.muted, modifier = Modifier.size(18.dp)
            )
        }
    }
}
