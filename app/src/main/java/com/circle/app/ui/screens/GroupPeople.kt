package com.circle.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circle.app.data.repo.GroupView
import com.circle.app.data.repo.JoinRequestView
import com.circle.app.data.repo.PersonRef
import com.circle.app.ui.format.relativeTime
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.res.stringResource
import com.circle.app.R
import com.circle.app.ui.a11y.rememberReduceMotion
import com.circle.app.ui.components.Avatar
import com.circle.app.ui.components.GroupTile
import com.circle.app.ui.components.SectionLabel
import com.circle.app.ui.orbit.*
import com.circle.app.ui.theme.*

/** What sits in one of the 12 member-ring slots. */
private sealed interface Slot {
    data class Member(val p: PersonRef) : Slot
    data class Request(val p: PersonRef) : Slot
    data object More : Slot
}

/**
 * The People tab of a group: hosts on the inner ring, members on the dashed outer ring, join
 * requests as dashed accent circles (visible to every member, not just the hosts), and a "+N"
 * chip. Every person opens their profile, rendered by your relationship to them.
 */
@Composable
fun GroupPeople(group: GroupView, showMap: Boolean, onPerson: (String) -> Unit, host: HostActions? = null) {
    // Members only: the repository returns no people to anyone else.
    val people = group.people ?: return
    val hosts = people.hosts
    val members = people.members
    val hidden = people.hiddenCount

    if (showMap) GroupOrbit(group, hosts, members, people.requests.map { it.person }, hidden, onPerson)
    else GroupList(hosts, members, hidden, onPerson, host)
    AskedToJoin(people.requests, onPerson, host)
}

/** What a host can do in the People tab. Ordinary members get none of these. */
class HostActions(val approve: (String) -> Unit, val decline: (String) -> Unit, val remove: (String) -> Unit)

@Composable
private fun GroupOrbit(group: GroupView, hosts: List<PersonRef>, members: List<PersonRef>, requests: List<PersonRef>, hidden: Int, onPerson: (String) -> Unit) {
    val colors = CircleTheme.colors
    val g = GroupPeopleGeometry
    val reduceMotion = rememberReduceMotion()

    // Slot order from the brief: five members, a request, three members, a request, a member, the "+N" chip.
    val slots = buildList<Slot> {
        val m = members.iterator()
        val r = requests.iterator()
        for (i in 0 until g.MEMBER_SLOTS) {
            when {
                i == g.MEMBER_SLOTS - 1 && hidden > 0 -> add(Slot.More)
                (i == 5 || i == 9) && r.hasNext() -> add(Slot.Request(r.next()))
                m.hasNext() -> add(Slot.Member(m.next()))
                r.hasNext() -> add(Slot.Request(r.next()))
            }
        }
    }
    val shownMembers = slots.count { it is Slot.Member }
    val more = (group.memberCount - hosts.size - shownMembers).coerceAtLeast(0)

    OrbitMap(g.WIDTH, g.HEIGHT) {
        Canvas(Modifier.fillMaxSize()) {
            val c = px(Pt(g.CX, g.CY))
            orbitRing(c, px(g.MEMBER_R), colors.border, dashed = true)
            orbitRing(c, px(g.HOST_R), colors.border, dashed = false)
        }
        Box(Modifier.centeredAt(Pt(g.CX, g.CY), g.CENTER_TILE.dp)) { GroupTile(group.ref.initial, group.ref.color, size = g.CENTER_TILE.toInt()) }
        OrbitLabel("HOSTS", 8.5f, Modifier.placedAt(g.CX - 40f, g.CY + 28f).width((80 * scale).dp))

        hosts.take(2).forEachIndexed { i, p ->
            OrbitNode(
                center = compassPoint(g.CX, g.CY, g.HOST_R, if (i == 0) 0f else 180f),
                size = 38.dp, shape = NodeShape.Circle, fill = p.color,
                description = "${p.name}, host", accentRing = true, halo = 2.dp,
                reduceMotion = reduceMotion, onClick = { onPerson(p.id) }
            ) { Text(p.initials, color = avatarTextColor(p.color), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
        slots.forEachIndexed { i, slot ->
            val pt = compassPoint(g.CX, g.CY, g.MEMBER_R, g.MEMBER_OFFSET_DEG + i * 30f)
            when (slot) {
                is Slot.Member -> OrbitNode(
                    center = pt, size = 36.dp, shape = NodeShape.Circle, fill = slot.p.color,
                    description = "${slot.p.name}, member", halo = 2.dp, reduceMotion = reduceMotion,
                    onClick = { onPerson(slot.p.id) }
                ) { Text(slot.p.initials, color = avatarTextColor(slot.p.color), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                is Slot.Request -> OrbitNode(
                    center = pt, size = 36.dp, shape = NodeShape.Circle, fill = colors.background,
                    border = NodeBorder(2.dp, colors.accent, dashed = true),
                    description = "${slot.p.name}, asked to join", reduceMotion = reduceMotion,
                    onClick = { onPerson(slot.p.id) }
                ) { Text(slot.p.initials, color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                is Slot.More -> OrbitNode(
                    center = pt, size = 36.dp, shape = NodeShape.Circle, fill = colors.track,
                    description = "$more more members", reduceMotion = reduceMotion
                ) { Text("+$more", color = colors.muted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun GroupList(hosts: List<PersonRef>, members: List<PersonRef>, hidden: Int, onPerson: (String) -> Unit, host: HostActions?) {
    Column {
        ListHeading("HOSTS")
        hosts.forEach { PersonListRow(it, "Host", onPerson) }
        ListHeading("MEMBERS")
        members.forEach { m ->
            PersonListRow(m, "Member", onPerson) {
                if (host != null) SmallPill(stringResource(R.string.group_remove), filled = false) { host.remove(m.id) }
            }
        }
        if (hidden > 0) {
            Text("and $hidden more members", color = CircleTheme.colors.muted, fontSize = 12.5.sp, modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun AskedToJoin(requests: List<JoinRequestView>, onPerson: (String) -> Unit, host: HostActions?) {
    if (requests.isEmpty()) return
    Column(Modifier.padding(top = 8.dp)) {
        ListHeading("ASKED TO JOIN · ${requests.size}")
        Text(
            "Dashed circles are join requests. Every member can see them, not just the hosts.",
            color = CircleTheme.colors.muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp)
        )
        requests.forEach { r ->
            PersonListRow(r.person, "asked ${relativeTime(r.askedAt)} ago", onPerson) {
                if (host != null) {
                    SmallPill(stringResource(R.string.group_approve), filled = true) { host.approve(r.person.id) }
                    Spacer(Modifier.width(6.dp))
                    SmallPill(stringResource(R.string.profile_decline), filled = false) { host.decline(r.person.id) }
                }
            }
        }
    }
}

@Composable
private fun ListHeading(text: String) {
    Box(Modifier.padding(top = 14.dp, bottom = 4.dp).semantics { heading() }) { SectionLabel(text) }
}

/** A person in a list: the whole row (at least 48dp) opens their profile. */
@Composable
fun PersonListRow(p: PersonRef, subtitle: String, onPerson: (String) -> Unit, trailing: (@Composable RowScope.() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).heightIn(min = 52.dp).clip(RoundedCornerShape(10.dp))
                .clickable(onClickLabel = "Open ${p.name}'s profile") { onPerson(p.id) }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(p.initials, p.color, size = 36)
            Spacer(Modifier.width(11.dp))
            Column {
                Text(p.name, color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                Text(subtitle, color = CircleTheme.colors.muted, fontSize = 11.5.sp)
            }
        }
        trailing?.invoke(this)
    }
}

@Composable
fun SmallPill(text: String, filled: Boolean, onClick: () -> Unit) {
    val colors = CircleTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Box(
        Modifier.heightIn(min = 40.dp).clip(shape)
            .background(if (filled) colors.accent else colors.background)
            .border(1.dp, if (filled) colors.accent else colors.border, shape)
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
            .padding(horizontal = 13.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = if (filled) colors.onAccent else colors.text, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
}
