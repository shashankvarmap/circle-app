package com.circle.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circle.app.R
import com.circle.app.data.ReportKind
import com.circle.app.data.ReportReason
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.Outcome
import com.circle.app.ui.theme.Body
import com.circle.app.ui.theme.CircleTheme
import com.circle.app.ui.theme.Display
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel

class ReportViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    fun send(kind: ReportKind, id: String, reason: ReportReason, note: String, then: (Outcome<Unit>) -> Unit) =
        run({ report(kind, id, reason, note) }, then)
}

private val reasonLabels = mapOf(
    ReportReason.SPAM to R.string.report_reason_spam,
    ReportReason.HARASSMENT to R.string.report_reason_harassment,
    ReportReason.HATE to R.string.report_reason_hate,
    ReportReason.SEXUAL to R.string.report_reason_sexual,
    ReportReason.VIOLENCE to R.string.report_reason_violence,
    ReportReason.SELF_HARM to R.string.report_reason_self_harm,
    ReportReason.IMPERSONATION to R.string.report_reason_impersonation,
    ReportReason.OTHER to R.string.report_reason_other,
)

/**
 * The report flow (FRONTEND_COMPLETE 7): a reason, an optional note, then "Thanks. We'll look at this."
 * The report is recorded; nobody is told who reported them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSheet(kind: ReportKind, targetId: String, onDone: () -> Unit) {
    val colors = CircleTheme.colors
    val vm = circleViewModel { ReportViewModel(it) }
    var reason by rememberSaveable { mutableStateOf<ReportReason?>(null) }
    var note by rememberSaveable { mutableStateOf("") }
    var sent by rememberSaveable { mutableStateOf(false) }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDone, sheetState = sheet, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), containerColor = colors.surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            if (sent) {
                Text(
                    stringResource(R.string.report_thanks), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 21.sp,
                    color = colors.text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().semantics { heading() }
                )
                Text(
                    stringResource(R.string.report_thanks_body), color = colors.muted, fontSize = 13.5.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 18.dp)
                )
                ReportButton(stringResource(R.string.report_done), enabled = true, onClick = onDone)
                return@Column
            }
            Text(
                stringResource(R.string.report_title), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 21.sp,
                color = colors.text, modifier = Modifier.semantics { heading() }
            )
            Text(stringResource(R.string.report_subtitle), color = colors.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            ReportReason.entries.forEach { r ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp))
                        .selectable(selected = reason == r, role = Role.RadioButton) { reason = r },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = reason == r, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = colors.accent))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(reasonLabels.getValue(r)), color = colors.text, fontSize = 14.5.sp)
                }
            }
            val hint = stringResource(R.string.report_note_hint)
            Box(
                Modifier.padding(top = 10.dp).fillMaxWidth().heightIn(min = 72.dp).clip(RoundedCornerShape(12.dp))
                    .background(colors.background).border(1.dp, colors.border, RoundedCornerShape(12.dp)).padding(12.dp)
            ) {
                if (note.isEmpty()) Text(hint, color = colors.muted, fontSize = 14.sp)
                BasicTextField(
                    note, { note = it.take(500) },
                    textStyle = TextStyle(color = colors.text, fontSize = 14.sp, fontFamily = Body),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint }
                )
            }
            Spacer(Modifier.height(16.dp))
            ReportButton(stringResource(R.string.report_send), enabled = reason != null) {
                reason?.let { r -> vm.send(kind, targetId, r, note) { if (it is Outcome.Success) sent = true } }
            }
        }
    }
}

@Composable
private fun ReportButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = CircleTheme.colors
    Box(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).alpha(if (enabled) 1f else 0.4f).clip(RoundedCornerShape(999.dp))
            .background(colors.accent).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
}
