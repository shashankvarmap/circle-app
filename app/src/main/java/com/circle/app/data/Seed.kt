package com.circle.app.data

import com.circle.app.data.access.Audience
import com.circle.app.data.access.EventVisibility
import com.circle.app.ui.theme.*
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Demo seed data, taken from the design prototype (design-handoff/reference/prototype-logic.js).
 *
 * Deviations from the prototype, so the friend graph is consistent:
 * - Riya Sharma and Marco Diaz are pending friend requests (brief §4), so they are NOT friends
 *   yet. Their two places among the 12 people shown in Your circle go to Mira Lange (inner)
 *   and Ines Moreau (outer).
 * - Elena Voss is not a friend (NOT_FRIENDS_YET.md §6): a note Priya shares in your chat, a
 *   message in the Aachen Photo Walk chat, and a comment on Amara's note.
 * - No mutual-friend numbers anywhere (MASTER_PROMPT 2).
 * - Times are Instants relative to [Clock.now]; events always fall in the coming week.
 * - "128 friends" is real: 12 named friends plus 116 generated ones, so the count is derived.
 */
internal object Seed {
    val me = Person(ME, "You", AvatarSlate, 0)

    /** The 12 friends the prototype shows in Your circle, in its order. */
    val namedFriends = listOf(
        Person("AO", "Amara Osei", AvatarOrange, 2023, "Puzzles, long walks and too much tea."),
        Person("PK", "Priya Kapoor", AvatarSage, 2023, "Photographs the cathedral at every hour. Board games on Fridays."),
        Person("JL", "Jonah Lee", AvatarBlue, 2023, "Good coffee is the whole plan."),
        Person("TN", "Tomas Novak", AvatarSlate, 2024, "Gardening badly, photographing well."),
        Person("ML", "Mira Lange", AvatarRose, 2024, "Ceramics on Tuesdays, rowing on Sundays."),
        Person("EK", "Elin Kaur", AvatarSlate, 2024, "Reading my way through the library, A to Z."),
        Person("IM", "Ines Moreau", AvatarTeal, 2024, "Bread, bikes, and bad puns."),
        Person("SY", "Sana Yildiz", AvatarAmber, 2025, "New in Aachen. Say hi."),
        Person("NB", "Nils Baum", AvatarSage, 2025, "Trail runner, brunch enthusiast."),
        Person("LF", "Lena Fuchs", AvatarBlue, 2025, "Choir, cello, coffee."),
        Person("OH", "Omar Haddad", AvatarOrange, 2025, "Fixing bikes for friends since 2019."),
        Person("CZ", "Clara Zeller", AvatarRose, 2026, "Learning to cook one recipe a week."),
    )

    val initialInnerCircle = listOf("AO", "PK", "JL", "TN", "ML")

    /** People who sent you a friend request. */
    val requesters = listOf(
        Person("RS", "Riya Sharma", AvatarRose, 2026, "Sourdough, finally. Telling everyone."),
        Person("MD", "Marco Diaz", AvatarTeal, 2026, "Weekend hikes and weeknight pasta."),
    )

    /** People who aren't your friends and haven't asked: you can find them and ask. */
    val others = listOf(
        Person("EV", "Elena Voss", AvatarRose, 0, "Museums on rainy days, the Lousberg on sunny ones."),
        Person("LB", "Lukas Brandt", AvatarRose, 0, "Architecture student. Always sketching."),
        Person("NK", "Nora Kim", AvatarAmber, 0, "Cyclist. Will talk about bread."),
        Person("DW", "Daniel Weiss", AvatarBlue, 0, "Film photography, slow mornings."),
        Person("HS", "Hana Sato", AvatarTeal, 0, "Climbing on weekdays, ramen on weekends."),
        Person("FB", "Felix Braun", AvatarSage, 0, "Teaching myself the piano, one song at a time."),
    )

    /** "People you may know" from contacts: three friends and every non-friend above. */
    val contactMatchIds = listOf("PK", "JL", "AO") + others.map { it.id }
    fun incomingRequests() = listOf(
        FriendRequest("RS", ago(m = 50)),
        FriendRequest("MD", ago(h = 3)),
    )

    /** I am in these friends' inner circles (FRONTEND_COMPLETE 4: treat the seed friends as inner). */
    val innerCirclesIAmIn = setOf("AO", "PK", "JL", "TN", "ML", "EK", "IM", "SY", "NB", "LF", "OH", "CZ")

    private fun ago(d: Long = 0, h: Long = 0, m: Long = 0): Instant =
        Clock.ago(Duration.ofDays(d).plusHours(h).plusMinutes(m))

    /** [daysAhead] days from today at a local clock time. */
    private fun ahead(daysAhead: Long, hhmm: String): Instant {
        val zone = ZoneId.systemDefault()
        return Clock.now().atZone(zone).toLocalDate().plusDays(daysAhead).atTime(LocalTime.parse(hhmm)).atZone(zone).toInstant()
    }

    private var commentSeq = 0
    private fun c(author: String, at: Instant, text: String, replies: List<Comment> = emptyList()) =
        Comment("c${++commentSeq}", author, at, text, replies)

    private val firstNames = listOf(
        "Ada", "Ben", "Carla", "David", "Emil", "Fatima", "Georg", "Hanna", "Ivan", "Julia", "Karim",
        "Lea", "Mats", "Nadia", "Oskar", "Paula", "Rafael", "Sofia", "Theo", "Ursula", "Vera", "Yusuf",
        "Zoe", "Anton", "Bianca", "Cem", "Dana", "Felix", "Greta", "Jan"
    )
    private val lastNames = listOf(
        "Albrecht", "Brandt", "Costa", "Dekker", "Engel", "Fischer", "Graf", "Hoffmann", "Janssen",
        "Keller", "Lorenz", "Meyer", "Nowak", "Okafor", "Peters", "Quint", "Roth", "Schulz", "Tanaka",
        "Vogel", "Weber", "Yilmaz", "Ziegler", "Moreno", "Laine", "Berg", "Arslan", "Hahn", "Kraus"
    )
    private val avatarCycle = listOf(AvatarOrange, AvatarSage, AvatarBlue, AvatarSlate, AvatarRose, AvatarTeal, AvatarAmber)

    /** 116 more friends so the friend list really has 128 people. Deterministic. */
    val generatedFriends: List<Person> = List(116) { i ->
        val first = firstNames[i % firstNames.size]
        val last = lastNames[(i * 7 + i / firstNames.size) % lastNames.size]
        Person(
            id = "f%03d".format(i),
            name = "$first $last",
            color = avatarCycle[(i * 3) % avatarCycle.size],
            sinceYear = 2019 + (i % 8),
        )
    }

    /** Newest first, as Home shows them (chronological, never ranked). */
    fun posts(): List<Post> {
        commentSeq = 0
        return listOf(
            Post("you-media", ME, ago(m = 40), "", hasMedia = true),
            Post("you-coffee", ME, ago(h = 1), "This is exactly the energy I needed today.", quotedPostId = "jonah-coffee"),
            Post(
                "amara-puzzle", "AO", ago(h = 2),
                "Finally finished the puzzle we started at Mom's over the summer.",
                comments = listOf(
                    c("PK", ago(h = 1), "Aww, that's so wholesome!", listOf(
                        c("AO", ago(m = 58), "It took us three months 😅"),
                        c("JL", ago(m = 40), "Frame it!"),
                    )),
                    c("JL", ago(h = 1), "Which puzzle was it?", listOf(c("AO", ago(m = 50), "The 2000-piece lighthouse one."))),
                    c("EV", ago(m = 30), "Mom must be thrilled."),
                )
            ),
            Post("you-repost-seaglass", ME, ago(h = 3), "", repostOfId = "priya-seaglass"),
            Post(
                "priya-seaglass", "PK", ago(h = 5),
                "Sea glass hunting turned into a two hour walk. Worth it.", hasMedia = true,
                comments = listOf(c("AO", ago(h = 4), "Save me a green one!"))
            ),
            Post("jonah-coffee", "JL", ago(d = 1, h = 2), "Some days the good coffee is the whole plan."),
            // Elena isn't your friend: her note only reaches you when a friend shares it (as Priya does in chat).
            Post(
                "elena-forum", "EV", ago(h = 3),
                "Spent the whole afternoon at the Ludwig Forum. The new light installation upstairs is worth the trip on its own. Go early, before the school groups arrive."
            ),
            Post("tomas-tomatoes", "TN", ago(d = 1, h = 4), "Officially declared the tomatoes a lost cause. Onward to garlic."),
            Post("you-plants", ME, ago(d = 1, h = 6), "Repotted every plant on the balcony before the first frost. Overdue by a month."),
            Post("you-laptop", ME, ago(d = 3), "Finally cleared the laptop repair backlog sitting around since spring. Small victories."),
            Post("you-sunset", ME, ago(d = 5), "Sunset over the river walk, worth the detour.", hasMedia = true),
            Post("you-workshop", ME, ago(d = 9), "Workshop table is finally starting to look like a table.", hasMedia = true),

            // Group posts stay in their group.
            Post(
                "group-golden-hour", "PK", ago(m = 40),
                "Golden hour shoot this Sunday: meet at the cathedral steps, 6:30 sharp.", groupId = "photo-walk",
                comments = listOf(c("TN", ago(m = 20), "I'll bring the spare tripod."))
            ),
            Post("group-catan", "JL", ago(h = 2), "Anyone up for Catan tonight? Need a fourth.", groupId = "board-game-club"),
            Post("group-long-exposure", "TN", ago(h = 3), "Finally got the long-exposure shot of the fountain right.", groupId = "photo-walk"),
            Post("group-trail-route", "AO", ago(h = 5), "Route for this week is up. Easy pace, brunch after.", groupId = "sunday-trail"),
        )
    }

    /** Posts the seed user had already liked (private: only the author would know). */
    val initialLikes = listOf("you-coffee", "group-catan", "group-long-exposure")

    fun groups() = listOf(
        GroupInfo(
            "photo-walk", "Aachen Photo Walk", "Photography", "PHOTOGRAPHY",
            "Weekend photo walks around Aachen: the cathedral, the Elisenbrunnen, wherever the light looks good. All levels welcome.",
            AvatarOrange, isPublic = true, compassDeg = 322f, memberCount = 128, isMember = true
        ).apply {
            hostIds.addAll(listOf("PK", "JL"))
            memberIds.addAll(listOf("AO", "TN", "RS", "EK", "MD", "SY", "NB", "EV", "OH", "NK", "DW", "HS", "FB"))
            joinRequests.addAll(listOf("LB" to ago(h = 2), "CZ" to ago(d = 1)))
        },
        GroupInfo(
            "sunday-trail", "Sunday Trail Runners", "Fitness & Sports", "FITNESS & SPORTS",
            "Easy Sunday morning trail runs, followed by a very necessary brunch.",
            AvatarSage, isPublic = true, compassDeg = 62f, memberCount = 128, isMember = false
        ),
        GroupInfo(
            "board-game-club", "RWTH Board Game Club", "Gaming", "GAMING",
            "Weekly game nights on campus. Newcomers and rules-lawyers equally welcome.",
            AvatarBlue, isPublic = true, compassDeg = 138f, memberCount = 64, isMember = false
        ),
        GroupInfo(
            "aachen-choir", "Aachen Choir", "Music", "MUSIC", "",
            AvatarRose, isPublic = true, compassDeg = 248f, memberCount = 42, isMember = false
        )
    )

    fun events() = listOf(
        EventInfo(
            "trail-brunch", "Trail Run + Brunch", "OUTDOORS", 40f, ahead(3, "09:00"),
            "Forest Park Trailhead", EventVisibility.OPEN_NEARBY, "PK",
            "Easy 8k loop, then pancakes. Walkers welcome for the brunch half.",
            capacity = 10, goingBase = 4, joined = false,
            comments = listOf(c("PK", ago(h = 3), "Is the trail muddy after the rain?"))
        ),
        EventInfo(
            "board-game-night", "Board Game Night", "SOCIAL", 125f, ahead(2, "18:00"),
            "Kaffeehaus Zeitlos", EventVisibility.FRIENDS_OF_FRIENDS, "PK",
            "Monthly board game night: bringing Catan and Wingspan, but open to requests. Snacks provided, BYO drink.",
            capacity = 12, goingBase = 6, joined = false,
            comments = listOf(
                c("JL", ago(h = 2), "Bringing Wingspan too if anyone wants a break from Catan."),
                c("AO", ago(h = 1), "Count me in, I'll bring the good snacks this time."),
            )
        ),
        EventInfo(
            "open-mic", "Studio Open Mic", "MUSIC", 215f, ahead(5, "20:30"),
            "MTI Courtyard", EventVisibility.FRIENDS, "JL", "",
            capacity = 20, goingBase = 3, joined = false
        ),
        EventInfo(
            "market-walk", "Market Walk", "SOCIAL", 165f, ahead(4, "10:00"),
            "Elisenbrunnen", EventVisibility.OPEN_NEARBY, "AO", "",
            capacity = 12, goingBase = 6, joined = false
        ),
        EventInfo(
            "gallery-night", "Gallery Night", "MAKING", 305f, ahead(6, "19:00"),
            "Ludwig Forum", EventVisibility.FRIENDS_OF_FRIENDS, "RS", "",
            capacity = 15, goingBase = 5, joined = false
        )
    )

    /**
     * Quiet alerts. Only what is allowed: likes go to the author only; activity never reveals who
     * is friends with whom or who liked what (the prototype's "Marco and Priya are now friends" and
     * "Priya liked Jonah's post" rows were dropped for that reason).
     */
    fun alerts() = listOf(
        Alert("a1", "PK", AlertKind.LIKED_YOUR_NOTE, ago(m = 10), postId = "you-plants"),
        Alert("a2", "JL", AlertKind.COMMENTED_ON_YOUR_NOTE, ago(m = 25), quote = "Where was this?", postId = "you-sunset"),
        Alert("a3", "AO", AlertKind.REPOSTED_YOUR_NOTE, ago(h = 1), postId = "you-laptop"),
        Alert("a4", "TN", AlertKind.QUOTED_YOUR_NOTE, ago(h = 3), quote = "so real", postId = "you-workshop"),
        Alert("b1", "AO", AlertKind.FRIEND_COMMENTED, ago(m = 45), quote = "ha, same", postId = "tomas-tomatoes", aboutId = "TN"),
        Alert("b2", "JL", AlertKind.FRIEND_REPOSTED, ago(h = 2), postId = "tomas-tomatoes", aboutId = "TN"),
    )
}
