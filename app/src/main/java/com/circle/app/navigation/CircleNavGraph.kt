package com.circle.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.circle.app.ui.screens.*

object Routes {
    const val ONBOARDING = "onboarding"
    const val CONTACTS_SYNC = "contacts_sync"
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val NOTIFICATIONS = "notifications"
    const val MESSAGES = "messages"
    const val PROFILE = "profile"
    const val COMPOSE_POST = "compose_post"
    const val GROUP_DETAIL = "group_detail"
    const val EVENT_DETAIL = "event_detail"
    const val CREATE_GROUP = "create_group"
    const val CREATE_EVENT = "create_event"
}

@Composable
fun CircleNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.ONBOARDING) {
        composable(Routes.ONBOARDING) { OnboardingScreen(navController) }
        composable(Routes.CONTACTS_SYNC) { ContactsSyncScreen(navController) }
        composable(Routes.HOME) { HomeScreen(navController) }
        composable(Routes.EXPLORE) { ExploreScreen(navController) }
        composable(Routes.NOTIFICATIONS) { NotificationsScreen(navController) }
        composable(Routes.MESSAGES) { MessagesScreen(navController) }
        composable(Routes.PROFILE) { ProfileScreen(navController) }
        composable(Routes.COMPOSE_POST) { ComposePostScreen(navController) }
        composable(Routes.GROUP_DETAIL) { GroupScreen(navController) }
        composable(Routes.EVENT_DETAIL) { EventScreen(navController) }
        composable(Routes.CREATE_GROUP) { CreateGroupScreen(navController) }
        composable(Routes.CREATE_EVENT) { CreateEventScreen(navController) }
    }
}
