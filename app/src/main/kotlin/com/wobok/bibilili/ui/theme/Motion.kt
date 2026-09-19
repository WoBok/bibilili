package com.wobok.bibilili.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween

/**
 * 素笺的动效克制：幅度小、时长短、缓动柔，**不做弹性回弹**。
 * 数值与 docs/07 §1.4 一一对应。
 */
object PaperMotion {

    /** easeOutQuart —— 起步快、收尾缓，是整套动效的默认缓动。 */
    val EaseOutQuart: Easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)

    const val DURATION_DEFAULT = 280
    const val DURATION_PAGE = 240
    const val DURATION_SHARED_ELEMENT = 360
    const val DURATION_DRAWER = 300
    const val DURATION_DETAIL_SHEET = 320
    const val DURATION_THEME_SWITCH = 240

    /** 倍速浮层：进得快、退得缓，避免松手瞬间闪一下。 */
    const val DURATION_SPEED_BADGE_IN = 120
    const val DURATION_SPEED_BADGE_OUT = 180

    /** 控件显隐。 */
    const val DURATION_CONTROLS = 200

    /** 列表逐项入场的间隔。 */
    const val STAGGER_STEP_MILLIS = 30
    const val DURATION_LIST_ITEM = 200

    /** 控件无操作自动隐藏的时间。 */
    const val CONTROLS_AUTO_HIDE_MILLIS = 3_000L

    fun <T> default(durationMillis: Int = DURATION_DEFAULT) =
        tween<T>(durationMillis = durationMillis, easing = EaseOutQuart)
}
