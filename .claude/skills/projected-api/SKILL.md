---
name: projected-api
description: Jetpack Projected APIリファレンス。スマートフォンとAIグラス間の通信、デバイス接続状態監視、入力イベント（カメラボタン等）、ディスプレイ制御、グラスアクティビティ起動に関するタスクでは必ずこのスキルを使う。
---

# Jetpack Projected APIリファレンス

`androidx.xr.projected:projected:1.0.0-alpha05`

スマートフォンとAIグラス間のブリッジ。全APIは `@OptIn(ExperimentalProjectedApi::class)` が必要。

#### 2. Jetpack Projected (`androidx.xr.projected:projected:1.0.0-alpha05`)

スマートフォンとAIグラス間のブリッジ。全APIは `@OptIn(ExperimentalProjectedApi::class)` が必要。

##### 主要API

**ProjectedContext** (シングルトン)
```kotlin
// グラスデバイスコンテキスト作成（API 34+）
ProjectedContext.createProjectedDeviceContext(context: Context): Context

// ホスト（スマートフォン）デバイスコンテキスト作成（API 34+）
ProjectedContext.createHostDeviceContext(context: Context): Context

// デバイス名取得 — "ProjectionDevice" or null（API 34+）
ProjectedContext.getProjectedDeviceName(context: Context): String?

// コンテキストがグラス用かチェック（API 34+）
ProjectedContext.isProjectedDeviceContext(context: Context): Boolean

// グラス用アクティビティオプション作成（API 35+）
ProjectedContext.createProjectedActivityOptions(context: Context): ActivityOptions

// デバイス接続状態（API 36+）
ProjectedContext.isProjectedDeviceConnected(context: Context, coroutineContext: CoroutineContext): Flow<Boolean>
```

**ProjectedActivityCompat** (AutoCloseable)
```kotlin
// サービスに接続してインスタンス作成（API 34+）
suspend ProjectedActivityCompat.create(activity: Activity): ProjectedActivityCompat
// ※ Context版は非推奨

// 入力イベントのFlow
val projectedInputEvents: Flow<ProjectedInputEvent>

// 切断
fun close()
```

**ProjectedInputEvent / ProjectedInputAction**
```kotlin
val inputAction: ProjectedInputAction
// 現在定義されているアクション:
// ProjectedInputAction.TOGGLE_APP_CAMERA — カメラボタン押下
```

**ProjectedDeviceController**
```kotlin
// デバイス能力を問い合わせ（API 34+）
suspend ProjectedDeviceController.create(context: Context): ProjectedDeviceController

val capabilities: Set<Capability>
// Capability.CAPABILITY_VISUAL_UI = ディスプレイあり
```

**ProjectedDisplayController** (AutoCloseable)
```kotlin
// ディスプレイサービスに接続（API 34+）
suspend ProjectedDisplayController.create(activity: Activity): ProjectedDisplayController

// レイアウトフラグ操作
fun addLayoutParamsFlags(flags: Int)      // FLAG_KEEP_SCREEN_ON等
fun removeLayoutParamsFlags(flags: Int)

// プレゼンテーションモード変更リスナー（API 24+）
fun addPresentationModeChangedListener(executor: Executor?, listener: Consumer<PresentationModeFlags>)
fun removePresentationModeChangedListener(listener: Consumer<PresentationModeFlags>)

// PresentationMode: VISUALS_ON, AUDIO_ON
// PresentationModeFlags.hasPresentationMode(mode) / hasPresentationMode(Set<mode>)
```

**ProjectedPermissionsRequestParams / ProjectedPermissionsResultContract**
```kotlin
// パーミッション要求（API 34+）
ProjectedPermissionsRequestParams(permissions: List<String>, rationale: String?)
// ActivityResultContract<List<ProjectedPermissionsRequestParams>, Map<String, Boolean>>
```

**APIレベル要件:** コア=34, ActivityOptions=35, isConnected=36

##### テスト用API (`projected-testing`)

**ProjectedTestRule** (JUnit4 TestRule、API 34+)
```kotlin
val testRule = ProjectedTestRule()
// 設定可能なプロパティ:
testRule.isDeviceConnected = true/false
testRule.capabilities = setOf(Capability.CAPABILITY_VISUAL_UI)
testRule.lifecycleState = Lifecycle.State.RESUMED
testRule.presentationModeFlags = setOf(PresentationMode.VISUALS_ON)
testRule.shouldThrowIllegalStateExceptionWhenCreatingControllers = false
// 読み取り専用:
testRule.projectedLayoutParamFlags  // Int

// テスト操作:
testRule.launchTestProjectedDeviceActivity { activity -> /* ... */ }
testRule.sendProjectedInputEvent(ProjectedInputAction.TOGGLE_APP_CAMERA)
```

---
