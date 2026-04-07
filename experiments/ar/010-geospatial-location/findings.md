# 010: Geospatial Location - Findings

## Review Score: 10/10 (PASS)

## Key Discoveries
1. **GeospatialMode.VPS_AND_GPS** enables both Visual Positioning System and GPS fusion.
2. **DeviceTrackingMode.LAST_KNOWN** (not SPATIAL_LAST_KNOWN) is the correct enum value.
3. **CreateGeospatialPoseFromPoseSuccess.pose** returns GeospatialPose with lat/lng/alt.
4. **Pose.Identity check** is a practical way to detect tracking state before Geospatial conversion.
5. **ArDevice.state.collect** provides continuous pose updates as StateFlow.

## API Insights
- Top-level result classes: SessionCreateSuccess, CreateGeospatialPoseFromPoseSuccess
- Top-level mode enums: GeospatialMode, DeviceTrackingMode (NOT Config.nested)
- Config constructor takes top-level enums but they also exist as Config.nested types
- GeospatialPose has: latitude, longitude, altitude, eastUpSouthQuaternion
- play-services-location dependency required for GPS fusion

## Patterns Extracted
- Geospatial session initialization pattern
- Geospatial pose tracking flow
