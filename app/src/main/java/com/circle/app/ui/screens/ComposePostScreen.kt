package com.circle.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.data.Audience
import com.circle.app.data.CircleStore
import com.circle.app.navigation.Routes
import com.circle.app.ui.components.Avatar
import com.circle.app.ui.components.EmbeddedPost
import com.circle.app.ui.theme.*

private const val SOFT_LIMIT = 500

/**
 * Unified composer — a Thought and a Post are the same object; media is optional.
 * [quoteId] embeds that note as a quote; [groupId] posts into that group instead of Home.
 */
@Composable
fun ComposePostScreen(nav: NavHostController, quoteId: String? = null, groupId: String? = null) {
    val colors = CircleTheme.colors
    var text by rememberSaveable { mutableStateOf("") }
    var audience by rememberSaveable { mutableStateOf(Audience.FRIENDS) }
    var quoteRemoved by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }

    val group = groupId?.let { CircleStore.groupOrNull(it) }
    val quoted = quoteId?.takeIf { !quoteRemoved }?.let { CircleStore.post(it) }?.let { CircleStore.original(it) }
    val remaining = SOFT_LIMIT - text.length
    val canPost = text.isNotBlank() && remaining >= 0
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    fun leave() = nav.popBackStack()
    fun cancel() { if (text.isNotBlank()) confirmDiscard = true else leave() }
    fun post() {
        if (!canPost) return
        CircleStore.addPost(text, audience, quotedPostId = quoted?.id, groupId = group?.id) ?: return
        if (group != null) {
            leave() // back to the group, where the post now sits at the top
        } else {
            // Back to Home, where the new note is at the top.
            nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
        }
    }

    BackHandler(onBack = ::cancel)

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard this note?") },
            text = { Text("What you've written will be lost.") },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; leave() }) { Text("Discard", color = colors.accent) } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing", color = colors.text) } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cancel", color = colors.body, fontSize = 14.5.sp, modifier = Modifier.clickable(onClick = ::cancel).padding(vertical = 10.dp))
            Box(
                modifier = Modifier
                    .alpha(if (canPost) 1f else 0.4f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.accent)
                    .clickable(enabled = canPost, onClick = ::post)
                    .semantics { if (!canPost) disabled() }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) { Text("Post", color = colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 13.5.sp) }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Row {
                Avatar(CircleStore.me.initials, CircleStore.me.color, size = 38)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f).padding(top = 6.dp)) {
                    val placeholder = when {
                        quoted != null -> "Add your thoughts…"
                        group != null -> "Post to ${group.name}…"
                        else -> "What's on your mind?"
                    }
                    if (text.isEmpty()) Text(placeholder, color = colors.muted, fontSize = 17.sp, fontFamily = Body)
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        textStyle = TextStyle(color = colors.text, fontSize = 17.sp, lineHeight = 24.5.sp, fontFamily = Body),
                        cursorBrush = SolidColor(colors.accent),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).focusRequester(focus)
                            .semantics { contentDescription = placeholder }
                    )
                }
            }
            if (quoted != null) {
                Box(Modifier.padding(start = 50.dp, top = 12.dp)) {
                    EmbeddedPost(quoted)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { quoteRemoved = true }
                            .semantics { contentDescription = "Remove the quoted note" },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Outlined.Close, contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp)) }
                }
            }
            Spacer(Modifier.height(16.dp))
            val note = when {
                group != null -> "Only members of ${group.name} can see this. It stays in the group."
                audience == Audience.FRIENDS -> "All your friends can see this."
                else -> "Only your ${CircleStore.innerCount} inner-circle friends can see this."
            }
            Text(note, color = colors.muted, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            if (group == null) {
                Text(
                    "Edit who's in your inner circle",
                    color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.End).clickable { nav.navigate(Routes.YOUR_CIRCLE) }.padding(vertical = 8.dp)
                )
            }
        }

        // Bottom bar rides above the keyboard (the window resizes for the IME).
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                // Photos and video arrive with the posts stage (system picker).
                Icon(
                    Icons.Outlined.Image, contentDescription = "Add photos: coming soon",
                    tint = colors.muted, modifier = Modifier.alpha(0.5f)
                )
                if (remaining <= 50) {
                    Text(
                        "$remaining", color = if (remaining < 0) colors.alertDot else colors.muted,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { contentDescription = "$remaining characters left" }
                    )
                }
                if (group != null) {
                    Text(group.name, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.surface)
                            .border(1.5.dp, colors.accent, RoundedCornerShape(999.dp))
                            .clickable(onClickLabel = "Change audience") {
                                audience = if (audience == Audience.FRIENDS) Audience.INNER_CIRCLE else Audience.FRIENDS
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (audience == Audience.FRIENDS) "Friends" else "Inner circle",
                            color = colors.text, fontWeight = FontWeight.Bold, fontSize = 12.5.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = colors.muted, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
