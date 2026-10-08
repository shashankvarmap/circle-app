package com.circle.app.ui.orbit

import org.junit.Assert.assertEquals
import org.junit.Test

class OrbitTest {
    private val eps = 0.01f

    private fun assertPt(expected: Pt, actual: Pt) {
        assertEquals(expected.x, actual.x, eps)
        assertEquals(expected.y, actual.y, eps)
    }

    @Test
    fun compassPointUsesZeroUpClockwise() {
        assertPt(Pt(100f, 50f), compassPoint(100f, 100f, 50f, 0f))   // up
        assertPt(Pt(150f, 100f), compassPoint(100f, 100f, 50f, 90f)) // right
        assertPt(Pt(100f, 150f), compassPoint(100f, 100f, 50f, 180f)) // down
        assertPt(Pt(50f, 100f), compassPoint(100f, 100f, 50f, 270f)) // left
    }

    @Test
    fun compassPointMatchesThePrototypeSinpos() {
        // Prototype Explore: Trail Run at phi 40 on the inner ring (195, 170, r 68).
        val p = compassPoint(195f, 170f, 68f, 40f)
        assertPt(Pt(195f + 68f * 0.6428f, 170f - 68f * 0.7660f), p)
    }

    @Test
    fun ringPointsStartAtTheTopAndSpaceEvenly() {
        val pts = ringPoints(0f, 0f, 10f, 4)
        assertEquals(4, pts.size)
        assertPt(Pt(0f, -10f), pts[0])
        assertPt(Pt(10f, 0f), pts[1])
        assertPt(Pt(0f, 10f), pts[2])
        assertPt(Pt(-10f, 0f), pts[3])
    }

    @Test
    fun ringPointsHonourTheOffset() {
        val pts = ringPoints(0f, 0f, 10f, 2, offsetDeg = 90f)
        assertPt(Pt(10f, 0f), pts[0])
        assertPt(Pt(-10f, 0f), pts[1])
    }

    @Test
    fun ringPointsOfZeroIsEmpty() {
        assertEquals(0, ringPoints(0f, 0f, 10f, 0).size)
    }

    @Test
    fun outerOffsetIsHalfASlot() {
        assertEquals(30f, outerOffsetDeg(6), eps)
        assertEquals(25.714f, outerOffsetDeg(7), eps)
        assertEquals(180f, outerOffsetDeg(1), eps)
        assertEquals(180f, outerOffsetDeg(0), eps) // no division by zero
    }

    @Test
    fun sectorsFollowTheSpokes() {
        assertEquals(Sector.OUTDOORS, Sector.of(40f))
        assertEquals(Sector.SOCIAL, Sector.of(125f))
        assertEquals(Sector.MUSIC, Sector.of(215f))
        assertEquals(Sector.MAKING, Sector.of(305f))
        assertEquals(Sector.MAKING, Sector.of(-10f))
        assertEquals(Sector.OUTDOORS, Sector.of(360f))
    }

    @Test
    fun seatSizeShrinksAboveTwelve() {
        assertEquals(38f, EventTableGeometry.seatSize(12), eps)
        assertEquals(30f, EventTableGeometry.seatSize(13), eps)
    }
}
