package com.circle.app.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.circle.app.R
import com.circle.app.data.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Times are stored as Instants (UTC) and shown in the viewer's zone and locale.
 * Pure helpers return a [Relative] so they are testable; composables turn it into text.
 */
sealed interface Relative {
    data object Now : Relative
    data class Minutes(val n: Long) : Relative
    data class Hours(val n: Long) : Relative
    data object Yesterday : Relative
    data class Days(val n: Long) : Relative
    data class Date(val date: LocalDate) : Relative
}

fun relative(at: Instant, now: Instant = Clock.now(), zone: ZoneId = ZoneId.systemDefault()): Relative {
    val d = Duration.between(at, now).coerceAtLeast(Duration.ZERO)
    val today = now.atZone(zone).toLocalDate()
    val day = at.atZone(zone).toLocalDate()
    return when {
        d.toMinutes() < 1 -> Relative.Now
        d.toHours() < 1 -> Relative.Minutes(d.toMinutes())
        d.toHours() < 12 || day == today -> Relative.Hours(d.toHours())
        day == today.minusDays(1) -> Relative.Yesterday
        d.toDays() < 7 -> Relative.Days(d.toDays().coerceAtLeast(2))
        else -> Relative.Date(day)
    }
}

/** "now", "5m", "2h", "yesterday", "3d", or a short date. */
@Composable
fun relativeTime(at: Instant): String = when (val r = relative(at)) {
    Relative.Now -> stringResource(R.string.time_now)
    is Relative.Minutes -> stringResource(R.string.time_minutes, r.n)
    is Relative.Hours -> stringResource(R.string.time_hours, r.n)
    Relative.Yesterday -> stringResource(R.string.time_yesterday)
    is Relative.Days -> stringResource(R.string.time_days, r.n)
    is Relative.Date -> r.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}

/** Clock time in the viewer's locale, e.g. "21:40" or "9:40 PM". */
fun clockTime(at: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    at.atZone(zone).toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

/** Chat day separator: TODAY, YESTERDAY, or the date. */
@Composable
fun dayLabel(at: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
    val today = Clock.now().atZone(zone).toLocalDate()
    val day = at.atZone(zone).toLocalDate()
    return when (day) {
        today -> stringResource(R.string.time_today_caps)
        today.minusDays(1) -> stringResource(R.string.time_yesterday_caps)
        else -> day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)).uppercase()
    }
}

/** An event's start, e.g. "Fri 16 Oct, 18:00", in the viewer's zone. */
fun eventWhen(at: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
    val z = at.atZone(zone)
    return z.format(DateTimeFormatter.ofPattern("EEE d MMM")) + ", " + clockTime(at, zone)
}

/** Day number and short month for the date tiles, e.g. "16" / "OCT". */
fun eventDay(at: Instant, zone: ZoneId = ZoneId.systemDefault()): String = at.atZone(zone).dayOfMonth.toString()
fun eventMonth(at: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    at.atZone(zone).format(DateTimeFormatter.ofPattern("MMM")).uppercase()
fun eventDateSpoken(at: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    at.atZone(zone).format(DateTimeFormatter.ofPattern("d MMMM"))

/** "Thu 22 Oct": the day a temporary group ends or is deleted, in the viewer's zone. */
fun shortDate(at: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    at.atZone(zone).format(DateTimeFormatter.ofPattern("EEE d MMM"))
