package com.circle.app.data

/** Who fills an event's seats, in order, after the host. From the prototype's seat pool. */
internal val SeatPool = listOf("PK", "JL", "AO", "TN", "RS", "MD", "EK", "SY", "NB", "LF", "OH", "CZ")

enum class SeatKind { PERSON, ME, EMPTY, MORE }

/** One place on the event table ring. [moreCount] is set on the "+N more" chip only. */
data class Seat(val kind: SeatKind, val personId: String? = null, val isHost: Boolean = false, val moreCount: Int = 0)

/** The ring shows at most this many places; above it the last place becomes a "+N more" chip. */
const val MAX_RING_SEATS = 20

/**
 * Every place at the table, filled or empty: seat 0 is the host, then the people going, then
 * you (if you took a seat), then empty seats. Above [MAX_RING_SEATS] places the ring shows 19
 * seats and a "+N more" chip for the rest.
 */
fun eventSeats(e: EventInfo, iHaveJoined: Boolean): List<Seat> {
    val hostIsMe = e.hostId == ME
    val going = mutableListOf<Seat>()
    going += if (hostIsMe) Seat(SeatKind.ME, ME, isHost = true) else Seat(SeatKind.PERSON, e.hostId, isHost = true)
    SeatPool.filter { it != e.hostId }.take((e.goingBase - 1).coerceAtLeast(0)).forEach { going += Seat(SeatKind.PERSON, it) }
    if (iHaveJoined && !hostIsMe) going += Seat(SeatKind.ME, ME)
    val all = going.take(e.capacity) + List((e.capacity - going.size).coerceAtLeast(0)) { Seat(SeatKind.EMPTY) }
    if (all.size <= MAX_RING_SEATS) return all
    val shown = all.take(MAX_RING_SEATS - 1)
    return shown + Seat(SeatKind.MORE, moreCount = all.size - shown.size)
}
