package com.circle.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.circle.app.R
import com.circle.app.data.access.Relationship
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.ProfileView
import com.circle.app.data.repo.isSuccess
import com.circle.app.ui.theme.CircleTheme
import com.circle.app.ui.theme.Display
import com.circle.app.ui.vm.ActionsViewModel
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import kotlinx.coroutines.launch

/** What the gate needs: who it is about (Limited at most) and the button state. */
class GateViewModel(repo: CircleRepository, personId: String) : RepoViewModel(repo) {
    val profile = repo.profile(personId).state(ProfileView.NotFound)
    val relationship = repo.relationshipTo(personId).state(Relationship.NONE)
}

/** Which gate: a note's thread, or an event for the host's friends. */
enum class GateKind { NOTE, EVENT }

/**
 * The gate shown when something belongs to a person you aren't friends with (a shared note's
 * thread, a friends-only event). You can always see who they are and ask; never more.
 * Actions: Send friend request, View profile (Limited), Not now.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotFriendsSheet(
    personId: String,
    kind: GateKind = GateKind.NOTE,
    onViewProfile: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close(then: () -> Unit = {}) {
        scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss(); then() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        containerColor = CircleTheme.colors.surface,
    ) {
        GateBody(personId, kind, onViewProfile = { close(onViewProfile) }, onDone = { close() })
    }
}

/** The gate's content, also shown in place of a thread that a route or deep link asked for. */
@Composable
fun GateBody(personId: String, kind: GateKind, onViewProfile: () -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CircleTheme.colors
    val context = LocalContext.current
    val gate = circleViewModel(key = "gate-$personId") { GateViewModel(it, personId) }
    val actions = circleViewModel { ActionsViewModel(it) }
    val profile by gate.profile.collectAsStateWithLifecycle()
    val rel by gate.relationship.collectAsStateWithLifecycle()
    val firstName = when (val p = profile) {
        is ProfileView.Limited -> p.firstName
        is ProfileView.Full -> p.person.firstName
        ProfileView.NotFound -> ""
    }
    Column(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(colors.accent.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp)) }
        Text(
            if (kind == GateKind.NOTE) stringResource(R.string.gate_note_title, firstName) else stringResource(R.string.gate_event_title),
            fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, color = colors.text,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp).semantics { heading() }
        )
        Text(
            if (kind == GateKind.NOTE) stringResource(R.string.gate_note_body) else stringResource(R.string.gate_event_body, firstName),
            color = colors.muted, fontSize = 13.5.sp, lineHeight = 19.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
        Spacer(Modifier.height(18.dp))
        when (rel) {
            Relationship.NONE -> SheetButton(
                stringResource(if (kind == GateKind.NOTE) R.string.gate_send_request else R.string.gate_send_request_host), filled = true
            ) {
                val sent = context.getString(R.string.toast_request_sent)
                actions.sendFriendRequest(personId) { if (it.isSuccess) Toast.makeText(context, sent, Toast.LENGTH_SHORT).show() }
                onDone()
            }
            Relationship.REQUEST_SENT -> SheetButton(stringResource(R.string.profile_requested), filled = false, enabled = false) {}
            Relationship.REQUEST_RECEIVED -> SheetButton(stringResource(R.string.gate_accept, firstName), filled = true) {
                val friends = context.getString(R.string.toast_now_friends)
                actions.acceptRequest(personId) { if (it.isSuccess) Toast.makeText(context, friends, Toast.LENGTH_SHORT).show() }
                onDone()
            }
            else -> Unit
        }
        if (profile !is ProfileView.NotFound) {
            Spacer(Modifier.height(10.dp))
            SheetButton(stringResource(R.string.gate_view_profile, firstName), filled = false, onClick = onViewProfile)
        }
        Text(
            stringResource(R.string.gate_not_now), color = colors.muted, fontWeight = FontWeight.Bold, fontSize = 13.5.sp,
            modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onDone).padding(horizontal = 16.dp, vertical = 14.dp)
        )
    }
}

@Composable
private fun SheetButton(text: String, filled: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = CircleTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Box(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(shape)
            .background(if (filled) colors.accent else colors.surface)
            .border(1.5.dp, if (filled) colors.accent else colors.border, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = if (filled) colors.onAccent else if (enabled) colors.text else colors.muted, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
}
