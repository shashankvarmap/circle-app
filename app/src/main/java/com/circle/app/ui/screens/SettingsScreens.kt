package com.circle.app.ui.screens

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
}

@Composable
fun SettingsScreen(nav: NavHostController, onLogOut: () -> Unit, onAccountDeleted: () -> Unit) {
    val colors = CircleTheme.colors
    val context = LocalContext.current
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val vm = circleViewModel { SettingsViewModel(it) }
    val privacy by vm.privacy.collectAsStateWithLifecycle()
    val notif by vm.notifications.collectAsStateWithLifecycle()
    val theme by vm.theme.collectAsStateWithLifecycle()
    val circle by vm.circle.collectAsStateWithLifecycle()
    val blockedList by vm.blocked.collectAsStateWithLifecycle()
    val mutedList by vm.muted.collectAsStateWithLifecycle()
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }


    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out of Circle?") },
            text = { Text("You'll go back to the Welcome screen. Everything stays as it is for when you log back in.") },
            confirmButton = { TextButton(onClick = { confirmLogout = false; onLogOut() }) { Text("Log out", color = colors.accent) } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancel", color = colors.text) } }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete your account?") },
            text = {
                Text(
                    "This permanently removes your profile, photo and pinned thought; your notes, reposts, quotes and " +
                        "comments; your likes; your friends list and inner circle; your messages; and your group and " +
                        "event memberships. Your friends are not told why. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteAccount(onAccountDeleted)
                }) { Text("Delete account", color = colors.alertDot) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep my account", color = colors.text) } }
        )
    }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        DetailHeader("Settings") { nav.popBackStack() }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item { SectionHeading("Account") }
            item { NavRow("Edit profile", "Name, username, photo and pinned thought") { nav.navigate(Routes.EDIT_PROFILE) } }
            item { NavRow("Your circle", "${circle.friendCount} friends · ${circle.innerCount} inner circle") { nav.navigate(Routes.YOUR_CIRCLE) } }
            item { NavRow("Blocked users", countLabel(blockedList.size, "person", "people", "Nobody")) { nav.navigate(Routes.peopleList("blocked")) } }
            item { NavRow("Muted friends", countLabel(mutedList.size, "friend", "friends", "Nobody")) { nav.navigate(Routes.peopleList("muted")) } }

            item { SectionHeading("Privacy") }
            item {
                ToggleRow(
                    "Let people find me by name or username",
                    "People who find you see your photo and pinned thought only.",
                    privacy.findableByName
                ) { vm.setPrivacy(privacy.copy(findableByName = it)) }
            }
            item {
                ToggleRow("Let friends find me from their phone contacts", null, privacy.findableFromContacts) {
                    vm.setPrivacy(privacy.copy(findableFromContacts = it))
                }
            }
            item {
                SegmentedRow("Who can send me friend requests", RequestsFrom.entries, privacy.requestsFrom, { it.label }) {
                    vm.setPrivacy(privacy.copy(requestsFrom = it))
                }
            }
            item {
                SegmentedRow("Who can invite me to events and groups", InvitesFrom.entries, privacy.invitesFrom, { it.label }) {
                    vm.setPrivacy(privacy.copy(invitesFrom = it))
                }
            }
            item {
                ToggleRow("Read receipts in messages", "Friends see when you've read their message, and you see theirs.", privacy.readReceipts) {
                    vm.setPrivacy(privacy.copy(readReceipts = it))
                }
            }
            item { AlwaysPrivateBox() }
            item { InfoRow("Download my data", "Coming with the backend") }

            item { SectionHeading("Notifications") }
            item { ToggleRow("Likes", null, notif.likes) { vm.setNotifications(notif.copy(likes = it)) } }
            item { ToggleRow("Comments and replies", null, notif.comments) { vm.setNotifications(notif.copy(comments = it)) } }
            item { ToggleRow("Friend requests", null, notif.friendRequests) { vm.setNotifications(notif.copy(friendRequests = it)) } }
            item { ToggleRow("Event updates", null, notif.events) { vm.setNotifications(notif.copy(events = it)) } }
            item { ToggleRow("Group activity", null, notif.groups) { vm.setNotifications(notif.copy(groups = it)) } }
            item { ToggleRow("Messages", null, notif.messages) { vm.setNotifications(notif.copy(messages = it)) } }
            item {
                SegmentedRow("Pause all", PauseAll.entries, notif.pause, { it.label }) {
                    vm.setNotifications(notif.copy(pause = it))
                }
            }

            item { SectionHeading("Appearance") }
            item {
                SegmentedRow("Theme", ThemeMode.entries, theme, { it.label }, note = "System follows your phone's day and night setting.") {
                    vm.setTheme(it)
                }
            }

            item { SectionHeading("About") }
            item { NavRow("About Circle", null) { nav.navigate(Routes.info("about")) } }
            item { NavRow("Why I built it", null) { nav.navigate(Routes.info("why")) } }
            item { InfoRow("Membership", "About €1 a month, just for the servers. No ads. Nothing about you to sell. Payment comes later.") }
            item { NavRow("Privacy policy", null) { nav.navigate(Routes.info("privacy")) } }
            item { NavRow("Terms", null) { nav.navigate(Routes.info("terms")) } }
            item { NavRow("Open-source licenses", "Fraunces and Public Sans (SIL Open Font License)") { nav.navigate(Routes.info("licenses")) } }
            item { InfoRow("Version", version) }

            item { Spacer(Modifier.height(20.dp)) }
            item { ActionRow("Log out", colors.text) { confirmLogout = true } }
            item { ActionRow("Delete account", colors.alertDot, weight = FontWeight.Normal) { confirmDelete = true } }
        }
    }
}

private fun countLabel(n: Int, one: String, many: String, none: String) = when (n) {
    0 -> none; 1 -> "1 $one"; else -> "$n $many"
}

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
    val nameProblem = if (name.isBlank()) "Add a display name" else null
    val changed = name != saved.displayName || cleanHandle != saved.handle || pinned != saved.pinnedThought || avatar != saved.avatarUri
    val canSave = changed && handleProblem == null && nameProblem == null && pinned.length <= 160

    fun leave() = nav.popBackStack()
    fun cancel() { if (changed) confirmDiscard = true else leave() }
    BackHandler(onBack = ::cancel)

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard your changes?") },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; leave() }) { Text("Discard", color = colors.accent) } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing", color = colors.text) } }
        )
    }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cancel", color = colors.body, fontSize = 14.5.sp, modifier = Modifier.clickable(onClick = ::cancel).padding(vertical = 10.dp))
            Text(
                "Edit profile", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 16.sp,
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
            ) { Text("Save", color = colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 13.5.sp) }
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
                        .clickable(onClickLabel = "Change photo") {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        .semantics { contentDescription = if (avatar == null) "Your initial. Tap to add a photo" else "Your photo. Tap to change it" }
                )
                Row(Modifier.padding(top = 6.dp)) {
                    Text(
                        if (avatar == null) "Add photo" else "Change photo",
                        color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }.padding(10.dp)
                    )
                    if (avatar != null) {
                        Text(
                            "Remove", color = colors.muted, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            modifier = Modifier.clickable { avatar = null }.padding(10.dp)
                        )
                    }
                }
            }

            LabeledField("DISPLAY NAME", name, { name = it.take(40) }, "Your name")
            nameProblem?.let { FieldError(it) }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LabeledField("USERNAME", "@$cleanHandle", { handle = it }, "@yourname")
                if (handleProblem != null) FieldError(handleProblem)
                else Text("Friends can find you by @$cleanHandle.", color = colors.muted, fontSize = 11.5.sp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LabeledField("PINNED THOUGHT", pinned, { pinned = it }, "A line about you", multiline = true)
                Row {
                    Text(
                        "This is your bio. With your photo, it's all a stranger sees.",
                        color = colors.muted, fontSize = 11.5.sp, modifier = Modifier.weight(1f)
                    )
                    Text("${pinned.length}/160", color = if (pinned.length > 160) colors.alertDot else colors.muted, fontSize = 11.5.sp)
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
        DetailHeader(if (blocked) "Blocked users" else "Muted friends") { nav.popBackStack() }
        Text(
            if (blocked) "People you block can't see you or reach you, and they disappear from your feeds, search, groups, chats and event seats."
            else "Muted friends stay friends. Their notes just don't show on your Home. They're never told.",
            color = colors.muted, fontSize = 12.sp, modifier = Modifier.padding(16.dp)
        )
        if (people.isEmpty()) {
            Text(
                if (blocked) "You haven't blocked anyone." else "You haven't muted anyone.",
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
                    ) { Text(if (blocked) "Unblock" else "Unmute", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 12.5.sp) }
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
        "about" -> "About Circle"
        "why" -> "Why I built it"
        "privacy" -> "Privacy policy"
        "terms" -> "Terms"
        else -> "Open-source licenses"
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
                    "The full ${title.lowercase()} will be published here before Circle launches, together with the backend. " +
                        "Until then, the short version is in Settings under \"Always private\"."
                )
                else -> Licenses()
            }
        }
    }
}

@Composable
private fun AboutCircle() {
    Text(
        "A small place for the people you actually know.",
        fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, color = CircleTheme.colors.text
    )
    Paragraph("What makes it different:")
    Bullet("No ads, ever.", "You pay about €1 a month for the servers. You are not the product.")
    Bullet("No algorithm.", "Newest first. Nothing is ranked, boosted or pushed at you.")
    Bullet("No public numbers.", "Likes are private. Your friends list is private. Nobody is performing for a score.")
    Bullet("Mutual friends only.", "You ask, they accept. No followers.")
    Bullet("It ends.", "Circle tells you when you're caught up, so you can close it and get on with your day.")
    Bullet("It gets you outside.", "Events and groups are there to take you out of the house and back to real people.")
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = CircleTheme.colors.text)) { append("Why \"Circle\"? ") }
            append(
                "It's your circle of friends. And a circle closes: when you've seen everyone's day, the circle is closed, " +
                    "and you're done. (That's why the name ends with a full stop.)"
            )
        },
        color = CircleTheme.colors.body, fontSize = 15.sp, lineHeight = 23.sp
    )
}

@Composable
private fun WhyIBuiltIt() {
    Paragraph(
        "I built Circle because the apps I used to love for keeping up with friends turned into endless scrolling. I wanted a " +
            "place where I open the app, see what my friends are up to, and close it again. No ads, no tricks to keep me there."
    )
    Paragraph("If you want a bigger circle, make an event, go outside and meet someone. Then come back and tell your friends about it.")
    Paragraph("Thank you for being here.")
    Text("Shashank", fontFamily = Display, fontStyle = FontStyle.Italic, fontSize = 20.sp, color = CircleTheme.colors.text)
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
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.text)
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
private fun <T> SegmentedRow(title: String, options: List<T>, selected: T, label: (T) -> String, note: String? = null, onSelect: (T) -> Unit) {
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
            Text("Always private", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Text(
            "Your friends list and counts. Your likes. Who is in your inner circle.",
            color = colors.body, fontSize = 13.5.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp)
        )
    }
}
