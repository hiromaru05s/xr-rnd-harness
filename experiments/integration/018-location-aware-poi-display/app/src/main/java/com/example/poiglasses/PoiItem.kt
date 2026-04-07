package com.example.poiglasses

data class PoiItem(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
    val distanceMeters: Double = 0.0,
    val bearingDegrees: Double = 0.0,
    val relativeDirection: String = "",
)
