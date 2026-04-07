# 010: Geospatial Location

## Hypothesis
ARCore Geospatial API with VPS+GPS can obtain geographic coordinates (latitude, longitude, altitude) and display them on glasses UI.

## Technologies
- Skills: xr-runtime, glimmer-api
- Libraries: ARCore (arcore), XR Runtime, Geospatial, play-services-location, Glimmer

## Implementation
- Config(geospatial=GeospatialMode.VPS_AND_GPS, deviceTracking=DeviceTrackingMode.SPATIAL_LAST_KNOWN)
- Session.create() with SessionCreateResult sealed class handling
- Geospatial.getInstance(session) for geospatial instance
- ArDevice.getInstance(session).state for device pose flow
- createGeospatialPoseFromPose(pose) for geographic coordinates
- GeoState sealed class (6 states: Initializing, CreatingSession, SessionReady, Tracking, NotTracking, Error)
- Glimmer UI with TitleChip status + Card showing lat/lng/alt

## How to Run
1. Open in Android Studio Canary
2. Start Smartphone AVD and AI Glasses AVD
3. Target Smartphone AVD and run

## Findings
(After test/review)

## Extracted Patterns
(After PASS, reference patterns/ links)
