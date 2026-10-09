package com.circle.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventSeatsTest {
    private fun event(capacity: Int, going: Int, host: String = "PK") =
        EventInfo("e", "E", "SOCIAL", 0f, java.time.Instant.EPOCH, "", com.circle.app.data.access.EventVisibility.FRIENDS, host, "", capacity = capacity, goingBase = going, joined = false)

    @Test
    fun oneSeatPerPlaceHostFirstThenPeopleThenEmpty() {
        val seats = eventSeats(event(12, 6), iHaveJoined = false)
        assertEquals(12, seats.size)
        assertEquals(Seat(SeatKind.PERSON, "PK", isHost = true), seats[0])
        assertEquals(6, seats.count { it.kind == SeatKind.PERSON })
        assertEquals(6, seats.count { it.kind == SeatKind.EMPTY })
        assertTrue(seats.drop(6).all { it.kind == SeatKind.EMPTY })
    }

    @Test
    fun joiningTakesTheNextSeat() {
        val seats = eventSeats(event(10, 4), iHaveJoined = true)
        assertEquals(SeatKind.ME, seats[4].kind)
        assertEquals(5, seats.count { it.kind != SeatKind.EMPTY })
    }

    @Test
    fun aFullTableHasNoEmptySeats() {
        val seats = eventSeats(event(3, 3), iHaveJoined = false)
        assertEquals(0, seats.count { it.kind == SeatKind.EMPTY })
    }

    @Test
    fun anEmptyTableStillHasTheHost() {
        val seats = eventSeats(event(5, 1), iHaveJoined = false)
        assertEquals(1, seats.count { it.kind == SeatKind.PERSON })
        assertEquals(4, seats.count { it.kind == SeatKind.EMPTY })
    }

    @Test
    fun youHostingFillsSeatZero() {
        val seats = eventSeats(event(4, 1, host = ME), iHaveJoined = false)
        assertEquals(Seat(SeatKind.ME, ME, isHost = true), seats[0])
    }

    @Test
    fun overTwelveSeatsStillOnePerPlace() {
        assertEquals(15, eventSeats(event(15, 5), false).size)
    }

    @Test
    fun theRingCapsAtTwentyWithAMoreChip() {
        val seats = eventSeats(event(30, 3), iHaveJoined = false)
        assertEquals(MAX_RING_SEATS, seats.size)
        assertEquals(SeatKind.MORE, seats.last().kind)
        assertEquals(11, seats.last().moreCount)
    }
}
