package com.circle.app.ui.orbit

import kotlin.math.cos
import kotlin.math.sin

/**
 * Polar layout used by Explore, Your circle, Group people and Event table.
 * UNVERIFIED STARTER: pure Kotlin, no UI. Add unit tests (see BUILD_BRIEF.md section 9).
 *
 * Convention: "compass angle" phi in degrees. 0 = straight up, positive = clockwise.
 * All coordinates are dp within the orbit's own container, origin at its top-left.
 */
data class Pt(val x: Float, val y: Float)

fun compassPoint(cx: Float, cy: Float, r: Float, phiDeg: Float): Pt {
    val rad = Math.toRadians(phiDeg.toDouble())
    return Pt(cx + r * sin(rad).toFloat(), cy - r * cos(rad).toFloat())
}

/** n points evenly spaced on a ring, starting at the top, shifted by offsetDeg. */
fun ringPoints(cx: Float, cy: Float, r: Float, n: Int, offsetDeg: Float = 0f): List<Pt> =
    List(n) { k -> compassPoint(cx, cy, r, offsetDeg + k * 360f / n) }

/** Outer-ring friends sit half a slot off, so they sit between inner-ring people. */
fun outerOffsetDeg(n: Int): Float = 360f / (2 * maxOf(1, n))

/** Geometry constants, in dp. Source of truth: reference/prototype-logic.js. */
object ExploreGeometry {
    const val WIDTH = 390f; const val HEIGHT = 340f
    const val CX = 195f; const val CY = 170f
    const val EVENT_RING_R = 68f; const val GROUP_RING_R = 124f
    const val EVENT_NODE = 40f; const val GROUP_NODE = 36f
    const val SELECTED_SCALE = 1.18f
    /** Sector labels sit just outside the outer ring, on the diagonals. */
    val SECTOR_LABEL_DEG = listOf("OUTDOORS" to 45f, "SOCIAL" to 135f, "MUSIC" to 225f, "MAKING" to 315f)
    const val LABEL_R = 154f
    /** Faint spokes between sectors, from SPOKE_FROM to SPOKE_TO. */
    val SPOKE_DEG = listOf(0f, 90f, 180f, 270f)
    const val SPOKE_FROM = 40f; const val SPOKE_TO = 146f
}

object YourCircleGeometry {
    const val CX = 195f; const val CY = 170f
    const val INNER_R = 64f; const val OUTER_R = 122f
    const val INNER_NODE = 38f; const val OUTER_NODE = 36f
    const val ME_NODE = 48f
}

object GroupPeopleGeometry {
    const val CX = 195f; const val CY = 156f
    const val HOST_R = 58f; const val MEMBER_R = 118f
    const val CENTER_TILE = 44f
    const val MEMBER_SLOTS = 12
    /** Members start 15 degrees round so nobody sits exactly at the top. */
    const val MEMBER_OFFSET_DEG = 15f
}

object EventTableGeometry {
    const val CX = 195f; const val CY = 150f
    const val SEAT_R = 108f; const val TABLE_R = 58f
    /** 38dp seats up to 12 places, 30dp above that. */
    fun seatSize(capacity: Int): Float = if (capacity <= 12) 38f else 30f
}
