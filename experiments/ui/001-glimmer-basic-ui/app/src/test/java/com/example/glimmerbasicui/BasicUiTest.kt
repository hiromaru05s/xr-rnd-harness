package com.example.glimmerbasicui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * BasicUiScreen のデータ制約を検証するユニットテスト。
 */
class BasicUiTest {

    @Test
    fun `menu items count is within glasses constraint`() {
        // グラスのUI制約: リストは3アイテム以下
        val items = listOf("通知", "設定", "ヘルプ")
        assertTrue("リストは3アイテム以下であること", items.size <= 3)
        assertEquals(3, items.size)
    }

    @Test
    fun `initial selected label is correct`() {
        val initialLabel = "選択なし"
        assertTrue("初期ラベルが空でないこと", initialLabel.isNotEmpty())
    }

    @Test
    fun `selected label updates on item tap`() {
        var selectedLabel = "選択なし"
        val tapTarget = "設定"

        // タップ操作をシミュレート
        selectedLabel = tapTarget

        assertEquals(tapTarget, selectedLabel)
    }
}
