package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.data.access.EventVisibility
import com.circle.app.data.repo.Outcome
import com.circle.app.ui.vm.circleViewModel
import com.circle.app.ui.components.SectionLabel
import com.circle.app.ui.theme.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.circle.app.R
import com.circle.app.navigation.Routes
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val Topics = listOf("OUTDOORS", "SOCIAL", "MUSIC", "MAKING")

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(nav: NavHostController) {
    val vm = circleViewModel { CreateViewModel(it) }
    var capacity by rememberSaveable { mutableStateOf(12) }
    var visibility by rememberSaveable { mutableStateOf(0) } // 0 friends, 1 fof, 2 open
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var venue by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf("SOCIAL") }
    // Date (epoch day) and time (minutes after midnight), in the viewer's zone. Default: tomorrow 18:00.
    val zone = ZoneId.systemDefault()
    var dateDay by rememberSaveable { mutableStateOf(LocalDate.now(zone).plusDays(1).toEpochDay()) }
    var minutes by rememberSaveable { mutableStateOf(18 * 60) }
    var picking by rememberSaveable { mutableStateOf<String?>(null) } // "date" | "time"
    val visibilityLabels = listOf("Friends", "Friends of friends", "Open nearby")
    val startsAt = LocalDate.ofEpochDay(dateDay).atTime(LocalTime.of(minutes / 60, minutes % 60)).atZone(zone).toInstant()
    val inPast = startsAt.isBefore(java.time.Instant.now())

    if (picking == "date") {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(dateDay).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dateDay = java.time.Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
                    picking = null
                }) { Text(stringResource(R.string.compose_save), color = CircleTheme.colors.accent) }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text(stringResource(R.string.cancel), color = CircleTheme.colors.text) } }
        ) { DatePicker(state) }
    }
    if (picking == "time") {
        val state = rememberTimePickerState(minutes / 60, minutes % 60, is24Hour = android.text.format.DateFormat.is24HourFormat(androidx.compose.ui.platform.LocalContext.current))
        AlertDialog(
            onDismissRequest = { picking = null },
            text = { TimePicker(state) },
            confirmButton = { TextButton(onClick = { minutes = state.hour * 60 + state.minute; picking = null }) { Text(stringResource(R.string.compose_save), color = CircleTheme.colors.accent) } },
            dismissButton = { TextButton(onClick = { picking = null }) { Text(stringResource(R.string.cancel), color = CircleTheme.colors.text) } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cancel", color = CircleTheme.colors.body, fontSize = 14.5.sp, modifier = Modifier.clickable { nav.popBackStack() })
            Text("New event", color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            val canCreate = title.isNotBlank() && !inPast
            Box(
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(if (canCreate) CircleTheme.colors.accent else CircleTheme.colors.track)
                    .clickable(enabled = canCreate) {
                        vm.createEvent(title, EventVisibility.entries[visibility], description, capacity, startsAt, topic, venue) {
                            if (it is Outcome.Success) nav.navigate(Routes.eventDetail(it.value)) { popUpTo(Routes.CREATE_EVENT) { inclusive = true } }
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) { Text("Create", color = if (canCreate) CircleTheme.colors.onAccent else CircleTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LabeledField("TITLE", title, { title = it }, "e.g. Board Game Night")

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel("DATE")
                    PickerRow(Icons.Outlined.CalendarToday, LocalDate.ofEpochDay(dateDay).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))) { picking = "date" }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel("TIME")
                    PickerRow(Icons.Outlined.Schedule, com.circle.app.ui.format.clockTime(startsAt)) { picking = "time" }
                }
            }
            if (inPast) Text(stringResource(R.string.event_in_past), color = CircleTheme.colors.alertDot, fontSize = 12.sp)

            LabeledField("VENUE", venue, { venue = it.take(80) }, "A public place, e.g. Kaffeehaus Zeitlos")

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("TOPIC")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Topics.forEach { t -> VisibilityChip(t.lowercase().replaceFirstChar { it.uppercase() }, topic == t) { topic = t } }
                }
                Text(stringResource(R.string.event_topic_note), color = CircleTheme.colors.muted, fontSize = 11.sp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel("CAPACITY")
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(CircleTheme.colors.surface).padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Max attendees", color = CircleTheme.colors.body, fontSize = 13.5.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            modifier = Modifier.size(26.dp).clip(CircleShape).background(CircleTheme.colors.track)
                                .clickable { if (capacity > 1) capacity-- },
                            contentAlignment = Alignment.Center
                        ) { Text("−", color = CircleTheme.colors.text, fontWeight = FontWeight.Bold) }
                        Text("$capacity", color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Box(
                            modifier = Modifier.size(26.dp).clip(CircleShape).background(CircleTheme.colors.track)
                                .clickable { capacity++ },
                            contentAlignment = Alignment.Center
                        ) { Text("+", color = CircleTheme.colors.text, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("WHO CAN SEE THIS")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    visibilityLabels.forEachIndexed { i, label ->
                        VisibilityChip(label, visibility == i) { visibility = i }
                    }
                }
            }

            LabeledField("DESCRIPTION", description, { description = it }, "What should people know before joining?", multiline = true)

            Text(
                "The conversation opens only for people who take a seat. It isn't a public comment section. Report and block are on every event.",
                color = CircleTheme.colors.muted, fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun PickerRow(icon: androidx.compose.ui.graphics.vector.ImageVector, placeholder: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(11.dp)).background(CircleTheme.colors.surface)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = CircleTheme.colors.muted, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(8.dp))
        Text(placeholder, color = CircleTheme.colors.text, fontSize = 13.5.sp)
    }
}

@Composable
private fun VisibilityChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) CircleTheme.colors.accent else androidx.compose.ui.graphics.Color.Transparent)
            .border(1.dp, if (selected) CircleTheme.colors.accent else CircleTheme.colors.border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (selected) CircleTheme.colors.onAccent else CircleTheme.colors.body, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
    }
}
