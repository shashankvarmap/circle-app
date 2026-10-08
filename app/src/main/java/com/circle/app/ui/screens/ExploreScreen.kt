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
import androidx.compose.foundation.lazy.items as rowItems
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
import com.circle.app.data.CircleStore
import com.circle.app.data.EventInfo
import com.circle.app.data.GroupInfo
import com.circle.app.ui.components.*
import com.circle.app.ui.theme.*

private enum class SearchTab { PEOPLE, EVENTS, GROUPS, POSTS }

@Composable
fun ExploreScreen(nav: NavHostController) {
    var searching by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(SearchTab.PEOPLE) }

    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
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
                        .background(CircleTheme.colors.surface)
                        .clickable { searching = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = CircleTheme.colors.muted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("Search", color = CircleTheme.colors.muted, fontSize = 13.5.sp)
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
                        SearchTab.GROUPS -> GroupsList(nav)
                        SearchTab.POSTS -> PostsGrid()
                    }
                }
            } else {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CircleTheme.colors.surface).padding(16.dp)
                    ) {
                        Text("Explore", fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = CircleTheme.colors.text)
                        Spacer(Modifier.height(6.dp))
                        Text("Groups to join, events to go to, and posts people have chosen to share here.", color = CircleTheme.colors.body, fontSize = 12.5.sp)
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
                        Text("EXPLORE POSTS", color = CircleTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        Spacer(Modifier.height(10.dp))
                        PostsGrid()
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Only posts their owner opted into Explore. Tapping through still only shows photo + bio — nothing more unlocks.",
                            color = CircleTheme.colors.muted, fontSize = 11.sp
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
        Text("See All", color = CircleTheme.colors.accent, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
    }
}

@Composable
private fun GroupsRail(nav: NavHostController) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item { CreateTile(icon = true, wide = false) { nav.navigate(Routes.CREATE_GROUP) } }
        // Your groups plus public ones you can discover; newly created groups appear first.
        rowItems(CircleStore.groups.filter { it.isMember || it.isPublic }, key = { it.id }) { g ->
            GroupRailItem(g.name, g.category, g.initial, g.color) { nav.navigate(Routes.groupDetail(g.id)) }
        }
    }
}

@Composable
private fun GroupRailItem(name: String, category: String, initial: String, color: Color, onClick: () -> Unit) {
    Column(modifier = Modifier.width(104.dp).clickable(onClick = onClick)) {
        GroupTile(initial, color, size = 96)
        Spacer(Modifier.height(6.dp))
        Text(name, color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 2)
        Text(category, color = CircleTheme.colors.muted, fontSize = 11.sp)
    }
}

@Composable
private fun EventsRail(nav: NavHostController, vertical: Boolean = false) {
    if (vertical) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CircleStore.events.forEach { e -> EventRailCard(e) { nav.navigate(Routes.eventDetail(e.id)) } }
        }
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item { CreateTile(icon = true, wide = true) { nav.navigate(Routes.CREATE_EVENT) } }
            rowItems(CircleStore.events, key = { it.id }) { e ->
                EventRailCard(e) { nav.navigate(Routes.eventDetail(e.id)) }
            }
        }
    }
}

@Composable
private fun EventRailCard(event: EventInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CircleTheme.colors.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(9.dp)).background(CircleTheme.colors.accent.copy(alpha = 0.14f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(event.month, color = CircleTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                Text(event.day, color = CircleTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Text("${event.going}/${event.capacity}", color = CircleTheme.colors.muted, fontSize = 11.sp)
        }
        Text(event.title, color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
        Text(event.venue, color = CircleTheme.colors.body, fontSize = 11.5.sp, maxLines = 1)
    }
}

@Composable
private fun CreateTile(icon: Boolean, wide: Boolean, onClick: () -> Unit) {
    val mod = if (wide) Modifier.width(180.dp).height(96.dp) else Modifier.size(width = 104.dp, height = 96.dp)
    Box(
        modifier = mod
            .clip(RoundedCornerShape(14.dp))
            .background(CircleTheme.colors.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Add, contentDescription = "Create", tint = CircleTheme.colors.accent)
            Spacer(Modifier.height(6.dp))
            Text(if (wide) "Create an event" else "Create a group", color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                Box(modifier = Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(12.dp)).background(CircleTheme.colors.track))
                Spacer(Modifier.height(6.dp))
                Text(caption, color = CircleTheme.colors.body, fontSize = 11.5.sp)
            }
        }
    }
}

@Composable
private fun PeopleResults() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PersonRow("Lukas Brandt", "2 mutual friends", "LB", AvatarRose)
        PersonRow("Marco Diaz", "Friends with Priya Kapoor", "MD", AvatarTeal)
        PersonRow("Nora Kim", "1 mutual friend", "NK", AvatarAmber)
    }
}

@Composable
private fun PersonRow(name: String, subtitle: String, initials: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(initials, color, size = 40)
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            Text(subtitle, color = CircleTheme.colors.muted, fontSize = 11.5.sp)
        }
        FriendRequestButton(name)
    }
}

@Composable
private fun GroupsList(nav: NavHostController) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CircleStore.groups.filter { it.isPublic }.forEach { g ->
            GroupListRow(g) { nav.navigate(Routes.groupDetail(g.id)) }
        }
    }
}

@Composable
private fun GroupListRow(group: GroupInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(CircleTheme.colors.surface).clickable(onClick = onClick).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(group.name, color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("PUBLIC", color = CircleTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
        }
        if (group.description.isNotEmpty()) Text(group.description, color = CircleTheme.colors.body, fontSize = 12.5.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${group.memberCount} members", color = CircleTheme.colors.muted, fontSize = 11.5.sp)
            JoinRequestButton(group)
        }
    }
}
