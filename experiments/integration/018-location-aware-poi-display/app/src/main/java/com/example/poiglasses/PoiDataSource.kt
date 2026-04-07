package com.example.poiglasses

object PoiDataSource {
    fun getSamplePois(): List<PoiItem> = listOf(
        PoiItem("Starbucks", 35.6812, 139.7671, "Cafe"),
        PoiItem("Tokyo Station", 35.6812, 139.7671, "Station"),
        PoiItem("Imperial Palace", 35.6852, 139.7528, "Landmark"),
        PoiItem("Marunouchi Oazo", 35.6825, 139.7652, "Shopping"),
        PoiItem("KITTE", 35.6797, 139.7649, "Shopping"),
        PoiItem("Nihonbashi", 35.6839, 139.7745, "Area"),
    )
}
