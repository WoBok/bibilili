package com.wobok.bibilili.core.bili.play

/**
 * 会被记住的播放设置。竖屏播放页与全屏播放共用同一份，
 * 在全屏的三点菜单里改完，竖屏立刻生效。
 */
data class PlaybackSettings(
    val seekStepSeconds: Int = DEFAULT_SEEK_STEP,
    val longPressSpeed: Float = DEFAULT_LONG_PRESS_SPEED,
    val autoPlayNext: Boolean = true,
    val skipOpeningEnding: Boolean = true,
    val sleepTimer: SleepTimer = SleepTimer.Off,
    val preferredQn: Int = PlayQuality.Q_4K.qn,
) {
    companion object {
        /** 需求原文里的「快进秒数」，界面上叫**双击快进步长**。 */
        val SEEK_STEP_OPTIONS = listOf(5, 10, 15, 30)
        const val DEFAULT_SEEK_STEP = 10

        /** 倍速档位。长按倍速从中选，默认 2.0X。 */
        val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)

        /** 可作为「长按倍速」的档位——低于 1 倍没有意义。 */
        val LONG_PRESS_SPEED_OPTIONS = SPEED_OPTIONS.filter { it > 1.0f }
        const val DEFAULT_LONG_PRESS_SPEED = 2.0f
    }
}

sealed interface SleepTimer {
    data object Off : SleepTimer
    data class After(val minutes: Int) : SleepTimer
    /** 播完本集就停。 */
    data object EndOfEpisode : SleepTimer

    companion object {
        val OPTIONS: List<SleepTimer> =
            listOf(Off, After(15), After(30), After(60), EndOfEpisode)
    }
}

/** 长按倍速浮层的文案。需求明确要求不带 emoji。 */
fun formatSpeedBadge(speed: Float): String {
    val text = if (speed % 1f == 0f) "${speed.toInt()}.0X" else "${speed}X"
    return "$text 倍速播放中"
}
