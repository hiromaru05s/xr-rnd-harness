package com.example.poiglasses

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {
    private const val EARTH_RADIUS_M = 6371000.0

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }

    fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLon = Math.toRadians(lon2 - lon1)
        val y = sin(dLon) * cos(Math.toRadians(lat2))
        val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
                sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    fun relativeDirection(poiBearing: Double, deviceHeading: Double): String {
        val diff = ((poiBearing - deviceHeading) + 360) % 360
        return when {
            diff < 22.5 || diff >= 337.5 -> "Ahead"
            diff < 67.5 -> "Front Right"
            diff < 112.5 -> "Right"
            diff < 157.5 -> "Behind Right"
            diff < 202.5 -> "Behind"
            diff < 247.5 -> "Behind Left"
            diff < 292.5 -> "Left"
            else -> "Front Left"
        }
    }

    fun formatDistance(meters: Double): String {
        return if (meters < 1000) {
            String.format("%.0fm", meters)
        } else {
            String.format("%.1fkm", meters / 1000)
        }
    }
}
