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
