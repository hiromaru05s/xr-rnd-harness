---
name: xr-runtime
description: XR RuntimeとARCore APIリファレンス。デバイスポーズトラッキング、Geospatial API（VPS+GPS）、ARセッション管理、空間認識に関するタスクでは必ずこのスキルを使う。位置情報やAR機能を実装する場合はこれを参照。
---

# XR Runtime + ARCore for Jetpack XR APIリファレンス

- `androidx.xr.runtime:runtime:1.0.0-alpha12`
- `androidx.xr.arcore:arcore:1.0.0-alpha11`

#### 3. XR Runtime (`androidx.xr.runtime:runtime:1.0.0-alpha12`)

Jetpack XRスイート全体の基盤。ARCore、Projected等が依存する。

##### Session管理
```kotlin
// セッション作成（複数オーバーロード）
Session.create(activity: Activity): SessionCreateResult
Session.create(activity: Activity, coroutineContext: CoroutineContext): SessionCreateResult
Session.create(context: Context, lifecycleOwner: LifecycleOwner): SessionCreateResult

// 結果型（sealed class、nonexhaustive）
SessionCreateSuccess(session: Session)
SessionCreateApkRequired(requiredApk: String)
SessionCreateTimedOut()
SessionCreateUnknownError(errorMessage: String)
SessionCreateUnsupportedDevice()

// 設定
session.configure(config: Config): SessionConfigureResult
session.state: StateFlow<CoreState>  // タイミング・状態のFlow
```

##### Config（パーセプション設定）
```kotlin
Config(
    planeTracking: PlaneTrackingMode = DISABLED,
    deviceTracking: DeviceTrackingMode = DISABLED,
    anchorPersistence: AnchorPersistenceMode = DISABLED,
    geospatial: GeospatialMode = DISABLED,
    // handTracking, depthEstimation, faceTracking, augmentedObjectCategories もあるがAIグラスでは使わない
)
```

各モードの定義値:
- `PlaneTrackingMode`: DISABLED, HORIZONTAL_AND_VERTICAL
- `DeviceTrackingMode`: DISABLED, SPATIAL_LAST_KNOWN
- `AnchorPersistenceMode`: DISABLED, LOCAL
- `GeospatialMode`: DISABLED, VPS_AND_GPS

##### TrackingState
`TRACKING`, `TRACKING_DEGRADED`, `PAUSED`, `STOPPED`

##### DisplayBlendMode
`ADDITIVE`（加算=透過ディスプレイ）, `ALPHA_BLEND`, `NO_DISPLAY`

##### XrDevice
```kotlin
XrDevice.getCurrentDevice(context: Context): XrDevice  // @ExperimentalXrDeviceLifecycleApi
xrDevice.getPreferredDisplayBlendMode(): DisplayBlendMode
xrDevice.getLifecycle(): Lifecycle
```

##### 数学型（`androidx.xr.runtime.math`）

**AIグラスで主に使う型:**
- `Vector3(x, y, z)` — 3D位置。`+`, `-`, `*`, `/`, `dot()`, `cross()`演算子。`Zero`, `Forward`, `Up`等の定数
- `Quaternion(x, y, z, w)` — 回転。`Identity`定数。`fromAxisAngle()`, `fromEulerAngles()`, `slerp()`, `lerp()`
- `Pose(translation: Vector3, rotation: Quaternion)` — 位置+回転。`Identity`定数。`compose()`, `transformPoint()`, `translate()`, `rotate()`, `getInverse()`。方向ベクトル: `forward`, `backward`, `left`, `right`, `up`, `down`
- `GeospatialPose(latitude, longitude, altitude, eastUpSouthQuaternion)` — Geospatial API用

**その他（必要に応じて使用）:** Vector2, Vector4, Matrix3, Matrix4, Ray, BoundingBox, FieldOfView, FloatSize2d/3d, IntSize2d, MathHelper

バリアント: `runtime`（コア）のみ使用。Guava/RxJava3バリアントは不要。

---

#### 4. ARCore for Jetpack XR (`androidx.xr.arcore:arcore:1.0.0-alpha11`)

**AIグラスでは`arcore`モジュール（高レベルAPI）を使用する。** `arcore-projected`は内部実装（公開APIなし）。

##### AIグラスで使う主要API

**ArDevice** — デバイス姿勢（**AIグラスの最重要API**）
```kotlin
ArDevice.getInstance(): ArDevice
// State: devicePose, trackingState
```

**Geospatial** — ジオスパーシャル（VPS + GPS）
```kotlin
Geospatial.getInstance(): Geospatial
Geospatial.queryVpsAvailability(lat, lng): VpsAvailabilityResult  // suspend
Geospatial.createGeospatialPoseFromPose(pose): CreateGeospatialPoseFromPoseResult  // suspend
Geospatial.createPoseFromGeospatialPose(geoPose): CreatePoseFromGeospatialPoseResult  // suspend
// State: pose(GeospatialPose), horizontalAccuracy, verticalAccuracy, orientationYawAccuracy, trackingState
```

**Plane** — 平面検出（Anchorable）
```kotlin
Plane.subscribe(): StateFlow<List<Plane.State>>
// Plane.Type: HORIZONTAL_UPWARD_FACING, HORIZONTAL_DOWNWARD_FACING, VERTICAL
// Plane.Label: FLOOR, WALL, CEILING, TABLE, SEAT, DOOR, WINDOW, OTHER
// createAnchor(pose): AnchorResult
```

**Anchor** — 空間アンカー
```kotlin
Anchor.create(): AnchorResult
Anchor.load(): AnchorResult    // 永続化アンカーの読み込み
anchor.persist(): UUID  // suspend
anchor.detach()
```

##### 結果型（sealed class）
- `AnchorResult`: `AnchorCreateSuccess`, `AnchorCreateTrackingUnavailable`, `AnchorCreateResourcesExhausted`
- `VpsAvailabilityResult`: `Available`, `Unavailable`, `NotAuthorized`, `NetworkError`, `ErrorInternal`, `ResourceExhausted`

##### ヘッドセット向けAPI（AIグラスでは使わない）
以下はARCoreに含まれるがAIグラスのセンサーでは動作しない/不要:
Hand, Face, Eye, DepthMap, AugmentedObject, HitResult, Interaction, DragGesture, TiltGesture

---
## デバイスポーズトラッキング（ARCore）

### セッション構成
```kotlin
val newConfig = session.config.copy(
    deviceTracking = DeviceTrackingMode.LAST_KNOWN
)
when (val configResult = session.configure(newConfig)) {
    is SessionConfigureSuccess -> { /* トラッキング有効 */ }
    else -> { /* エラー処理 */ }
}
```

### ポーズ取得
```kotlin
val arDevice = ArDevice.getInstance(session)

// 1. 現在のポーズを1回取得
val devicePose = arDevice.state.value.devicePose

// 2. 継続的にポーズを受信
arDevice.state.collect { state ->
    val translation = state.devicePose.translation  // Vector3(x, y, z)
    val rotation = state.devicePose.rotation        // Quaternion(x, y, z, w)
}
```

---

## Geospatial API（VPS + GPS）

Google VPS + GPSでグローバル規模のAR位置情報を提供。

### 追加依存関係
```kotlin
implementation("com.google.android.gms:play-services-location:21.3.0")
```

### 必要な権限
`INTERNET`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`

### セッション構成
```kotlin
val newConfig = Config(
    geospatial = GeospatialMode.VPS_AND_GPS,
    deviceTracking = DeviceTrackingMode.LAST_KNOWN
)
session.configure(newConfig)
```

### VPS可用性チェック
```kotlin
val geospatial = Geospatial.getInstance(session)
when (val result = geospatial.checkVpsAvailability(latitude, longitude)) {
    is VpsAvailabilityAvailable -> { /* VPS利用可能 */ }
    is VpsAvailabilityUnavailable -> { /* VPS利用不可 */ }
    // ... その他のエラーハンドリング
}
```

### デバイスポーズ → 地理空間ポーズ変換
```kotlin
val devicePose = ArDevice.getInstance(session).state.value.devicePose
when (val result = geospatial.createGeospatialPoseFromPose(devicePose)) {
    is CreateGeospatialPoseFromPoseSuccess -> {
        val geoPose = result.pose
        val lat = geoPose.latitude
        val lon = geoPose.longitude
        val alt = geoPose.altitude
        val orientation = geoPose.eastUpSouthQuaternion
    }
    is CreateGeospatialPoseFromPoseNotTracking -> { /* 未トラッキング */ }
}
```

### 地理空間ポーズ → デバイスポーズ変換
```kotlin
when (val result = geospatial.createPoseFromGeospatialPose(geoPose)) {
    is CreatePoseFromGeospatialPoseSuccess -> {
        val devicePose: Pose = result.pose
    }
    is CreatePoseFromGeospatialPoseNotTracking -> { /* 未トラッキング */ }
}
```

**前提**: Google CloudプロジェクトでARCore APIを有効化し、APIキー認証を設定すること。

---
