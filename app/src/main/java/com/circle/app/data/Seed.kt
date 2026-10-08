package com.circle.app.data

import com.circle.app.ui.theme.*

/**
 * Demo seed data, taken from the design prototype (design-handoff/reference/prototype-logic.js).
 *
 * Deviations from the prototype, so the friend graph is consistent:
 * - Riya Sharma and Marco Diaz are pending friend requests (brief §4), so they are NOT friends
 *   yet. Their two places among the 12 people shown in Your circle go to Mira Lange (inner)
 *   and Ines Moreau (outer).
 * - Elena Voss is not a friend (NOT_FRIENDS_YET.md §6): 3 mutual friends, a note Priya shares
 *   in your chat, a message in the Aachen Photo Walk chat, and a comment on Amara's note.
 * - "128 friends" is real: 12 named friends plus 116 generated ones, so the count is derived.
 */
internal object Seed {
    val me = Person(ME, "You", AvatarSlate, 0)

    /** The 12 friends the prototype shows in Your circle, in its order. */
    val namedFriends = listOf(
        Person("AO", "Amara Osei", AvatarOrange, 2023, "Puzzles, long walks and too much tea.", 14),
        Person("PK", "Priya Kapoor", AvatarSage, 2023, "Photographs the cathedral at every hour. Board games on Fridays.", 21),
        Person("JL", "Jonah Lee", AvatarBlue, 2023, "Good coffee is the whole plan.", 17),
        Person("TN", "Tomas Novak", AvatarSlate, 2024, "Gardening badly, photographing well.", 9),
        Person("ML", "Mira Lange", AvatarRose, 2024, "Ceramics on Tuesdays, rowing on Sundays.", 11),
        Person("EK", "Elin Kaur", AvatarSlate, 2024, "Reading my way through the library, A to Z.", 6),
        Person("IM", "Ines Moreau", AvatarTeal, 2024, "Bread, bikes, and bad puns.", 5),
        Person("SY", "Sana Yildiz", AvatarAmber, 2025, "New in Aachen. Say hi.", 4),
        Person("NB", "Nils Baum", AvatarSage, 2025, "Trail runner, brunch enthusiast.", 7),
        Person("LF", "Lena Fuchs", AvatarBlue, 2025, "Choir, cello, coffee.", 3),
        Person("OH", "Omar Haddad", AvatarOrange, 2025, "Fixing bikes for friends since 2019.", 8),
        Person("CZ", "Clara Zeller", AvatarRose, 2026, "Learning to cook one recipe a week.", 2),
    )

    val initialInnerCircle = listOf("AO", "PK", "JL", "TN", "ML")

    /** People who sent you a friend request. */
    val requesters = listOf(
        Person("RS", "Riya Sharma", AvatarRose, 2026, "Sourdough, finally. Telling everyone.", 3),
        Person("MD", "Marco Diaz", AvatarTeal, 2026, "Weekend hikes and weeknight pasta.", 1),
    )

    /** People who aren't your friends and haven't asked: you can find them and ask. */
    val others = listOf(
        Person("EV", "Elena Voss", AvatarRose, 0, "Museums on rainy days, the Lousberg on sunny ones.", 3),
        Person("LB", "Lukas Brandt", AvatarRose, 0, "Architecture student. Always sketching.", 2),
        Person("NK", "Nora Kim", AvatarAmber, 0, "Cyclist. Will talk about bread.", 1),
    )
    val incomingRequests = listOf(
        FriendRequest("RS", "3 mutual friends"),
        FriendRequest("MD", "1 mutual friend"),
    )

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
            mutualFriends = 1 + (i % 9)
        )
    }

    /** Newest first, as Home shows them (chronological, never ranked). */
    fun posts() = listOf(
        Post("you-media", ME, "You", ME, AvatarSlate, "40m", "", hasMedia = true),
        Post(
            "you-coffee", ME, "You", ME, AvatarSlate, "1h",
            "This is exactly the energy I needed today.",
            quotedPostId = "jonah-coffee"
        ),
        Post(
            "amara-puzzle", "AO", "Amara Osei", "AO", AvatarOrange, "2h",
            "Finally finished the puzzle we started at Mom's over the summer.",
            comments = listOf(
                Comment(
                    "Priya Kapoor", "PK", AvatarSage, "1h", "Aww, that's so wholesome!",
                    replies = listOf(
                        Comment("Amara Osei", "AO", AvatarOrange, "58m", "It took us three months \uD83D\uDE05"),
                        Comment("Jonah Lee", "JL", AvatarBlue, "40m", "Frame it!")
                    )
                ),
                Comment(
                    "Jonah Lee", "JL", AvatarBlue, "1h", "Which puzzle was it?",
                    replies = listOf(Comment("Amara Osei", "AO", AvatarOrange, "50m", "The 2000-piece lighthouse one."))
                ),
                Comment("Elena Voss", "EV", AvatarRose, "30m", "Mom must be thrilled.")
            )
        ),
        Post("you-repost-seaglass", ME, "You", ME, AvatarSlate, "3h", "", repostOfId = "priya-seaglass"),
        Post(
            "priya-seaglass", "PK", "Priya Kapoor", "PK", AvatarSage, "5h",
            "Sea glass hunting turned into a two hour walk. Worth it.", hasMedia = true,
            comments = listOf(Comment("Amara Osei", "AO", AvatarOrange, "4h", "Save me a green one!"))
        ),
        Post("jonah-coffee", "JL", "Jonah Lee", "JL", AvatarBlue, "yesterday", "Some days the good coffee is the whole plan."),
        // Elena isn't your friend: her note only reaches you when a friend shares it (as Priya does in chat).
        Post(
            "elena-forum", "EV", "Elena Voss", "EV", AvatarRose, "3h",
            "Spent the whole afternoon at the Ludwig Forum. The new light installation upstairs is worth the trip on its own. Go early, before the school groups arrive."
        ),
        Post("tomas-tomatoes", "TN", "Tomas Novak", "TN", AvatarSlate, "yesterday", "Officially declared the tomatoes a lost cause. Onward to garlic."),
        Post("you-plants", ME, "You", ME, AvatarSlate, "1d", "Repotted every plant on the balcony before the first frost. Overdue by a month."),
        Post("you-laptop", ME, "You", ME, AvatarSlate, "3d", "Finally cleared the laptop repair backlog sitting around since spring. Small victories."),
        Post("you-sunset", ME, "You", ME, AvatarSlate, "5d", "Sunset over the river walk \u2014 worth the detour.", hasMedia = true),
        Post("you-workshop", ME, "You", ME, AvatarSlate, "9d", "Workshop table is finally starting to look like a table.", hasMedia = true),

        // Group posts stay in their group.
        Post(
            "group-golden-hour", "PK", "Priya Kapoor", "PK", AvatarSage, "40m",
            "Golden hour shoot this Sunday \u2014 meet at the cathedral steps, 6:30 sharp.", groupId = "photo-walk",
            comments = listOf(Comment("Tomas Novak", "TN", AvatarSlate, "20m", "I'll bring the spare tripod."))
        ),
        Post("group-catan", "JL", "Jonah Lee", "JL", AvatarBlue, "2h", "Anyone up for Catan tonight? Need a fourth.", groupId = "board-game-club"),
        Post("group-long-exposure", "TN", "Tomas Novak", "TN", AvatarSlate, "3h", "Finally got the long-exposure shot of the fountain right.", groupId = "photo-walk"),
        Post("group-trail-route", "AO", "Amara Osei", "AO", AvatarOrange, "5h", "Route for this week is up. Easy pace, brunch after.", groupId = "sunday-trail")
    )

    /** Posts the seed user had already liked (private: only the author would know). */
    val initialLikes = listOf("you-coffee", "group-catan", "group-long-exposure")

    fun groups() = listOf(
        GroupInfo(
            "photo-walk", "Aachen Photo Walk", "Photography", "PHOTOGRAPHY",
            "Weekend photo walks around Aachen — the cathedral, the Elisenbrunnen, wherever the light looks good. All levels welcome.",
            AvatarOrange, isPublic = true, compassDeg = 322f, memberCount = 128, isMember = true
        ).apply {
            hostIds.addAll(listOf("PK", "JL"))
            memberIds.addAll(listOf("AO", "TN", "RS", "EK", "MD", "SY", "NB", "EV", "OH"))
            joinRequests.addAll(listOf("LB" to "2h", "CZ" to "1d"))
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
            "trail-brunch", "Trail Run + Brunch", "SEP", "19", "OUTDOORS", 40f,
            "Sat 19 Sep, 9:00", "Forest Park Trailhead", "OPEN NEARBY", "PK",
            "Easy 8k loop, then pancakes. Walkers welcome for the brunch half.",
            capacity = 10, goingBase = 4, joined = false,
            comments = listOf(Comment("Priya Kapoor", "PK", AvatarSage, "3h", "Is the trail muddy after the rain?"))
        ),
        EventInfo(
            "board-game-night", "Board Game Night", "SEP", "18", "SOCIAL", 125f,
            "Fri 18 Sep, 18:00", "Kaffeehaus Zeitlos", "FRIENDS OF FRIENDS", "PK",
            "Monthly board game night — bringing Catan and Wingspan, but open to requests. Snacks provided, BYO drink.",
            capacity = 12, goingBase = 6, joined = false,
            comments = listOf(
                Comment("Jonah Lee", "JL", AvatarBlue, "2h", "Bringing Wingspan too if anyone wants a break from Catan."),
                Comment("Amara Osei", "AO", AvatarOrange, "1h", "Count me in, I'll bring the good snacks this time.")
            )
        ),
        EventInfo(
            "open-mic", "Studio Open Mic", "SEP", "21", "MUSIC", 215f,
            "Mon 21 Sep, 20:30", "MTI Courtyard", "FRIENDS", "JL", "",
            capacity = 20, goingBase = 3, joined = false
        ),
        EventInfo(
            "market-walk", "Market Walk", "SEP", "20", "SOCIAL", 165f,
            "Sun 20 Sep, 10:00", "Elisenbrunnen", "OPEN NEARBY", "AO", "",
            capacity = 12, goingBase = 6, joined = false
        ),
        EventInfo(
            "gallery-night", "Gallery Night", "SEP", "22", "MAKING", 305f,
            "Tue 22 Sep, 19:00", "Ludwig Forum", "FRIENDS OF FRIENDS", "RS", "",
            capacity = 15, goingBase = 5, joined = false
        )
    )
}
