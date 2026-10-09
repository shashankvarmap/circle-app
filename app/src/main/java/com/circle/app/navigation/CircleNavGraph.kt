package com.circle.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.circle.app.data.session.Destination
import com.circle.app.ui.launch.SessionViewModel
import com.circle.app.ui.screens.*

object Routes {
    const val WELCOME = "welcome"
    /** The 4-slide story: only reached right after creating an account. */
    const val STORY = "story"
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
    /** Compose; [quote] embeds that note as a quote, [group] posts into that group, [edit] edits your note. */
    fun compose(quote: String? = null, group: String? = null, edit: String? = null) = buildString {
        append(COMPOSE_POST)
        val args = listOfNotNull(quote?.let { "quote=$it" }, group?.let { "group=$it" }, edit?.let { "edit=$it" })
        if (args.isNotEmpty()) append("?").append(args.joinToString("&"))
    }

    const val VIEWER = "viewer/{noteId}?index={index}"
    /** Full-screen photo viewer for a note's media (re-checked through the repository). */
    fun viewer(noteId: String, index: Int = 0) = "viewer/$noteId?index=$index"
    fun groupDetail(groupId: String) = "$GROUP_DETAIL?groupId=$groupId"
    fun eventDetail(eventId: String) = "$EVENT_DETAIL?eventId=$eventId"
}

/** The route for a step of getting in. */
fun Destination.route(): String = when (this) {
    Destination.HOME -> Routes.HOME
    Destination.WELCOME, Destination.WELCOME_LOG_IN_ONLY -> Routes.WELCOME
    Destination.FIND_FRIENDS -> Routes.CONTACTS_SYNC
    Destination.STORY -> Routes.STORY
}

/** Goes to [d] and clears everything behind it (getting in and out never leaves a back stack). */
fun NavHostController.resetTo(d: Destination) = navigate(d.route()) { popUpTo(graph.id) { inclusive = true } }

@Composable
fun CircleNavGraph(
    session: SessionViewModel,
    start: Destination,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = start.route()) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                session,
                onCreateAccount = { session.createAccount { navController.resetTo(it) } },
                onLogIn = { session.logIn { navController.resetTo(it) } },
                onLegal = { navController.navigate(Routes.info("terms")) }
            )
        }
        composable(Routes.STORY) { OnboardingScreen(onDone = { session.storyDone { navController.resetTo(it) } }) }
        composable(Routes.CONTACTS_SYNC) { ContactsSyncScreen(onDone = { session.findFriendsDone { navController.resetTo(it) } }) }
        composable(Routes.HOME) { HomeScreen(navController) }
        composable(Routes.EXPLORE) { ExploreScreen(navController) }
        composable(Routes.NOTIFICATIONS) { NotificationsScreen(navController) }
        composable(Routes.MESSAGES) { MessagesScreen(navController) }
        composable(Routes.PROFILE) { ProfileScreen(navController) }
        composable(
            "${Routes.COMPOSE_POST}?quote={quote}&group={group}&edit={edit}",
            arguments = listOf(
                navArgument("quote") { nullable = true; defaultValue = null },
                navArgument("group") { nullable = true; defaultValue = null },
                navArgument("edit") { nullable = true; defaultValue = null }
            )
        ) { entry ->
            ComposePostScreen(navController, entry.arguments?.getString("quote"), entry.arguments?.getString("group"), entry.arguments?.getString("edit"))
        }
        composable(Routes.VIEWER, arguments = listOf(navArgument("index") { type = androidx.navigation.NavType.IntType; defaultValue = 0 })) { entry ->
            PhotoViewerScreen(navController, entry.arguments?.getString("noteId").orEmpty(), entry.arguments?.getInt("index") ?: 0)
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
        composable(Routes.SETTINGS) {
            SettingsScreen(
                navController,
                onLogOut = { session.logOut { navController.resetTo(it) } },
                onAccountDeleted = { session.accountDeleted { navController.resetTo(it) } }
            )
        }
        composable(Routes.PERSON, deepLinks = listOf(navDeepLink { uriPattern = "$DEEP_LINK_BASE/person/{personId}" })) { entry -> PersonProfileScreen(navController, entry.arguments?.getString("personId").orEmpty()) }
        composable(Routes.CHAT) { entry -> ChatScreen(navController, entry.arguments?.getString("chatId").orEmpty()) }
        composable(Routes.EDIT_PROFILE) { EditProfileScreen(navController) }
        composable(Routes.INFO) { entry -> InfoScreen(navController, entry.arguments?.getString("page").orEmpty()) }
        composable(Routes.PEOPLE_LIST) { entry -> PeopleListScreen(navController, entry.arguments?.getString("kind").orEmpty()) }
        composable(Routes.THREAD, deepLinks = listOf(navDeepLink { uriPattern = "$DEEP_LINK_BASE/thread/{postId}" })) { entry ->
            ThreadScreen(navController, entry.arguments?.getString("postId").orEmpty())
        }
    }
}

/**
 * Opens someone's profile. Yourself: your Profile tab. Anyone else: the one profile screen, which
 * asks the repository and renders Full, Limited or NotFound (blocked people have no profile).
 */
fun NavHostController.openPerson(personId: String) {
    if (personId == com.circle.app.data.ME) navigate(Routes.PROFILE) else navigate(Routes.person(personId))
}

/** Deep links (and notification taps) use the same routes, so they pass the same checks. */
const val DEEP_LINK_BASE = "circle://app"
