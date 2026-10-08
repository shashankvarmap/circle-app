package com.circle.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
    const val THREAD = "thread/{postId}"
    const val YOUR_CIRCLE = "your_circle"
    const val EXPLORE_SEARCH = "explore_search"
    const val SETTINGS = "settings"
    const val EDIT_PROFILE = "edit_profile"
    const val INFO = "info/{page}"
    const val PEOPLE_LIST = "people/{kind}"

    const val CHAT = "chat/{chatId}"
    const val PERSON = "person/{personId}"
    fun person(personId: String) = "person/$personId"
    fun chat(chatId: String) = "chat/$chatId"
    fun info(page: String) = "info/$page"
    fun peopleList(kind: String) = "people/$kind"

    fun thread(postId: String) = "thread/$postId"
    /** Compose; [quote] embeds that note as a quote, [group] posts into that group. */
    fun compose(quote: String? = null, group: String? = null) = buildString {
        append(COMPOSE_POST)
        val args = listOfNotNull(quote?.let { "quote=$it" }, group?.let { "group=$it" })
        if (args.isNotEmpty()) append("?").append(args.joinToString("&"))
    }
    fun groupDetail(groupId: String) = "$GROUP_DETAIL?groupId=$groupId"
    fun eventDetail(eventId: String) = "$EVENT_DETAIL?eventId=$eventId"
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
        composable(
            "${Routes.COMPOSE_POST}?quote={quote}&group={group}",
            arguments = listOf(
                navArgument("quote") { nullable = true; defaultValue = null },
                navArgument("group") { nullable = true; defaultValue = null }
            )
        ) { entry ->
            ComposePostScreen(navController, entry.arguments?.getString("quote"), entry.arguments?.getString("group"))
        }
        composable(
            "${Routes.GROUP_DETAIL}?groupId={groupId}",
            arguments = listOf(navArgument("groupId") { nullable = true; defaultValue = null })
        ) { entry -> GroupScreen(navController, entry.arguments?.getString("groupId")) }
        composable(
            "${Routes.EVENT_DETAIL}?eventId={eventId}",
            arguments = listOf(navArgument("eventId") { nullable = true; defaultValue = null })
        ) { entry -> EventScreen(navController, entry.arguments?.getString("eventId")) }
        composable(Routes.CREATE_GROUP) { CreateGroupScreen(navController) }
        composable(Routes.CREATE_EVENT) { CreateEventScreen(navController) }
        composable(Routes.YOUR_CIRCLE) { YourCircleScreen(navController) }
        composable(Routes.EXPLORE_SEARCH) { ExploreSearchScreen(navController) }
        composable(Routes.SETTINGS) { SettingsScreen(navController) }
        composable(Routes.PERSON) { entry -> PersonProfileScreen(navController, entry.arguments?.getString("personId").orEmpty()) }
        composable(Routes.CHAT) { entry -> ChatScreen(navController, entry.arguments?.getString("chatId").orEmpty()) }
        composable(Routes.EDIT_PROFILE) { EditProfileScreen(navController) }
        composable(Routes.INFO) { entry -> InfoScreen(navController, entry.arguments?.getString("page").orEmpty()) }
        composable(Routes.PEOPLE_LIST) { entry -> PeopleListScreen(navController, entry.arguments?.getString("kind").orEmpty()) }
        composable(Routes.THREAD) { entry ->
            ThreadScreen(navController, entry.arguments?.getString("postId").orEmpty())
        }
    }
}

/**
 * Opens someone's profile, rendered by your relationship to them. Yourself: your Profile tab.
 * Someone you blocked: nothing (they don't appear anywhere, so there is no profile to open).
 */
fun NavHostController.openPerson(personId: String) {
    when (com.circle.app.data.CircleStore.relationshipTo(personId)) {
        com.circle.app.data.Relationship.SELF -> navigate(Routes.PROFILE)
        com.circle.app.data.Relationship.BLOCKED -> Unit
        else -> if (com.circle.app.data.CircleStore.person(personId) != null) navigate(Routes.person(personId))
    }
}
