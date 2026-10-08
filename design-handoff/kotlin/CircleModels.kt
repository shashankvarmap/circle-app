package com.circle.app.data

import com.circle.app.ui.orbit.EventTableGeometry
import com.circle.app.ui.orbit.Pt
import com.circle.app.ui.orbit.ringPoints

/** UNVERIFIED STARTER. Plain models + seed data copied from the prototype. */

data class Person(val id: String, val name: String, val colorArgb: Long, val sinceYear: Int) {
    val initials: String get() = id
}

enum class Visibility(val label: String) {
    FRIENDS("FRIENDS"), FRIENDS_OF_FRIENDS("FRIENDS OF FRIENDS"), OPEN_NEARBY("OPEN NEARBY")
}

data class EventItem(
    val id: String, val title: String, val day: String, val month: String, val topicLabel: String,
    val compassDeg: Float, val whenText: String, val place: String,
    val goingBase: Int, val capacity: Int, val visibility: Visibility, val hostId: String,
)

data class GroupItem(
    val id: String, val title: String, val letter: String, val colorArgb: Long, val topicLabel: String,
    val compassDeg: Float, val members: Int, val memberInitially: Boolean,
)

object Seed {
    val people = listOf(
        Person("AO", "Amara Osei", 0xFFD98C4A, 2023), Person("PK", "Priya Kapoor", 0xFF7FA88C, 2023),
        Person("JL", "Jonah Lee", 0xFF8DA6D9, 2023), Person("TN", "Tomas Novak", 0xFF3B4351, 2024),
        Person("RS", "Riya Sharma", 0xFFC77B8F, 2024), Person("EK", "Elin Kaur", 0xFF3B4351, 2024),
        Person("MD", "Marco Diaz", 0xFF6FA8B5, 2024), Person("SY", "Sana Yildiz", 0xFFE9C46A, 2025),
        Person("NB", "Nils Baum", 0xFF7FA88C, 2025), Person("LF", "Lena Fuchs", 0xFF8DA6D9, 2025),
        Person("OH", "Omar Haddad", 0xFFD98C4A, 2025), Person("CZ", "Clara Zeller", 0xFFC77B8F, 2026),
    )
    val initialInnerCircle = listOf("AO", "PK", "JL", "TN", "RS")

    val events = listOf(
        EventItem("trail", "Trail Run + Brunch", "19", "SEP", "OUTDOORS", 40f, "Sat 19 Sep, 9:00", "Forest Park Trailhead", 4, 10, Visibility.OPEN_NEARBY, "PK"),
        EventItem("boardgames", "Board Game Night", "18", "SEP", "SOCIAL", 125f, "Fri 18 Sep, 18:00", "Kaffeehaus Zeitlos", 6, 12, Visibility.FRIENDS_OF_FRIENDS, "PK"),
        EventItem("openmic", "Studio Open Mic", "21", "SEP", "MUSIC", 215f, "Mon 21 Sep, 20:30", "MTI Courtyard", 3, 20, Visibility.FRIENDS, "JL"),
        EventItem("market", "Market Walk", "20", "SEP", "SOCIAL", 165f, "Sun 20 Sep, 10:00", "Elisenbrunnen", 6, 12, Visibility.OPEN_NEARBY, "AO"),
        EventItem("gallery", "Gallery Night", "22", "SEP", "MAKING", 305f, "Tue 22 Sep, 19:00", "Ludwig Forum", 5, 15, Visibility.FRIENDS_OF_FRIENDS, "RS"),
    )
    val groups = listOf(
        GroupItem("photo", "Aachen Photo Walk", "A", 0xFFD98C4A, "PHOTOGRAPHY", 322f, 128, true),
        GroupItem("trailrun", "Sunday Trail Runners", "S", 0xFF7FA88C, "FITNESS & SPORTS", 62f, 128, false),
        GroupItem("boardclub", "RWTH Board Game Club", "R", 0xFF8DA6D9, "GAMING", 138f, 64, false),
        GroupItem("choir", "Aachen Choir", "C", 0xFFC77B8F, "MUSIC", 248f, 42, false),
    )
    /** Who fills the seats, in order. Seat 0 is always the host. */
    val seatPool = listOf("PK", "JL", "AO", "TN", "RS", "MD", "EK", "SY", "NB", "LF", "OH", "CZ", "PK", "JL", "AO", "TN", "RS", "MD", "EK", "SY")
}

enum class SeatKind { PERSON, ME, EMPTY }
data class Seat(val kind: SeatKind, val personId: String?, val isHost: Boolean, val pt: Pt)

/** Port of the prototype's seat logic. Every place at the table is drawn, filled or empty. */
fun eventSeats(event: EventItem, iHaveJoined: Boolean): List<Seat> {
    val pts = ringPoints(EventTableGeometry.CX, EventTableGeometry.CY, EventTableGeometry.SEAT_R, event.capacity)
    return pts.mapIndexed { k, pt ->
        when {
            k < event.goingBase -> Seat(SeatKind.PERSON, if (k == 0) event.hostId else Seed.seatPool[k], k == 0, pt)
            k == event.goingBase && iHaveJoined -> Seat(SeatKind.ME, null, false, pt)
            else -> Seat(SeatKind.EMPTY, null, false, pt)
        }
    }
}
