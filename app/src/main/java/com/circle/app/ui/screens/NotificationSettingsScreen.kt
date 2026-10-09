package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.circle.app.data.NotificationPrefs
import com.circle.app.data.PauseAll
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.Outcome
import com.circle.app.ui.theme.CircleTheme
import com.circle.app.ui.theme.Display
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class NotificationSettingsViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    val prefs = repo.notificationPrefs().state(NotificationPrefs())
    fun set(p: NotificationPrefs) = run({ setNotificationPrefs(p); Outcome.Success(Unit) })
}

/** Notifications (MESSAGES_AND_SETTINGS_V2 4): six switches, Pause all, quiet hours. Writes to the settings store. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(nav: NavHostController) {
    val colors = CircleTheme.colors
    val vm = circleViewModel { NotificationSettingsViewModel(it) }
    val p by vm.prefs.collectAsStateWithLifecycle()
    var picking by rememberSaveable { mutableStateOf<String?>(null) } // "start" | "end"

    picking?.let { which ->
        val minutes = if (which == "start") p.quietStart else p.quietEnd
        val state = rememberTimePickerState(minutes / 60, minutes % 60, is24Hour = android.text.format.DateFormat.is24HourFormat(androidx.compose.ui.platform.LocalContext.current))
        AlertDialog(
            onDismissRequest = { picking = null },
            title = { Text(stringResource(if (which == "start") R.string.notif_quiet_from else R.string.notif_quiet_until)) },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = {
                    val m = state.hour * 60 + state.minute
                    vm.set(if (which == "start") p.copy(quietStart = m, quietHours = true) else p.copy(quietEnd = m, quietHours = true))
                    picking = if (which == "start") "end" else null
                }) { Text(stringResource(if (which == "start") R.string.newchat_next else R.string.compose_save), color = colors.accent) }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text(stringResource(R.string.cancel), color = colors.text) } }
        )
    }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        DetailHeader(stringResource(R.string.notif_title)) { nav.popBackStack() }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.notif_helper), color = colors.muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))

            Caps(stringResource(R.string.notif_alert_me_caps))
            SwitchRow(R.string.notif_likes, R.string.notif_likes_sub, p.likes) { vm.set(p.copy(likes = it)) }
            SwitchRow(R.string.notif_comments, R.string.notif_comments_sub, p.comments) { vm.set(p.copy(comments = it)) }
            SwitchRow(R.string.notif_requests, R.string.notif_requests_sub, p.friendRequests) { vm.set(p.copy(friendRequests = it)) }
            SwitchRow(R.string.notif_events, R.string.notif_events_sub, p.events) { vm.set(p.copy(events = it)) }
            SwitchRow(R.string.notif_groups, R.string.notif_groups_sub, p.groups) { vm.set(p.copy(groups = it)) }
            SwitchRow(R.string.notif_messages, R.string.notif_messages_sub, p.messages) { vm.set(p.copy(messages = it)) }

            Spacer(Modifier.height(16.dp))
            Caps(stringResource(R.string.notif_pause_caps))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                PauseAll.entries.forEach { option ->
                    val selected = p.pause == option
                    val label = stringResource(
                        when (option) { PauseAll.OFF -> R.string.notif_pause_off; PauseAll.ONE_HOUR -> R.string.notif_pause_hour; PauseAll.UNTIL_TOMORROW -> R.string.notif_pause_tomorrow }
                    )
                    Box(
                        Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(999.dp))
                            .background(if (selected) colors.accent else colors.background)
                            .border(1.dp, if (selected) colors.accent else colors.border, RoundedCornerShape(999.dp))
                            .selectable(selected, role = Role.RadioButton) { vm.set(p.copy(pause = option)) }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(label, color = if (selected) colors.onAccent else colors.text, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                }
            }

            Spacer(Modifier.height(16.dp))
            Caps(stringResource(R.string.notif_quiet_caps))
            val fmt = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            fun t(m: Int) = LocalTime.of(m / 60, m % 60).format(fmt)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(10.dp)).clickable { picking = "start" },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.notif_quiet_range, t(p.quietStart), t(p.quietEnd)), color = colors.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Switch(
                    checked = p.quietHours, onCheckedChange = { vm.set(p.copy(quietHours = it)) },
                    colors = SwitchDefaults.colors(checkedTrackColor = colors.accent, checkedThumbColor = colors.onAccent)
                )
            }
            Text(stringResource(R.string.notif_quiet_note), color = colors.muted, fontSize = 12.sp)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Caps(text: String) {
    Text(text, color = CircleTheme.colors.muted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.47.sp, modifier = Modifier.semantics { heading() })
}

@Composable
private fun SwitchRow(title: Int, sub: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = CircleTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).toggleable(checked, role = Role.Switch, onValueChange = onChange).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(stringResource(sub), color = colors.muted, fontSize = 12.5.sp)
        }
        Switch(
            checked = checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = colors.accent, checkedThumbColor = colors.onAccent)
        )
    }
}
