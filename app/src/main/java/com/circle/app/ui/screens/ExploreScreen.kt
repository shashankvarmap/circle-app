package com.circle.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.EventSummary
import com.circle.app.data.repo.ExploreView
import com.circle.app.data.repo.GroupView
import com.circle.app.ui.format.*
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel

import com.circle.app.navigation.Routes
import com.circle.app.ui.a11y.rememberReduceMotion
import com.circle.app.ui.a11y.rememberScreenReaderOn
import com.circle.app.ui.components.*
import com.circle.app.ui.orbit.*
import com.circle.app.ui.theme.*

/** One thing on the Explore map: an event (inner ring) or a public group (outer ring). */
private sealed interface ExploreItem {
    val key: String
    val compassDeg: Float?
    val title: String

    data class Event(val e: EventSummary) : ExploreItem {
        override val key get() = "e:${e.id}"
        override val compassDeg get() = e.compassDeg
        override val title get() = e.title
    }

    data class Group(val g: GroupView) : ExploreItem {
        override val key get() = "g:${g.id}"
        override val compassDeg get() = g.compassDeg
        override val title get() = g.name
    }
}

class ExploreViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    val explore = repo.explore().state(ExploreView(emptyList(), emptyList()))
}

/**
 * Explore: the one public layer. Angle = topic, distance = how soon (events this week on the
 * inner ring, groups on the outer ring). Never ranked or reordered by popularity.
 */
@Composable
fun ExploreScreen(nav: NavHostController) {
    val colors = CircleTheme.colors
    val screenReader = rememberScreenReaderOn()
    var showMap by rememberSaveable { mutableStateOf(!screenReader) }
    var selectedKey by rememberSaveable { mutableStateOf("e:board-game-night") }
    var topic by rememberSaveable { mutableStateOf<String?>(null) } // Sector name, null = all

    // Private groups and events you may not see never reach this screen (the repository filters them).
    val vm = circleViewModel { ExploreViewModel(it) }
    val explore by vm.explore.collectAsStateWithLifecycle()
    val events = explore.events.map { ExploreItem.Event(it) }
    val groups = explore.groups.map { ExploreItem.Group(it) }
    val onMap = (events + groups).filter { it.compassDeg != null }
    val selected = onMap.firstOrNull { it.key == selectedKey } ?: onMap.firstOrNull()

    fun open(item: ExploreItem) = when (item) {
        is ExploreItem.Event -> nav.navigate(Routes.eventDetail(item.e.id))
        is ExploreItem.Group -> nav.navigate(Routes.groupDetail(item.g.id))
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        ExploreHeader(nav)

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (events.isEmpty() && groups.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        EmptyKind.EXPLORE,
                        action = androidx.compose.ui.res.stringResource(com.circle.app.R.string.empty_explore_action),
                        onAction = { nav.navigate(Routes.CREATE_EVENT) }
                    )
                }
            } else if (showMap) {
                item(key = "map") {
                    Box {
                        ExploreMap(onMap, selected?.key) { selectedKey = it.key }
                        MapListToggle(showMap, { showMap = it }, Modifier.align(Alignment.TopEnd).padding(end = 14.dp))
                    }
                }
                if (selected != null) item(key = "preview") { PreviewCard(selected) { open(selected) } }
                item(key = "foot") {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Placed by topic and how soon. Nothing here is ranked.",
                            color = colors.muted, fontSize = 11.sp, textAlign = TextAlign.Center
                        )
                        Text(
                            "See all", color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.5.sp,
                            modifier = Modifier.clickable(onClickLabel = "See everything as a list") { showMap = false }.padding(10.dp)
                        )
                    }
                }
            } else {
                item(key = "list-head") {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 14.dp, top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Everything nearby", color = colors.text, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            modifier = Modifier.weight(1f).semantics { heading() }
                        )
                        MapListToggle(showMap, { showMap = it })
                    }
                }
                item(key = "chips") { TopicChips(topic) { topic = it } }

                fun inTopic(item: ExploreItem) =
                    topic == null || item.compassDeg?.let { Sector.of(it).name } == topic
                // Events by how soon, groups by name. Never by popularity.
                val listEvents = events.filter(::inTopic).sortedBy { it.e.startsAt }
                val listGroups = groups.filter(::inTopic).sortedBy { it.g.name }

                if (listEvents.isEmpty() && listGroups.isEmpty()) {
                    item(key = "empty") {
                        Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            val name = Sector.entries.firstOrNull { it.name == topic }?.label?.lowercase() ?: ""
                            Text("Nothing in $name this week.", color = colors.muted, fontSize = 13.sp)
                            Text(
                                "Show all topics", color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                modifier = Modifier.clickable { topic = null }.padding(8.dp)
                            )
                        }
                    }
                }
                if (listEvents.isNotEmpty()) {
                    item(key = "events-label") { ListSection("EVENTS · THIS WEEK") }
                    listEvents.forEach { item -> item(key = item.key) { EventListRow(item.e) { open(item) } } }
                }
                if (listGroups.isNotEmpty()) {
                    item(key = "groups-label") { ListSection("GROUPS") }
                    listGroups.forEach { item -> item(key = item.key) { GroupListRow(item.g) { open(item) } } }
                }
                item(key = "list-foot") {
                    Text(
                        "Placed by topic and how soon. Nothing here is ranked.",
                        color = colors.muted, fontSize = 11.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    )
                }
            }
        }
        CircleBottomNav(nav, NavTab.EXPLORE)
    }
}

@Composable
private fun ExploreHeader(nav: NavHostController) {
    val colors = CircleTheme.colors
    var cityMenu by remember { mutableStateOf(false) }
    var createMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Explore", fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 24.sp,
                letterSpacing = (-0.48).sp, color = colors.text,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(999.dp))
                        .clickable(onClickLabel = "Change city") { cityMenu = true }
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = colors.text, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Aachen", color = colors.text, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = colors.muted, modifier = Modifier.size(12.dp))
                }
                DropdownMenu(expanded = cityMenu, onDismissRequest = { cityMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Aachen") },
                        trailingIcon = { Icon(Icons.Outlined.Check, contentDescription = "Current city", tint = colors.accent) },
                        onClick = { cityMenu = false }
                    )
                    DropdownMenuItem(text = { Text("Other cities aren't available yet") }, enabled = false, onClick = {})
                }
            }
            Spacer(Modifier.width(6.dp))
            Box {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).clickable { createMenu = true },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Add, contentDescription = "Create a group or event", tint = colors.accent) }
                DropdownMenu(expanded = createMenu, onDismissRequest = { createMenu = false }) {
                    DropdownMenuItem(text = { Text("Create group") }, onClick = {
                        createMenu = false; nav.navigate(Routes.CREATE_GROUP)
                    })
                    DropdownMenuItem(text = { Text("Create event") }, onClick = {
                        createMenu = false; nav.navigate(Routes.CREATE_EVENT)
                    })
                }
            }
        }
        Row(
            modifier = Modifier
                .padding(top = 9.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClickLabel = "Open search") { nav.navigate(Routes.EXPLORE_SEARCH) }
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(9.dp))
            Text("Search", color = colors.muted, fontSize = 13.5.sp)
        }
    }
}

@Composable
private fun ExploreMap(items: List<ExploreItem>, selectedKey: String?, onSelect: (ExploreItem) -> Unit) {
    val colors = CircleTheme.colors
    val g = ExploreGeometry
    val reduceMotion = rememberReduceMotion()

    OrbitMap(g.WIDTH, g.HEIGHT) {
        Canvas(Modifier.fillMaxSize()) {
            val c = px(Pt(g.CX, g.CY))
            orbitRing(c, px(g.GROUP_RING_R), colors.border, dashed = true)
            orbitRing(c, px(g.EVENT_RING_R), colors.border, dashed = false)
            g.SPOKE_DEG.forEach { deg ->
                drawLine(
                    colors.border.copy(alpha = colors.border.alpha * 0.8f),
                    start = px(compassPoint(g.CX, g.CY, g.SPOKE_FROM, deg)),
                    end = px(compassPoint(g.CX, g.CY, g.SPOKE_TO, deg)),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
        Sector.entries.forEach { s ->
            val p = compassPoint(g.CX, g.CY, g.LABEL_R, s.labelDeg)
            OrbitLabel(s.label, 8.5f, Modifier.placedAt(p.x - 34f, p.y - 6f).width((68 * scale).dp))
        }
        Box(
            modifier = Modifier.centeredAt(Pt(g.CX, g.CY), g.CENTER_PIN.dp).size(g.CENTER_PIN.dp)
                .clip(CircleShape).background(colors.accent),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Outlined.LocationOn, contentDescription = "Aachen, the centre of the map", tint = colors.onAccent, modifier = Modifier.size(21.dp)) }
        OrbitLabel("AACHEN", 9f, Modifier.placedAt(155f, 196f).width((80 * scale).dp))

        items.forEach { item ->
            val isSelected = item.key == selectedKey
            when (item) {
                is ExploreItem.Event -> {
                    val e = item.e
                    OrbitNode(
                        center = compassPoint(g.CX, g.CY, g.EVENT_RING_R, e.compassDeg!!),
                        size = g.EVENT_NODE.dp,
                        shape = NodeShape.Circle,
                        fill = colors.surface,
                        border = NodeBorder(1.5.dp, colors.accent),
                        description = "${e.title}, event, ${e.dateSpoken()}, ${e.going} of ${e.capacity} going" +
                            if (isSelected) ", selected" else "",
                        selected = isSelected,
                        selectedGap = 3.dp,
                        selectedScale = g.SELECTED_SCALE,
                        reduceMotion = reduceMotion,
                        onClick = { onSelect(item) }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(e.day, color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 13.sp)
                            Text(
                                e.month, color = colors.accent, fontSize = 7.5.sp, fontWeight = FontWeight.Bold,
                                letterSpacing = 0.45.sp, lineHeight = 8.sp
                            )
                        }
                    }
                }
                is ExploreItem.Group -> {
                    val grp = item.g
                    OrbitNode(
                        center = compassPoint(g.CX, g.CY, g.GROUP_RING_R, grp.compassDeg!!),
                        size = g.GROUP_NODE.dp,
                        shape = NodeShape.Rounded(g.GROUP_NODE_RADIUS.dp),
                        fill = grp.color,
                        description = "${grp.name}, group, ${grp.memberCount} members" + if (isSelected) ", selected" else "",
                        selected = isSelected,
                        selectedGap = 3.dp,
                        selectedScale = g.SELECTED_SCALE,
                        reduceMotion = reduceMotion,
                        onClick = { onSelect(item) }
                    ) {
                        Text(grp.initial, color = avatarTextColor(grp.color), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
        Text(
            "SQUARES = GROUPS · CIRCLES = EVENTS",
            color = colors.muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.26.sp,
            modifier = Modifier.placedAt(14f, 8f)
        )
    }
}

@Composable
private fun PreviewCard(item: ExploreItem, onOpen: () -> Unit) {
    val colors = CircleTheme.colors
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .cardShadow(colors, 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        val kicker: String
        val meta: String
        when (item) {
            is ExploreItem.Event -> { kicker = "EVENT · ${item.e.topic}"; meta = "${item.e.whenLabel} · ${item.e.venue}" }
            is ExploreItem.Group -> {
                kicker = "GROUP · ${item.g.topic}"
                meta = "${item.g.memberCount} members, " + if (item.g.isPublic) "public" else "private"
            }
        }
        Text(kicker, color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Text(
            item.title, fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp,
            letterSpacing = (-0.22).sp, color = colors.text, modifier = Modifier.padding(top = 3.dp)
        )
        Text(meta, color = colors.muted, fontSize = 12.5.sp, modifier = Modifier.padding(top = 3.dp))
        if (item is ExploreItem.Event) CapacityBar(item.e, Modifier.padding(top = 10.dp))
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.End) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.accent)
                    .clickable(role = Role.Button, onClick = onOpen)
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    if (item is ExploreItem.Event) "View event" else "View group",
                    color = colors.onAccent, fontSize = 12.5.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CapacityBar(e: EventSummary, modifier: Modifier = Modifier) {
    val colors = CircleTheme.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(999.dp)).background(colors.track)) {
            Box(
                Modifier.fillMaxHeight()
                    .fillMaxWidth(if (e.capacity > 0) e.going.toFloat() / e.capacity else 0f)
                    .background(colors.accent)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("${e.going} of ${e.capacity} going", color = colors.muted, fontSize = 11.5.sp)
    }
}

@Composable
private fun TopicChips(selected: String?, onSelect: (String?) -> Unit) {
    val colors = CircleTheme.colors
    val options = listOf<Pair<String?, String>>(null to "All") + Sector.entries.map { it.name to it.label.lowercase().replaceFirstChar { c -> c.uppercase() } }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) colors.accent else Color.Transparent)
                    .border(1.dp, if (active) colors.accent else colors.border, RoundedCornerShape(999.dp))
                    .clickable(role = Role.Tab) { onSelect(value) }
                    .semantics { this.selected = active }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(label, color = if (active) colors.onAccent else colors.body, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ListSection(text: String) {
    Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp).semantics { heading() }) { SectionLabel(text) }
}

@Composable
private fun EventListRow(e: EventSummary, onClick: () -> Unit) {
    val colors = CircleTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "View event", onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "${e.title}, event, ${e.dateSpoken()}, ${e.venue}, ${e.going} of ${e.capacity} going"
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EventDateTile(e)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(e.title, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("${e.whenLabel} · ${e.venue}", color = colors.muted, fontSize = 12.sp, maxLines = 1)
            CapacityBar(e, Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun GroupListRow(g: GroupView, onClick: () -> Unit) {
    val colors = CircleTheme.colors
    val status = when {
        g.isMember -> "Member"
        g.joinRequested -> "Requested"
        else -> null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "View group", onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "${g.name}, group, ${g.topic.lowercase()}, ${g.memberCount} members" + (status?.let { ", $it" } ?: "")
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GroupTile(g.initial, g.color, size = 42)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(g.name, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("${g.category} · ${g.memberCount} members", color = colors.muted, fontSize = 12.sp)
        }
        if (status != null) Text(status, color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
