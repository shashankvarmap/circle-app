package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.circle.app.data.repo.CircleRepository
import com.circle.app.ui.format.*
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import com.circle.app.navigation.openPerson
import androidx.compose.runtime.saveable.rememberSaveable
import com.circle.app.ui.components.*
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.circle.app.R
import com.circle.app.data.repo.Outcome
import com.circle.app.ui.theme.*

class GroupViewModel(repo: CircleRepository, private val groupId: String) : RepoViewModel(repo) {
    /** Null while loading or when the group isn't visible to you. */
    val group = repo.group(groupId).state(null)
    /** Empty unless you're a member: group posts are by membership, never friendship. */
    val posts = repo.groupPosts(groupId).state(emptyList())
    fun leave() = run({ leaveGroup(groupId) })
    fun approve(personId: String) = run({ approveJoinRequest(groupId, personId) })
    fun decline(personId: String) = run({ declineJoinRequest(groupId, personId) })
    fun remove(personId: String) = run({ removeMember(groupId, personId) })
    fun edit(name: String, description: String, isPublic: Boolean) = run({ editGroup(groupId, name, description, isPublic) })
    fun invite(ids: List<String>, then: (Outcome<Int>) -> Unit) = run({ inviteToGroup(groupId, ids) }, then)
    /** A temporary group the server deleted (it and its chat, for everyone). */
    val gone = repo.wasDeleted(groupId).state(false)
    fun extend(days: Int, then: (Outcome<Unit>) -> Unit) = run({ extendLifetime(groupId, days) }, then)
    fun makePermanent(then: (Outcome<Unit>) -> Unit) = run({ makePermanent(groupId) }, then)
    fun endNow(then: (Outcome<Unit>) -> Unit) = run({ endNow(groupId) }, then)
}

private enum class GroupDialog { LEAVE, EDIT, INVITE, REPORT, REMOVE }

/**
 * Group detail. Outsiders see "Request to join", which stays pending: never instant.
 */
@Composable
fun GroupScreen(nav: NavHostController, groupId: String?) {
    val vm = circleViewModel(key = "group-$groupId") { GroupViewModel(it, groupId.orEmpty()) }
    val loaded by vm.group.collectAsStateWithLifecycle()
    val groupPosts by vm.posts.collectAsStateWithLifecycle()
    val gone by vm.gone.collectAsStateWithLifecycle()
    if (loaded == null && gone) {
        com.circle.app.ui.components.DeletedTimedState(group = true, onBack = {
            nav.navigate(com.circle.app.navigation.Routes.MESSAGES) { popUpTo(com.circle.app.navigation.Routes.MESSAGES) { inclusive = true }; launchSingleTop = true }
        })
        return
    }
    val group = loaded ?: run {
        Column(Modifier.fillMaxSize().background(CircleTheme.colors.background)) { DetailHeader(androidx.compose.ui.res.stringResource(com.circle.app.R.string.group_title)) { nav.popBackStack() } }
        return
    }
    val isMember = group.isMember
    var tab by rememberSaveable { mutableStateOf(0) } // 0 Posts, 1 People
    val screenReader = com.circle.app.ui.a11y.rememberScreenReaderOn()
    val fits = com.circle.app.ui.orbit.rememberOrbitFits()
    var showMapChoice by rememberSaveable { mutableStateOf(!screenReader) }
    val showMap = showMapChoice && fits
    var dialog by rememberSaveable { mutableStateOf<GroupDialog?>(null) }
    var removing by rememberSaveable { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val life = group.lifetime
    var extending by rememberSaveable { mutableStateOf(false) }
    fun toast(text: String) = android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show()
    var saving by rememberSaveable { mutableStateOf(false) }
    val savedToast = stringResource(R.string.save_done_toast)
    if (saving) com.circle.app.ui.components.SaveCopySheet(group.id, onDismiss = { saving = false }, onSaved = { toast(savedToast) })
    if (extending && life != null) {
        val permanentDone = stringResource(R.string.extend_permanent_done_group)
        val endDone = stringResource(R.string.extend_end_done)
        com.circle.app.ui.components.ExtendSheet(
            canEndNow = life.state != com.circle.app.data.lifetime.LifeState.WRAP_UP,
            onDismiss = { extending = false },
            onExtend = { d -> extending = false; vm.extend(d) { if (it is Outcome.Success) toast(context.resources.getQuantityString(R.plurals.extend_done, d, d)) } },
            onPermanent = { extending = false; vm.makePermanent { if (it is Outcome.Success) toast(permanentDone) } },
            onEndNow = { extending = false; vm.endNow { if (it is Outcome.Success) toast(endDone) } },
        )
    }

    when (dialog) {
        GroupDialog.LEAVE -> ConfirmDialog(
            stringResource(R.string.group_leave_title, group.name), stringResource(R.string.group_leave_body), stringResource(R.string.group_leave),
            destructive = true, onDismiss = { dialog = null }
        ) { vm.leave() }
        GroupDialog.REMOVE -> ConfirmDialog(
            stringResource(R.string.group_remove_title), stringResource(R.string.group_remove_body), stringResource(R.string.group_remove),
            destructive = true, onDismiss = { dialog = null; removing = null }
        ) { removing?.let { vm.remove(it) } }
        GroupDialog.EDIT -> {
            val name = remember { mutableStateOf(group.name) }
            val description = remember { mutableStateOf(group.description) }
            var isPublic by remember { mutableStateOf(group.isPublic) }
            EditFieldsDialog(
                stringResource(R.string.group_edit),
                listOf(stringResource(R.string.group_field_name) to name, stringResource(R.string.group_field_description) to description),
                extra = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.group_field_public), color = CircleTheme.colors.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        androidx.compose.material3.Switch(isPublic, { isPublic = it })
                    }
                },
                onDismiss = { dialog = null }
            ) { vm.edit(name.value, description.value, isPublic) }
        }
        GroupDialog.INVITE -> FriendPickerSheet(stringResource(R.string.group_invite), stringResource(R.string.group_invite_send), onDismiss = { dialog = null }) { ids ->
            vm.invite(ids) { if (it is Outcome.Success) android.widget.Toast.makeText(context, context.resources.getQuantityString(R.plurals.invites_sent, it.value, it.value), android.widget.Toast.LENGTH_SHORT).show() }
        }
        GroupDialog.REPORT -> ReportSheet(com.circle.app.data.ReportKind.GROUP, group.id, onDone = { dialog = null })
        null -> Unit
    }

    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
        Row(modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable { nav.popBackStack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back), tint = CircleTheme.colors.body)
            }
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.group_title), color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Box {
                Box(Modifier.size(44.dp).clip(CircleShape).clickable { menu = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.group_options), tint = CircleTheme.colors.text)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (life?.canSaveCopy == true) DropdownMenuItem(text = { Text(stringResource(R.string.timed_save_copy)) }, onClick = { menu = false; saving = true })
                    if (life?.canChange == true) DropdownMenuItem(text = { Text(stringResource(R.string.timed_menu_extend)) }, onClick = { menu = false; extending = true })
                    if (group.iAmHost && group.isOpen) DropdownMenuItem(text = { Text(stringResource(R.string.group_edit)) }, onClick = { menu = false; dialog = GroupDialog.EDIT })
                    if (isMember && group.isOpen) DropdownMenuItem(text = { Text(stringResource(R.string.group_invite)) }, onClick = { menu = false; dialog = GroupDialog.INVITE })
                    if (isMember) DropdownMenuItem(text = { Text(stringResource(R.string.group_leave)) }, onClick = { menu = false; dialog = GroupDialog.LEAVE })
                    if (!group.iAmHost) DropdownMenuItem(text = { Text(stringResource(R.string.group_report)) }, onClick = { menu = false; dialog = GroupDialog.REPORT })
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GroupTile(group.initial, group.color, size = 64)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(group.name, color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(group.category, color = CircleTheme.colors.muted, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (group.isPublic) {
                                Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.exploresearch_public), color = CircleTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            } else {
                                Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.group_private), color = CircleTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                            Spacer(Modifier.width(7.dp))
                            Text(androidx.compose.ui.res.pluralStringResource(com.circle.app.R.plurals.members_count, group.memberCount, group.memberCount), color = CircleTheme.colors.muted, fontSize = 11.5.sp)
                        }
                    }
                }
            }
            if (life != null) item {
                // "Ends Thu 22 Oct" under the name; "Ended … · read-only" in wrap-up.
                val ended = life.state == com.circle.app.data.lifetime.LifeState.WRAP_UP
                com.circle.app.ui.components.TimerChip(
                    life,
                    label = stringResource(if (ended) R.string.group_ended_chip else R.string.group_ends_chip, com.circle.app.ui.components.lastDay(life.endsAt))
                )
            }
            if (life != null && isMember && group.chatId != null) item {
                GroupChatRow { nav.navigate(com.circle.app.navigation.Routes.chat(group.chatId)) }
            }
            if (life != null && isMember) item {
                com.circle.app.ui.components.LifetimeBanner(
                    life, isGroupChat = true, onSave = { saving = true }, onExtend = { extending = true },
                    modifier = Modifier.padding(horizontal = 0.dp), onGroupScreen = true
                )
            }
            item {
                if (group.description.isNotEmpty()) Text(group.description, color = CircleTheme.colors.body, fontSize = 13.sp)
            }
            item {
                if (isMember) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).border(1.dp, CircleTheme.colors.border, RoundedCornerShape(999.dp)).padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.common_member), color = CircleTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.5.sp) }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.group_leave), color = CircleTheme.colors.muted, fontSize = 12.sp,
                            modifier = Modifier.weight(1f, fill = false).clip(RoundedCornerShape(8.dp)).clickable { dialog = GroupDialog.LEAVE }.padding(12.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (group.isPublic && group.isOpen) {
                            JoinRequestButton(group, modifier = Modifier.fillMaxWidth(), onTemporary = { nav.navigate(com.circle.app.navigation.Routes.joinGroup(group.ref.id)) })
                            Text(
                                if (group.joinRequested) androidx.compose.ui.res.stringResource(com.circle.app.R.string.group_request_sent_note)
                                else androidx.compose.ui.res.stringResource(com.circle.app.R.string.group_groups_default_to_private_this),
                                color = CircleTheme.colors.muted, fontSize = 11.sp
                            )
                        } else {
                            Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.group_this_group_is_private_members), color = CircleTheme.colors.muted, fontSize = 11.sp)
                        }
                    }
                }
            }
            if (isMember) {
                item {
                    val stack = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
                    if (stack) Column(Modifier.fillMaxWidth()) {
                        UnderlineTabs(listOf(androidx.compose.ui.res.stringResource(com.circle.app.R.string.group_posts), androidx.compose.ui.res.stringResource(com.circle.app.R.string.group_people)), tab) { tab = it }
                        if (tab == 1 && fits) com.circle.app.ui.orbit.MapListToggle(showMap, { showMapChoice = it }, Modifier.align(Alignment.End).padding(top = 8.dp))
                    } else Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { UnderlineTabs(listOf(androidx.compose.ui.res.stringResource(com.circle.app.R.string.group_posts), androidx.compose.ui.res.stringResource(com.circle.app.R.string.group_people)), tab) { tab = it } }
                        if (tab == 1 && fits) com.circle.app.ui.orbit.MapListToggle(showMap, { showMapChoice = it })
                    }
                }
            }
            if (isMember && tab == 1) {
                item {
                    GroupPeople(
                        group, showMap, onPerson = { nav.openPerson(it) },
                        host = if (group.iAmHost) HostActions(
                            approve = vm::approve, decline = vm::decline,
                            remove = { removing = it; dialog = GroupDialog.REMOVE }
                        ) else null
                    )
                }
            } else if (isMember) {
                if (group.isOpen) item { ComposeToGroupRow(nav, group.id) }
                else item { com.circle.app.ui.components.ReadOnlyBar(group = true) }

                if (groupPosts.isEmpty()) {
                    item { Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.group_nothing_posted_here_yet_be), color = CircleTheme.colors.muted, fontSize = 13.sp) }
                }
                items(groupPosts, key = { it.id }) { post -> GroupPostCard(nav, post, showGroup = false) }
            } else {
                item { LockedPanel(androidx.compose.ui.res.stringResource(com.circle.app.R.string.group_join_to_see_what_s)) }
            }
        }
    }
}

@Composable
fun LockedPanel(text: String, footnote: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CircleTheme.colors.surface)
            .border(1.dp, CircleTheme.colors.border, RoundedCornerShape(14.dp))
            .padding(vertical = 22.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = CircleTheme.colors.muted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(8.dp))
        Text(text, color = CircleTheme.colors.body, fontSize = 12.5.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        footnote?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = CircleTheme.colors.muted, fontSize = 11.sp)
        }
    }
}

/** A temporary group's chat: "Group chat · Ends with the group". */
@Composable
private fun GroupChatRow(onOpen: () -> Unit) {
    val colors = CircleTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(colors.surface).border(1.dp, colors.border, shape)
            .clickable(onClick = onOpen).heightIn(min = 56.dp).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.group_chat_row), color = colors.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(stringResource(R.string.group_chat_ends), color = colors.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 1.dp))
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ComposeToGroupRow(nav: NavHostController, groupId: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CircleTheme.colors.surface)
            .clickable { nav.navigate(com.circle.app.navigation.Routes.compose(group = groupId)) }.padding(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MeAvatar(28)
        Spacer(Modifier.width(10.dp))
        Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.group_post_to_the_group), color = CircleTheme.colors.muted, fontSize = 13.sp)
    }
}

