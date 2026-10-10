package com.circle.app.data.lifetime

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** How long a group or chat lasts (TIMED_GROUPS 3). A group and its chat share one term. */
sealed interface Term {
    data object Permanent : Term
    /** Ends at [endsAt] (UTC): the end of the chosen calendar day in the creator's time zone. */
    data class Temporary(val endsAt: Instant) : Term
}

/** Where a term stands at a moment. [PERMANENT] never changes on its own. */
enum class LifeState {
    PERMANENT,
    /** now < endsAt - 3 days. */
    ACTIVE,
    /** The last 3 days: endsAt - 3 days <= now < endsAt. */
    ENDING_SOON,
    /** Read-only for 7 days: endsAt <= now < endsAt + 7 days. Members can save a copy. */
    WRAP_UP,
    /** now >= endsAt + 7 days: gone from the server for everyone. */
    DELETED;

    /** Posting, commenting, sending, joining and inviting are possible. */
    val isOpen: Boolean get() = this == PERMANENT || this == ACTIVE || this == ENDING_SOON
}

/**
 * The timed-group state machine. Pure Kotlin with an injectable [Clock]; no Android, no UI.
 * The server is the only authority on time: this class computes what the server would and the app
 * only displays it. Who may change a term (the host) is [com.circle.app.data.access.AccessPolicy]'s job.
 */
class Lifetime(private val clock: Clock) {

    sealed interface Change {
        data class Done(val term: Term) : Change
        data class Refused(val why: Why) : Change
    }

    enum class Why {
        /** Past deleteAt: nothing can be changed any more. */
        DELETED,
        /** Permanent terms can't be ended, extended or made temporary in v1. */
        PERMANENT,
        /** Extending must move the end forward. */
        NOT_LATER,
        /** One stretch is at most [MAX_DAYS] days from today. */
        TOO_LONG,
        /** Less than [MIN_DAYS] day ahead. */
        TOO_SHORT,
        /** End now on something already in wrap-up. */
        ALREADY_ENDED,
    }

    fun now(): Instant = clock.instant()

    /** The end of [lastDay] in [zone], as an instant: the start of the next day there (DST-safe). */
    fun endOfDay(lastDay: LocalDate, zone: ZoneId): Instant = lastDay.plusDays(1).atStartOfDay(zone).toInstant()

    /** Today in [zone] by this clock. */
    fun today(zone: ZoneId): LocalDate = now().atZone(zone).toLocalDate()

    /** A new temporary term that ends at the end of the day [days] days from today in the creator's [zone]. */
    fun create(days: Int, zone: ZoneId): Change = when {
        days < MIN_DAYS -> Change.Refused(Why.TOO_SHORT)
        days > MAX_DAYS -> Change.Refused(Why.TOO_LONG)
        else -> Change.Done(Term.Temporary(endOfDay(today(zone).plusDays(days.toLong()), zone)))
    }

    fun state(term: Term): LifeState = when (term) {
        Term.Permanent -> LifeState.PERMANENT
        is Term.Temporary -> {
            val now = now()
            when {
                now >= deleteAt(term.endsAt) -> LifeState.DELETED
                now >= term.endsAt -> LifeState.WRAP_UP
                now >= term.endsAt.minus(ENDING_SOON) -> LifeState.ENDING_SOON
                else -> LifeState.ACTIVE
            }
        }
    }

    /** When the server deletes it, or null for a permanent term. */
    fun deleteAt(term: Term): Instant? = (term as? Term.Temporary)?.let { deleteAt(it.endsAt) }

    /** Time left until the end (zero once ended), or null for a permanent term. */
    fun remaining(term: Term): Duration? = (term as? Term.Temporary)?.let {
        Duration.between(now(), it.endsAt).let { d -> if (d.isNegative) Duration.ZERO else d }
    }

    /**
     * Moves the end to the end of [lastDay] in [zone]. Works in ACTIVE, ENDING_SOON and WRAP_UP; from
     * WRAP_UP a future end brings it back to ACTIVE or ENDING_SOON and cancels the deletion.
     */
    fun extend(term: Term, lastDay: LocalDate, zone: ZoneId): Change {
        if (term !is Term.Temporary) return Change.Refused(Why.PERMANENT)
        if (state(term) == LifeState.DELETED) return Change.Refused(Why.DELETED)
        val newEnd = endOfDay(lastDay, zone)
        if (newEnd <= term.endsAt || newEnd <= now()) return Change.Refused(Why.NOT_LATER)
        if (lastDay > today(zone).plusDays(MAX_DAYS.toLong())) return Change.Refused(Why.TOO_LONG)
        return Change.Done(Term.Temporary(newEnd))
    }

    /** Removes the end completely, in any state before DELETED. */
    fun makePermanent(term: Term): Change = when {
        term is Term.Permanent -> Change.Done(term)
        state(term) == LifeState.DELETED -> Change.Refused(Why.DELETED)
        else -> Change.Done(Term.Permanent)
    }

    /** Ends it now: endsAt = now, wrap-up starts. Only from ACTIVE or ENDING_SOON. */
    fun endNow(term: Term): Change {
        if (term !is Term.Temporary) return Change.Refused(Why.PERMANENT)
        return when (state(term)) {
            LifeState.ACTIVE, LifeState.ENDING_SOON -> Change.Done(Term.Temporary(now()))
            LifeState.WRAP_UP -> Change.Refused(Why.ALREADY_ENDED)
            else -> Change.Refused(Why.DELETED)
        }
    }

    companion object {
        const val MIN_DAYS = 1
        const val MAX_DAYS = 90
        val ENDING_SOON: Duration = Duration.ofDays(3)
        val WRAP_UP: Duration = Duration.ofDays(7)

        fun deleteAt(endsAt: Instant): Instant = endsAt.plus(WRAP_UP)
    }
}
