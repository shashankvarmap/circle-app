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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circle.app.data.CircleStore
import com.circle.app.data.Relationship
import com.circle.app.ui.theme.CircleTheme
import com.circle.app.ui.theme.Display
import kotlinx.coroutines.launch

/**
 * The gate shown when something belongs to a person you aren't friends with (a shared note's
 * thread, a friends-only event). You can always see who they are and ask; never more.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotFriendsSheet(
    personId: String,
    title: String,
    message: String,
    requestLabel: String = "Send friend request",
    onViewProfile: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = CircleTheme.colors
    val context = LocalContext.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val person = CircleStore.person(personId)
    val rel = CircleStore.relationshipTo(personId)

    fun close(then: () -> Unit = {}) {
        scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss(); then() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        containerColor = colors.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(colors.accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp)) }
            Text(
                title, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, color = colors.text,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp).semantics { heading() }
            )
            Text(
                message, color = colors.muted, fontSize = 13.5.sp, lineHeight = 19.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.height(18.dp))
            when (rel) {
                Relationship.NONE -> SheetButton(requestLabel, filled = true) {
                    if (CircleStore.sendFriendRequest(personId)) Toast.makeText(context, "Friend request sent", Toast.LENGTH_SHORT).show()
                    close()
                }
                Relationship.REQUEST_SENT -> SheetButton("Requested", filled = false, enabled = false) {}
                Relationship.REQUEST_RECEIVED -> SheetButton("Accept ${person?.firstName ?: ""}'s request", filled = true) {
                    CircleStore.acceptRequest(personId)
                    Toast.makeText(context, "You're friends now", Toast.LENGTH_SHORT).show()
                    close()
                }
                else -> Unit
            }
            Spacer(Modifier.height(10.dp))
            SheetButton("View ${person?.firstName ?: "their"}'s profile", filled = false) { close(onViewProfile) }
            Text(
                "Not now", color = colors.muted, fontWeight = FontWeight.Bold, fontSize = 13.5.sp,
                modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(8.dp)).clickable { close() }.padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }
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
