package com.wobok.bibilili.player

import androidx.compose.ui.graphics.ImageBitmap
import com.wobok.bibilili.cast.CastDevice
import com.wobok.bibilili.core.bili.play.PlaybackSettings
import com.wobok.bibilili.core.bili.season.EpisodeTab

/** 全屏时可以打开的覆盖层。同一时刻只有一个。 */
enum class PlayerOverlay { None, Episodes, Speed, Quality, Settings, Cast }

/**
 * 进度条小窗预览的雪碧图索引。
 *
 * `x/player/videoshot` 给的是几张大图，每张切成 `xLen × yLen` 格；
 * `index[i]` 是第 i 格对应的秒数。拖动时按秒反查格子即可，不必逐帧请求。
 */
data class PreviewSprites(
    val images: List<String>,
    val secondsIndex: List<Int>,
    val xLen: Int,
    val yLen: Int,
) {
    private val perImage: Int get() = xLen * yLen

    data class Cell(val imageUrl: String, val column: Int, val row: Int)

    fun cellAt(seconds: Long, durationSeconds: Long): Cell? {
        if (images.isEmpty() || perImage <= 0) return null

        // index 缺失时退化成按时长均分——总比完全没有预览好
        val slot = if (secondsIndex.isEmpty()) {
            if (durationSeconds <= 0) return null
            val total = images.size * perImage
            ((seconds * total) / durationSeconds).toInt()
        } else {
            secondsIndex.indexOfLast { it <= seconds }.coerceAtLeast(0)
        }.coerceIn(0, images.size * perImage - 1)

        val image = images.getOrNull(slot / perImage) ?: return null
        val within = slot % perImage
        return Cell(image, within % xLen, within / xLen)
    }
}

data class PlayerUiState(
    val seasonId: Long = 0,
    val title: String = "",
    val episodeTitle: String = "",
    val cover: String = "",
    val description: String = "",
    val originName: String = "",
    val alias: String = "",
    val meta: List<String> = emptyList(),
    val styles: List<String> = emptyList(),
    val score: String = "",
    val scoreCount: String = "",
    val tabs: List<EpisodeTab> = emptyList(),
    val selectedTab: Int = 0,
    val currentEpId: Long = 0,
    val currentQn: Int = 0,
    val availableQn: List<Int> = emptyList(),
    val fullscreen: Boolean = false,
    val controlsVisible: Boolean = true,
    val overlay: PlayerOverlay = PlayerOverlay.None,
    val settings: PlaybackSettings = PlaybackSettings(),

    // ---- 播放进度 ----
    val playing: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val bufferedMs: Long = 0,
    /** 正在拖动进度条或左右滑动调进度。 */
    val scrubbing: Boolean = false,
    val scrubPositionMs: Long = 0,
    val previewSprites: PreviewSprites? = null,
    val previewFrame: ImageBitmap? = null,
    val previewCell: PreviewSprites.Cell? = null,

    /** 当前播放速度。倍速菜单的高亮看它，而不是看「长按倍速」的设定值。 */
    val speed: Float = 1.0f,
    /** 长按倍速中。松手恢复。 */
    val boosting: Boolean = false,

    val favourited: Boolean = false,
    val castDevices: List<CastDevice> = emptyList(),
    val castSearching: Boolean = false,
    val castingTo: String? = null,
    val castError: String? = null,

    val showDetail: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
) {
    /** 进度条要画的位置：拖动中画手指的位置，否则画真实播放位置。 */
    val displayPositionMs: Long get() = if (scrubbing) scrubPositionMs else positionMs

    val progressFraction: Float
        get() = if (durationMs <= 0) 0f else (displayPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)

    val bufferedFraction: Float
        get() = if (durationMs <= 0) 0f else (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

/** `01:02:03` / `02:03`。 */
fun formatTime(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
