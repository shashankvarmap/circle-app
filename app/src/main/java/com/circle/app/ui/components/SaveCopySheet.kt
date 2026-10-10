package com.circle.app.ui.components

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkInfo
import com.circle.app.R
import com.circle.app.data.export.ExportKind
import com.circle.app.data.export.ExportPlan
import com.circle.app.data.repo.CircleRepository
import com.circle.app.export.SaveCopyJobs
import com.circle.app.export.SaveCopyWorker
import com.circle.app.ui.theme.CircleTheme
import com.circle.app.ui.theme.Display
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import java.text.NumberFormat

class SaveCopyViewModel(repo: CircleRepository, id: String) : RepoViewModel(repo) {
    val plan = repo.exportPlan(id).state(null)
}

/**
 * Save a copy (TIMED_GROUPS 8): choose Everything or Text only, then a progress state while the
 * job runs (it keeps going if you leave), then Saved to your phone. [id] is a group, a chat with
 * friends, or a group's chat. Failures say what happened and offer Try again.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveCopySheet(id: String, onDismiss: () -> Unit, onSaved: () -> Unit = {}) {
    val colors = CircleTheme.colors
    val context = LocalContext.current
    val vm = circleViewModel(key = "save-$id") { SaveCopyViewModel(it, id) }
    val plan by vm.plan.collectAsStateWithLifecycle()
    val job by remember(id) { SaveCopyJobs.follow(context, id) }.collectAsStateWithLifecycle(null)
    var everything by rememberSaveable { mutableStateOf(true) }
    // A job already running when the sheet opens (you came back to it) shows its progress.
    var started by rememberSaveable { mutableStateOf(false) }
    // Until this run is seen, an older finished job (a copy saved earlier) must not show as done.
    var seenRunning by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(job?.state) {
        if (job?.state == WorkInfo.State.RUNNING || job?.state == WorkInfo.State.ENQUEUED) { started = true; seenRunning = true }
    }
    val state = when {
        !started -> null
        !seenRunning -> WorkInfo.State.ENQUEUED
        else -> job?.state
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), containerColor = colors.surface
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 28.dp)) {
            when (state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED -> {
                    val p = job?.progress?.getInt(SaveCopyWorker.KEY_PROGRESS, 0) ?: 0
                    Title(stringResource(R.string.save_preparing))
                    Text(stringResource(R.string.save_keep_open), color = colors.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    val label = stringResource(R.string.save_progress_cd, p)
                    Box(
                        Modifier.padding(top = 20.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)).background(colors.track)
                            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(p / 100f, 0f..1f) }
                    ) { Box(Modifier.fillMaxHeight().fillMaxWidth(p / 100f).background(colors.accent)) }
                    Text("$p%", color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp).semantics { contentDescription = label })
                    TextAction(stringResource(R.string.save_cancel), colors.muted) { SaveCopyJobs.cancel(context, id); started = false }
                }
                WorkInfo.State.SUCCEEDED -> {
                    val out = job!!.outputData
                    val size = Formatter.formatShortFileSize(context, out.getLong(SaveCopyWorker.KEY_SIZE, 0))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.size(56.dp).clip(CircleShape).background(colors.accent.copy(alpha = 0.09f)).border(1.dp, colors.accent.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Outlined.Check, null, tint = colors.accent, modifier = Modifier.size(26.dp)) }
                    }
                    Text(
                        stringResource(R.string.save_done_title), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = colors.text,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).semantics { heading() }
                    )
                    Row(
                        Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, colors.border, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp).semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.Description, null, tint = colors.accent, modifier = Modifier.size(22.dp))
                        Column(Modifier.weight(1f)) {
                            Text(out.getString(SaveCopyWorker.KEY_NAME).orEmpty(), color = colors.text, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                stringResource(if (out.getString(SaveCopyWorker.KEY_WHERE) == SaveCopyWorker.WHERE_APP) R.string.save_where_app else R.string.save_where_downloads, size),
                                color = colors.muted, fontSize = 12.sp
                            )
                        }
                    }
                    PrimaryButton(stringResource(R.string.save_done), top = 16) { onSaved(); onDismiss() }
                }
                WorkInfo.State.FAILED -> {
                    Title(stringResource(R.string.save_title))
                    val why = when (job?.outputData?.getString(SaveCopyWorker.KEY_FAIL)) {
                        SaveCopyWorker.FAIL_OFFLINE -> R.string.save_fail_offline
                        SaveCopyWorker.FAIL_DISK_FULL -> R.string.save_fail_disk
                        SaveCopyWorker.FAIL_GONE -> R.string.save_fail_gone
                        else -> R.string.save_fail_other
                    }
                    Text(stringResource(why), color = colors.text, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 8.dp))
                    if (why != R.string.save_fail_gone) PrimaryButton(stringResource(R.string.save_try_again), top = 16) { seenRunning = false; SaveCopyJobs.start(context, id, everything) }
                    TextAction(stringResource(R.string.save_done), colors.muted, onDismiss)
                }
                else -> Choose(plan, everything, { everything = it }) { started = true; seenRunning = false; SaveCopyJobs.start(context, id, everything) }
            }
        }
    }
}

@Composable
private fun Choose(plan: ExportPlan?, everything: Boolean, onPick: (Boolean) -> Unit, onSave: () -> Unit) {
    val colors = CircleTheme.colors
    val context = LocalContext.current
    Title(stringResource(R.string.save_title))
    Text(stringResource(R.string.save_intro), color = colors.muted, fontSize = 13.sp, lineHeight = 19.5.sp, modifier = Modifier.padding(top = 4.dp))
    if (plan == null) return
    val nf = NumberFormat.getIntegerInstance()
    val media = pluralStringResource(R.plurals.save_n_media, plan.mediaCount, nf.format(plan.mediaCount))
    val group = plan.kind == ExportKind.GROUP
    val all = if (group) stringResource(R.string.save_desc_group_all, pluralStringResource(R.plurals.save_n_chat_messages, plan.messageCount, nf.format(plan.messageCount)), media)
        else stringResource(R.string.save_desc_chat_all, pluralStringResource(R.plurals.save_n_messages, plan.messageCount, nf.format(plan.messageCount)), media)
    Option(stringResource(R.string.save_everything), all, Formatter.formatShortFileSize(context, plan.everythingBytes), everything, top = 14) { onPick(true) }
    Option(
        stringResource(R.string.save_text_only), stringResource(if (group) R.string.save_desc_group_text else R.string.save_desc_chat_text),
        Formatter.formatShortFileSize(context, plan.textBytes), !everything, top = 10
    ) { onPick(false) }
    Text(stringResource(R.string.save_names_only), color = colors.muted, fontSize = 11.5.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 10.dp))
    PrimaryButton(stringResource(R.string.save_to_phone), top = 14, onClick = onSave)
}

@Composable
private fun Title(text: String) {
    Text(text, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = CircleTheme.colors.text, modifier = Modifier.semantics { heading() })
}

@Composable
private fun Option(title: String, body: String, size: String, selected: Boolean, top: Int, onClick: () -> Unit) {
    val colors = CircleTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.padding(top = top.dp).fillMaxWidth().clip(shape)
            .background(if (selected) colors.accent.copy(alpha = 0.08f) else colors.surface)
            .border(1.5.dp, if (selected) colors.accent else colors.border, shape)
            .clickable(role = Role.RadioButton, onClick = onClick).semantics(mergeDescendants = true) { this.selected = selected }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(body, color = colors.muted, fontSize = 12.5.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text(size, color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PrimaryButton(text: String, top: Int, onClick: () -> Unit) {
    val colors = CircleTheme.colors
    Box(
        Modifier.padding(top = top.dp).fillMaxWidth().heightIn(min = 50.dp).clip(RoundedCornerShape(999.dp)).background(colors.accent)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = colors.onAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun TextAction(text: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(
        Modifier.padding(top = 8.dp).fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(999.dp)).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
}
