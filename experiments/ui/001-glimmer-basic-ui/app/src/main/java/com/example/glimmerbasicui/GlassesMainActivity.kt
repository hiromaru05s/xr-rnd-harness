package com.example.glimmerbasicui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.xr.glimmer.GlimmerTheme
import com.example.glimmerbasicui.ui.BasicUiScreen

/**
 * AIグラス用アクティビティ。
 * GlimmerTheme でラップし、透過ディスプレイ向けの基本UIを表示する。
 */
class GlassesMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GlimmerTheme {
                BasicUiScreen()
            }
        }
    }
}
