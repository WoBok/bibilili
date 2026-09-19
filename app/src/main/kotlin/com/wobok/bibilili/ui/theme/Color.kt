package com.wobok.bibilili.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 素笺 Paper Zen 配色。日间是米白纸感，夜间是纯黑。
 *
 * 纯黑底下的三条硬规则（在 docs/07 里有推导）：
 *  1. 正文绝不用纯白 —— `#FFFFFF` 压在纯黑上会产生光晕，长时间阅读疲劳；
 *  2. 卡片绝不用纯黑 —— 否则和背景没有边界；
 *  3. 阴影一律换成 1px 描边 —— 纯黑底上投影根本看不见。
 */
object PaperColors {

    // ---- 日：宣纸 ----
    val LightBg = Color(0xFFFBF9F4)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurface2 = Color(0xFFF5F2EA)
    val LightOutline = Color(0xFFE9E4D9)
    val LightOutline2 = Color(0xFFE0D9CB)
    val LightPrimary = Color(0xFF1F4E46)          // 墨绿
    val LightPrimaryContainer = Color(0xFFDCE8E4)
    val LightAccent = Color(0xFFC2410C)           // 陶土橙
    val LightOnBg = Color(0xFF1A1917)
    val LightOnBg2 = Color(0xFF6B655C)
    val LightOnBg3 = Color(0xFF9A948A)

    // ---- 夜：纯黑 ----
    val DarkBg = Color(0xFF000000)
    val DarkSurface = Color(0xFF0D0E0F)
    val DarkSurface2 = Color(0xFF16181A)
    val DarkOutline = Color(0xFF1C1F21)
    val DarkOutline2 = Color(0xFF2A2E31)
    val DarkPrimary = Color(0xFF7FB3A8)           // 提亮墨绿
    val DarkPrimaryContainer = Color(0xFF16302B)
    val DarkAccent = Color(0xFFE8814F)            // 提亮陶土
    val DarkOnBg = Color(0xFFE6E1D6)              // 宣纸米白，刻意不用纯白
    val DarkOnBg2 = Color(0xFF8E897F)
    val DarkOnBg3 = Color(0xFF5C5850)

    // ---- 播放器：日夜同 ----
    val PlayerBg = Color(0xFF000000)

    /**
     * 全屏播放专用强调色：低饱和中国红（檀）。
     *
     * 陶土橙压在真实画面上偏跳，换成檀色更沉。亮度刻意抬到能在深色覆盖层上
     * 过 4.5:1 —— 再暗的红在这个底上就看不清了。
     * **只作用于全屏播放**，竖屏与浏览类页面仍用陶土橙。
     */
    val PlayerAccent = Color(0xFFBC6A60)

    /** 全屏覆盖层的文字色，比正文再亮一档。 */
    val PlayerOnOverlay = Color(0xFFE6E1D6)
    val PlayerOnOverlayTitle = Color(0xFFF2EEE5)
}
