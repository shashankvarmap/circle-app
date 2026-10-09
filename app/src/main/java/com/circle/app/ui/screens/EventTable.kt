package com.circle.app.ui.screens

import com.circle.app.ui.a11y.label
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circle.app.data.repo.EventView
import com.circle.app.data.repo.SeatKind
import com.circle.app.data.repo.SeatView
import com.circle.app.ui.components.MeAvatar
import com.circle.app.ui.a11y.rememberReduceMotion
import com.circle.app.ui.orbit.*
import com.circle.app.ui.theme.*

/**
 * The event as a table: one seat per place, seat 0 is the host (accent ring), filled seats are
 * people, empty seats are dashed circles, your seat has the accent ring. People open their profile.
 * The ring caps at 20 places with a "+N more" chip.
 */
@Composable
fun EventTable(view: EventView, showMap: Boolean, onPerson: (String) -> Unit) {
    if (showMap) TableOrbit(view, view.seats, onPerson) else TableList(view.seats, onPerson)
}

@Composable
private fun TableOrbit(view: EventView, seats: List<SeatView>, onPerson: (String) -> Unit) {
    val e = view.summary
    val colors = CircleTheme.colors
    val g = EventTableGeometry
    val reduceMotion = rememberReduceMotion()
    val size = g.seatSize(seats.size).dp
    val pts = ringPoints(g.CX, g.CY, g.SEAT_R, seats.size)

    OrbitMap(g.WIDTH, g.HEIGHT) {
        Canvas(Modifier.fillMaxSize()) { orbitRing(px(Pt(g.CX, g.CY)), px(g.SEAT_R), colors.border, dashed = true) }
        // The table: going / capacity
        Box(
            Modifier.centeredAt(Pt(g.CX, g.CY), (g.TABLE_R * 2).dp).size((g.TABLE_R * 2).dp)
                .clip(CircleShape).background(colors.surface).border(1.dp, colors.border, CircleShape)
                .then(Modifier.label(androidx.compose.ui.res.stringResource(com.circle.app.R.string.table_seats_taken_of, e.going, e.capacity))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    buildAnnotatedString {
                        append("${e.going}")
                        withStyle(SpanStyle(fontSize = 20.sp, color = colors.muted)) { append("/${e.capacity}") }
                    },
                    fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 42.sp, color = colors.text
                )
                Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.eventtable_at_the_table), color = colors.muted, fontSize = 11.sp)
            }
        }
        seats.forEachIndexed { i, seat ->
            val pt = pts[i]
            when (seat.kind) {
                SeatKind.PERSON, SeatKind.ME, SeatKind.TAKEN -> {
                    val p = seat.person
                    val isMe = seat.kind == SeatKind.ME
                    // TAKEN: someone hidden from you by a block. Just a filled seat, no name, no tap.
                    val fill = if (p == null) colors.track else p.color
                    OrbitNode(
                        center = pt, size = size, shape = NodeShape.Circle, fill = fill,
                        description = when {
                            p == null -> androidx.compose.ui.res.stringResource(com.circle.app.R.string.table_seat_taken)
                            isMe -> if (seat.isHost) androidx.compose.ui.res.stringResource(com.circle.app.R.string.eventtable_you_host) else androidx.compose.ui.res.stringResource(com.circle.app.R.string.chat_you)
                            else -> if (seat.isHost) androidx.compose.ui.res.stringResource(com.circle.app.R.string.table_name_host, p.name) else p.name
                        },
                        accentRing = seat.isHost || isMe, halo = 2.dp, reduceMotion = reduceMotion,
                        onClick = if (p == null || isMe) null else ({ onPerson(p.id) })
                    ) {
                        if (p != null) {
                            Text(
                                p.initials,
                                color = avatarTextColor(fill), fontSize = if (size.value >= 38f) 12.sp else 10.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                SeatKind.EMPTY -> OrbitNode(
                    center = pt, size = size, shape = NodeShape.Circle, fill = colors.background,
                    border = NodeBorder(2.dp, colors.muted, dashed = true), description = androidx.compose.ui.res.stringResource(com.circle.app.R.string.table_empty_seat),
                    reduceMotion = reduceMotion, alpha = 0.6f
                ) {}
                SeatKind.MORE -> OrbitNode(
                    center = pt, size = size, shape = NodeShape.Circle, fill = colors.track,
                    description = androidx.compose.ui.res.pluralStringResource(com.circle.app.R.plurals.table_more_seats, seat.moreCount, seat.moreCount), reduceMotion = reduceMotion
                ) { Text("+${seat.moreCount}", color = colors.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun TableList(seats: List<SeatView>, onPerson: (String) -> Unit) {
    Column {
        seats.filter { it.kind == SeatKind.PERSON || it.kind == SeatKind.ME }.forEach { seat ->
            val p = seat.person ?: return@forEach
            val label = buildString {
                append(if (seat.kind == SeatKind.ME) androidx.compose.ui.res.stringResource(com.circle.app.R.string.chat_you) else androidx.compose.ui.res.stringResource(com.circle.app.R.string.eventtable_at_the_table_2))
                if (seat.isHost) append(" · " + androidx.compose.ui.res.stringResource(com.circle.app.R.string.table_host))
            }
            PersonListRow(p, label, onPerson)
        }
        val taken = seats.count { it.kind == SeatKind.TAKEN }
        if (taken > 0) Text(
            androidx.compose.ui.res.pluralStringResource(com.circle.app.R.plurals.table_taken_seats, taken, taken),
            color = CircleTheme.colors.muted, fontSize = 12.5.sp, modifier = Modifier.padding(vertical = 4.dp)
        )
        val empty = seats.count { it.kind == SeatKind.EMPTY } + (seats.firstOrNull { it.kind == SeatKind.MORE }?.moreCount ?: 0)
        if (empty > 0) Text(
            androidx.compose.ui.res.pluralStringResource(com.circle.app.R.plurals.table_empty_seats, empty, empty),
            color = CircleTheme.colors.muted, fontSize = 12.5.sp, modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}
