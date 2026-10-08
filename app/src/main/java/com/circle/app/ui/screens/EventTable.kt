package com.circle.app.ui.screens

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
import com.circle.app.data.*
import com.circle.app.ui.a11y.rememberReduceMotion
import com.circle.app.ui.orbit.*
import com.circle.app.ui.theme.*

/**
 * The event as a table: one seat per place, seat 0 is the host (accent ring), filled seats are
 * people, empty seats are dashed circles, your seat has the accent ring. People open their profile.
 * The ring caps at 20 places with a "+N more" chip.
 */
@Composable
fun EventTable(e: EventInfo, showMap: Boolean, onPerson: (String) -> Unit) {
    val seats = eventSeats(e, e.joined)
    if (showMap) TableOrbit(e, seats, onPerson) else TableList(seats, onPerson)
}

@Composable
private fun TableOrbit(e: EventInfo, seats: List<Seat>, onPerson: (String) -> Unit) {
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
                .semantics { contentDescription = "${e.going} of ${e.capacity} seats taken" },
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
                Text("at the table", color = colors.muted, fontSize = 11.sp)
            }
        }
        seats.forEachIndexed { i, seat ->
            val pt = pts[i]
            when (seat.kind) {
                SeatKind.PERSON, SeatKind.ME -> {
                    val p = seat.personId?.let { CircleStore.person(it) }
                    val isMe = seat.kind == SeatKind.ME
                    val blocked = p != null && CircleStore.relationshipTo(p.id) == Relationship.BLOCKED
                    val fill = if (blocked || p == null) colors.track else p.color
                    OrbitNode(
                        center = pt, size = size, shape = NodeShape.Circle, fill = fill,
                        description = when {
                            blocked -> "Seat taken"
                            isMe -> if (seat.isHost) "You, host" else "You"
                            else -> "${p?.name}" + if (seat.isHost) ", host" else ""
                        },
                        accentRing = seat.isHost || isMe, halo = 2.dp, reduceMotion = reduceMotion,
                        onClick = if (blocked || p == null) null else ({ onPerson(p.id) })
                    ) {
                        if (!blocked && p != null) {
                            Text(
                                if (isMe) CircleStore.profile.initial else p.initials,
                                color = avatarTextColor(fill), fontSize = if (size.value >= 38f) 12.sp else 10.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                SeatKind.EMPTY -> OrbitNode(
                    center = pt, size = size, shape = NodeShape.Circle, fill = colors.background,
                    border = NodeBorder(2.dp, colors.muted, dashed = true), description = "Empty seat",
                    reduceMotion = reduceMotion, alpha = 0.6f
                ) {}
                SeatKind.MORE -> OrbitNode(
                    center = pt, size = size, shape = NodeShape.Circle, fill = colors.track,
                    description = "${seat.moreCount} more seats", reduceMotion = reduceMotion
                ) { Text("+${seat.moreCount}", color = colors.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun TableList(seats: List<Seat>, onPerson: (String) -> Unit) {
    Column {
        seats.filter { it.kind == SeatKind.PERSON || it.kind == SeatKind.ME }.forEach { seat ->
            val p = seat.personId?.let { CircleStore.person(it) } ?: return@forEach
            if (CircleStore.relationshipTo(p.id) == Relationship.BLOCKED) return@forEach
            val label = buildString {
                append(if (seat.kind == SeatKind.ME) "You" else when (CircleStore.relationshipTo(p.id)) {
                    Relationship.FRIEND -> "Friend"
                    else -> if (p.mutualFriends == 1) "1 mutual friend" else "${p.mutualFriends} mutual friends"
                })
                if (seat.isHost) append(" · host")
            }
            PersonListRow(p, label, onPerson)
        }
        val empty = seats.count { it.kind == SeatKind.EMPTY } + (seats.firstOrNull { it.kind == SeatKind.MORE }?.moreCount ?: 0)
        if (empty > 0) Text(
            if (empty == 1) "1 empty seat" else "$empty empty seats",
            color = CircleTheme.colors.muted, fontSize = 12.5.sp, modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}
