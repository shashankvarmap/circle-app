package com.circle.app.ui.screens

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.circle.app.AppGraph
import com.circle.app.R
import com.circle.app.data.access.Audience
import com.circle.app.data.media.MediaKind
import com.circle.app.data.media.MediaRef
import com.circle.app.data.media.VideoTooLong
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.CircleSummary
import com.circle.app.data.repo.NoteView
import com.circle.app.data.repo.Outcome
import com.circle.app.navigation.Routes
import com.circle.app.ui.components.EmbeddedPost
import com.circle.app.ui.components.MeAvatar
import com.circle.app.ui.components.MediaThumb
import com.circle.app.ui.theme.*
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SOFT_LIMIT = 500
private const val MAX_MEDIA = 10

class ComposeViewModel(repo: CircleRepository, quoteId: String?, groupId: String?, editId: String?) : RepoViewModel(repo) {
    val quoted = (quoteId?.let { repo.note(it) } ?: flowOf(null)).state(null)
    val group = (groupId?.let { repo.group(it) } ?: flowOf(null)).state(null)
    val editing = (editId?.let { repo.note(it) } ?: flowOf(null)).state(null)
    val circle = repo.circleSummary().state(CircleSummary(0, 0))
    fun post(text: String, audience: Audience, quoteId: String?, groupId: String?, media: List<MediaRef>, then: (Outcome<String>) -> Unit) =
        run({ post(text, audience, quoteId, groupId, media) }, then)
    fun save(editId: String, text: String, then: (Outcome<Unit>) -> Unit) = run({ editNote(editId, text) }, then)
}

/** Picked media survive rotation and process death as plain strings. */
private val MediaListSaver = listSaver<MutableList<MediaRef>, String>(
    save = { list -> list.map { "${it.kind}|${it.durationMs ?: ""}|${it.id}|${it.uri}" } },
    restore = { saved ->
        saved.map { s ->
            val (kind, dur, id, uri) = s.split("|", limit = 4)
            MediaRef(id, uri, MediaKind.valueOf(kind), dur.toLongOrNull())
        }.toMutableStateList()
    }
)

/**
 * Unified composer: a thought and a post are the same object; media is optional.
 * [quoteId] embeds that note as a quote; [groupId] posts into that group; [editId] edits your note.
 */
@Composable
fun ComposePostScreen(nav: NavHostController, quoteId: String? = null, groupId: String? = null, editId: String? = null) {
    val colors = CircleTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    var audience by rememberSaveable { mutableStateOf(Audience.FRIENDS) }
    var quoteRemoved by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var prefilled by rememberSaveable { mutableStateOf(false) }
    val media = rememberSaveable(saver = MediaListSaver) { mutableStateListOf() }

    val vm = circleViewModel(key = "compose-$quoteId-$groupId-$editId") { ComposeViewModel(it, quoteId, groupId, editId) }
    val group by vm.group.collectAsStateWithLifecycle()
    val quotedView by vm.quoted.collectAsStateWithLifecycle()
    val editingView by vm.editing.collectAsStateWithLifecycle()
    val circle by vm.circle.collectAsStateWithLifecycle()
    val editing = (editingView as? NoteView.Full)?.note?.takeIf { it.isMine }
    LaunchedEffect(editing?.id) {
        if (editing != null && !prefilled) { text = editing.text; prefilled = true }
    }
    // Only a note you can see in full can be quoted.
    val quoted = (quotedView as? NoteView.Full)?.takeIf { !quoteRemoved }
    val remaining = SOFT_LIMIT - text.length
    val hasContent = text.isNotBlank() || media.isNotEmpty() || (editing?.hasMedia == true)
    val canPost = hasContent && remaining >= 0 && (editId == null || editing != null)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val tooLong = stringResource(R.string.compose_video_too_long)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_MEDIA)) { uris ->
        scope.launch {
            uris.take(MAX_MEDIA - media.size).forEach { uri ->
                val picked = withContext(Dispatchers.IO) { describe(context, uri) }
                AppGraph.mediaStore.add(uri.toString(), picked.first, picked.second)
                    .onSuccess { media.add(it) }
                    .onFailure { if (it is VideoTooLong) Toast.makeText(context, tooLong, Toast.LENGTH_LONG).show() }
            }
        }
    }

    fun leave() = nav.popBackStack()
    fun cancel() {
        val changed = if (editing != null) text != editing.text else text.isNotBlank() || media.isNotEmpty()
        if (changed) confirmDiscard = true else leave()
    }
    fun post() {
        if (!canPost) return
        if (editId != null) {
            vm.save(editId, text) { if (it is Outcome.Success) leave() }
            return
        }
        val inGroup = group != null
        vm.post(text, audience, quoted?.note?.id, group?.ref?.id, media.toList()) { outcome ->
            if (outcome !is Outcome.Success) return@post
            if (inGroup) leave() // back to the group, where the post now sits at the top
            else nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } // Home, new note on top
        }
    }

    BackHandler(onBack = ::cancel)

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(if (editId != null) R.string.compose_discard_changes_title else R.string.compose_discard_title)) },
            text = { Text(stringResource(R.string.compose_discard_body)) },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; leave() }) { Text(stringResource(R.string.compose_discard), color = colors.accent) } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.compose_keep_editing), color = colors.text) } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.cancel), color = colors.body, fontSize = 14.5.sp, modifier = Modifier.clickable(onClick = ::cancel).padding(vertical = 10.dp))
            Box(
                modifier = Modifier
                    .alpha(if (canPost) 1f else 0.4f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.accent)
                    .clickable(enabled = canPost, role = Role.Button, onClick = ::post)
                    .semantics { if (!canPost) disabled() }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(stringResource(if (editId != null) R.string.compose_save else R.string.compose_post), color = colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))

        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Row {
                MeAvatar(38)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f).padding(top = 6.dp)) {
                    val placeholder = when {
                        quoted != null -> stringResource(R.string.compose_hint_quote)
                        group != null -> stringResource(R.string.compose_hint_group, group!!.ref.name)
                        else -> stringResource(R.string.compose_hint)
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
            if (media.isNotEmpty()) {
                Row(
                    Modifier.padding(start = 50.dp, top = 12.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    media.forEach { m ->
                        Box(Modifier.size(96.dp).clip(RoundedCornerShape(11.dp)).background(colors.track)) {
                            MediaThumb(m)
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp).clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f)).clickable { media.remove(m) },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.compose_remove_media), tint = Color.White, modifier = Modifier.size(15.dp)) }
                        }
                    }
                }
            }
            if (quoted != null) {
                Box(Modifier.padding(start = 50.dp, top = 12.dp)) {
                    EmbeddedPost(quoted)
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp).clip(CircleShape)
                            .clickable { quoteRemoved = true }
                            .semantics { contentDescription = context.getString(R.string.compose_remove_quote) },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Outlined.Close, contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp)) }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (editId == null) {
                val note = when {
                    group != null -> stringResource(R.string.compose_note_group, group!!.ref.name)
                    audience == Audience.FRIENDS -> stringResource(R.string.compose_note_friends)
                    else -> androidx.compose.ui.res.pluralStringResource(R.plurals.compose_note_inner, circle.innerCount, circle.innerCount)
                }
                Text(note, color = colors.muted, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                if (group == null) {
                    Text(
                        stringResource(R.string.compose_edit_inner),
                        color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.End).clickable { nav.navigate(Routes.YOUR_CIRCLE) }.padding(vertical = 8.dp)
                    )
                }
            }
        }

        // Bottom bar rides above the keyboard.
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                if (editId == null) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).clickable(enabled = media.size < MAX_MEDIA, role = Role.Button) {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Outlined.Image, contentDescription = stringResource(R.string.compose_add_media), tint = colors.muted) }
                } else Spacer(Modifier.size(44.dp))
                if (remaining <= 50) {
                    val left = stringResource(R.string.compose_chars_left, remaining)
                    Text(
                        "$remaining", color = if (remaining < 0) colors.alertDot else colors.muted,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { contentDescription = left }
                    )
                }
                when {
                    editId != null -> Spacer(Modifier.size(1.dp))
                    group != null -> Text(group!!.ref.name, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    else -> Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.surface)
                            .border(1.5.dp, colors.accent, RoundedCornerShape(999.dp))
                            .clickable(onClickLabel = stringResource(R.string.note_change_audience)) {
                                audience = if (audience == Audience.FRIENDS) Audience.INNER_CIRCLE else Audience.FRIENDS
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(if (audience == Audience.FRIENDS) R.string.compose_audience_friends else R.string.compose_audience_inner),
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

/** A picked item's kind and, for a video, its length. Keeps read access across restarts where allowed. */
private fun describe(context: Context, uri: Uri): Pair<MediaKind, Long?> {
    runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    val isVideo = context.contentResolver.getType(uri)?.startsWith("video") == true
    if (!isVideo) return MediaKind.PHOTO to null
    val r = MediaMetadataRetriever()
    val duration = try {
        r.setDataSource(context, uri)
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
    } catch (e: RuntimeException) {
        null
    } finally {
        r.release()
    }
    return MediaKind.VIDEO to duration
}
