package com.example.glimmerbasicui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.Text
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * スマートフォン側ランチャーアクティビティ。
 *
 * グラスデバイスの接続状態を監視し、接続時に
 * ProjectedContext.createProjectedActivityOptions() 経由で
 * GlassesMainActivity をグラスディスプレイ上に起動する。
 *
 * [FB対応] FLAG_ACTIVITY_CLEAR_TOP + FLAG_ACTIVITY_SINGLE_TOP で起動することで、
 * 既存の GlassesMainActivity インスタンスを再利用し、onNewIntent で
 * DisplayController を再初期化する。
 */
@OptIn(ExperimentalProjectedApi::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GlimmerTheme {
                PhoneLauncherScreen(
                    onLaunchGlasses = { launchGlassesActivity() }
                )
            }
        }

        // グラス接続を監視し、自動起動（API 36+のみ）
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

    /**
     * [FB対応] GlassesMainActivity を ProjectedContext 経由で起動。
     * FLAG_ACTIVITY_CLEAR_TOP + FLAG_ACTIVITY_SINGLE_TOP を設定し、
     * 既存インスタンスがある場合は onNewIntent で再初期化させる。
     */
    private fun launchGlassesActivity() {
        try {
            val options = ProjectedContext.createProjectedActivityOptions(this)
            val intent = Intent(this, GlassesMainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent, options.toBundle())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch glasses activity", e)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

@Composable
private fun PhoneLauncherScreen(
    onLaunchGlasses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            title = { Text("Glimmer Basic UI") },
            action = {
                Button(
                    onClick = onLaunchGlasses,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                        )
                    }
                ) {
                    Text("グラスに表示")
                }
            }
        ) {
            Text("AIグラスを接続すると自動的にUIが表示されます。手動で起動する場合はボタンをタップしてください。")
        }
    }
}
