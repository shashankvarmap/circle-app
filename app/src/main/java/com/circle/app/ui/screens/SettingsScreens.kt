package com.circle.app.ui.screens

import com.circle.app.ui.format.text
import com.circle.app.ui.a11y.label
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.circle.app.R
import com.circle.app.data.HandleRules
import com.circle.app.data.InvitesFrom
import com.circle.app.data.NotificationPrefs
import com.circle.app.data.PauseAll
import com.circle.app.data.PrivacyPrefs
import com.circle.app.data.RequestsFrom
import com.circle.app.data.ThemeMode
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.CircleSummary
import com.circle.app.data.repo.Outcome
import com.circle.app.ui.vm.LocalMe
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import com.circle.app.navigation.Routes
import com.circle.app.ui.components.Avatar
import com.circle.app.ui.components.MeAvatar
import com.circle.app.ui.theme.*

// ---------------------------------------------------------------- Settings

class SettingsViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    val privacy = repo.privacy().state(PrivacyPrefs())
    val notifications = repo.notificationPrefs().state(NotificationPrefs())
    val theme = repo.theme().state(ThemeMode.SYSTEM)
    val circle = repo.circleSummary().state(CircleSummary(0, 0))
    val blocked = repo.blocked().state(emptyList())
    val muted = repo.muted().state(emptyList())
    fun setPrivacy(p: PrivacyPrefs) = run({ setPrivacy(p); Outcome.Success(Unit) })
    fun setNotifications(p: NotificationPrefs) = run({ setNotificationPrefs(p); Outcome.Success(Unit) })
    fun setTheme(t: ThemeMode) = run({ setTheme(t); Outcome.Success(Unit) })
    fun deleteAccount(then: () -> Unit) = run({ deleteAccount() }) { then() }
    fun unblock(id: String) = run({ unblock(id) })
    fun unmute(id: String) = run({ unmute(id) })
    fun saveProfile(name: String, handle: String, pinned: String, avatar: String?, then: (Outcome<*>) -> Unit) =
        run({ saveProfile(name, handle, pinned, avatar) }, then)

    /** Demo-only controls; null with a real backend (the rows are hidden). */
    private val demo = repo as? com.circle.app.data.repo.DemoControls
    val hasDemo = demo != null
    val failing = (demo?.failing() ?: kotlinx.coroutines.flow.flowOf(false)).state(false)
    fun setFailing(on: Boolean) = run({ demo?.setFailing(on); Outcome.Success(Unit) })
    fun resetDemo(then: () -> Unit) = run({ demo?.resetDemoData(); Outcome.Success(Unit) }) { then() }
}

@Composable
fun SettingsScreen(nav: NavHostController, onLogOut: () -> Unit, onAccountDeleted: () -> Unit) {
    val colors = CircleTheme.colors
    val context = LocalContext.current
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    var inviting by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    if (inviting) com.circle.app.ui.components.InviteSheet { inviting = false }
    val vm = circleViewModel { SettingsViewModel(it) }
    val privacy by vm.privacy.collectAsStateWithLifecycle()
    val notif by vm.notifications.collectAsStateWithLifecycle()
    val theme by vm.theme.collectAsStateWithLifecycle()
    val circle by vm.circle.collectAsStateWithLifecycle()
    val blockedList by vm.blocked.collectAsStateWithLifecycle()
    val mutedList by vm.muted.collectAsStateWithLifecycle()
    val failing by vm.failing.collectAsStateWithLifecycle()
    val debuggable = remember { context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0 }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.settings_reset_demo_title)) },
            text = { Text(stringResource(R.string.settings_reset_demo_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    vm.resetDemo { android.widget.Toast.makeText(context, context.getString(R.string.settings_reset_demo_done), android.widget.Toast.LENGTH_SHORT).show() }
                }) { Text(stringResource(R.string.settings_reset_demo_confirm), color = colors.accent) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel), color = colors.text) } }
        )
    }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }


    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_log_out_of_circle)) },
            text = { Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_you_ll_go_back_to)) },
            confirmButton = { TextButton(onClick = { confirmLogout = false; onLogOut() }) { Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_log_out), color = colors.accent) } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.cancel), color = colors.text) } }
        )
    }
    Column(Modifier.fillMaxSize().background(colors.background)) {
        DetailHeader(androidx.compose.ui.res.stringResource(com.circle.app.R.string.profile_settings)) { nav.popBackStack() }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item { SectionHeading(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_account)) }
            item { NavRow(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_edit_profile), stringResource(R.string.settings_edit_profile_sub)) { nav.navigate(Routes.EDIT_PROFILE) } }
            item { NavRow(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_your_circle), stringResource(R.string.circle_summary, circle.friendCount, circle.innerCount)) { nav.navigate(Routes.YOUR_CIRCLE) } }
            item { NavRow(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_blocked_users), countLabel(blockedList.size, com.circle.app.R.plurals.count_people)) { nav.navigate(Routes.peopleList("blocked")) } }
            item { NavRow(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_muted_friends), countLabel(mutedList.size, com.circle.app.R.plurals.count_friends)) { nav.navigate(Routes.peopleList("muted")) } }
            item { NavRow(stringResource(R.string.settings_your_data), stringResource(R.string.settings_your_data_sub)) { nav.navigate(Routes.YOUR_DATA) } }
            item { NavRow(stringResource(R.string.settings_membership), stringResource(R.string.settings_membership_sub)) { nav.navigate(Routes.MEMBERSHIP) } }
            item { NavRow(stringResource(R.string.settings_invite), null) { inviting = true } }

            item { SectionHeading(androidx.compose.ui.res.stringResource(com.circle.app.R.string.legal_privacy)) }
            item {
                ToggleRow(
                    androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_let_people_find_me_by),
                    androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_findable_sub),
                    privacy.findableByName
                ) { vm.setPrivacy(privacy.copy(findableByName = it)) }
            }
            item {
                ToggleRow(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_let_friends_find_me_from), null, privacy.findableFromContacts) {
                    vm.setPrivacy(privacy.copy(findableFromContacts = it))
                }
            }
            item {
                SegmentedRow(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_who_can_send_me_friend), RequestsFrom.entries, privacy.requestsFrom, { it.text() }) {
                    vm.setPrivacy(privacy.copy(requestsFrom = it))
                }
            }
            item {
                SegmentedRow(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_who_can_invite_me_to), InvitesFrom.entries, privacy.invitesFrom, { it.text() }) {
                    vm.setPrivacy(privacy.copy(invitesFrom = it))
                }
            }
            item {
                ToggleRow(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_read_receipts_in_messages), stringResource(R.string.settings_read_receipts_sub), privacy.readReceipts) {
                    vm.setPrivacy(privacy.copy(readReceipts = it))
                }
            }
            item { AlwaysPrivateBox() }


            item { SectionHeading(androidx.compose.ui.res.stringResource(com.circle.app.R.string.notif_title)) }
            item {
                // One row; the switches, pause and quiet hours live on their own screen (V2 4).
                val summary = if (notif.pause != PauseAll.OFF) stringResource(R.string.settings_notifications_paused)
                else stringResource(R.string.settings_notifications_summary, notif.onCount)
                NavRow(stringResource(R.string.notif_title), summary) { nav.navigate(Routes.NOTIFICATION_SETTINGS) }
            }

            item { SectionHeading(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_appearance)) }
            item {
                SegmentedRow(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_theme), ThemeMode.entries, theme, { it.text() }, note = androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_system_follows_your_phone_s)) {
                    vm.setTheme(it)
                }
            }

            item { SectionHeading(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_about)) }
            item { NavRow(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_about_circle), null) { nav.navigate(Routes.info("about")) } }
            item { NavRow(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_why_i_built_it), null) { nav.navigate(Routes.info("why")) } }
            item { NavRow(stringResource(R.string.settings_legal), null) { nav.navigate(Routes.LEGAL) } }
            item { NavRow(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_open_source_licenses), stringResource(R.string.settings_licenses_sub)) { nav.navigate(Routes.info("licenses")) } }
            item { InfoRow(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_version), version) }

            if (vm.hasDemo) {
                item { SectionHeading(stringResource(R.string.settings_demo)) }
                item { NavRow(stringResource(R.string.settings_reset_demo), stringResource(R.string.settings_reset_demo_sub)) { confirmReset = true } }
                if (debuggable) {
                    item {
                        ToggleRow(stringResource(R.string.settings_debug_fail), stringResource(R.string.settings_debug_fail_sub), failing) { vm.setFailing(it) }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
            item { ActionRow(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_log_out), colors.text) { confirmLogout = true } }
            item { ActionRow(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_delete_account), colors.alertDot, weight = FontWeight.Normal) { nav.navigate(Routes.DELETE_ACCOUNT) } }
        }
    }
}

@Composable
private fun countLabel(n: Int, plural: Int) =
    if (n == 0) androidx.compose.ui.res.stringResource(com.circle.app.R.string.count_nobody) else androidx.compose.ui.res.pluralStringResource(plural, n, n)

// ---------------------------------------------------------------- Edit profile

@Composable
fun EditProfileScreen(nav: NavHostController) {
    val colors = CircleTheme.colors
    val context = LocalContext.current
    val vm = circleViewModel { SettingsViewModel(it) }
    val saved = LocalMe.current ?: return
    var name by rememberSaveable { mutableStateOf(saved.displayName) }
    var handle by rememberSaveable { mutableStateOf(saved.handle) }
    var pinned by rememberSaveable { mutableStateOf(saved.pinnedThought) }
    var avatar by rememberSaveable { mutableStateOf(saved.avatarUri) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            // Keep access across restarts where the picker allows it.
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            avatar = uri.toString()
        }
    }

    val cleanHandle = handle.trim().removePrefix("@").lowercase()
    val handleProblem = HandleRules.problem(cleanHandle)
    val nameProblem = if (name.isBlank()) androidx.compose.ui.res.stringResource(com.circle.app.R.string.edit_add_display_name) else null
    val changed = name != saved.displayName || cleanHandle != saved.handle || pinned != saved.pinnedThought || avatar != saved.avatarUri
    val canSave = changed && handleProblem == null && nameProblem == null && pinned.length <= 160

    fun leave() = nav.popBackStack()
    fun cancel() { if (changed) confirmDiscard = true else leave() }
    BackHandler(onBack = ::cancel)

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.compose_discard_changes_title)) },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; leave() }) { Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.compose_discard), color = colors.accent) } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.compose_keep_editing), color = colors.text) } }
        )
    }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.cancel), color = colors.body, fontSize = 14.5.sp, modifier = Modifier.clickable(onClick = ::cancel).padding(vertical = 10.dp))
            Text(
                androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_edit_profile), color = colors.text, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier.weight(1f).semantics { heading() }, textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .alpha(if (canSave) 1f else 0.4f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.accent)
                    .clickable(enabled = canSave) {
                        vm.saveProfile(name, cleanHandle, pinned, avatar) { if (it is Outcome.Success) leave() }
                    }
                    .semantics { if (!canSave) disabled() }
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) { Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.compose_save), color = colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 13.5.sp) }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                MeAvatar(
                    88, uri = avatar,
                    modifier = Modifier
                        .clickable(onClickLabel = androidx.compose.ui.res.stringResource(com.circle.app.R.string.signup_change_photo)) {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        .then(Modifier.label(if (avatar == null) androidx.compose.ui.res.stringResource(com.circle.app.R.string.edit_avatar_add_label) else androidx.compose.ui.res.stringResource(com.circle.app.R.string.edit_avatar_change_label)))
                )
                Row(Modifier.padding(top = 6.dp)) {
                    Text(
                        if (avatar == null) androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_add_photo) else androidx.compose.ui.res.stringResource(com.circle.app.R.string.signup_change_photo),
                        color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }.padding(10.dp)
                    )
                    if (avatar != null) {
                        Text(
                            androidx.compose.ui.res.stringResource(com.circle.app.R.string.group_remove), color = colors.muted, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            modifier = Modifier.clickable { avatar = null }.padding(10.dp)
                        )
                    }
                }
            }

            LabeledField(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_display_name), name, { name = it.take(40) }, androidx.compose.ui.res.stringResource(com.circle.app.R.string.signup_name_hint))
            nameProblem?.let { FieldError(it) }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LabeledField(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_username), "@$cleanHandle", { handle = it }, androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_yourname))
                if (handleProblem != null) FieldError(handleProblem)
                else Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.edit_find_by_handle, cleanHandle), color = colors.muted, fontSize = 11.5.sp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LabeledField(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_pinned_thought), pinned, { pinned = it }, androidx.compose.ui.res.stringResource(com.circle.app.R.string.signup_pinned_hint), multiline = true)
                Row {
                    Text(
                        androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_this_is_your_bio_with),
                        color = colors.muted, fontSize = 11.5.sp, modifier = Modifier.weight(1f)
                    )
                    Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.count_of, pinned.length, 160), color = if (pinned.length > 160) colors.alertDot else colors.muted, fontSize = 11.5.sp)
                }
            }
        }
    }
}

@Composable
private fun FieldError(text: String) {
    Text(text, color = CircleTheme.colors.alertDot, fontSize = 11.5.sp)
}

// ---------------------------------------------------------------- Blocked / muted

@Composable
fun PeopleListScreen(nav: NavHostController, kind: String) {
    val colors = CircleTheme.colors
    val blocked = kind == "blocked"
    val vm = circleViewModel { SettingsViewModel(it) }
    val blockedList by vm.blocked.collectAsStateWithLifecycle()
    val mutedList by vm.muted.collectAsStateWithLifecycle()
    val people = if (blocked) blockedList else mutedList

    Column(Modifier.fillMaxSize().background(colors.background)) {
        DetailHeader(if (blocked) androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_blocked_users) else androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_muted_friends)) { nav.popBackStack() }
        Text(
            if (blocked) androidx.compose.ui.res.stringResource(com.circle.app.R.string.blocked_note)
            else androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_muted_friends_stay_friends_their),
            color = colors.muted, fontSize = 12.sp, modifier = Modifier.padding(16.dp)
        )
        if (people.isEmpty()) {
            Text(
                if (blocked) androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_you_haven_t_blocked_anyone) else androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_you_haven_t_muted_anyone),
                color = colors.muted, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        LazyColumn(Modifier.weight(1f)) {
            items(people, key = { it.id }) { p ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(p.initials, p.color, size = 36)
                    Spacer(Modifier.width(12.dp))
                    Text(p.name, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Box(
                        Modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, colors.border, RoundedCornerShape(999.dp))
                            .clickable { if (blocked) vm.unblock(p.id) else vm.unmute(p.id) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) { Text(if (blocked) androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_unblock) else androidx.compose.ui.res.stringResource(com.circle.app.R.string.messages_unmute), color = colors.text, fontWeight = FontWeight.Bold, fontSize = 12.5.sp) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- About, Why, policies, licenses

@Composable
fun InfoScreen(nav: NavHostController, page: String) {
    val colors = CircleTheme.colors
    val title = when (page) {
        "about" -> androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_about_circle)
        "why" -> androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_why_i_built_it)
        "privacy" -> androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_privacy_policy)
        "terms" -> androidx.compose.ui.res.stringResource(com.circle.app.R.string.legal_terms)
        else -> androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_open_source_licenses)
    }
    Column(Modifier.fillMaxSize().background(colors.background)) {
        DetailHeader(title) { nav.popBackStack() }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (page) {
                "about" -> AboutCircle()
                "why" -> WhyIBuiltIt()
                "privacy", "terms" -> Paragraph(
                    androidx.compose.ui.res.stringResource(com.circle.app.R.string.legal_placeholder_page, title.lowercase())
                )
                else -> Licenses()
            }
        }
    }
}

/** A subtle tinted card (V2 5): accent at 8% fill, accent at 30% border, 18dp radius. No gradient, glow or motion. */
@Composable
private fun TintedCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = CircleTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.accent.copy(alpha = 0.08f))
            .border(1.dp, colors.accent.copy(alpha = 0.30f), RoundedCornerShape(18.dp)).padding(18.dp),
        content = content
    )
}

@Composable
private fun Kicker(text: String, accent: Boolean) {
    Text(
        text, color = if (accent) CircleTheme.colors.accent else CircleTheme.colors.muted,
        fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp, modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun AboutCircle() {
    val colors = CircleTheme.colors
    TintedCard {
        Kicker(stringResource(R.string.about_kicker), accent = true)
        Text(
            stringResource(R.string.about_headline), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 25.sp,
            lineHeight = 31.sp, color = colors.text, modifier = Modifier.padding(top = 8.dp)
        )
    }
    Kicker(stringResource(R.string.about_how_caps), accent = false)
    val points = listOf(
        R.string.about_p1_lead to R.string.about_p1, R.string.about_p2_lead to R.string.about_p2, R.string.about_p3_lead to R.string.about_p3,
        R.string.about_p4_lead to R.string.about_p4, R.string.about_p5_lead to R.string.about_p5, R.string.about_p6_lead to R.string.about_p6,
    )
    Column {
        points.forEachIndexed { i, (lead, rest) ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
            Row(Modifier.padding(vertical = 12.dp)) {
                Box(Modifier.padding(top = 8.dp, end = 12.dp).size(7.dp).background(colors.accent, CircleShape))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.text)) { append(stringResource(lead)) }
                        append(" "); append(stringResource(rest))
                    },
                    color = colors.body, fontSize = 15.sp, lineHeight = 23.sp
                )
            }
        }
    }
    TintedCard {
        Kicker(stringResource(R.string.about_why_caps), accent = true)
        Text(
            buildAnnotatedString {
                append(stringResource(R.string.wordmark_name))
                withStyle(SpanStyle(color = colors.accent, fontWeight = FontWeight.Bold)) { append(stringResource(R.string.wordmark_stop)) }
            },
            fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, color = colors.text, modifier = Modifier.padding(top = 8.dp)
        )
        Text(stringResource(R.string.about_why_body), color = colors.body, fontSize = 15.sp, lineHeight = 23.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun WhyIBuiltIt() {
    val colors = CircleTheme.colors
    TintedCard {
        Text("“", fontFamily = Display, fontSize = 64.sp, lineHeight = 64.sp, color = colors.accent.copy(alpha = 0.55f))
        listOf(R.string.why_p1, R.string.why_p2, R.string.why_p3).forEach {
            Text(
                stringResource(it), fontFamily = Display, fontSize = 17.5.sp, lineHeight = 27.sp, color = colors.text,
                modifier = Modifier.padding(bottom = 14.dp)
            )
        }
        Box(Modifier.size(36.dp, 2.dp).background(colors.accent))
        Text(
            stringResource(R.string.why_signature), fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 20.sp,
            color = colors.accent, modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun Licenses() {
    val context = LocalContext.current
    listOf("Fraunces" to "licenses/Fraunces-OFL.txt", "Public Sans" to "licenses/PublicSans-OFL.txt").forEach { (name, path) ->
        val text = remember(path) { runCatching { context.assets.open(path).bufferedReader().use { it.readText() } }.getOrDefault("") }
        Text(name, color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.semantics { heading() })
        Text(text, color = CircleTheme.colors.body, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun Paragraph(text: String) {
    Text(text, color = CircleTheme.colors.body, fontSize = 15.sp, lineHeight = 23.sp)
}

@Composable
private fun Bullet(lead: String, rest: String) {
    Row {
        Box(Modifier.padding(top = 9.dp, end = 10.dp).size(5.dp).background(CircleTheme.colors.accent, CircleShape))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = CircleTheme.colors.text)) { append(lead) }
                append(" "); append(rest)
            },
            color = CircleTheme.colors.body, fontSize = 15.sp, lineHeight = 23.sp
        )
    }
}

// ---------------------------------------------------------------- Building blocks

/** Back arrow + Fraunces title, for detail screens. */
@Composable
fun DetailHeader(title: String, onBack: () -> Unit) {
    val colors = CircleTheme.colors
    Column {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(com.circle.app.R.string.back), tint = colors.text)
            }
            Text(
                title, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, letterSpacing = (-0.21).sp,
                color = colors.text, modifier = Modifier.padding(start = 2.dp).semantics { heading() }
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text.uppercase(), color = CircleTheme.colors.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.54.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 6.dp).semantics { heading() }
    )
}

@Composable
private fun RowText(title: String, subtitle: String?, modifier: Modifier = Modifier, titleColor: Color = CircleTheme.colors.text) {
    Column(modifier) {
        Text(title, color = titleColor, fontSize = 15.sp)
        if (subtitle != null) Text(subtitle, color = CircleTheme.colors.muted, fontSize = 12.5.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun NavRow(title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowText(title, subtitle, Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = CircleTheme.colors.muted)
    }
}

/** A row that only informs (placeholders say what they're waiting for). */
@Composable
private fun InfoRow(title: String, subtitle: String) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics(mergeDescendants = true) {}.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { RowText(title, subtitle, Modifier.weight(1f)) }
}

@Composable
private fun ActionRow(title: String, color: Color, weight: FontWeight = FontWeight.SemiBold, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) { Text(title, color = color, fontSize = 15.sp, fontWeight = weight) }
}

@Composable
private fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = CircleTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowText(title, subtitle, Modifier.weight(1f).padding(end = 12.dp))
        Switch(
            checked = checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onAccent, checkedTrackColor = colors.accent, checkedBorderColor = colors.accent,
                uncheckedThumbColor = colors.muted, uncheckedTrackColor = colors.track, uncheckedBorderColor = colors.border
            )
        )
    }
}

/** A choice of three or fewer options. */
@Composable
private fun <T> SegmentedRow(title: String, options: List<T>, selected: T, label: @Composable (T) -> String, note: String? = null, onSelect: (T) -> Unit) {
    val colors = CircleTheme.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(title, color = colors.text, fontSize = 15.sp)
        Row(
            Modifier.padding(top = 10.dp).fillMaxWidth()
                .clip(RoundedCornerShape(999.dp)).background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(999.dp)).padding(3.dp)
                .selectableGroup()
        ) {
            options.forEach { option ->
                val active = option == selected
                Box(
                    Modifier.weight(1f).heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (active) colors.accent else Color.Transparent)
                        .selectable(selected = active, role = Role.RadioButton) { onSelect(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label(option), color = if (active) colors.onAccent else colors.body, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
        if (note != null) Text(note, color = colors.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun AlwaysPrivateBox() {
    val colors = CircleTheme.colors
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth()
            .clip(RoundedCornerShape(13.dp)).background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(13.dp)).padding(14.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_always_private), color = colors.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Text(
            androidx.compose.ui.res.stringResource(com.circle.app.R.string.settings_your_friends_list_and_counts),
            color = colors.body, fontSize = 13.5.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp)
        )
    }
}
