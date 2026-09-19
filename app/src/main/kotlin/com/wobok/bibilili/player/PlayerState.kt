package com.wobok.bibilili.player

import com.wobok.bibilili.core.bili.play.PlaybackSettings
import com.wobok.bibilili.core.bili.season.EpisodeTab

/** 全屏时可以打开的覆盖层。同一时刻只有一个。 */
enum class PlayerOverlay { None, Episodes, Speed, Quality, Settings, Cast }

data class PlayerUiState(
    val title: String = "",
    val episodeTitle: String = "",
    val cover: String = "",
    val description: String = "",
    val originName: String = "",
    val alias: String = "",
    val meta: List<String> = emptyList(),
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
    /** 长按倍速中。松手恢复。 */
    val boosting: Boolean = false,
    val showDetail: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
)
