package com.example.poiglasses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.ButtonSize
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.TitleChip
import androidx.xr.glimmer.onIndirectPointerGesture

@Composable
fun PoiScreen(
    poiState: PoiState,
    onSwipeForward: () -> Unit,
    onSwipeBackward: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)
        .padding(horizontal = 24.dp, vertical = 16.dp)
        .onIndirectPointerGesture(onSwipeForward = onSwipeForward,
            onSwipeBackward = onSwipeBackward, onClick = onClick)
        .focusTarget(),
        contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()) {

            when (poiState) {
                is PoiState.Initializing -> {
                    TitleChip(leadingIcon = { Icon(Icons.Default.Explore, null) }) {
                        Text("Initializing...")
                    }
                    Card(title = { Text("POI Display") }) { Text("Starting location services...") }
                }
                is PoiState.WaitingForLocation -> {
                    TitleChip(leadingIcon = { Icon(Icons.Default.LocationOn, null) }) {
                        Text("Acquiring location...")
                    }
                    Card(title = { Text("Waiting") }) { Text("Searching for GPS signal...") }
                }
                is PoiState.Tracking -> {
                    val poi = poiState.currentPoi
                    val idx = poiState.currentIndex + 1
                    val total = poiState.allPois.size
                    TitleChip(leadingIcon = { Icon(Icons.Default.Navigation, null) }) {
                        Text(poi.relativeDirection + " - " + idx.toString() + "/" + total.toString())
                    }
                    Card(title = { Text(poi.name) },
                        subtitle = { Text(poi.category) },
                        action = {
                            Button(onClick = onClick, buttonSize = ButtonSize.Medium,
                                color = GlimmerTheme.colors.positive,
                                leadingIcon = { Icon(Icons.Default.Navigation, null) }
                            ) { Text("Read") }
                        }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(GeoUtils.formatDistance(poi.distanceMeters),
                                style = GlimmerTheme.typography.titleMedium,
                                color = GlimmerTheme.colors.positive)
                            Text(poi.relativeDirection)
                        }
                    }
                }
                is PoiState.Error -> {
                    TitleChip(leadingIcon = { Icon(Icons.Default.Explore, null) }) {
                        Text("Error")
                    }
                    Card(title = { Text("Error") }) { Text(poiState.message) }
                }
            }
        }
    }
}
