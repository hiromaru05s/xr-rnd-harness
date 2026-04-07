package com.example.poiglasses

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoUtilsTest {

    @Test
    fun testDistanceSamePoint() {
        val d = GeoUtils.distanceMeters(35.6812, 139.7671, 35.6812, 139.7671)
        assertEquals(0.0, d, 0.01)
    }

    @Test
    fun testDistancePositive() {
        val d = GeoUtils.distanceMeters(35.6812, 139.7671, 35.6852, 139.7528)
        assertTrue(d > 0)
        assertTrue(d < 5000)
    }

    @Test
    fun testBearingRange() {
        val b = GeoUtils.bearingDegrees(35.6812, 139.7671, 35.6852, 139.7528)
        assertTrue(b >= 0)
        assertTrue(b < 360)
    }

    @Test
    fun testRelativeDirectionAhead() {
        assertEquals("Ahead", GeoUtils.relativeDirection(0.0, 0.0))
        assertEquals("Ahead", GeoUtils.relativeDirection(350.0, 350.0))
    }

    @Test
    fun testRelativeDirectionRight() {
        assertEquals("Right", GeoUtils.relativeDirection(90.0, 0.0))
    }

    @Test
    fun testRelativeDirectionBehind() {
        assertEquals("Behind", GeoUtils.relativeDirection(180.0, 0.0))
    }

    @Test
    fun testRelativeDirectionLeft() {
        assertEquals("Left", GeoUtils.relativeDirection(270.0, 0.0))
    }

    @Test
    fun testFormatDistanceMeters() {
        assertEquals("500m", GeoUtils.formatDistance(500.0))
    }

    @Test
    fun testFormatDistanceKilometers() {
        assertEquals("1.5km", GeoUtils.formatDistance(1500.0))
    }

    @Test
    fun testRelativeDirectionWithHeading() {
        assertEquals("Ahead", GeoUtils.relativeDirection(90.0, 90.0))
        assertEquals("Right", GeoUtils.relativeDirection(180.0, 90.0))
    }
}
