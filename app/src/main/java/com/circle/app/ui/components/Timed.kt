package com.circle.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circle.app.R
import com.circle.app.data.lifetime.LifeState
import com.circle.app.data.repo.LifetimeView
import com.circle.app.ui.a11y.rememberReduceMotion
import com.circle.app.ui.format.shortDate
import com.circle.app.ui.theme.CircleTheme
import com.circle.app.ui.theme.Display
import java.time.Instant

/*
 * Temporary groups and chats (TIMED_GROUPS 6): one chip, one ring, one dashed outline, and the
 * banners, bars and sheets every timed screen shares. Sizes from prototype-timed-groups-*.
 */

/** The last day of a term (or of wrap-up): the day just before [at], which is a midnight. */
fun lastDay(at: Instant): String = shortDate(at.minusNanos(1))

/** Ending soon and ended are marked with the accent; active stays quiet. */
val LifetimeView.isLoud: Boolean get() = state == LifeState.ENDING_SOON || state == LifeState.WRAP_UP

/** A ring that fills as time passes: the track, then the accent arc from the top, clockwise. */
@Composable
fun LifetimeRing(elapsed: Float, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    val colors = CircleTheme.colors
    val reduceMotion = rememberReduceMotion()
    val shown by animateFloatAsState(elapsed, label = "ring")
    val fraction = if (reduceMotion) elapsed else shown
    Canvas(modifier.size(size)) {
        // Prototype: r=8, stroke 2.4 in a 20-unit box.
        val unit = this.size.minDimension / 20f
        val stroke = 2.4f * unit
        val r = 8f * unit
        val topLeft = Offset(center.x - r, center.y - r)
        drawCircle(colors.track, radius = r, style = Stroke(stroke))
        if (fraction > 0f) {
            drawArc(
                colors.accent, -90f, 360f * fraction.coerceIn(0f, 1f), useCenter = false,
                topLeft = topLeft, size = Size(r * 2, r * 2), style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
    }
}

/** "12 days", "3 days" (accent when ending soon), "Ended" (ring full, accent). The label is always there. */
@Composable
fun TimerChip(lifetime: LifetimeView, modifier: Modifier = Modifier, label: String? = null) {
    val ended = lifetime.state == LifeState.WRAP_UP || lifetime.state == LifeState.DELETED
    val text = label ?: if (ended) stringResource(R.string.timer_ended)
        else pluralStringResource(R.plurals.timer_days, lifetime.daysLeft, lifetime.daysLeft)
    val spoken = if (ended) stringResource(R.string.timer_cd_ended)
        else pluralStringResource(R.plurals.timer_cd_left, lifetime.daysLeft, lifetime.daysLeft)
    ChipShell(loud = lifetime.isLoud, text = text, modifier = modifier.clearAndSetSemantics { contentDescription = spoken }) {
        LifetimeRing(if (ended) 1f else lifetime.elapsed)
    }
}

/** The chip's pill: 4/11/4/7 padding, 6dp gap, 11.5sp bold; accent border and tint when [loud]. */
@Composable
fun ChipShell(loud: Boolean, text: String, modifier: Modifier = Modifier, icon: @Composable () -> Unit) {
    val colors = CircleTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier.clip(shape)
            .background(if (loud) colors.accent.copy(alpha = 0.08f) else Color.Transparent)
            .border(1.dp, if (loud) colors.accent else colors.border, shape)
            .padding(start = 7.dp, end = 11.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        icon()
        Text(text, color = if (loud) colors.accent else colors.muted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** A dashed accent outline: temporary groups' tiles and Explore nodes (never colour alone: a tag goes with it). */
fun Modifier.dashedOutline(color: Color, corner: Dp, width: Dp = 2.dp): Modifier = drawBehind {
    val w = width.toPx()
    drawRoundRect(
        color, topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(corner.toPx() - w / 2),
        style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.5.dp.toPx())))
    )
}

/** A temporary group's square tile: dashed accent outline, accent letter, faint tint. */
@Composable
fun TemporaryGroupTile(initial: String, size: Int = 52, modifier: Modifier = Modifier) {
    val colors = CircleTheme.colors
    val corner = (size * 17 / 52f).dp
    Box(
        modifier.size(size.dp).clip(RoundedCornerShape(corner)).background(colors.accent.copy(alpha = 0.08f))
            .dashedOutline(colors.accent, corner),
        contentAlignment = Alignment.Center
    ) { Text(initial, color = colors.accent, fontWeight = FontWeight.ExtraBold, fontSize = (size * 18 / 52f).sp) }
}

/** The Save a copy pill (accent, download icon). */
@Composable
fun SaveCopyButton(onClick: () -> Unit) {
    val colors = CircleTheme.colors
    Row(
        Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(999.dp)).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.clip(RoundedCornerShape(999.dp)).background(colors.accent).padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.FileDownload, null, tint = colors.onAccent, modifier = Modifier.size(16.dp))
            Text(stringResource(R.string.timed_save_copy), color = colors.onAccent, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * The banner over a temporary chat: ENDING SOON (tinted) in the last 3 days, THIS CHAT HAS ENDED
 * (lock, accent border) in wrap-up. Nothing in other states.
 */
@Composable
fun LifetimeBanner(lifetime: LifetimeView, isGroupChat: Boolean, onSave: () -> Unit, onExtend: () -> Unit, modifier: Modifier = Modifier, onGroupScreen: Boolean = false) {
    val colors = CircleTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val ending = lifetime.state == LifeState.ENDING_SOON
    if (!ending && lifetime.state != LifeState.WRAP_UP) return
    Column(
        modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 10.dp).clip(shape)
            .background(if (ending) colors.accent.copy(alpha = 0.08f) else colors.surface)
            .border(if (ending) 1.dp else 1.5.dp, if (ending) colors.accent.copy(alpha = 0.3f) else colors.accent, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.semantics(mergeDescendants = true) { heading() }) {
            Icon(if (ending) Icons.Outlined.AccessTime else Icons.Outlined.Lock, null, tint = colors.accent, modifier = Modifier.size(15.dp))
            Text(
                stringResource(
                    when {
                        ending -> R.string.timed_ending_soon_kicker
                        onGroupScreen -> R.string.timed_group_ended_kicker
                        else -> R.string.timed_ended_kicker
                    }
                ),
                color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp
            )
        }
        Text(
            if (ending) pluralStringResource(R.plurals.timed_ends_in, lifetime.daysLeft, lifetime.daysLeft)
            else stringResource(R.string.timed_deleted_on, lastDay(lifetime.deleteAt)),
            fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp, color = colors.text,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            stringResource(if (ending) R.string.timed_ending_soon_body else R.string.timed_ended_body),
            color = colors.body, fontSize = 13.sp, lineHeight = 19.5.sp, modifier = Modifier.padding(top = 4.dp)
        )
        if (isGroupChat) {
            Text(stringResource(R.string.timed_together), color = colors.text, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
        }
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (lifetime.canSaveCopy) SaveCopyButton(onSave)
            if (!ending && lifetime.canChange) {
                Box(
                    Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(999.dp)).clickable(role = Role.Button, onClick = onExtend),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.timed_extend_or_keep), color = colors.text, fontSize = 13.5.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).border(1.5.dp, colors.border, RoundedCornerShape(999.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

/** Replaces the composer once a temporary chat has ended. */
@Composable
fun ReadOnlyBar(modifier: Modifier = Modifier, group: Boolean = false) {
    val colors = CircleTheme.colors
    Column(modifier.fillMaxWidth().background(colors.background)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp).semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Lock, null, tint = colors.muted, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (group) R.string.timed_group_read_only else R.string.timed_read_only), color = colors.muted, fontSize = 13.sp)
        }
    }
}

/** Host only: +7 / +14 / +30 days, make it permanent, end it now. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtendSheet(canEndNow: Boolean, onDismiss: () -> Unit, onExtend: (Int) -> Unit, onPermanent: () -> Unit, onEndNow: () -> Unit) {
    val colors = CircleTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), containerColor = colors.surface) {
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 28.dp)) {
            Text(
                stringResource(R.string.extend_title), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp,
                color = colors.text, modifier = Modifier.semantics { heading() }
            )
            Text(stringResource(R.string.extend_body), color = colors.muted, fontSize = 13.sp, lineHeight = 19.5.sp, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 14, 30).forEach { d ->
                    Box(
                        Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, colors.border, RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button) { onExtend(d) },
                        contentAlignment = Alignment.Center
                    ) { Text(pluralStringResource(R.plurals.extend_plus, d, d), color = colors.text, fontSize = 13.5.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Box(
                Modifier.padding(top = 10.dp).fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(999.dp))
                    .background(colors.accent).clickable(role = Role.Button, onClick = onPermanent),
                contentAlignment = Alignment.Center
            ) { Text(stringResource(R.string.extend_permanent), color = colors.onAccent, fontSize = 14.5.sp, fontWeight = FontWeight.Bold) }
            if (canEndNow) {
                Box(
                    Modifier.padding(top = 8.dp).fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(999.dp))
                        .clickable(role = Role.Button, onClick = onEndNow),
                    contentAlignment = Alignment.Center
                ) { Text(stringResource(R.string.extend_end_now), color = colors.alertDot, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

/** The ⋯ menu of a temporary group or chat: Save a copy, Extend or make permanent (host only), Leave. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifetimeMenuSheet(lifetime: LifetimeView, onDismiss: () -> Unit, onSave: () -> Unit, onExtend: () -> Unit, onLeave: (() -> Unit)?) {
    val colors = CircleTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), containerColor = colors.surface) {
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 28.dp)) {
            @Composable
            fun item(text: String, color: Color, icon: (@Composable () -> Unit)?, divider: Boolean, onClick: () -> Unit) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button) { onDismiss(); onClick() }.padding(horizontal = 4.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    icon?.invoke()
                    Text(text, color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
            }
            if (lifetime.canSaveCopy) {
                item(stringResource(R.string.timed_save_copy), colors.text, { Icon(Icons.Outlined.FileDownload, null, tint = colors.accent, modifier = Modifier.size(20.dp)) }, true, onSave)
            }
            if (lifetime.canChange) {
                item(stringResource(R.string.timed_menu_extend), colors.text, { Icon(Icons.Outlined.AccessTime, null, tint = colors.accent, modifier = Modifier.size(20.dp)) }, true, onExtend)
            }
            if (onLeave != null) item(stringResource(R.string.timed_menu_leave), colors.alertDot, null, false, onLeave)
        }
    }
}

/** What a stale link or an open screen shows once a temporary chat or group was deleted. */
@Composable
fun DeletedTimedState(onBack: () -> Unit, modifier: Modifier = Modifier, group: Boolean = false) {
    val colors = CircleTheme.colors
    Column(modifier.fillMaxSize().background(colors.background)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Canvas(Modifier.size(120.dp)) {
                drawCircle(colors.border, radius = 52.dp.toPx(), style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 7.dp.toPx()))))
                drawCircle(colors.accent.copy(alpha = 0.08f), radius = 31.dp.toPx())
                drawCircle(colors.accent.copy(alpha = 0.5f), radius = 31.dp.toPx(), style = Stroke(1.5.dp.toPx()))
            }
            Text(
                stringResource(if (group) R.string.timed_group_deleted_title else R.string.timed_deleted_title), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 25.sp, lineHeight = 31.sp,
                color = colors.text, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 18.dp).semantics { heading() }
            )
            Text(
                stringResource(if (group) R.string.timed_group_deleted_body else R.string.timed_deleted_body), color = colors.muted, fontSize = 14.sp, lineHeight = 21.7.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 8.dp)
            )
        }
        Box(
            Modifier.padding(start = 24.dp, end = 24.dp, bottom = 30.dp).fillMaxWidth().heightIn(min = 50.dp)
                .clip(RoundedCornerShape(999.dp)).border(1.5.dp, colors.border, RoundedCornerShape(999.dp))
                .clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center
        ) { Text(stringResource(R.string.timed_back_to_messages), color = colors.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
    }
}

/** The round icon used for timed alerts (clock, lock or check in an accent ring). */
@Composable
fun TimedAlertIcon(kind: com.circle.app.data.AlertKind) {
    val colors = CircleTheme.colors
    val icon = when (kind) {
        com.circle.app.data.AlertKind.TIMED_ENDED, com.circle.app.data.AlertKind.TIMED_DELETED_TOMORROW -> Icons.Outlined.Lock
        com.circle.app.data.AlertKind.TIMED_EXTENDED, com.circle.app.data.AlertKind.TIMED_MADE_PERMANENT -> Icons.Outlined.Check
        else -> Icons.Outlined.AccessTime
    }
    Box(Modifier.size(40.dp).clip(CircleShape).border(1.5.dp, colors.accent, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = colors.accent, modifier = Modifier.size(18.dp))
    }
}

