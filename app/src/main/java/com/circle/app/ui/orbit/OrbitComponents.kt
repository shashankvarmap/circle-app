package com.circle.app.ui.orbit

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circle.app.ui.theme.CircleTheme
import kotlin.math.min

/** Placement helpers for children of [OrbitMap]. Design coordinates are dp in a [width]×[height] box. */
class OrbitScope internal constructor(val scale: Float) {
    /** Puts a [size]-sized box so its center sits on [pt]. */
    fun Modifier.centeredAt(pt: Pt, size: Dp): Modifier =
        offset(x = (pt.x * scale).dp - size / 2, y = (pt.y * scale).dp - size / 2)

    /** Puts a box with its top-left at design coordinates ([left], [top]). */
    fun Modifier.placedAt(left: Float, top: Float): Modifier = offset(x = (left * scale).dp, y = (top * scale).dp)

    /** A design point, scaled to the drawing canvas. */
    fun DrawScope.px(p: Pt): Offset = Offset((p.x * scale).dp.toPx(), (p.y * scale).dp.toPx())
    fun DrawScope.px(v: Float): Float = (v * scale).dp.toPx()
}

/**
 * A fixed-size polar map, centered horizontally. On screens narrower than [width] the
 * geometry (rings, positions) shrinks to fit; node sizes and touch targets stay the same.
 */
@Composable
fun OrbitMap(width: Float, height: Float, modifier: Modifier = Modifier, content: @Composable OrbitScope.() -> Unit) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        val scale = min(1f, maxWidth.value / width)
        Box(Modifier.size((width * scale).dp, (height * scale).dp)) {
            OrbitScope(scale).content()
        }
    }
}

/** Ring stroke as in the prototype: 1.4dp, dashed rings use a "2 6" round-capped dash. */
fun DrawScope.orbitRing(center: Offset, radius: Float, color: Color, dashed: Boolean) {
    val w = 1.4.dp.toPx()
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(
            width = w,
            cap = if (dashed) StrokeCap.Round else StrokeCap.Butt,
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 6.dp.toPx())) else null
        )
    )
}

sealed interface NodeShape {
    data object Circle : NodeShape
    data class Rounded(val corner: Dp) : NodeShape
}

/** An outline drawn on the node itself, e.g. the dashed border of an empty seat or a join request. */
data class NodeBorder(val width: Dp, val color: Color, val dashed: Boolean = false)

private const val MIN_TOUCH_DP = 48f

/**
 * One person, event or group on an orbit.
 *
 * - The touch target is at least 48dp even though the visible node is 30–48dp.
 * - [halo] draws a background-colored ring around the node (separates it from ring lines).
 * - When [selected], an accent ring is drawn [selectedGap] outside the node, and the node
 *   scales by [selectedScale]. Selection is never shown by color alone.
 * - [description] is the full screen-reader label, e.g. "Priya Kapoor, inner circle, selected".
 */
@Composable
fun OrbitScope.OrbitNode(
    center: Pt,
    size: Dp,
    shape: NodeShape,
    fill: Color,
    description: String,
    selected: Boolean = false,
    /** Draws the accent double ring without marking the node selected (hosts, your own seat). */
    accentRing: Boolean = false,
    halo: Dp = 0.dp,
    selectedGap: Dp = 2.dp,
    selectedScale: Float = 1f,
    border: NodeBorder? = null,
    reduceMotion: Boolean = false,
    /** Visual opacity (e.g. 0.6 for empty seats). */
    alpha: Float = 1f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = CircleTheme.colors
    val hit = maxOf(size, MIN_TOUCH_DP.dp)
    val scale by animateFloatAsState(
        targetValue = if (selected) selectedScale else 1f,
        animationSpec = if (reduceMotion) snap() else tween(200),
        label = "nodeScale"
    )
    val ring = 2.dp
    val composeShape: Shape = when (shape) {
        NodeShape.Circle -> CircleShape
        is NodeShape.Rounded -> RoundedCornerShape(shape.corner)
    }

    Box(
        modifier = Modifier
            .centeredAt(center, hit)
            .size(hit)
            .then(
                if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
            )
            .semantics {
                contentDescription = description
                this.selected = selected
                if (onClick != null) role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                .drawBehind {
                    val ringed = selected || accentRing
                    val accentOut = if (ringed) (selectedGap + ring).toPx() else 0f
                    val haloOut = if (ringed) selectedGap.toPx() else halo.toPx()
                    if (accentOut > 0f) drawNodeShape(shape, colors.accent, accentOut)
                    if (haloOut > 0f) drawNodeShape(shape, colors.background, haloOut)
                }
                .clip(composeShape)
                .background(fill)
                .then(if (border != null) Modifier.drawWithContent { drawContent(); drawNodeBorder(shape, border) } else Modifier)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

/** Fills the node's shape grown by [outset] on every side (CSS box-shadow spread). */
private fun DrawScope.drawNodeShape(shape: NodeShape, color: Color, outset: Float) {
    when (shape) {
        NodeShape.Circle -> drawCircle(color, radius = size.minDimension / 2 + outset)
        is NodeShape.Rounded -> {
            val r = shape.corner.toPx() + outset
            drawRoundRect(
                color,
                topLeft = Offset(-outset, -outset),
                size = Size(size.width + 2 * outset, size.height + 2 * outset),
                cornerRadius = CornerRadius(r, r)
            )
        }
    }
}

private fun DrawScope.drawNodeBorder(shape: NodeShape, border: NodeBorder) {
    val w = border.width.toPx()
    val effect = if (border.dashed) PathEffect.dashPathEffect(floatArrayOf(w * 2.5f, w * 2f)) else null
    val stroke = Stroke(width = w, pathEffect = effect)
    when (shape) {
        NodeShape.Circle -> drawCircle(border.color, radius = size.minDimension / 2 - w / 2, style = stroke)
        is NodeShape.Rounded -> {
            val r = shape.corner.toPx()
            drawRoundRect(
                border.color, topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w),
                cornerRadius = CornerRadius(r, r), style = stroke
            )
        }
    }
}

/** Small uppercase label used on orbits ("INNER CIRCLE", "AACHEN", sector names). */
@Composable
fun OrbitLabel(text: String, fontSize: Float, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        color = CircleTheme.colors.muted,
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (fontSize * 0.16f).sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        maxLines = 1,
        softWrap = false
    )
}

/**
 * Map | List switch for every orbit screen. The list is the accessible equivalent of the map,
 * with the same actions; screens default to it when a screen reader is on.
 */
@Composable
fun MapListToggle(showMap: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = CircleTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(999.dp))
            .padding(2.dp)
    ) {
        listOf("Map" to true, "List" to false).forEach { (label, isMap) ->
            val active = showMap == isMap
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) colors.track else Color.Transparent)
                    .clickable(role = Role.Tab) { onChange(isMap) }
                    .semantics { selected = active; contentDescription = "$label view" }
                    .defaultMinSize(minHeight = 32.dp)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (active) colors.text else colors.muted,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
