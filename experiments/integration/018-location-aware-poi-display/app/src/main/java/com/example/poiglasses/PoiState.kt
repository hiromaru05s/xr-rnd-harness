package com.example.poiglasses

sealed class PoiState {
    data object Initializing : PoiState()
    data object WaitingForLocation : PoiState()
    data class Tracking(
        val currentPoi: PoiItem,
        val allPois: List<PoiItem>,
        val currentIndex: Int,
        val deviceHeading: Double,
        val latitude: Double,
        val longitude: Double,
    ) : PoiState()
    data class Error(val message: String) : PoiState()
}
