package com.example.geospatiallocation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.TitleChip

sealed class GeoState {
    data object Initializing : GeoState()
    data object CreatingSession : GeoState()
    data object SessionReady : GeoState()
    data class Tracking(
        val latitude: Double,
        val longitude: Double,
        val altitude: Double,
        val trackingState: String,
    ) : GeoState()
    data class NotTracking(val reason: String) : GeoState()
    data class Error(val message: String) : GeoState()
}

@Composable
fun GeospatialScreen(
    geoState: GeoState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black).padding(24.dp, 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val statusText = when (geoState) {
                is GeoState.Initializing -> "Initializing..."
                is GeoState.CreatingSession -> "Creating Session..."
                is GeoState.SessionReady -> "Session Ready"
                is GeoState.Tracking -> "Tracking"
                is GeoState.NotTracking -> "Not Tracking"
                is GeoState.Error -> "Error"
            }
            val statusIcon = when (geoState) {
                is GeoState.Tracking -> Icons.Default.Check
                is GeoState.Error -> Icons.Default.Warning
                else -> Icons.Default.Place
            }
            TitleChip(leadingIcon = { Icon(imageVector = statusIcon, contentDescription = null) }) {
                Text(statusText)
            }

            when (geoState) {
                is GeoState.Tracking -> {
                    Card(
                        title = { Text("VPS + GPS Location") },
                        subtitle = { Text("ARCore Geospatial API") },
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Lat: %.6f".format(geoState.latitude),
                                style = GlimmerTheme.typography.bodySmall,
                                color = GlimmerTheme.colors.positive,
                            )
                            Text(
                                text = "Lng: %.6f".format(geoState.longitude),
                                style = GlimmerTheme.typography.bodySmall,
                                color = GlimmerTheme.colors.positive,
                            )
                            Text(
                                text = "Alt: %.1fm".format(geoState.altitude),
                                style = GlimmerTheme.typography.bodySmall,
                                color = GlimmerTheme.colors.primary,
                            )
                            Text(
                                text = "State: " + geoState.trackingState,
                                style = GlimmerTheme.typography.caption,
                            )
                        }
                    }
                }
                is GeoState.NotTracking -> {
                    Card(title = { Text("Not Tracking") }) {
                        Text(
                            text = geoState.reason,
                            style = GlimmerTheme.typography.bodySmall,
                            color = GlimmerTheme.colors.negative,
                        )
                    }
                }
                is GeoState.Error -> {
                    Card(title = { Text("Geospatial Error") }) {
                        Text(
                            text = geoState.message,
                            style = GlimmerTheme.typography.bodySmall,
                            color = GlimmerTheme.colors.negative,
                        )
                    }
                }
                else -> {
                    Card(title = { Text("Setting Up Geospatial") }) {
                        Text(
                            text = "Please wait...",
                            style = GlimmerTheme.typography.bodySmall,
                            color = GlimmerTheme.colors.primary,
                        )
                    }
                }
            }
        }
    }
}
