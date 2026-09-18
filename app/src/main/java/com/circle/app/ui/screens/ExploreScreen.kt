package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.navigation.Routes
import com.circle.app.ui.components.*
import com.circle.app.ui.theme.*

private enum class SearchTab { PEOPLE, EVENTS, GROUPS, POSTS }

@Composable
fun ExploreScreen(nav: NavHostController) {
    var searching by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(SearchTab.PEOPLE) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface)
                        .clickable { searching = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("Search", color = TextMuted, fontSize = 13.5.sp)
                }
            }

            if (searching) {
                item {
                    UnderlineTabs(listOf("People", "Events", "Groups", "Posts"), tab.ordinal) {
                        tab = SearchTab.entries[it]
                    }
                }
                item {
                    when (tab) {
                        SearchTab.PEOPLE -> PeopleResults()
                        SearchTab.EVENTS -> EventsRail(nav, vertical = true)
                        SearchTab.GROUPS -> GroupsList()
                        SearchTab.POSTS -> PostsGrid()
                    }
                }
            } else {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface).padding(16.dp)
                    ) {
                        Text("Explore", fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = TextPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text("Groups to join, events to go to, and posts people have chosen to share here.", color = TextBody, fontSize = 12.5.sp)
                    }
                }
                item {
                    Column {
                        SectionHeaderRow("GROUPS")
                        Spacer(Modifier.height(10.dp))
                        GroupsRail(nav)
                    }
                }
                item {
                    Column {
                        SectionHeaderRow("EVENTS")
                        Spacer(Modifier.height(10.dp))
                        EventsRail(nav)
                    }
                }
                item {
                    Column {
                        Text("EXPLORE POSTS", color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        Spacer(Modifier.height(10.dp))
                        PostsGrid()
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Only posts their owner opted into Explore. Tapping through still only shows photo + bio — nothing more unlocks.",
                            color = TextDim, fontSize = 11.sp
                        )
                    }
                }
            }
        }
        CircleBottomNav(nav, NavTab.EXPLORE)
    }
}

@Composable
private fun SectionHeaderRow(label: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        SectionLabel(label)
        Text("See All", color = AccentDefault, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
    }
}

@Composable
private fun GroupsRail(nav: NavHostController) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item { CreateTile(icon = true, wide = false) { nav.navigate(Routes.CREATE_GROUP) } }
        item { GroupRailItem("Aachen Photo Walk", "Photography", "A", AvatarOrange) { nav.navigate(Routes.GROUP_DETAIL) } }
        item { GroupRailItem("Sunday Trail Runners", "Fitness & Sports", "S", AvatarSage) { nav.navigate(Routes.GROUP_DETAIL) } }
        item { GroupRailItem("RWTH Board Game Club", "Gaming · Aachen", "R", AvatarBlue) { nav.navigate(Routes.GROUP_DETAIL) } }
    }
}

@Composable
private fun GroupRailItem(name: String, category: String, initial: String, color: Color, onClick: () -> Unit) {
    Column(modifier = Modifier.width(104.dp).clickable(onClick = onClick)) {
        GroupTile(initial, color, size = 96)
        Spacer(Modifier.height(6.dp))
        Text(name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 2)
        Text(category, color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun EventsRail(nav: NavHostController, vertical: Boolean = false) {
    if (vertical) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EventRailCard("Board Game Night", "SEP", "18", "6/12", "Kaffeehaus Zeitlos") { nav.navigate(Routes.EVENT_DETAIL) }
            EventRailCard("Trail Run + Brunch", "SEP", "19", "4/10", "Forest Park Trailhead") { nav.navigate(Routes.EVENT_DETAIL) }
        }
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item { CreateTile(icon = true, wide = true) { nav.navigate(Routes.CREATE_EVENT) } }
            item { EventRailCard("Board Game Night", "SEP", "18", "6/12", "Kaffeehaus Zeitlos") { nav.navigate(Routes.EVENT_DETAIL) } }
            item { EventRailCard("Trail Run + Brunch", "SEP", "19", "4/10", "Forest Park Trailhead") { nav.navigate(Routes.EVENT_DETAIL) } }
        }
    }
}

@Composable
private fun EventRailCard(title: String, month: String, day: String, ratio: String, venue: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(9.dp)).background(AccentDefault.copy(alpha = 0.14f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(month, color = AccentDefault, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                Text(day, color = AccentDefault, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Text(ratio, color = TextMuted, fontSize = 11.sp)
        }
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(venue, color = TextBody, fontSize = 11.5.sp)
    }
}

@Composable
private fun CreateTile(icon: Boolean, wide: Boolean, onClick: () -> Unit) {
    val mod = if (wide) Modifier.width(180.dp).height(96.dp) else Modifier.size(width = 104.dp, height = 96.dp)
    Box(
        modifier = mod
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Add, contentDescription = "Create", tint = AccentDefault)
            Spacer(Modifier.height(6.dp))
            Text(if (wide) "Create an event" else "Create a group", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PostsGrid() {
    val captions = listOf(
        "Golden hour never disappoints.", "Grateful for days like this.",
        "Could not have asked for a better view.", "This one is for the memories.",
        "Small wins count too.", "Still thinking about this one."
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.height(420.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(captions) { caption ->
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceAlt))
                Spacer(Modifier.height(6.dp))
                Text(caption, color = TextBody, fontSize = 11.5.sp)
            }
        }
    }
}

@Composable
private fun PeopleResults() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PersonRow("Elena Voss", "2 mutual friends", "EV", AvatarRose, "Add friend")
        PersonRow("Marco Diaz", "Friends with Priya Kapoor", "MD", AvatarTeal, "Requested")
        PersonRow("Sana Yildiz", "1 mutual friend", "SY", AvatarSlate, "Add friend")
    }
}

@Composable
private fun PersonRow(name: String, subtitle: String, initials: String, color: Color, action: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(initials, color, size = 40)
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            Text(subtitle, color = TextMuted, fontSize = 11.5.sp)
        }
        if (action == "Add friend") {
            Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(AccentDefault).padding(horizontal = 14.dp, vertical = 7.dp)) {
                Text(action, color = OnAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        } else {
            Text(action, color = TextMuted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
        }
    }
}

@Composable
private fun GroupsList() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GroupListRow("Aachen Trail Runners", "Weekend runs around the Dreiländerreck. All paces welcome.", "128 members")
        GroupListRow("RWTH Photography Club", "Campus shoots, darkroom nights, the occasional exhibit.", "212 members")
    }
}

@Composable
private fun GroupListRow(name: String, description: String, members: String) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Surface).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("PUBLIC", color = Success, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
        }
        Text(description, color = TextBody, fontSize = 12.5.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(members, color = TextMuted, fontSize = 11.5.sp)
            Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(AccentDefault).padding(horizontal = 14.dp, vertical = 7.dp)) {
                Text("Request to join", color = OnAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
