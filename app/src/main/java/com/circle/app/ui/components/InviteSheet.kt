package com.circle.app.ui.components

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.circle.app.R
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.Outcome
import com.circle.app.ui.theme.AvatarSage
import com.circle.app.ui.theme.CircleTheme
import com.circle.app.ui.theme.Display
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InviteViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    val link = repo.inviteLink().state("")
    val shared = repo.invitesShared().state(0)
    fun recordShared() = run({ recordInviteShared(); Outcome.Success(Unit) })
}

/** A phone contact to invite (read on the device only, never uploaded). */
private data class Contact(val name: String, val number: String)

private fun readContacts(context: Context, limit: Int = 30): List<Contact> = runCatching {
    context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
        null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
    )?.use { c ->
        buildList {
            while (c.moveToNext() && size < limit) add(Contact(c.getString(0).orEmpty(), c.getString(1).orEmpty()))
        }.distinctBy { it.name }
    }.orEmpty()
}.getOrDefault(emptyList())

/**
 * Invite a friend (ACCESS_AND_LAUNCH 2): your link with Copy, share buttons that open the apps,
 * and a few contacts with Invite, which opens your SMS app with the text (never sent automatically).
 * An invite never makes anyone a friend.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteSheet(onDismiss: () -> Unit) {
    val colors = CircleTheme.colors
    val context = LocalContext.current
    val vm = circleViewModel { InviteViewModel(it) }
    val link by vm.link.collectAsStateWithLifecycle()
    val shared by vm.shared.collectAsStateWithLifecycle()
    val url = "https://$link"
    val message = stringResource(R.string.invite_message, url)
    val copied = stringResource(R.string.invite_copied)
    var contacts by remember { mutableStateOf<List<Contact>?>(null) }
    var invited by rememberSaveable { mutableStateOf(setOf<String>()) }
    val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    val askContacts = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) contacts = readContacts(context)
    }
    LaunchedEffect(hasPermission) { if (hasPermission) contacts = withContext(Dispatchers.IO) { readContacts(context) } }

    fun share(pkg: String?) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, message)
        if (pkg != null) send.setPackage(pkg)
        val ok = runCatching { context.startActivity(if (pkg == null) Intent.createChooser(send, null) else send) }.isSuccess
        if (!ok) context.startActivity(Intent.createChooser(send.setPackage(null), null)) // that app isn't installed: the system sheet
        vm.recordShared()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), containerColor = colors.surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Text(stringResource(R.string.invite_title), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = colors.text, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.invite_body), color = colors.muted, fontSize = 13.5.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 6.dp))
            Row(
                Modifier.padding(top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.accent.copy(alpha = 0.08f))
                    .border(1.dp, colors.accent.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).padding(start = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(link, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.invite_copy), color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button) {
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                            .setPrimaryClip(ClipData.newPlainText("Circle", url))
                        Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                    }.padding(14.dp)
                )
            }
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShareChip(stringResource(R.string.invite_messages), Modifier.weight(1f)) {
                    val sms = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).putExtra("sms_body", message)
                    runCatching { context.startActivity(sms) }.onFailure { share(null) }
                    vm.recordShared()
                }
                ShareChip(stringResource(R.string.invite_whatsapp), Modifier.weight(1f)) { share("com.whatsapp") }
                ShareChip(stringResource(R.string.invite_mail), Modifier.weight(1f)) {
                    val mail = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(Intent.EXTRA_TEXT, message)
                        .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.invite_mail_subject))
                    runCatching { context.startActivity(mail) }.onFailure { share(null) }
                    vm.recordShared()
                }
                ShareChip(stringResource(R.string.invite_more), Modifier.weight(1f)) { share(null) }
            }
            if (shared > 0) Text(pluralStringResource(R.plurals.invites_shared, shared, shared), color = colors.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))

            Text(stringResource(R.string.invite_contacts_caps), color = colors.muted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.47.sp, modifier = Modifier.padding(top = 20.dp, bottom = 6.dp))
            val list = contacts
            if (list == null) {
                Text(
                    stringResource(R.string.invite_connect_contacts), color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 13.5.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { askContacts.launch(Manifest.permission.READ_CONTACTS) }.padding(vertical = 12.dp)
                )
            } else if (list.isEmpty()) {
                Text(stringResource(R.string.invite_no_contacts), color = colors.muted, fontSize = 13.sp)
            } else list.take(8).forEach { c ->
                val done = c.number in invited
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(c.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }, AvatarSage, size = 36)
                    Spacer(Modifier.width(12.dp))
                    Text(c.name, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    SmallPillButton(stringResource(if (done) R.string.invite_invited else R.string.invite_invite), filled = !done) {
                        // Opens your SMS app with the text; you press send.
                        val sms = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(c.number))).putExtra("sms_body", message)
                        runCatching { context.startActivity(sms) }
                        invited = invited + c.number
                        vm.recordShared()
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareChip(text: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = CircleTheme.colors
    Box(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, maxLines = 1) }
}

@Composable
private fun SmallPillButton(text: String, filled: Boolean, onClick: () -> Unit) {
    val colors = CircleTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Box(
        Modifier.heightIn(min = 40.dp).clip(shape).background(if (filled) colors.accent else colors.surface)
            .border(1.dp, if (filled) colors.accent else colors.border, shape)
            .clickable(enabled = filled, role = Role.Button, onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = if (filled) colors.onAccent else colors.muted, fontWeight = FontWeight.Bold, fontSize = 12.5.sp) }
}
