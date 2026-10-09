package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.CircleSummary
import com.circle.app.ui.vm.LocalMe
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import com.circle.app.navigation.Routes
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.circle.app.ui.components.*
import com.circle.app.ui.theme.*

private enum class ProfileTab { ALL, THOUGHTS, POSTS }

class MyProfileViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    val notes = repo.myNotes().state(emptyList())
    val circle = repo.circleSummary().state(CircleSummary(0, 0))
}

@Composable
fun ProfileScreen(nav: NavHostController) {
    var tab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(ProfileTab.ALL) }
    val vm = circleViewModel { MyProfileViewModel(it) }
    val mine by vm.notes.collectAsStateWithLifecycle()
    val circle by vm.circle.collectAsStateWithLifecycle()
    val me = LocalMe.current
    // All: everything of yours, reposts and quotes included. Thoughts: text notes. Posts: photo posts.
    val visiblePosts = when (tab) {
        ProfileTab.ALL -> mine
        ProfileTab.THOUGHTS -> mine.filter { !it.isRepost && it.quoted == null && !it.hasMedia }
        ProfileTab.POSTS -> mine.filter { !it.isRepost && it.hasMedia }
    }

    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MeAvatar(68)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            me?.displayName.orEmpty(), fontFamily = Display, fontWeight = FontWeight.SemiBold,
                            fontSize = 24.sp, letterSpacing = (-0.36).sp, color = CircleTheme.colors.text
                        )
                        Text("@${me?.handle.orEmpty()}", color = CircleTheme.colors.muted, fontSize = 13.sp)
                    }
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).clickable { nav.navigate(Routes.SETTINGS) },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Outlined.Settings, contentDescription = androidx.compose.ui.res.stringResource(com.circle.app.R.string.profile_settings), tint = CircleTheme.colors.text) }
                }
            }
            item {
                CircleCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.PushPin, contentDescription = null, tint = CircleTheme.colors.muted, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.messages_pinned_caps), color = CircleTheme.colors.muted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Text(
                        me?.pinnedThought.orEmpty().ifBlank { androidx.compose.ui.res.stringResource(com.circle.app.R.string.profile_no_pinned) },
                        color = CircleTheme.colors.body, fontSize = 14.5.sp
                    )
                }
            }
            item { FriendsRow(circle, onOpen = { nav.navigate(Routes.YOUR_CIRCLE) }) }
            item {
                UnderlineTabs(listOf(androidx.compose.ui.res.stringResource(com.circle.app.R.string.profile_all), androidx.compose.ui.res.stringResource(com.circle.app.R.string.profile_thoughts), androidx.compose.ui.res.stringResource(com.circle.app.R.string.group_posts)), tab.ordinal) { tab = ProfileTab.entries[it] }
            }
            if (visiblePosts.isEmpty()) {
                item {
                    EmptyState(
                        EmptyKind.PROFILE,
                        action = androidx.compose.ui.res.stringResource(com.circle.app.R.string.empty_profile_action),
                        onAction = { nav.navigate(Routes.COMPOSE_POST) }
                    )
                }
            }
            items(visiblePosts, key = { it.id }) { post -> FeedCard(nav, post) }
            // Your photo notes, newest first (no grid at all when there are none).
            val photoNotes = mine.filter { it.hasMedia && !it.isRepost }
            if (photoNotes.isNotEmpty()) item(key = "photos") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(androidx.compose.ui.res.stringResource(com.circle.app.R.string.profile_photos))
                    photoNotes.take(9).chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { n ->
                                Box(
                                    Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(12.dp)).background(CircleTheme.colors.track)
                                        .clickable(onClickLabel = androidx.compose.ui.res.stringResource(com.circle.app.R.string.media_open)) { nav.navigate(Routes.viewer(n.id)) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val first = n.media.firstOrNull()
                                    if (first != null) MediaThumb(first)
                                    else Icon(Icons.Outlined.Image, contentDescription = null, tint = CircleTheme.colors.muted)
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            item {
                CircleCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = CircleTheme.colors.muted, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.profile_private_by_default), color = CircleTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(
                        androidx.compose.ui.res.stringResource(com.circle.app.R.string.profile_if_someone_who_isn_t),
                        color = CircleTheme.colors.muted, fontSize = 12.sp
                    )
                }
            }
        }
        CircleBottomNav(nav, NavTab.PROFILE)
    }
}

/** The highlighted entry to Your circle. Counts come from the store and are private to you. */
@Composable
private fun FriendsRow(circle: CircleSummary, onOpen: () -> Unit) {
    val colors = CircleTheme.colors
    val friends = circle.friendCount
    val inner = circle.innerCount
    val friendsRowLabel = androidx.compose.ui.res.stringResource(com.circle.app.R.string.profile_friends_row, friends, inner)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(colors.surface)
                .border(1.5.dp, colors.accent, RoundedCornerShape(13.dp))
                .clickable(onClickLabel = androidx.compose.ui.res.stringResource(com.circle.app.R.string.profile_open_your_circle), onClick = onOpen)
                .semantics(mergeDescendants = true) {
                    contentDescription = friendsRowLabel
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // The counts wrap onto a second line at large font sizes, so "See your circle" always fits.
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$friends", color = colors.text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.profile_friends), color = colors.muted, fontSize = 13.sp)
                    Text("·", color = colors.muted, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$inner", color = colors.text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.profile_inner_circle), color = colors.muted, fontSize = 13.sp)
                }
            }
            Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.profile_see_your_circle), color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
        }
        Row(Modifier.padding(start = 2.dp, top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.muted, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(5.dp))
            Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.profile_only_you_see_this_tap), color = colors.muted, fontSize = 11.sp)
        }
    }
}

