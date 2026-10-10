package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.circle.app.R
import com.circle.app.data.AlertKind
import com.circle.app.data.repo.AlertView
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.RequestView
import com.circle.app.navigation.Routes
import com.circle.app.navigation.openPerson
import com.circle.app.ui.components.*
import com.circle.app.ui.format.relativeTime
import com.circle.app.ui.theme.*
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel

private enum class NTab { NOTIFICATIONS, ACTIVITY }

class AlertsViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    val requests = repo.incomingRequests().state(emptyList())
    val alerts = repo.alerts().load()
    val activity = repo.activity().state(emptyList())
    val hasUnread = repo.hasUnreadAlerts().state(false)
    fun accept(id: String) = run({ acceptRequest(id) })
    fun decline(id: String) = run({ declineRequest(id) })
    fun markAllRead() = run({ markAllAlertsRead() })
}

@Composable
fun NotificationsScreen(nav: NavHostController) {
    var tab by rememberSaveable { mutableStateOf(NTab.NOTIFICATIONS) }
    val vm = circleViewModel { AlertsViewModel(it) }
    val requests by vm.requests.collectAsStateWithLifecycle()
    val alertsLoad by vm.alerts.collectAsStateWithLifecycle()
    // The server doesn't know who saved a copy; this phone does, and hides "deleted tomorrow" for those (TIMED_GROUPS 7).
    val savedCopies by androidx.compose.runtime.remember { com.circle.app.AppGraph.savedCopies.saved() }.collectAsStateWithLifecycle(emptySet())
    val alerts = (alertsLoad as? com.circle.app.ui.vm.Load.Ready)?.value.orEmpty()
        .filterNot { it.kind == AlertKind.TIMED_DELETED_TOMORROW && it.timed?.id in savedCopies }
    val hasUnread by vm.hasUnread.collectAsStateWithLifecycle()
    val activity by vm.activity.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val header = rememberCollapsingHeaderState { listState.canScrollForward || listState.canScrollBackward }

    fun open(a: AlertView) {
        val timed = a.timed
        when {
            timed != null -> nav.navigate(timed.chatId?.let { Routes.chat(it) } ?: Routes.groupDetail(timed.id))
            a.postId != null -> nav.navigate(Routes.thread(a.postId))
            else -> nav.openPerson(a.actor.id)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
        OfflineBanner()
        CollapsingHeaderLayout(
            state = header,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            header = {
                TabbedScreenHeader(
                    title = stringResource(R.string.alerts_title),
                    tabs = listOf(stringResource(R.string.alerts_tab_notifications), stringResource(R.string.alerts_tab_activity)),
                    selected = tab.ordinal,
                    onSelect = { tab = NTab.entries[it] },
                    action = {
                        Text(
                            stringResource(R.string.alerts_mark_all_read),
                            color = if (hasUnread) CircleTheme.colors.accent else CircleTheme.colors.muted,
                            fontWeight = FontWeight.Bold, fontSize = 12.5.sp,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = hasUnread) { vm.markAllRead() }
                                .heightIn(min = 48.dp).padding(horizontal = 10.dp, vertical = 14.dp)
                        )
                    }
                )
            }
        ) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (tab == NTab.NOTIFICATIONS && alertsLoad is com.circle.app.ui.vm.Load.Loading) {
                    item(key = "loading") { SkeletonList() }
                } else if (tab == NTab.NOTIFICATIONS && alertsLoad is com.circle.app.ui.vm.Load.Failed) {
                    item(key = "error") { ErrorState(onRetry = vm::retry) }
                } else if (tab == NTab.NOTIFICATIONS && requests.isEmpty() && alerts.isEmpty()) {
                    item(key = "empty") { EmptyState(EmptyKind.ALERTS) }
                } else if (tab == NTab.NOTIFICATIONS) {
                    item { SectionLabel(stringResource(R.string.alerts_friend_requests_caps)) }
                    if (requests.isEmpty()) {
                        item { Text(stringResource(R.string.alerts_no_requests), color = CircleTheme.colors.muted, fontSize = 12.5.sp, modifier = Modifier.padding(vertical = 8.dp)) }
                    }
                    items(requests, key = { it.person.id }) { request ->
                        RequestRow(
                            request = request,
                            onOpen = { nav.openPerson(request.person.id) },
                            onAccept = { vm.accept(request.person.id) },
                            onDecline = { vm.decline(request.person.id) }
                        )
                    }
                    item { Spacer(Modifier.height(4.dp)); SectionLabel(stringResource(R.string.alerts_recent_caps)) }
                    items(alerts, key = { it.id }) { a -> if (a.timed != null) TimedAlertRow(a) { open(a) } else ActivityRow(a) { open(a) } }
                } else {
                    if (activity.isEmpty()) item(key = "empty-activity") { EmptyState(EmptyKind.ALERTS) }
                    items(activity, key = { it.id }) { a -> ActivityRow(a) { open(a) } }
                }
            }
        }
        CircleBottomNav(nav, NavTab.NOTIFICATIONS)
    }
}

@Composable
private fun RequestRow(request: RequestView, onOpen: () -> Unit, onAccept: () -> Unit, onDecline: () -> Unit) {
    val person = request.person
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable(onClickLabel = stringResource(R.string.open_profile_of, person.name), onClick = onOpen)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(person.initials, person.color, size = 40)
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(person.name, color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                Text(
                    when {
                        request.accepted -> stringResource(R.string.alerts_now_friends)
                        // "Asked 3d ago", but "Asked yesterday" and "Asked 10 Oct 2026" (no "ago" after a day or a date).
                        com.circle.app.ui.format.relative(request.receivedAt).let {
                            it is com.circle.app.ui.format.Relative.Now || it is com.circle.app.ui.format.Relative.Yesterday || it is com.circle.app.ui.format.Relative.Date
                        } -> stringResource(R.string.alerts_asked_on, relativeTime(request.receivedAt))
                        else -> stringResource(R.string.alerts_asked, relativeTime(request.receivedAt))
                    },
                    color = CircleTheme.colors.muted, fontSize = 11.5.sp
                )
            }
        }
        if (request.accepted) {
            Text(stringResource(R.string.profile_friends_check), color = CircleTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        } else {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(CircleTheme.colors.accent)
                    .clickable(onClick = onAccept).padding(horizontal = 13.dp, vertical = 7.dp)
            ) {
                Text(stringResource(R.string.profile_accept), color = CircleTheme.colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, CircleTheme.colors.border, RoundedCornerShape(999.dp))
                    .clickable(onClick = onDecline).padding(horizontal = 13.dp, vertical = 7.dp)
            ) {
                Text(stringResource(R.string.profile_decline), color = CircleTheme.colors.body, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
    }
}

/** What an alert says. A quoted comment is only present when the repository allowed it. */
@Composable
private fun alertText(a: AlertView): String {
    val about = a.about?.firstName.orEmpty()
    return when (a.kind) {
        AlertKind.LIKED_YOUR_NOTE -> stringResource(R.string.alert_liked_your_note)
        AlertKind.COMMENTED_ON_YOUR_NOTE -> a.quote?.let { stringResource(R.string.alert_commented_quote, it) } ?: stringResource(R.string.alert_commented)
        AlertKind.REPOSTED_YOUR_NOTE -> stringResource(R.string.alert_reposted_your_note)
        AlertKind.QUOTED_YOUR_NOTE -> a.quote?.let { stringResource(R.string.alert_quoted_quote, it) } ?: stringResource(R.string.alert_quoted)
        AlertKind.FRIEND_COMMENTED -> a.quote?.let { stringResource(R.string.alert_friend_commented_quote, about, it) } ?: stringResource(R.string.alert_friend_commented, about)
        AlertKind.FRIEND_REPOSTED -> stringResource(R.string.alert_friend_reposted, about)
        else -> ""
    }
}

/** Reminders and changes for temporary groups and chats (TIMED_GROUPS 7): a title, one line, and when. */
@Composable
private fun TimedAlertRow(a: AlertView, onOpen: () -> Unit) {
    val t = a.timed ?: return
    val colors = CircleTheme.colors
    val title = when (a.kind) {
        AlertKind.TIMED_ENDS_SOON -> androidx.compose.ui.res.pluralStringResource(R.plurals.alert_timed_ends_in, 3, t.name, 3)
        AlertKind.TIMED_ENDS_TOMORROW -> androidx.compose.ui.res.pluralStringResource(R.plurals.alert_timed_ends_in, 1, t.name, 1)
        AlertKind.TIMED_ENDED -> stringResource(R.string.alert_timed_ended_title, t.name)
        AlertKind.TIMED_DELETED_TOMORROW -> stringResource(R.string.alert_timed_delete_title, t.name)
        AlertKind.TIMED_EXTENDED -> stringResource(R.string.alert_timed_extended_title, a.actor.firstName, t.name)
        else -> stringResource(R.string.alert_timed_permanent_title, a.actor.firstName, t.name)
    }
    val date = t.at?.let { com.circle.app.ui.components.lastDay(it) }.orEmpty()
    val body = when (a.kind) {
        AlertKind.TIMED_ENDS_SOON -> stringResource(R.string.alert_timed_ends_soon_body)
        AlertKind.TIMED_ENDS_TOMORROW -> stringResource(R.string.alert_timed_tomorrow_body)
        AlertKind.TIMED_ENDED -> stringResource(R.string.alert_timed_ended_body, date)
        AlertKind.TIMED_DELETED_TOMORROW -> stringResource(R.string.alert_timed_delete_body)
        AlertKind.TIMED_EXTENDED -> stringResource(R.string.alert_timed_extended_body, date)
        else -> stringResource(R.string.alert_timed_permanent_body)
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(if (a.unread) colors.accent.copy(alpha = 0.08f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onOpen).padding(vertical = 13.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        com.circle.app.ui.components.TimedAlertIcon(a.kind)
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(body, color = colors.body, fontSize = 13.sp, lineHeight = 18.9.sp, modifier = Modifier.padding(top = 2.dp))
            Text(relativeTime(a.createdAt), color = colors.muted, fontSize = 11.5.sp, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun ActivityRow(a: AlertView, onOpen: () -> Unit) {
    val action = alertText(a)
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(if (a.unread) CircleTheme.colors.accent.copy(alpha = 0.08f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onOpen).padding(vertical = 10.dp, horizontal = 6.dp)
    ) {
        Avatar(a.actor.initials, a.actor.color, size = 38)
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = CircleTheme.colors.text, fontWeight = FontWeight.Bold)) { append(a.actor.name) }
                    withStyle(SpanStyle(color = CircleTheme.colors.body)) { append(" "); append(action) }
                },
                fontSize = 13.5.sp
            )
            Text(relativeTime(a.createdAt), color = CircleTheme.colors.muted, fontSize = 11.5.sp)
        }
    }
}
