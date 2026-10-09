package com.circle.app.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circle.app.R
import com.circle.app.ui.a11y.rememberReduceMotion
import com.circle.app.ui.theme.CircleTheme
import com.circle.app.ui.theme.Display

/** The nine empty / offline / error kinds of ACCESS_AND_LAUNCH 3, with their copy. */
enum class EmptyKind(val title: Int, val body: Int) {
    HOME(R.string.empty_home_title, R.string.empty_home_body),
    MESSAGES(R.string.empty_messages_title, R.string.empty_messages_body),
    EXPLORE(R.string.empty_explore_title, R.string.empty_explore_body),
    ALERTS(R.string.empty_alerts_title, R.string.empty_alerts_body),
    YOUR_CIRCLE(R.string.empty_circle_title, R.string.empty_circle_body),
    GROUPS(R.string.empty_groups_title, R.string.empty_groups_body),
    PROFILE(R.string.empty_profile_title, R.string.empty_profile_body),
    OFFLINE(R.string.empty_offline_title, R.string.empty_offline_body),
    ERROR(R.string.empty_error_title, R.string.empty_error_body),
}

/**
 * One empty state: a 120dp ring (dashed outer ring, an accent dot at the top, a glyph in the
 * middle), a Fraunces title, a muted body and at most one accent action (plus an optional outlined one).
 */
@Composable
fun EmptyState(
    kind: EmptyKind,
    modifier: Modifier = Modifier,
    glyph: ImageVector? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    secondary: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    val colors = CircleTheme.colors
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2 - 2.dp.toPx()
                drawCircle(colors.border, r, style = Stroke(1.4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 6.dp.toPx()))))
                drawCircle(colors.border, r * 0.62f, style = Stroke(1.2.dp.toPx()))
                drawCircle(colors.accent, 5.dp.toPx(), center = Offset(center.x, center.y - r))
            }
            if (glyph != null) Icon(glyph, contentDescription = null, tint = colors.muted, modifier = Modifier.size(28.dp))
        }
        Text(
            stringResource(kind.title), fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, color = colors.text,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp).semantics { heading() }
        )
        Text(
            stringResource(kind.body), fontSize = 14.sp, lineHeight = 20.sp, color = colors.muted, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp).widthIn(max = 270.dp)
        )
        if (action != null && onAction != null) {
            Box(
                Modifier.padding(top = 18.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(999.dp)).background(colors.accent)
                    .clickable(role = Role.Button, onClick = onAction).padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) { Text(action, color = colors.onAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        }
        if (secondary != null && onSecondary != null) {
            Box(
                Modifier.padding(top = 10.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(999.dp)).border(1.5.dp, colors.border, RoundedCornerShape(999.dp))
                    .clickable(role = Role.Button, onClick = onSecondary).padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) { Text(secondary, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
        }
    }
}

/** "Couldn't load this": the error state, with Try again. */
@Composable
fun ErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) =
    EmptyState(EmptyKind.ERROR, modifier, action = stringResource(R.string.state_try_again), onAction = onRetry)

/** A slim banner on screens that still show cached content while offline. */
@Composable
fun OfflineBanner() {
    if (rememberIsOnline()) return
    val colors = CircleTheme.colors
    Row(
        Modifier.fillMaxWidth().background(colors.track).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = colors.muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.state_offline_banner), color = colors.text, fontSize = 12.5.sp)
    }
}

/** Whether the device has a validated internet connection, updated live. */
@Composable
fun rememberIsOnline(): Boolean {
    val context = LocalContext.current
    val cm = remember(context) { context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager }
    fun now(): Boolean = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: (cm == null)
    var online by remember { mutableStateOf(now()) }
    DisposableEffect(cm) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { online = true }
            override fun onLost(network: Network) { online = now() }
        }
        runCatching { cm?.registerDefaultNetworkCallback(callback) }
        onDispose { runCatching { cm?.unregisterNetworkCallback(callback) } }
    }
    return online
}

/** Calm loading placeholders for a list (the shimmer stops when animations are removed). */
@Composable
fun SkeletonList(rows: Int = 4, modifier: Modifier = Modifier) {
    val colors = CircleTheme.colors
    val reduceMotion = rememberReduceMotion()
    val pulse = if (reduceMotion) 0.6f else {
        val t = rememberInfiniteTransition(label = "skeleton")
        t.animateFloat(0.4f, 0.8f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse").value
    }
    Column(modifier.fillMaxWidth().padding(16.dp).alpha(pulse), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(rows) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(colors.track))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth(0.4f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(colors.track))
                    Box(Modifier.fillMaxWidth(0.85f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(colors.track))
                }
            }
        }
    }
}
