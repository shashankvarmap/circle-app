package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.circle.app.R
import com.circle.app.data.repo.ChatKind
import com.circle.app.data.repo.ChatRowView
import com.circle.app.data.repo.CircleRepository
import com.circle.app.ui.format.clockTime
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.navigation.Routes
import com.circle.app.ui.components.*
import com.circle.app.ui.theme.*

private enum class MTab { DIRECT, GROUP }

class MessagesViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    val chats = repo.chats().state(emptyList())
}

@Composable
fun MessagesScreen(nav: NavHostController) {
    var tab by rememberSaveable { mutableStateOf(MTab.DIRECT) }
    val vm = circleViewModel { MessagesViewModel(it) }
    val chats by vm.chats.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val header = rememberCollapsingHeaderState { listState.canScrollForward || listState.canScrollBackward }

    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
        CollapsingHeaderLayout(
            state = header,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            header = {
                TabbedScreenHeader(
                    title = "Messages",
                    tabs = listOf("Messages", "Group Messages"),
                    selected = tab.ordinal,
                    onSelect = { tab = MTab.entries[it] },
                    action = {
                        // New chat arrives with the messages stage; shown dimmed until then.
                        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Add, contentDescription = "New chat: coming with the messages update",
                                tint = CircleTheme.colors.muted, modifier = Modifier.alpha(0.5f)
                            )
                        }
                    }
                )
            }
        ) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                if (tab == MTab.DIRECT) {
                    items(chats.filter { it.kind != ChatKind.GROUP }, key = { it.id }) { c -> ChatRow(c) { nav.navigate(Routes.chat(c.id)) } }
                } else {
                    item {
                        Text(
                            "Only chats tied to a group you've joined show up here. Chats you start with friends live under Messages instead.",
                            color = CircleTheme.colors.muted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                    val groups = chats.filter { it.kind == ChatKind.GROUP }
                    if (groups.isEmpty()) {
                        item { Text("No group chats yet. They appear when you join a group.", color = CircleTheme.colors.muted, fontSize = 13.sp) }
                    }
                    items(groups, key = { it.id }) { c -> ChatRow(c) { nav.navigate(Routes.chat(c.id)) } }
                }
            }
        }
        CircleBottomNav(nav, NavTab.MESSAGES)
    }
}

/** A chat in the list: avatar (circle for people, rounded square for groups), name, preview, time, unread dot. */
@Composable
private fun ChatRow(chat: ChatRowView, onOpen: () -> Unit) {
    val colors = CircleTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClickLabel = "Open chat", onClick = onOpen).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            chat.group != null -> GroupTile(chat.group.initial, chat.group.color, size = 44)
            chat.person != null -> Avatar(chat.person.initials, chat.person.color, size = 44)
            else -> Box(modifier = Modifier.width(44.dp)) {
                chat.members.getOrNull(0)?.let { Avatar(it.initials, it.color, size = 32) }
                chat.members.getOrNull(1)?.let { Box(modifier = Modifier.offset(x = 20.dp)) { Avatar(it.initials, it.color, size = 32) } }
            }
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                chat.title, color = colors.text,
                fontWeight = if (chat.unread) FontWeight.ExtraBold else FontWeight.Bold, fontSize = 13.5.sp
            )
            val p = chat.preview
            val preview = when {
                p == null -> stringResource(R.string.messages_no_messages)
                p.mine -> stringResource(R.string.messages_preview_you, summary(p.kind, p.text))
                p.senderFirstName != null -> stringResource(R.string.messages_preview_named, p.senderFirstName, summary(p.kind, p.text))
                else -> summary(p.kind, p.text)
            }
            Text(
                preview, color = if (chat.unread) colors.text else colors.body,
                fontWeight = if (chat.unread) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 12.5.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(chat.lastAt?.let { clockTime(it) } ?: "", color = if (chat.unread) colors.accent else colors.muted, fontSize = 11.sp)
            if (chat.unread) {
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.size(8.dp).background(colors.accent, androidx.compose.foundation.shape.CircleShape).semantics { contentDescription = "Unread" })
            }
        }
    }
}
