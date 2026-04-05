---
name: glasses-arch
description: AIグラスアプリのアーキテクチャ、build.gradle.kts設定、AndroidManifest.xml、依存関係グラフ、典型的なコードパターン（GlassesMainActivity等）に関するタスクでは必ずこのスキルを使う。新規プロジェクト作成、ビルド設定、アプリ構造設計の際は必ず参照。
---

# AIグラスアプリ アーキテクチャ・ビルド設定リファレンス

プロジェクト構成、依存関係、Manifest設定、典型的なコードパターンの包括的リファレンス。

### ■ AIグラスでは絶対に使わないライブラリ

以下のライブラリはXRヘッドセット/有線XRグラス向け。**AIグラスプロジェクトで絶対にimportしないこと。**

| ライブラリ | 理由 |
|-----------|------|
| `androidx.xr.scenecore` | 没入型3Dシーングラフ。3Dモデル/スカイボックス/パススルー用。透過ディスプレイのパラダイムと根本的に不一致 |
| `androidx.xr.compose` | 空間UI（SpatialPanel, Orbiter）。**scenecoreに依存**しており、AIグラスには不適 |
| `androidx.xr.compose.material3` | ヘッドセット向けMaterial Design（NavigationRail, FloatingToolbar等）。Glimmerを使うこと |

---

## 依存関係グラフ

```
AIグラスで使うもの:
├── Runtime (基盤)
│   ├── 使用者: ARCore, Projected, すべて
│   └── 提供: Session, Config, 数学型, ライフサイクル, TrackingState
│
├── ARCore (パーセプション)
│   ├── 依存: Runtime
│   ├── 内部: arcore-projectedがAIグラス用バックエンド（公開APIなし）
│   └── 提供: Plane, Anchor, Hand, Face, Eye, Geospatial, DepthMap
│
├── Projected (グラスブリッジ)
│   ├── 依存: Runtime
│   └── 提供: ハードウェアアクセス, アクティビティ起動, 入力処理, ディスプレイ制御
│
└── Glimmer (UIツールキット)
    ├── 依存: Compose基盤のみ（Runtime/ARCore/SceneCoreに非依存）
    └── 提供: AIグラス最適化UIコンポーネント, ジェスチャー検出

使わないもの:
├── Compose (ヘッドセット没入型) ← SceneCoreに依存
├── Compose Material3 (ヘッドセット没入型) ← Composeに依存
└── SceneCore (3Dレンダリング) ← ヘッドセット専用
```

---

## build.gradle.kts 設定例

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.myglassesapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myglassesapp"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // === AIグラス必須ライブラリ ===
    implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha08")
    implementation("androidx.xr.projected:projected:1.0.0-alpha05")
    implementation("androidx.xr.runtime:runtime:1.0.0-alpha12")
    implementation("androidx.xr.arcore:arcore:1.0.0-alpha11")

    // === Compose基盤 ===
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.9.0")

    // === Kotlin/Android基盤 ===
    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")

    // === Gemini Live API（Firebase AI Logic） ===
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-ai")

    // === Geospatial API使用時 ===
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // === CameraX（グラスカメラアクセス） ===
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")

    // === テスト ===
    androidTestImplementation("androidx.xr.projected:projected-testing:1.0.0-alpha05")

    // === 使わない（ヘッドセット向け） ===
    // implementation("androidx.xr.scenecore:scenecore:...")      // ← 使わない
    // implementation("androidx.xr.compose:compose:...")           // ← 使わない
    // implementation("androidx.xr.compose.material3:material3:...") // ← 使わない
}
```

---

## AndroidManifest.xml 設定例

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Geospatial API使用時 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <!-- カメラ使用時 -->
    <uses-permission android:name="android.permission.CAMERA" />
    <!-- Gemini Live API音声会話使用時 -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <application
        android:label="@string/app_name"
        android:theme="@style/Theme.MyApp">

        <!-- メインアクティビティ（スマートフォン側ランチャー） -->
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- グラス用アクティビティ（公式推奨パターン） -->
        <activity
            android:name=".GlassesMainActivity"
            android:exported="true"
            android:requiredDisplayCategory="xr_projected"
            android:label="AI Glasses Activity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
            </intent-filter>
        </activity>

    </application>
</manifest>
```

**重要**: `android:requiredDisplayCategory="xr_projected"` を必ず指定すること。これにより、投影されたコンテキストを使用してグラスのハードウェアにアクセスする必要があることをシステムに伝える。

---

## アプリアーキテクチャ

```
Phone (Host)                    AI Glasses (Display)
┌──────────────────┐           ┌──────────────────┐
│ MainActivity     │           │                  │
│ (ランチャー)      │──Project──→│ GlimmerTheme UI  │
│                  │  edCtx    │ (透過ディスプレイ)  │
│ ProjectedContext │──────────→│                  │
│ ARCore Projected │           │ センサー/カメラ     │
└──────────────────┘           └──────────────────┘
```

1. スマートフォンの`MainActivity`が起点
2. `ProjectedContext.createProjectedDeviceContext()`でグラスとの接続を確立
3. `ProjectedContext.createProjectedActivityOptions()`でグラス用アクティビティを起動
4. グラス側のUIは`GlimmerTheme { ... }`でラップしたComposeで構築
5. グラスのハードウェアには`ProjectedContext`経由でアクセス
6. `ProjectedActivityCompat`で入力イベント（`ProjectedInputEvent`）を受信
7. `ProjectedDeviceController`でデバイス能力を問い合わせ（`CAPABILITY_VISUAL_UI`でディスプレイ有無確認）
8. `ProjectedDisplayController`で画面オン維持（`FLAG_KEEP_SCREEN_ON`）やプレゼンテーションモード監視

---

## 典型的なコードパターン

### 公式推奨: GlassesMainActivity（完全版）
```kotlin
@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    private var displayController: ProjectedDisplayController? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)
    private var isPermissionDenied by mutableStateOf(false)

    // 権限ランチャー登録（ProjectedPermissionsResultContract使用）
    private val requestPermissionLauncher: ActivityResultLauncher<List<ProjectedPermissionsRequestParams>> =
        registerForActivityResult(ProjectedPermissionsResultContract()) { results ->
            if (results[Manifest.permission.CAMERA] == true) {
                isPermissionDenied = false
                initializeGlassesFeatures()
            } else {
                isPermissionDenied = true
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ライフサイクルオブザーバーでDisplayControllerをクリーンアップ
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                displayController?.close()
                displayController = null
            }
        })

        if (hasCameraPermission()) {
            initializeGlassesFeatures()
        } else {
            requestHardwarePermissions()
        }

        setContent {
            GlimmerTheme {
                HomeScreen(
                    areVisualsOn = areVisualsOn,
                    isVisualUiSupported = isVisualUiSupported,
                    isPermissionDenied = isPermissionDenied,
                    onRetryPermission = { requestHardwarePermissions() },
                    onClose = { finish() }
                )
            }
        }
    }

    private fun initializeGlassesFeatures() {
        lifecycleScope.launch {
            // デバイス能力チェック（ディスプレイ有無）
            val projectedDeviceController = ProjectedDeviceController.create(this@GlassesMainActivity)
            isVisualUiSupported = projectedDeviceController.capabilities.contains(CAPABILITY_VISUAL_UI)

            // ディスプレイコントローラー初期化
            val controller = ProjectedDisplayController.create(this@GlassesMainActivity)
            displayController = controller

            // プレゼンテーションモード監視
            controller.addPresentationModeChangedListener { flags ->
                areVisualsOn = flags.hasPresentationMode(PresentationMode.VISUALS_ON)
            }
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun requestHardwarePermissions() {
        val params = ProjectedPermissionsRequestParams(
            permissions = listOf(Manifest.permission.CAMERA),
            rationale = "Camera access is required to overlay digital content on your physical environment."
        )
        requestPermissionLauncher.launch(listOf(params))
    }
}
```

### HomeScreen コンポーザブル（公式パターン）
```kotlin
@Composable
fun HomeScreen(
    areVisualsOn: Boolean,
    isVisualUiSupported: Boolean,
    isPermissionDenied: Boolean,
    onRetryPermission: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .surface(focusable = false)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isPermissionDenied) {
            Card(
                title = { Text("Permission Required") },
                action = { Button(onClick = onClose) { Text("Exit") } }
            ) {
                Text("Camera access is needed to use AI glasses features.")
                Button(onClick = onRetryPermission) { Text("Retry") }
            }
        } else if (isVisualUiSupported) {
            Card(
                title = { Text("Android XR") },
                action = { Button(onClick = onClose) { Text("Close") } }
            ) {
                if (areVisualsOn) {
                    Text("Hello, AI Glasses!")
                } else {
                    Text("Display is off. Audio guidance active.")
                }
            }
        } else {
            Text("Audio Guidance Mode Active")
        }
    }
}
```

### グラスアクティビティの起動（スマートフォンから）
```kotlin
// 投影コンテキスト経由でグラス用アクティビティを起動
val options = ProjectedContext.createProjectedActivityOptions(context)
val intent = Intent(context, GlassesMainActivity::class.java)
context.startActivity(intent, options.toBundle())
```

### グラス接続状態の監視
```kotlin
lifecycleScope.launch {
    ProjectedContext.isProjectedDeviceConnected(this@MainActivity, Dispatchers.Main)
        .collect { connected ->
            if (connected) { launchGlassesActivity() }
        }
}
```

### VerticalStack（カードフリップ）
```kotlin
GlimmerTheme {
    VerticalStack {
        item { Card(title = { Text("ページ1") }) { Text("内容1") } }
        item { Card(title = { Text("ページ2") }) { Text("内容2") } }
        item { Card(title = { Text("ページ3") }) { Text("内容3") } }
    }
}
```

### タッチパッドジェスチャー
```kotlin
Box(
    modifier = Modifier
        .onIndirectPointerGesture(
            onSwipeForward = { /* 次のアイテムへ */ },
            onSwipeBackward = { /* 前のアイテムへ */ },
            onClick = { /* 選択 */ },
        )
        .focusTarget()
)
```

### ボタンバリエーション
```kotlin
// テキストのみ
Button(onClick = { }) { Text("確認") }

// アイコン付き
Button(
    onClick = { },
    leadingIcon = { Icon(Icons.Default.Check, null) },
) { Text("承認") }

// 大サイズ
Button(
    onClick = { },
    buttonSize = ButtonSize.Large,
) { Text("メインアクション") }

// カスタムカラー
Button(
    onClick = { },
    color = GlimmerTheme.colors.primary,
) { Text("重要") }
```

### TitleChip + リスト
```kotlin
GlimmerTheme {
    VerticalList(
        title = { TitleChip { Text("通知一覧") } },
    ) {
        items(notifications) { notification ->
            ListItem(
                onClick = { /* 詳細表示 */ },
                supportingLabel = { Text(notification.time) },
                leadingIcon = { Icon(notification.icon, null) },
            ) {
                Text(notification.title)
            }
        }
    }
}
```

---
## コーディング規約

- Kotlin推奨、Kotlin DSL（build.gradle.kts）使用
- Composeベースの宣言的UI
- `@OptIn(ExperimentalProjectedApi::class)` をProjected API使用時に付与
- AIグラスのコンテキストを常に意識:
  - 透過ディスプレイの制約（黒=透明）
  - 限定的なFOV（50-70°）
  - 音声＋タッチパッド中心のインタラクション
  - 最小限・グランス可能なUI（リスト3アイテム以下）

---

