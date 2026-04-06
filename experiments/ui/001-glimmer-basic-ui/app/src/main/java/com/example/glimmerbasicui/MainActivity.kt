package com.example.glimmerbasicui

import android.content.Intent
import android.os.Build
import android.os.Bundle
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

    private fun launchGlassesActivity() {
        val options = ProjectedContext.createProjectedActivityOptions(this)
        val intent = Intent(this, GlassesMainActivity::class.java)
        startActivity(intent, options.toBundle())
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
