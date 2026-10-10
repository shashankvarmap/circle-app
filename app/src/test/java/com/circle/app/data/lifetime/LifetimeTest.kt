package com.circle.app.data.lifetime

import com.circle.app.data.lifetime.Lifetime.Change
import com.circle.app.data.lifetime.Lifetime.Why
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** TIMED_GROUPS 3: every boundary of the state machine, extend / make permanent / end now in each state, zones and DST. */
class LifetimeTest {

    private val endsAt: Instant = Instant.parse("2026-11-01T00:00:00Z")
    private val term = Term.Temporary(endsAt)

    private fun at(i: Instant) = Lifetime(Clock.fixed(i, ZoneOffset.UTC))
    private fun stateAt(i: Instant) = at(i).state(term)
    private fun done(c: Change) = (c as Change.Done).term
    private fun refused(c: Change) = (c as Change.Refused).why

    private val ms = Duration.ofMillis(1)
    private val soon = endsAt.minus(Duration.ofDays(3))
    private val deleteAt = endsAt.plus(Duration.ofDays(7))

    // ---------------------------------------------------------------- boundaries

    @Test fun `permanent is always PERMANENT`() {
        assertEquals(LifeState.PERMANENT, at(Instant.EPOCH).state(Term.Permanent))
        assertEquals(LifeState.PERMANENT, at(Instant.parse("2099-01-01T00:00:00Z")).state(Term.Permanent))
        assertNull(at(endsAt).deleteAt(Term.Permanent))
    }

    @Test fun `ACTIVE up to just before 3 days before the end`() {
        assertEquals(LifeState.ACTIVE, stateAt(endsAt.minus(Duration.ofDays(30))))
        assertEquals(LifeState.ACTIVE, stateAt(soon.minus(ms)))
    }

    @Test fun `exactly 3 days before the end is ENDING_SOON`() = assertEquals(LifeState.ENDING_SOON, stateAt(soon))

    @Test fun `just before the end is still ENDING_SOON`() = assertEquals(LifeState.ENDING_SOON, stateAt(endsAt.minus(ms)))

    @Test fun `exactly at endsAt is WRAP_UP`() = assertEquals(LifeState.WRAP_UP, stateAt(endsAt))

    @Test fun `just before deleteAt is still WRAP_UP`() = assertEquals(LifeState.WRAP_UP, stateAt(deleteAt.minus(ms)))

    @Test fun `exactly at deleteAt is DELETED`() {
        assertEquals(LifeState.DELETED, stateAt(deleteAt))
        assertEquals(deleteAt, at(endsAt).deleteAt(term))
        assertEquals(LifeState.DELETED, stateAt(deleteAt.plus(Duration.ofDays(400))))
    }

    @Test fun `remaining is zero once ended`() {
        assertEquals(Duration.ofDays(2), at(endsAt.minus(Duration.ofDays(2))).remaining(term))
        assertEquals(Duration.ZERO, at(endsAt.plus(Duration.ofDays(1))).remaining(term))
        assertNull(at(endsAt).remaining(Term.Permanent))
    }

    // ---------------------------------------------------------------- creating

    @Test fun `create ends at the end of the chosen day in the creator's zone`() {
        val berlin = ZoneId.of("Europe/Berlin")
        val now = Instant.parse("2026-06-10T10:00:00Z") // 12:00 in Berlin
        val t = done(at(now).create(5, berlin)) as Term.Temporary
        // End of 15 June in Berlin (UTC+2) = 16 June 00:00 local = 15 June 22:00 UTC.
        assertEquals(Instant.parse("2026-06-15T22:00:00Z"), t.endsAt)
    }

    @Test fun `create is 1 to 90 days`() {
        val l = at(endsAt)
        assertEquals(Why.TOO_SHORT, refused(l.create(0, ZoneOffset.UTC)))
        assertEquals(Why.TOO_LONG, refused(l.create(91, ZoneOffset.UTC)))
        assertEquals(Term.Temporary(Instant.parse("2026-11-03T00:00:00Z")), done(l.create(1, ZoneOffset.UTC))) // end of 2 Nov
        assertEquals(Term.Temporary(Instant.parse("2027-01-31T00:00:00Z")), done(l.create(90, ZoneOffset.UTC)))
    }

    @Test fun `the same day in two zones ends at different instants`() {
        val l = at(Instant.parse("2026-03-01T12:00:00Z"))
        val day = LocalDate.parse("2026-03-10")
        val tokyo = l.endOfDay(day, ZoneId.of("Asia/Tokyo"))
        val newYork = l.endOfDay(day, ZoneId.of("America/New_York"))
        assertEquals(Instant.parse("2026-03-10T15:00:00Z"), tokyo)
        assertEquals(Instant.parse("2026-03-11T04:00:00Z"), newYork) // 11 March is after the US DST change (UTC-4)
    }

    @Test fun `the day before a spring-forward DST change has the right end`() {
        val berlin = ZoneId.of("Europe/Berlin")
        // Clocks go forward on 29 March 2026; the 28th ends at midnight CET (UTC+1).
        assertEquals(Instant.parse("2026-03-28T23:00:00Z"), at(Instant.EPOCH).endOfDay(LocalDate.parse("2026-03-28"), berlin))
        // The 29th ends at midnight CEST (UTC+2): that day was 23 hours long.
        assertEquals(Instant.parse("2026-03-29T22:00:00Z"), at(Instant.EPOCH).endOfDay(LocalDate.parse("2026-03-29"), berlin))
    }

    @Test fun `a fall-back DST day is 25 hours and the end is still local midnight`() {
        val berlin = ZoneId.of("Europe/Berlin")
        val l = at(Instant.EPOCH)
        val start = l.endOfDay(LocalDate.parse("2026-10-24"), berlin)
        val end = l.endOfDay(LocalDate.parse("2026-10-25"), berlin)
        assertEquals(Duration.ofHours(25), Duration.between(start, end))
    }

    @Test fun `a zone whose midnight is skipped by DST ends at the first valid instant`() {
        // Santiago skips 00:00-01:00 on its spring-forward day; atStartOfDay moves to 01:00.
        val santiago = ZoneId.of("America/Santiago")
        val end = at(Instant.EPOCH).endOfDay(LocalDate.parse("2026-09-05"), santiago)
        assertEquals(LocalDate.parse("2026-09-06"), end.atZone(santiago).toLocalDate())
    }

    @Test fun `ENDING_SOON is 72 hours, not 3 calendar days, across DST`() {
        val berlin = ZoneId.of("Europe/Berlin")
        val t = Term.Temporary(at(Instant.EPOCH).endOfDay(LocalDate.parse("2026-03-30"), berlin))
        assertEquals(LifeState.ACTIVE, at(t.endsAt.minus(Duration.ofHours(72)).minus(ms)).state(t))
        assertEquals(LifeState.ENDING_SOON, at(t.endsAt.minus(Duration.ofHours(72))).state(t))
    }

    // ---------------------------------------------------------------- extend in each state

    private val utc = ZoneOffset.UTC

    @Test fun `extend while ACTIVE moves the end forward`() {
        val l = at(endsAt.minus(Duration.ofDays(10)))
        assertEquals(Term.Temporary(Instant.parse("2026-11-11T00:00:00Z")), done(l.extend(term, LocalDate.parse("2026-11-10"), utc)))
    }

    @Test fun `extend while ENDING_SOON returns to ACTIVE`() {
        val l = at(endsAt.minus(Duration.ofDays(1)))
        val t = done(l.extend(term, LocalDate.parse("2026-11-20"), utc))
        assertEquals(LifeState.ACTIVE, l.state(t))
    }

    @Test fun `extend during WRAP_UP to a future day cancels the deletion`() {
        val l = at(endsAt.plus(Duration.ofDays(2)))
        assertEquals(LifeState.WRAP_UP, l.state(term))
        val near = done(l.extend(term, LocalDate.parse("2026-11-04"), utc))
        assertEquals(LifeState.ENDING_SOON, l.state(near))
        val far = done(l.extend(term, LocalDate.parse("2026-12-01"), utc))
        assertEquals(LifeState.ACTIVE, l.state(far))
    }

    @Test fun `extend during WRAP_UP to a day already past is refused`() {
        val l = at(endsAt.plus(Duration.ofDays(5)))
        assertEquals(Why.NOT_LATER, refused(l.extend(term, LocalDate.parse("2026-11-03"), utc)))
    }

    @Test fun `extend to the same or an earlier end is refused`() {
        val l = at(endsAt.minus(Duration.ofDays(10)))
        assertEquals(Why.NOT_LATER, refused(l.extend(term, LocalDate.parse("2026-10-31"), utc)))
        assertEquals(Why.NOT_LATER, refused(l.extend(term, LocalDate.parse("2026-10-20"), utc)))
    }

    @Test fun `extend is at most 90 days from today`() {
        val now = endsAt.minus(Duration.ofDays(10))
        val l = at(now)
        val today = l.today(utc)
        assertEquals(Term.Temporary(l.endOfDay(today.plusDays(90), utc)), done(l.extend(term, today.plusDays(90), utc)))
        assertEquals(Why.TOO_LONG, refused(l.extend(term, today.plusDays(91), utc)))
    }

    @Test fun `extend after DELETED or on a permanent term is refused`() {
        assertEquals(Why.DELETED, refused(at(deleteAt).extend(term, LocalDate.parse("2027-01-01"), utc)))
        assertEquals(Why.PERMANENT, refused(at(endsAt).extend(Term.Permanent, LocalDate.parse("2027-01-01"), utc)))
    }

    // ---------------------------------------------------------------- make permanent in each state

    @Test fun `make permanent works in every state before DELETED`() {
        listOf(endsAt.minus(Duration.ofDays(10)), endsAt.minus(Duration.ofDays(1)), endsAt, deleteAt.minus(ms)).forEach {
            assertEquals(Term.Permanent, done(at(it).makePermanent(term)))
        }
        assertEquals(Term.Permanent, done(at(endsAt).makePermanent(Term.Permanent)))
    }

    @Test fun `make permanent after DELETED is refused`() = assertEquals(Why.DELETED, refused(at(deleteAt).makePermanent(term)))

    // ---------------------------------------------------------------- end now in each state

    @Test fun `end now from ACTIVE or ENDING_SOON starts wrap-up at once`() {
        listOf(endsAt.minus(Duration.ofDays(10)), endsAt.minus(Duration.ofDays(1))).forEach { now ->
            val l = at(now)
            val t = done(l.endNow(term)) as Term.Temporary
            assertEquals(now, t.endsAt)
            assertEquals(LifeState.WRAP_UP, l.state(t))
            assertEquals(now.plus(Duration.ofDays(7)), l.deleteAt(t))
        }
    }

    @Test fun `end now in WRAP_UP, DELETED or on a permanent term is refused`() {
        assertEquals(Why.ALREADY_ENDED, refused(at(endsAt).endNow(term)))
        assertEquals(Why.DELETED, refused(at(deleteAt).endNow(term)))
        assertEquals(Why.PERMANENT, refused(at(endsAt).endNow(Term.Permanent)))
    }

    @Test fun `isOpen is true only before the end`() {
        assertEquals(setOf(LifeState.PERMANENT, LifeState.ACTIVE, LifeState.ENDING_SOON), LifeState.entries.filter { it.isOpen }.toSet())
    }
}
