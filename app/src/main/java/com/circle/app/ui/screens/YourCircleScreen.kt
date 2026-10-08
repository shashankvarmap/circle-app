package com.circle.app.ui.screens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.data.CircleStore
import com.circle.app.data.Person
import com.circle.app.ui.a11y.rememberReduceMotion
import com.circle.app.ui.a11y.rememberScreenReaderOn
import com.circle.app.ui.components.Avatar
import com.circle.app.ui.components.SectionLabel
import com.circle.app.ui.orbit.*
import com.circle.app.ui.theme.*

private val MoveEasing = CubicBezierEasing(0.3f, 1.25f, 0.5f, 1f) // the prototype's slight overshoot

/**
 * Your circle: private to you. Inner circle on the solid ring, other friends on the dashed
 * ring. The map shows up to 12 people; everyone is in the searchable All friends list.
 */
@Composable
fun YourCircleScreen(nav: NavHostController) {
    val colors = CircleTheme.colors
    val reduceMotion = rememberReduceMotion()
    val screenReader = rememberScreenReaderOn()

    var showMap by rememberSaveable { mutableStateOf(!screenReader) }
    var selectedId by rememberSaveable { mutableStateOf("EK") }
    var query by rememberSaveable { mutableStateOf("") }
    // Outer-ring people picked from the list (or just moved out), kept visible on the map.
    val pinned = rememberSaveable(saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })) {
        mutableStateListOf<String>()
    }

    val friends = CircleStore.friends
    val inner = friends.filter { CircleStore.isInner(it.id) }.take(YourCircleGeometry.MAX_SHOWN)
    val outerSlots = (YourCircleGeometry.MAX_SHOWN - inner.size).coerceAtLeast(0)
    val outerIds = (pinned.filter { CircleStore.isFriend(it) && !CircleStore.isInner(it) } +
        friends.filter { !CircleStore.isInner(it.id) }.map { it.id }).distinct().take(outerSlots).toSet()
    val outer = friends.filter { it.id in outerIds } // stable friend order around the ring
    val selected = CircleStore.person(selectedId)?.takeIf { CircleStore.isFriend(it.id) } ?: friends.first()

    fun select(p: Person) {
        selectedId = p.id
        if (!CircleStore.isInner(p.id) && p.id !in outerIds) {
            pinned.remove(p.id); pinned.add(0, p.id)
        }
    }

    fun toggle(p: Person) {
        val movingOut = CircleStore.isInner(p.id)
        CircleStore.toggleInner(p.id)
        if (movingOut) { pinned.remove(p.id); pinned.add(0, p.id) }
    }

    val filtered = friends.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
        .sortedByDescending { CircleStore.isInner(it.id) } // stable: inner circle first, then friends

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable { nav.popBackStack() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.text) }
            Column(modifier = Modifier.weight(1f).padding(start = 2.dp)) {
                Text(
                    "Your circle", fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 21.sp,
                    letterSpacing = (-0.21).sp, color = colors.text, modifier = Modifier.semantics { heading() }
                )
                Text(
                    "${CircleStore.friendCount} friends · ${CircleStore.innerCount} inner circle",
                    color = colors.muted, fontSize = 11.5.sp
                )
            }
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.muted, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text("Only you", color = colors.muted, fontSize = 11.sp)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            item(key = "toggle") {
                Box(Modifier.fillMaxWidth().padding(top = 10.dp, end = 14.dp), contentAlignment = Alignment.CenterEnd) {
                    MapListToggle(showMap, { showMap = it })
                }
            }
            if (showMap) {
                item(key = "map") {
                    CircleMap(inner, outer, selected.id, reduceMotion) { select(it) }
                }
                item(key = "detail") {
                    DetailCard(selected, onToggle = { toggle(selected) })
                }
            }

            item(key = "all-header") {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 6.dp)) {
                    SectionLabel(if (showMap) "ALL FRIENDS · ${CircleStore.friendCount}" else "YOUR CIRCLE · ${CircleStore.friendCount}")
                    Spacer(Modifier.height(10.dp))
                    SearchField(query, onChange = { query = it }, placeholder = "Search your friends")
                }
            }
            if (filtered.isEmpty()) {
                item(key = "empty") {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No friends match \"${query.trim()}\".", color = colors.muted, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Clear search", color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            modifier = Modifier.clickable { query = "" }.padding(8.dp)
                        )
                    }
                }
            }
            items(filtered, key = { it.id }) { p ->
                FriendRow(
                    person = p,
                    isInner = CircleStore.isInner(p.id),
                    isSelected = showMap && p.id == selected.id,
                    showMoveButton = !showMap,
                    onClick = { if (showMap) select(p) },
                    onToggle = { toggle(p) }
                )
            }
            if (!showMap) item(key = "footnote") {
                Text(
                    "They're never told, and there's no badge. It only decides who sees your inner-circle notes.",
                    color = colors.muted, fontSize = 11.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun CircleMap(
    inner: List<Person>,
    outer: List<Person>,
    selectedId: String,
    reduceMotion: Boolean,
    onSelect: (Person) -> Unit
) {
    val colors = CircleTheme.colors
    val g = YourCircleGeometry
    val innerPts = ringPoints(g.CX, g.CY, g.INNER_R, inner.size)
    val outerPts = ringPoints(g.CX, g.CY, g.OUTER_R, outer.size, outerOffsetDeg(outer.size))
    val targets = inner.mapIndexed { i, p -> Triple(p, innerPts[i], true) } +
        outer.mapIndexed { i, p -> Triple(p, outerPts[i], false) }

    OrbitMap(g.WIDTH, g.HEIGHT) {
        Canvas(Modifier.fillMaxSize()) {
            val c = px(Pt(g.CX, g.CY))
            orbitRing(c, px(g.OUTER_R), colors.border, dashed = true)
            orbitRing(c, px(g.INNER_R), colors.border, dashed = false)
        }
        // You, in the middle
        Box(
            modifier = Modifier.centeredAt(Pt(g.CX, g.CY), g.ME_NODE.dp).size(g.ME_NODE.dp)
                .clip(CircleShape).background(CircleStore.me.color)
                .semantics { contentDescription = "You" },
            contentAlignment = Alignment.Center
        ) { Text("S", color = avatarTextColor(CircleStore.me.color), fontWeight = FontWeight.Bold, fontSize = 15.sp) }

        targets.forEach { (p, target, isInner) ->
            key(p.id) {
                val spec = if (reduceMotion) snap<Float>() else tween(550, easing = MoveEasing)
                val x by animateFloatAsState(target.x, spec, label = "x-${p.id}")
                val y by animateFloatAsState(target.y, spec, label = "y-${p.id}")
                val size = if (isInner) g.INNER_NODE else g.OUTER_NODE
                val isSelected = p.id == selectedId
                OrbitNode(
                    center = Pt(x, y),
                    size = size.dp,
                    shape = NodeShape.Circle,
                    fill = p.color,
                    description = buildString {
                        append(p.name)
                        append(if (isInner) ", inner circle" else ", friend")
                        if (isSelected) append(", selected")
                    },
                    selected = isSelected,
                    halo = 2.dp,
                    selectedGap = 2.dp,
                    reduceMotion = reduceMotion,
                    onClick = { onSelect(p) }
                ) {
                    Text(p.initials, color = avatarTextColor(p.color), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        // Drawn after the nodes, as in the prototype, so it stays on top.
        OrbitLabel("INNER CIRCLE", 8.5f, Modifier.placedAt(145f, 201f).width((100 * scale).dp))
        Text(
            "Tap a person. Move them between rings.",
            color = colors.muted, fontSize = 10.5.sp,
            modifier = Modifier.placedAt(14f, g.HEIGHT - 20f)
        )
    }
}

@Composable
private fun DetailCard(person: Person, onToggle: () -> Unit) {
    val colors = CircleTheme.colors
    val isInner = CircleStore.isInner(person.id)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(person.initials, person.color, size = 40)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(person.name, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(statusLine(person, isInner), color = colors.muted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        MoveButton(isInner, fill = true, onClick = onToggle, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(9.dp))
        Text(
            "They're never told, and there's no badge. It only decides who sees your inner-circle notes.",
            color = colors.muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun statusLine(p: Person, isInner: Boolean) =
    (if (isInner) "In your inner circle" else "Friend") + " · since ${p.sinceYear}"

/** "Move to inner circle" (accent fill) or "Move back to friends" (outlined). */
@Composable
private fun MoveButton(isInner: Boolean, fill: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CircleTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isInner) Color.Transparent else colors.accent)
            .border(1.5.dp, colors.accent, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = if (fill) 0.dp else 12.dp, vertical = if (fill) 11.dp else 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (isInner) "Move back to friends" else "Move to inner circle",
            color = if (isInner) colors.text else colors.onAccent,
            fontSize = if (fill) 13.sp else 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FriendRow(
    person: Person,
    isInner: Boolean,
    isSelected: Boolean,
    showMoveButton: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit
) {
    val colors = CircleTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) colors.surface else Color.Transparent)
            .then(
                if (showMoveButton) Modifier
                else Modifier.clickable(onClickLabel = "Show on the map", onClick = onClick)
            )
            .semantics(mergeDescendants = !showMoveButton) {}
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(person.initials, person.color, size = 36)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(person.name, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            Text(
                statusLine(person, isInner) + if (isSelected) " · selected" else "",
                color = colors.muted, fontSize = 11.5.sp
            )
        }
        if (showMoveButton) {
            Spacer(Modifier.width(8.dp))
            MoveButton(isInner, fill = false, onClick = onToggle)
        }
    }
}

/** Pill search field shared by Your circle and Explore. */
@Composable
fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val colors = CircleTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(9.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = colors.muted, fontSize = 13.5.sp)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.text, fontSize = 13.5.sp, fontFamily = Body),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = placeholder }
            )
        }
        if (value.isNotEmpty()) {
            Text(
                "Clear", color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onChange("") }.padding(start = 8.dp)
            )
        }
    }
}
