package com.example.geminivoiceloop

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
import androidx.compose.material.icons.filled.Mic
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

@OptIn(ExperimentalProjectedApi::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GlimmerTheme {
                PhoneLauncherScreen(onLaunchGlasses = { launchGlassesActivity() })
            }
        }

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
                        if (connected) launchGlassesActivity()
                    }
            }
        }
    }

    private fun launchGlassesActivity() {
        try {
            val options = ProjectedContext.createProjectedActivityOptions(this)
            val intent = Intent(this, GlassesMainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent, options.toBundle())
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to launch glasses activity", e)
        }
    }
}

@Composable
private fun PhoneLauncherScreen(
    onLaunchGlasses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            title = { Text("Gemini Voice Loop") },
            action = {
                Button(
                    onClick = onLaunchGlasses,
                    leadingIcon = { Icon(Icons.Default.Mic, null) }
                ) { Text("Start on Glasses") }
            }
        ) {
            Text("Gemini Live voice conversation loop. Tap to launch on AI glasses.")
        }
    }
}
