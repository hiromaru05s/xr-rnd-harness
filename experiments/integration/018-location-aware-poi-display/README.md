# 018: Location-Aware POI Display

## Hypothesis
Geospatial API location + device pose heading can be combined to show nearby POI with compass direction on glasses UI.

## Technologies Used
- Skills: glimmer-api, projected-api, glasses-hardware, glasses-arch, xr-runtime
- Libraries: ARCore (Geospatial), XR Runtime, Glimmer, TTS, Projected API

## Implementation
- Geospatial API for lat/lng, ArDevice pose for heading
- GeoUtils for distance/bearing calculation (Haversine formula)
- Dummy POI data sorted by distance
- Touchpad swipe to browse POIs, TTS reads name+distance
- Glimmer Card with direction arrow + distance + name

## Execution
1. Open in Android Studio Canary
2. Launch on phone AVD with glasses AVD

## Findings
(After test/review)

## Extracted Patterns
(After PASS)
