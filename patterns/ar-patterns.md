# arパターン集

> このファイルはAIがコンテキストとして読み込み、vibe codingの参照にする。
> コードスニペットはコピペで動くレベルの完全性を維持すること。

---

（まだパターンなし。実験がPASSするごとにOrchestratorが追記する）

## XR Session作成 + デバイストラッキング有効化

**いつ使う**: ARCore機能を使う前にXR Sessionを初期化するとき（全AR系実験の前提）
**前提**: `implementation("androidx.xr.runtime:runtime:1.0.0-alpha12")`, `implementation("androidx.xr.arcore:arcore:1.0.0-alpha11")`

```kotlin
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess

// Session作成（Activity内、lifecycleScope.launch内で呼ぶ）
val createResult = Session.create(activity)
when (createResult) {
    is SessionCreateSuccess -> {
        val session = createResult.session
        // デバイストラッキング有効化
        val config = Config(
            deviceTracking = DeviceTrackingMode.LAST_KNOWN,
        )
        session.configure(config)
        // ここからArDevice等を使用可能
    }
    else -> {
        // SessionCreateApkRequired, SessionCreateTimedOut,
        // SessionCreateUnknownError, SessionCreateUnsupportedDevice
        Log.e(TAG, "Session creation failed: ${createResult::class.simpleName}")
    }
}
```

**ハマりポイント**:
- `DeviceTrackingMode`は`androidx.xr.runtime.DeviceTrackingMode`（Config内部クラスではない）
- Session.create()はsuspend関数。lifecycleScope内で呼ぶ
- SessionCreateResultはsealed class。when式で網羅的にハンドリングする
- Sessionのクリーンアップはnullにするだけでよい（内部でlifecycleに紐付く）

**出典**: experiments/ar/006-device-pose-tracking

---

## ArDevice ポーズリアルタイム収集

**いつ使う**: AIグラスのデバイス姿勢(position+rotation)をリアルタイムで取得するとき
**前提**: Session.create() + Config(deviceTracking) が完了していること

```kotlin
import androidx.xr.arcore.ArDevice
import androidx.xr.runtime.math.Pose

// ArDeviceからポーズを継続的に収集
val arDevice = ArDevice.getInstance(session)
arDevice.state.collect { state ->
    val pose: Pose = state.devicePose
    val position = pose.translation // Vector3(x, y, z)
    val rotation = pose.rotation   // Quaternion(x, y, z, w)

    // Pose.Identityとの比較でトラッキング状態を推定
    val isTracking = pose != Pose.Identity
}
```

**ハマりポイント**:
- ArDevice.state はStateFlow。collectで最新値を受け取り続ける
- Pose.IdentityはデフォルトのPose（未トラッキング時に返される可能性がある）
- Vector3のx,y,zとQuaternionのx,y,z,wでデバイスの位置と向きがわかる
- エミュレータではポーズが固定値になる場合がある

**出典**: experiments/ar/006-device-pose-tracking

---

## Geospatial Session Initialization + Pose Tracking

**When to use**: When obtaining geographic coordinates (lat/lng/alt) via ARCore Geospatial API
**Prerequisites**: `implementation("androidx.xr.arcore:arcore:1.0.0-alpha11")`, `implementation("androidx.xr.runtime:runtime:1.0.0-alpha12")`, `implementation("com.google.android.gms:play-services-location:21.3.0")`

```kotlin
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.CreateGeospatialPoseFromPoseSuccess
import androidx.xr.arcore.Geospatial
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose

// 1. Create session and configure
when (val result = Session.create(activity)) {
    is SessionCreateSuccess -> {
        val session = result.session
        val config = Config(
            geospatial = GeospatialMode.VPS_AND_GPS,
            deviceTracking = DeviceTrackingMode.LAST_KNOWN,
        )
        session.configure(config)

        // 2. Get Geospatial and ArDevice instances
        val geospatial = Geospatial.getInstance(session)
        val arDevice = ArDevice.getInstance(session)

        // 3. Collect device pose and convert to geospatial
        arDevice.state.collect { deviceState ->
            val pose = deviceState.devicePose
            if (pose != Pose.Identity) {
                when (val geoResult = geospatial.createGeospatialPoseFromPose(pose)) {
                    is CreateGeospatialPoseFromPoseSuccess -> {
                        val geoPose = geoResult.pose
                        // geoPose.latitude, geoPose.longitude, geoPose.altitude
                    }
                    else -> { /* Not tracking */ }
                }
            }
        }
    }
    else -> { /* Session creation failed */ }
}
```

**Gotchas**:
- Use `DeviceTrackingMode.LAST_KNOWN` (NOT `SPATIAL_LAST_KNOWN`)
- Use top-level `GeospatialMode`/`DeviceTrackingMode` enums (NOT `Config.GeospatialMode`)
- Result types are top-level: `SessionCreateSuccess`, `CreateGeospatialPoseFromPoseSuccess`
- Check `pose != Pose.Identity` before attempting Geospatial conversion
- Success result field is `.pose` (type GeospatialPose)
- Requires INTERNET, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION permissions
- Google Cloud project must have ARCore API enabled

**Source**: experiments/ar/010-geospatial-location

---

## Geospatial + Device Heading POI Display

**When to use**: When showing nearby POIs with compass-relative direction on AI glasses
**Prerequisites**: `implementation("androidx.xr.arcore:arcore:1.0.0-alpha11")`, `implementation("androidx.xr.runtime:runtime:1.0.0-alpha12")`, `implementation("com.google.android.gms:play-services-location:21.3.0")`

```kotlin
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.CreateGeospatialPoseFromPoseSuccess
import androidx.xr.arcore.Geospatial
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// 1. Initialize session with Geospatial + DeviceTracking
when (val result = Session.create(activity)) {
    is SessionCreateSuccess -> {
        val session = result.session
        session.configure(Config(
            geospatial = GeospatialMode.VPS_AND_GPS,
            deviceTracking = DeviceTrackingMode.LAST_KNOWN))

        val geospatial = Geospatial.getInstance(session)
        val arDevice = ArDevice.getInstance(session)

        // 2. Collect device pose for heading + geospatial coords
        arDevice.state.collect { deviceState ->
            val pose = deviceState.devicePose
            if (pose != Pose.Identity) {
                val heading = poseToHeading(pose)
                when (val geo = geospatial.createGeospatialPoseFromPose(pose)) {
                    is CreateGeospatialPoseFromPoseSuccess -> {
                        val lat = geo.pose.latitude
                        val lng = geo.pose.longitude
                        // 3. Calculate distance and relative direction to POIs
                        val pois = allPois.map { poi ->
                            val dist = haversineDistance(lat, lng, poi.lat, poi.lng)
                            val bearing = bearingDegrees(lat, lng, poi.lat, poi.lng)
                            val dir = relativeDirection(bearing, heading)
                            poi.copy(distance = dist, direction = dir)
                        }.sortedBy { it.distance }.take(3)  // Top 3 nearest
                    }
                    else -> { /* not tracking */ }
                }
            }
        }
    }
    else -> { /* handle error */ }
}

// Quaternion to heading (yaw) conversion
fun poseToHeading(pose: Pose): Double {
    val q = pose.rotation
    val sinYaw = 2.0 * (q.w * q.z + q.x * q.y)
    val cosYaw = 1.0 - 2.0 * (q.y * q.y + q.z * q.z)
    return (Math.toDegrees(atan2(sinYaw.toDouble(), cosYaw.toDouble())) + 360) % 360
}

// 8-direction relative bearing from device heading
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

// Haversine distance (meters)
fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat/2)*sin(dLat/2) + cos(Math.toRadians(lat1))*cos(Math.toRadians(lat2))*sin(dLon/2)*sin(dLon/2)
    return R * 2 * atan2(sqrt(a), sqrt(1-a))
}
```

**Gotchas**:
- Use `DeviceTrackingMode.LAST_KNOWN` (not SPATIAL_LAST_KNOWN)
- Check `pose != Pose.Identity` before attempting geospatial conversion
- Quaternion yaw formula: sinYaw = 2*(w*z+x*y), cosYaw = 1-2*(y*y+z*z)
- Sort POIs by distance and take top 3 for FOV compliance
- TTS should read: "Name. Distance. Direction" with period separators
- 8 directions at 45-degree intervals centered on 0 (Ahead)
- Requires INTERNET, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION permissions

**Source**: experiments/integration/018-location-aware-poi-display

---
