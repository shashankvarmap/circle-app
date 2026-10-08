package com.circle.app.data

enum class ThemeMode(val label: String) { SYSTEM("System"), NIGHT("Night"), DAY("Day") }

enum class RequestsFrom(val label: String) { EVERYONE("Everyone"), FRIENDS_OF_FRIENDS("Friends of friends") }

enum class InvitesFrom(val label: String) { FRIENDS("Friends"), FRIENDS_OF_FRIENDS("Friends of friends") }

enum class PauseAll(val label: String) { OFF("Off"), ONE_HOUR("1 hour"), UNTIL_TOMORROW("Until tomorrow") }

/** Your own profile. The pinned thought is the bio: it's all a stranger sees, with your photo. */
data class MyProfile(
    val displayName: String = "Shashank",
    val handle: String = "shashank",
    val pinnedThought: String = "Slowly rebuilding the workshop table. Say hi if you're nearby — always up for coffee and bad puns.",
    /** Content URI of the chosen avatar photo, or null for initials. */
    val avatarUri: String? = null,
) {
    val initial: String get() = displayName.trim().firstOrNull()?.uppercase() ?: ME
}

data class NotificationPrefs(
    val likes: Boolean = true,
    val comments: Boolean = true,
    val friendRequests: Boolean = true,
    val events: Boolean = true,
    val groups: Boolean = true,
    val messages: Boolean = true,
    val pause: PauseAll = PauseAll.OFF,
)

data class PrivacyPrefs(
    val findableByName: Boolean = true,
    val findableFromContacts: Boolean = true,
    val requestsFrom: RequestsFrom = RequestsFrom.EVERYONE,
    val invitesFrom: InvitesFrom = InvitesFrom.FRIENDS, // brief gives no default; the more private one
    val readReceipts: Boolean = true,
)

object HandleRules {
    const val MIN = 3
    const val MAX = 20
    private val allowed = Regex("^[a-z0-9_]+$")

    /** Why a handle isn't valid, or null when it is. */
    fun problem(handle: String): String? = when {
        handle.length < MIN -> "At least $MIN characters"
        handle.length > MAX -> "At most $MAX characters"
        !allowed.matches(handle) -> "Lowercase letters, numbers and _ only"
        else -> null
    }
}
