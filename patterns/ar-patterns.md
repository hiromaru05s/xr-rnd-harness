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
