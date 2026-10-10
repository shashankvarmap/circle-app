package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.circle.app.data.access.Relationship
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.EventSummary
import com.circle.app.data.repo.ExploreView
import com.circle.app.data.repo.GroupView
import com.circle.app.data.repo.PersonResult
import com.circle.app.ui.format.*
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import com.circle.app.navigation.openPerson
import com.circle.app.navigation.Routes
import com.circle.app.ui.components.*
import com.circle.app.ui.theme.*

private enum class SearchTab(val label: Int) { PEOPLE(com.circle.app.R.string.search_tab_people), EVENTS(com.circle.app.R.string.search_tab_events), GROUPS(com.circle.app.R.string.search_tab_groups), POSTS(com.circle.app.R.string.search_tab_posts) }

private val explorePostCaptions = listOf(
    "Golden hour never disappoints.", "Grateful for days like this.",
    "Could not have asked for a better view.", "This one is for the memories.",
    "Small wins count too.", "Still thinking about this one."
)

/** Search shows only Limited information about people (photo, name, handle) and the button state. */
@OptIn(ExperimentalCoroutinesApi::class)
class ExploreSearchViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    private val query = MutableStateFlow("")
    val people = query.flatMapLatest { repo.searchPeople(it) }.state(emptyList())
    val explore = repo.explore().state(ExploreView(emptyList(), emptyList()))
    fun setQuery(q: String) { query.value = q }
}

/** Explore search, opened from the search pill on the map: People / Events / Groups / Posts. */
@Composable
fun ExploreSearchScreen(nav: NavHostController) {
    val colors = CircleTheme.colors
    var tab by rememberSaveable { mutableStateOf(SearchTab.PEOPLE) }
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim()
    val vm = circleViewModel { ExploreSearchViewModel(it) }
    LaunchedEffect(q) { vm.setQuery(q) }
    val people by vm.people.collectAsStateWithLifecycle()
    val explore by vm.explore.collectAsStateWithLifecycle()
    fun matches(vararg fields: String) = q.isEmpty() || fields.any { it.contains(q, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable { nav.popBackStack() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(com.circle.app.R.string.back), tint = colors.text) }
            SearchField(query, { query = it }, placeholder = androidx.compose.ui.res.stringResource(com.circle.app.R.string.exploresearch_search_explore), modifier = Modifier.weight(1f))
        }
        Box(Modifier.padding(start = 16.dp, top = 6.dp)) {
            UnderlineTabs(SearchTab.entries.map { androidx.compose.ui.res.stringResource(it.label) }, tab.ordinal) { tab = SearchTab.entries[it] }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (tab) {
                SearchTab.PEOPLE -> {
                    // Anyone you haven't blocked; with no query, people you might know.
                    if (people.isEmpty()) item { NoResults(q) { query = "" } }
                    people.forEach { p -> item(key = p.person.id) { PersonRow(p) { nav.openPerson(p.person.id) } } }
                }
                SearchTab.EVENTS -> {
                    val events = explore.events.filter { matches(it.title, it.venue, it.topic) }
                    if (events.isEmpty()) item { NoResults(q) { query = "" } }
                    events.forEach { e -> item(key = e.id) { EventResultCard(e) { nav.navigate(Routes.eventDetail(e.id)) } } }
                }
                SearchTab.GROUPS -> {
                    // Private groups never appear in Explore.
                    val groups = explore.groups.filter { matches(it.name, it.category, it.description) }
                    if (groups.isEmpty()) item { NoResults(q) { query = "" } }
                    groups.forEach { g -> item(key = g.id) { GroupListRow(g, onJoinTemporary = { nav.navigate(Routes.joinGroup(g.id)) }) { nav.navigate(Routes.groupDetail(g.id)) } } }
                }
                SearchTab.POSTS -> {
                    val captions = explorePostCaptions.filter { matches(it) }
                    if (captions.isEmpty()) item { NoResults(q) { query = "" } }
                    else item { PostsGrid(captions) }
                    item {
                        Text(
                            androidx.compose.ui.res.stringResource(com.circle.app.R.string.exploresearch_only_posts_their_owner_opted),
                            color = colors.muted, fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoResults(query: String, onClear: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (query.isEmpty()) androidx.compose.ui.res.stringResource(com.circle.app.R.string.search_nothing_yet) else androidx.compose.ui.res.stringResource(com.circle.app.R.string.search_nothing_matches, query),
            color = CircleTheme.colors.muted, fontSize = 13.sp
        )
        if (query.isNotEmpty()) {
            Text(
                androidx.compose.ui.res.stringResource(com.circle.app.R.string.exploresearch_clear_search), color = CircleTheme.colors.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onClear).padding(8.dp)
            )
        }
    }
}

@Composable
private fun EventResultCard(event: EventSummary, onClick: () -> Unit) {
    val colors = CircleTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EventDateTile(event)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(event.title, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1)
            Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.dot_pair, event.whenLabel, event.venue), color = colors.muted, fontSize = 11.5.sp, maxLines = 1)
        }
        Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.search_going_of, event.going, event.capacity), color = colors.muted, fontSize = 11.sp)
    }
}

/** Day over month in an accent-tinted tile. */
@Composable
fun EventDateTile(event: EventSummary) {
    val colors = CircleTheme.colors
    Column(
        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(9.dp)).background(colors.accent.copy(alpha = 0.14f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(event.month, color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 9.sp)
        Text(event.day, color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun PostsGrid(captions: List<String>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.height((((captions.size + 1) / 2) * 156).dp),
        userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(captions) { caption ->
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).background(CircleTheme.colors.track))
                Spacer(Modifier.height(6.dp))
                Text(caption, color = CircleTheme.colors.body, fontSize = 11.5.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PersonRow(r: PersonResult, onOpen: () -> Unit) {
    val p = r.person
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = androidx.compose.ui.res.stringResource(com.circle.app.R.string.open_profile_of, p.name), onClick = onOpen).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(p.initials, p.color, size = 40)
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(p.name, color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            Text(
                if (r.relationship == Relationship.FRIEND) "Friend" else "@${r.handle}",
                color = CircleTheme.colors.muted, fontSize = 11.5.sp
            )
        }
        FriendRequestButton(p.id, r.relationship)
    }
}

@Composable
private fun GroupListRow(group: GroupView, onJoinTemporary: () -> Unit, onClick: () -> Unit) {
    val colors = CircleTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(colors.surface).clickable(onClick = onClick).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GroupTile(group.initial, group.color, size = 32)
            Spacer(Modifier.width(10.dp))
            Text(group.name, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.exploresearch_public), color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
        }
        if (group.description.isNotEmpty()) Text(group.description, color = colors.body, fontSize = 12.5.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(androidx.compose.ui.res.pluralStringResource(com.circle.app.R.plurals.members_count, group.memberCount, group.memberCount), color = colors.muted, fontSize = 11.5.sp)
            JoinRequestButton(group, onTemporary = onJoinTemporary)
        }
    }
}
