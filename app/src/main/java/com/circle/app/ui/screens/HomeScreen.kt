package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.compose.ui.res.stringResource
import com.circle.app.R
import com.circle.app.navigation.Routes
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.GroupRef
import com.circle.app.ui.vm.Load
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel
import com.circle.app.ui.components.*
import com.circle.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Feed { FRIENDS, GROUPS }
private enum class Refresh { IDLE, CHECKING, DONE }

class HomeViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    val friends = repo.homeFeed().load()
    val groups = repo.groupFeed().state(emptyList())
    val memberGroups = repo.memberGroups().state(emptyList())
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavHostController) {
    var feed by rememberSaveable { mutableStateOf(Feed.FRIENDS) }
    var refresh by remember { mutableStateOf(Refresh.IDLE) }
    var inviting by rememberSaveable { mutableStateOf(false) }
    if (inviting) InviteSheet { inviting = false }
    val vm = circleViewModel { HomeViewModel(it) }
    val friendLoad by vm.friends.collectAsStateWithLifecycle()
    val friendFeed = (friendLoad as? Load.Ready)?.value.orEmpty()
    val groupPosts by vm.groups.collectAsStateWithLifecycle()
    val memberGroups by vm.memberGroups.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val header = rememberCollapsingHeaderState { listState.canScrollForward || listState.canScrollBackward }
    val scope = rememberCoroutineScope()

    // "New" pill: newer notes arrived at the top while you were further down. Never moves your place.
    var seenTopId by rememberSaveable { mutableStateOf<String?>(null) }
    val topId = friendFeed.firstOrNull()?.id
    val atTop by remember { derivedStateOf { listState.firstVisibleItemIndex <= 1 } }
    LaunchedEffect(topId, atTop) { if (atTop || seenTopId == null) seenTopId = topId }
    val showNew = feed == Feed.FRIENDS && topId != null && seenTopId != null && topId != seenTopId && !atTop

    // Pull to refresh, or tap the wordmark: scroll to the top, bring the header back and check.
    // The feed is live, so a check only confirms it: "Nothing to refresh. You're caught up."
    fun doRefresh(pulled: Boolean = false) {
        scope.launch {
            launch { header.reveal() }
            if (!pulled) listState.animateScrollToItem(0)
            refresh = Refresh.CHECKING
            delay(700)
            refresh = Refresh.DONE
            listState.animateScrollToItem(0)
            delay(2000)
            refresh = Refresh.IDLE
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
        OfflineBanner()
        CollapsingHeaderLayout(
            state = header,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            header = {
                HomeHeader(
                    feed = feed,
                    onWordmark = ::doRefresh,
                    onFeed = { feed = it }
                )
            }
        ) {
          androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = refresh == Refresh.CHECKING,
            onRefresh = { doRefresh(pulled = true) },
            modifier = Modifier.fillMaxSize()
          ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (feed == Feed.FRIENDS) {
                    item(key = "refresh") {
                        when (refresh) {
                            Refresh.CHECKING -> RefreshRow(stringResource(R.string.home_checking), CircleTheme.colors.muted)
                            Refresh.DONE -> RefreshRow(stringResource(R.string.home_nothing_to_refresh), CircleTheme.colors.accent, showCheck = true)
                            Refresh.IDLE -> {}
                        }
                    }
                    item(key = "compose") { ComposeRow(nav) }
                    when (friendLoad) {
                        Load.Loading -> item(key = "loading") { SkeletonList() }
                        Load.Failed -> item(key = "error") { ErrorState(onRetry = vm::retry) }
                        is Load.Ready -> if (friendFeed.isEmpty()) item(key = "empty") {
                            EmptyState(
                                EmptyKind.HOME,
                                action = stringResource(R.string.empty_home_action), onAction = { nav.navigate(Routes.CONTACTS_SYNC) },
                                secondary = stringResource(R.string.empty_home_invite), onSecondary = { inviting = true },
                            )
                        } else {
                            items(friendFeed, key = { it.id }) { post -> FeedCard(nav, post) }
                            item(key = "caught-up") { CaughtUp() }
                        }
                    }
                } else {
                    if (memberGroups.isNotEmpty()) item(key = "chips") { GroupsChipRow(nav, memberGroups) }
                    item(key = "note") {
                        Text(
                            androidx.compose.ui.res.stringResource(com.circle.app.R.string.home_group_posts_stay_in_the),
                            color = CircleTheme.colors.muted, fontSize = 11.sp
                        )
                    }
                    if (groupPosts.isEmpty()) {
                        item(key = "empty") {
                            EmptyState(EmptyKind.GROUPS, action = stringResource(R.string.empty_groups_action), onAction = { nav.navigate(Routes.EXPLORE) })
                        }
                    }
                    items(groupPosts, key = { it.id }) { post -> GroupPostCard(nav, post, showGroup = true) }
                }
            }
            if (showNew) {
                Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.TopCenter) {
                    Text(
                        stringResource(R.string.home_new_pill), color = CircleTheme.colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 12.5.sp,
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(CircleTheme.colors.accent)
                            .clickable(onClickLabel = stringResource(R.string.home_new_pill_label)) {
                                scope.launch { listState.animateScrollToItem(0); seenTopId = topId }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
          }
        }

        CircleBottomNav(nav, NavTab.HOME, onHomeIconHold = {
            feed = if (feed == Feed.FRIENDS) Feed.GROUPS else Feed.FRIENDS
        })
    }
}

/** Date line, "Circle." wordmark and the Friends | Groups tabs. Hides as the feed scrolls. */
@Composable
private fun HomeHeader(feed: Feed, onWordmark: () -> Unit, onFeed: (Feed) -> Unit) {
    val colors = CircleTheme.colors
    val today = remember {
        java.time.LocalDate.now().let { d ->
            val fmt = java.time.format.DateTimeFormatter.ofPattern("EEEE '\u00b7' d MMMM", java.util.Locale.ENGLISH)
            d.format(fmt).uppercase(java.util.Locale.ENGLISH)
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth().background(colors.background).padding(top = 12.dp, bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(today, color = colors.muted, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.9.sp)
        Text(
            androidx.compose.ui.res.stringResource(com.circle.app.R.string.onboarding_circle), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 24.sp,
            letterSpacing = (-0.48).sp, color = colors.text,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClickLabel = androidx.compose.ui.res.stringResource(com.circle.app.R.string.home_scroll_to_top_and_refresh), onClick = onWordmark)
                .padding(horizontal = 8.dp)
        )
        Box(Modifier.padding(top = 4.dp)) {
            UnderlineTabs(listOf(androidx.compose.ui.res.stringResource(com.circle.app.R.string.compose_audience_friends), androidx.compose.ui.res.stringResource(com.circle.app.R.string.messages_tab_groups)), if (feed == Feed.FRIENDS) 0 else 1, spacing = 28.dp) {
                onFeed(if (it == 0) Feed.FRIENDS else Feed.GROUPS)
            }
        }
        Text(
            androidx.compose.ui.res.stringResource(com.circle.app.R.string.home_tip_press_hold_the_home),
            color = colors.muted, fontSize = 9.5.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/** The calm ending of the feed: it ends, and says so. */
@Composable
private fun CaughtUp() {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.home_caught_up), fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = 19.sp, color = CircleTheme.colors.text)
        Text(stringResource(R.string.home_nothing_below), fontSize = 12.sp, color = CircleTheme.colors.muted, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun RefreshRow(text: String, color: androidx.compose.ui.graphics.Color, showCheck: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (showCheck) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = color, fontSize = 11.5.sp)
    }
}

@Composable
private fun ComposeRow(nav: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(CircleTheme.colors.surface)
            .clickable { nav.navigate(Routes.COMPOSE_POST) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MeAvatar(32)
        Spacer(Modifier.width(10.dp))
        Text(androidx.compose.ui.res.stringResource(com.circle.app.R.string.compose_hint), color = CircleTheme.colors.muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.Image, contentDescription = androidx.compose.ui.res.stringResource(com.circle.app.R.string.home_attach_media), tint = CircleTheme.colors.muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun GroupsChipRow(nav: NavHostController, groups: List<GroupRef>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(groups, key = { it.id }) { g ->
            GroupChip(g.initial, g.color) { nav.navigate(Routes.groupDetail(g.id)) }
        }
    }
}

@Composable
private fun GroupChip(initial: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(modifier = Modifier.clickable(onClick = onClick)) { GroupTile(initial, color, size = 52) }
}

