# architectureパターン集

> このファイルはAIがコンテキストとして読み込み、vibe codingの参照にする。
> コードスニペットはコピペで動くレベルの完全性を維持すること。

---

## スマホ→グラス 2アクティビティ起動パターン

**いつ使う**: AIグラスアプリの基本的なアクティビティ構成を作るとき。全てのグラスアプリはこのパターンが必須。
**前提**: `implementation("androidx.xr.projected:projected:1.0.0-alpha05")`, `implementation("androidx.xr.runtime:runtime:1.0.0-alpha12")`

```kotlin
// === MainActivity.kt: スマートフォン側ランチャー ===
package com.example.myapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalProjectedApi::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GlimmerTheme {
                // スマホ側のUI（手動起動ボタン等）
            }
        }

        // グラス接続を監視し自動起動（API 36+のみ）
        if (Build.VERSION.SDK_INT >= 36) {
            observeGlassesConnection()
        }
    }

    @RequiresApi(36)
    private fun observeGlassesConnection() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ProjectedContext.isProjectedDeviceConnected(this@MainActivity, Dispatchers.Main)
                    .collect { connected ->
                        if (connected) {
                            launchGlassesActivity()
                        }
                    }
            }
        }
    }

    private fun launchGlassesActivity() {
        val options = ProjectedContext.createProjectedActivityOptions(this)
        val intent = Intent(this, GlassesMainActivity::class.java)
        startActivity(intent, options.toBundle())
    }
}
```

```kotlin
// === GlassesMainActivity.kt: グラス側アクティビティ ===
package com.example.myapp

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.projected.ProjectedDeviceController
import androidx.xr.projected.ProjectedDeviceController.Capability.Companion.CAPABILITY_VISUAL_UI
import androidx.xr.projected.ProjectedDisplayController
import androidx.xr.projected.ProjectedDisplayController.PresentationMode
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    private var displayController: ProjectedDisplayController? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // DisplayControllerのライフサイクル管理
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                displayController?.close()
                displayController = null
            }
        })

        initializeGlassesFeatures()

        setContent {
            GlimmerTheme {
                if (isVisualUiSupported && areVisualsOn) {
                    // グラス側のUI
                }
            }
        }
    }

    private fun initializeGlassesFeatures() {
        lifecycleScope.launch {
            // ディスプレイ有無を確認
            val deviceController = ProjectedDeviceController.create(this@GlassesMainActivity)
            isVisualUiSupported = deviceController.capabilities.contains(CAPABILITY_VISUAL_UI)

            // ディスプレイ制御: スクリーンオン維持 + プレゼンテーションモード監視
            val controller = ProjectedDisplayController.create(this@GlassesMainActivity)
            displayController = controller
            controller.addLayoutParamsFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller.addPresentationModeChangedListener { flags ->
                areVisualsOn = flags.hasPresentationMode(PresentationMode.VISUALS_ON)
            }
        }
    }
}
```

```xml
<!-- === AndroidManifest.xml: 2アクティビティ構成 === -->
<application>
    <!-- スマホ側ランチャー: LAUNCHERカテゴリ付き -->
    <activity
        android:name=".MainActivity"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>

    <!-- グラス側: LAUNCHERなし + requiredDisplayCategory -->
    <activity
        android:name=".GlassesMainActivity"
        android:exported="true"
        android:requiredDisplayCategory="xr_projected"
        android:label="Glasses Activity">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
        </intent-filter>
    </activity>
</application>
```

**ハマりポイント**:
- GlassesMainActivityにLAUNCHERカテゴリを付けると、再起動時にprojected contextなしで起動→UI消失バグ
- `isProjectedDeviceConnected`はAPI 36以上が必要。`Build.VERSION.SDK_INT >= 36` + `@RequiresApi(36)`でガード
- `projected:1.0.0-alpha05`はAGP 8.9.1以上を要求
- `ExperimentalProjectedApi`のパッケージは `androidx.xr.projected.experimental`
- `PresentationMode`は`ProjectedDisplayController.PresentationMode`（ネストクラス）
- DisplayControllerは必ずLifecycleObserverでclose()すること（リソースリーク防止）

**出典**: experiments/ui/001-glimmer-basic-ui

---

## GlassesMainActivity 堅牢ライフサイクルパターン

**いつ使う**: GlassesMainActivityでDisplayControllerを使う場合。別画面から戻った際のUI再表示問題を防ぐ。全グラスアプリで必須。
**前提**: `implementation("androidx.xr.projected:projected:1.0.0-alpha05")`, `android:launchMode="singleTop"` をManifestに設定

```kotlin
// === GlassesMainActivity.kt: 堅牢なライフサイクル管理 ===
package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.projected.ProjectedDeviceController
import androidx.xr.projected.ProjectedDeviceController.Capability.Companion.CAPABILITY_VISUAL_UI
import androidx.xr.projected.ProjectedDisplayController
import androidx.xr.projected.ProjectedDisplayController.PresentationMode
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    private var displayController: ProjectedDisplayController? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ライフサイクルオブザーバーでDisplayControllerをクリーンアップ
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                releaseDisplayController()
            }
        })

        initializeGlassesFeatures()

        setContent {
            GlimmerTheme {
                if (isVisualUiSupported && areVisualsOn) {
                    // グラス側UIをここに配置
                }
            }
        }
    }

    // singleTopで再利用時にDisplayControllerを再初期化
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        releaseDisplayController()
        initializeGlassesFeatures()
    }

    // バックグラウンドからの復帰時にnullなら再初期化
    override fun onResume() {
        super.onResume()
        if (displayController == null) {
            initializeGlassesFeatures()
        }
    }

    // isFinishing時のみ早期解放（通常のバックグラウンド遷移では解放しない）
    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            releaseDisplayController()
        }
    }

    private fun initializeGlassesFeatures() {
        lifecycleScope.launch {
            try {
                val deviceController = ProjectedDeviceController.create(this@GlassesMainActivity)
                isVisualUiSupported = deviceController.capabilities.contains(CAPABILITY_VISUAL_UI)

                val controller = ProjectedDisplayController.create(this@GlassesMainActivity)
                displayController = controller
                controller.addLayoutParamsFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                controller.addPresentationModeChangedListener { flags ->
                    areVisualsOn = flags.hasPresentationMode(PresentationMode.VISUALS_ON)
                }
            } catch (e: Exception) {
                Log.e("GlassesMainActivity", "Failed to initialize", e)
            }
        }
    }

    private fun releaseDisplayController() {
        displayController?.let { controller ->
            try { controller.close() } catch (e: Exception) {
                Log.w("GlassesMainActivity", "Error closing DisplayController", e)
            }
        }
        displayController = null
    }
}
```

```xml
<!-- AndroidManifest.xml: singleTop必須 -->
<activity
    android:name=".GlassesMainActivity"
    android:exported="true"
    android:launchMode="singleTop"
    android:requiredDisplayCategory="xr_projected"
    android:label="Glasses Activity">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
    </intent-filter>
</activity>
```

```kotlin
// MainActivity側: CLEAR_TOP + SINGLE_TOP フラグで起動
private fun launchGlassesActivity() {
    val options = ProjectedContext.createProjectedActivityOptions(this)
    val intent = Intent(this, GlassesMainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    startActivity(intent, options.toBundle())
}
```

**ハマりポイント**:
- `launchMode="singleTop"` なしだと、再起動時に新しいインスタンスが作られ、前のインスタンスのDisplayControllerがリークする
- `onNewIntent` をオーバーライドしないと、singleTopでintentが更新されてもDisplayControllerが再初期化されない
- `onResume` でのnullチェックがないと、バックグラウンドからの復帰時にUIが出ない
- `onStop` で無条件にcloseすると、一時的なバックグラウンド遷移でもDisplayControllerが解放されてしまう。`isFinishing` でガードする
- `FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP` をMainActivity側で設定することで、既存インスタンスの再利用を確実にする

**出典**: experiments/ui/001-glimmer-basic-ui (人間フィードバック対応で確立)

---

## 通知ブリッジングパターン（スマホ→グラス）

**いつ使う**: AIグラスに通知をブリッジしたいとき
**前提**: `implementation("androidx.core:core-ktx:1.13.0")`

```kotlin
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person

// 1. IMPORTANCE_HIGHチャンネルを作成（ブリッジング条件）
val channel = NotificationChannel(
    "glasses_channel",
    "Glasses Notifications",
    NotificationManager.IMPORTANCE_HIGH, // IMPORTANCE_HIGH以上必須
)
context.getSystemService(NotificationManager::class.java)
    .createNotificationChannel(channel)

// 2. 標準通知（ブリッジング条件を全て満たす）
val notification = NotificationCompat.Builder(context, "glasses_channel").apply {
    setSmallIcon(android.R.drawable.ic_dialog_info)
    setContentTitle("タイトル必須") // null・空だとブリッジされない
    setContentText("通知本文")
    setAutoCancel(true)
    setOngoing(false) // 継続的通知はブリッジされない
    // FLAG_LOCAL_ONLYは付けない（デフォルト: ブリッジ可能）
    priority = NotificationCompat.PRIORITY_HIGH
}.build()
NotificationManagerCompat.from(context).notify(1, notification)

// 3. MessagingStyle通知（会話形式、グラスで特に見やすい）
val person = Person.Builder().setName("Sender").build()
val style = NotificationCompat.MessagingStyle(person)
    .setConversationTitle("会話タイトル")
    .addMessage("メッセージ本文", System.currentTimeMillis(), person)
val msgNotification = NotificationCompat.Builder(context, "glasses_channel").apply {
    setSmallIcon(android.R.drawable.ic_dialog_email)
    setStyle(style)
    setAutoCancel(true)
}.build()
NotificationManagerCompat.from(context).notify(2, msgNotification)
```

**ハマりポイント**:
- ブリッジング条件: IMPORTANCE_HIGH + タイトル非空 + FLAG_LOCAL_ONLYなし + 非ongoing
- POST_NOTIFICATIONS権限がAPI 33+で必要
- RemoteViewsカスタム通知はブリッジされない
- MessagingStyleはダイレクト返信対応で音声返信/スマートリプライが使える

**出典**: experiments/architecture/005-notification-bridge

---

## Glasses Permission Request Flow

**When to use**: When requesting hardware permissions (CAMERA, RECORD_AUDIO) on AI glasses
**Prerequisites**: `implementation("androidx.xr.projected:projected:1.0.0-alpha05")`

```kotlin
import androidx.activity.result.ActivityResultLauncher
import androidx.xr.projected.permissions.ProjectedPermissionsRequestParams
import androidx.xr.projected.permissions.ProjectedPermissionsResultContract
import androidx.xr.projected.experimental.ExperimentalProjectedApi

@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    // Register launcher (must be in property initializer, not onCreate)
    private val requestPermissionLauncher: ActivityResultLauncher<List<ProjectedPermissionsRequestParams>> =
        registerForActivityResult(ProjectedPermissionsResultContract()) { results ->
            // results: Map<String, Boolean>
            val cameraGranted = results[Manifest.permission.CAMERA] == true
            val audioGranted = results[Manifest.permission.RECORD_AUDIO] == true
        }

    private fun requestPermissions() {
        val params = ProjectedPermissionsRequestParams(
            permissions = listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
            ),
            rationale = "Camera and microphone are required for AI glasses features."
        )
        requestPermissionLauncher.launch(listOf(params))
    }
}
```

**Gotchas**:
- Import from `androidx.xr.projected.permissions` (NOT `androidx.xr.projected`)
- `registerForActivityResult` must be called during initialization (property/onCreate), not later
- Multiple permissions in single request: pass all in one `ProjectedPermissionsRequestParams`
- Result map keys are permission strings (e.g., `Manifest.permission.CAMERA`)
- Include TTS fallback for permission denial accessibility

**Source**: experiments/architecture/012-permissions-handling

---

## HostDeviceContext Cross-Device Access Pattern

**When to use**: When accessing phone hardware (vibrator, sensors) from glasses Activity
**Prerequisites**: `implementation("androidx.xr.projected:projected:1.0.0-alpha05")`

```kotlin
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi

@OptIn(ExperimentalProjectedApi::class)
fun exploreContexts(activity: ComponentActivity) {
    // Check if current context is glasses context
    val isGlasses = ProjectedContext.isProjectedDeviceContext(activity)  // true in GlassesMainActivity

    // Get projected device name
    val name = ProjectedContext.getProjectedDeviceName(activity)  // "ProjectionDevice" or null

    // Get phone context from glasses
    try {
        val phoneContext = ProjectedContext.createHostDeviceContext(activity)
        val isPhone = ProjectedContext.isProjectedDeviceContext(phoneContext)  // false

        // Access phone hardware
        val vibrator = phoneContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
    } catch (e: IllegalStateException) {
        // No projected device connected
    }
}
```

**Gotchas**:
- createHostDeviceContext() throws IllegalStateException if no device connected
- Do NOT use getApplicationContext() - may return wrong context type
- isProjectedDeviceContext() returns true for glasses, false for phone
- Host context is valid only while isProjectedDeviceConnected is true
- VIBRATE permission needed in AndroidManifest.xml

**Source**: experiments/architecture/015-host-device-context

---

## Multi-System State Machine Pattern

**When to use**: When coordinating multiple async systems (Camera + AI + Input Events) in a single Activity
**Prerequisites**: Kotlin sealed class

```kotlin
// Define unified state machine covering all subsystems
sealed class AppState {
    data object Initializing : AppState()
    data object Ready : AppState()
    data object Capturing : AppState()
    data class Analyzing(val description: String = "Analyzing...") : AppState()
    data class Conversing(val lastTranscript: String = "") : AppState()
    data class Error(val message: String) : AppState()
}

// In Activity: single mutableStateOf drives all UI
private var appState by mutableStateOf<AppState>(AppState.Initializing)

// Each subsystem reports completion -> check if all ready
private fun checkAllReady() {
    if (subsystem1Ready && subsystem2Ready && appState is AppState.Initializing) {
        appState = AppState.Ready
    }
}

// Transitions are explicit and safe
private fun triggerAction() {
    if (appState !is AppState.Ready) return  // Guard clause
    appState = AppState.Capturing
    // ... async operation ...
}

// UI uses exhaustive when() on sealed class
when (appState) {
    is AppState.Initializing -> { /* loading UI */ }
    is AppState.Ready -> { /* action buttons */ }
    is AppState.Capturing -> { /* progress */ }
    is AppState.Analyzing -> { /* AI working */ }
    is AppState.Conversing -> { /* transcript display */ }
    is AppState.Error -> { /* retry button */ }
}
```

**Gotchas**:
- Single source of truth: one mutableStateOf for the entire app state
- Guard clauses prevent invalid transitions (e.g., capture while already capturing)
- Exhaustive when() on sealed class ensures all states are handled
- Each subsystem init should call checkAllReady() on completion
- Error state should be reachable from any state
- retryFromError() should release and reinitialize all subsystems

**Source**: experiments/integration/016-camera-gemini-visual-qa

---
