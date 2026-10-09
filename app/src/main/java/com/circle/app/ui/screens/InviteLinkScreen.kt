package com.circle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.circle.app.R
import com.circle.app.data.repo.CircleRepository
import com.circle.app.data.repo.Outcome
import com.circle.app.navigation.Routes
import com.circle.app.ui.theme.CircleTheme
import com.circle.app.ui.vm.RepoViewModel
import com.circle.app.ui.vm.circleViewModel

class InviteLinkViewModel(repo: CircleRepository) : RepoViewModel(repo) {
    var personId by mutableStateOf<String?>(null)
        private set
    var notFound by mutableStateOf(false)
        private set
    fun resolve(handle: String) = run({ personByHandle(handle) }) {
        if (it is Outcome.Success) personId = it.value else notFound = true
    }
}

/**
 * An invite link (App Link) opens the inviter's profile: the not-friends profile with Add friend.
 * An invite never makes anyone a friend.
 */
@Composable
fun InviteLinkScreen(nav: NavHostController, handle: String) {
    val vm = circleViewModel(key = "invite-$handle") { InviteLinkViewModel(it) }
    LaunchedEffect(handle) { vm.resolve(handle) }
    LaunchedEffect(vm.personId) {
        vm.personId?.let { id -> nav.navigate(Routes.person(id)) { popUpTo(Routes.INVITE_LINK) { inclusive = true } } }
    }
    Column(Modifier.fillMaxSize().background(CircleTheme.colors.background)) {
        DetailHeader(stringResource(R.string.profile_title)) { nav.popBackStack() }
        if (vm.notFound) Text(stringResource(R.string.profile_unavailable), color = CircleTheme.colors.muted, fontSize = 14.sp, modifier = Modifier.padding(24.dp))
    }
}
