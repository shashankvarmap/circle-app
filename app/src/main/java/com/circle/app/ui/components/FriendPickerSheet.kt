package com.circle.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.circle.app.R
import com.circle.app.data.repo.CircleRepository
import com.circle.app.ui.screens.SearchField
import com.circle.app.ui.theme.CircleTheme
import com.circle.app.ui.theme.Display
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel

class FriendPickerViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    val friends = repo.friends().state(emptyList())
}

/** Pick several friends (Invite friends to a group or an event). Friends only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendPickerSheet(title: String, action: String, onDismiss: () -> Unit, onPick: (List<String>) -> Unit) {
    val colors = CircleTheme.colors
    val vm = circleViewModel { FriendPickerViewModel(it) }
    val friends by vm.friends.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var picked by rememberSaveable { mutableStateOf(listOf<String>()) }
    val shown = friends.filter { query.isBlank() || it.person.name.contains(query.trim(), ignoreCase = true) }
    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), containerColor = colors.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, color = colors.text, modifier = Modifier.weight(1f).semantics { heading() })
                val enabled = picked.isNotEmpty()
                Box(
                    Modifier.alpha(if (enabled) 1f else 0.4f).clip(RoundedCornerShape(999.dp)).background(colors.accent)
                        .clickable(enabled = enabled, role = Role.Button) { onDismiss(); onPick(picked) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text(action, color = colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }
            SearchField(query, { query = it }, placeholder = stringResource(R.string.note_share_search), modifier = Modifier.padding(vertical = 12.dp))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 440.dp)) {
                items(shown, key = { it.person.id }) { f ->
                    val checked = f.person.id in picked
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(10.dp))
                            .clickable(role = Role.Checkbox) { picked = if (checked) picked - f.person.id else picked + f.person.id }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(f.person.initials, f.person.color, size = 36)
                        Spacer(Modifier.width(12.dp))
                        Text(f.person.name, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Checkbox(checked = checked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = colors.accent, checkmarkColor = colors.onAccent))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** A small form dialog with labeled text fields (edit group / edit event). */
@Composable
fun EditFieldsDialog(
    title: String,
    fields: List<Pair<String, MutableState<String>>>,
    extra: (@Composable () -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val colors = CircleTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                fields.forEach { (label, state) ->
                    OutlinedTextField(state.value, { state.value = it }, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
                }
                extra?.invoke()
            }
        },
        confirmButton = { TextButton(onClick = { onDismiss(); onSave() }) { Text(stringResource(R.string.compose_save), color = colors.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = colors.text) } }
    )
}
