package com.wobok.bibilili.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.wobok.bibilili.BibiliApp
import com.wobok.bibilili.core.bili.error.ApiResult
import com.wobok.bibilili.core.bili.season.Episode
import com.wobok.bibilili.core.bili.season.Section
import com.wobok.bibilili.core.bili.season.SeasonGrouping
import com.wobok.bibilili.core.bili.season.SeasonRef
import com.wobok.bibilili.data.api.EpisodeDto
import com.wobok.bibilili.data.api.SeasonDto
import com.wobok.bibilili.data.network.BiliHeaderInterceptor
import com.wobok.bibilili.data.repo.PlaySource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as BibiliApp).container

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    /**
     * 视频直链必须带 Referer 与浏览器 UA，否则 CDN 直接 403。
     * ExoPlayer 自己发请求，不走 OkHttp 拦截器，所以要在这里单独配。
     */
    val player: ExoPlayer = ExoPlayer.Builder(app)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(
                DefaultHttpDataSource.Factory()
                    .setUserAgent(BiliHeaderInterceptor.BROWSER_UA)
                    .setDefaultRequestProperties(
                        mapOf("Referer" to BiliHeaderInterceptor.REFERER)
                    )
                    .setAllowCrossProtocolRedirects(true)
            )
        )
        .build()

    private var hideControlsJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            container.settings.playback.collect { settings ->
                _state.value = _state.value.copy(settings = settings)
            }
        }
    }

    fun load(seasonId: Long?, epId: Long?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            when (val result = container.catalogRepo.season(seasonId, epId)) {
                is ApiResult.Failure -> _state.value =
                    _state.value.copy(loading = false, error = "没能加载剧集信息")

                is ApiResult.Success -> {
                    val season = result.data
                    applySeason(season)
                    val first = season.episodes.firstOrNull()
                    if (first != null) playEpisode(first.id, first.cid)
                    else _state.value = _state.value.copy(loading = false)
                }
            }
        }
    }

    private fun applySeason(season: SeasonDto) {
        val episodes = season.episodes.map(EpisodeDto::toDomain)
        val tabs = SeasonGrouping.build(
            currentSeasonId = season.seasonId,
            currentSeasonTitle = season.seasonTitle.ifBlank { season.title },
            episodes = episodes,
            seasons = season.seasons.map {
                SeasonRef(it.seasonId, it.seasonTitle, it.cover, it.seasonId == season.seasonId)
            },
            sections = season.section.map {
                Section(it.id, it.title, it.episodes.map(EpisodeDto::toDomain))
            },
        )

        _state.value = _state.value.copy(
            title = season.title,
            cover = season.cover,
            description = season.evaluate,
            originName = season.originName,
            alias = season.alias,
            meta = buildList {
                val kind = listOfNotNull(
                    season.seasonTypeName(),
                    season.areas.firstOrNull()?.name,
                ).filter { it.isNotBlank() }
                if (kind.isNotEmpty()) add(kind.joinToString("　"))
                season.publish?.releaseDateShow?.takeIf(String::isNotBlank)?.let(::add)
                if (season.total > 0) add("全 ${season.total} 集")
            },
            score = season.rating?.score?.takeIf { it > 0 }?.toString().orEmpty(),
            scoreCount = season.rating?.count?.takeIf { it > 0 }?.let { "$it 人" }.orEmpty(),
            tabs = tabs,
            selectedTab = tabs.indexOfFirst { it.selected }.coerceAtLeast(0),
        )
    }

    fun playEpisode(epId: Long, cid: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, currentEpId = epId)
            val preferred = container.settings.playback.first().preferredQn

            when (val source = container.playbackRepo.localSource(epId, cid, preferred)) {
                is ApiResult.Failure -> _state.value =
                    _state.value.copy(loading = false, error = "这一集暂时放不了")

                is ApiResult.Success -> {
                    when (val data = source.data) {
                        is PlaySource.Dash -> {
                            // DASH 的 SegmentBase 没有现成的 .mpd 地址，
                            // 由 DashMpdBuilder 拼好落到缓存文件再交给 DashMediaSource。
                            // 用两个 ProgressiveMediaSource 拼音视频是错的。
                            val item = MediaItem.fromUri(data.manifestFile.toURI().toString())
                            val factory = DashMediaSource.Factory(
                                DefaultHttpDataSource.Factory()
                                    .setUserAgent(BiliHeaderInterceptor.BROWSER_UA)
                                    .setDefaultRequestProperties(
                                        mapOf("Referer" to BiliHeaderInterceptor.REFERER)
                                    )
                            )
                            player.setMediaSource(factory.createMediaSource(item))
                            _state.value = _state.value.copy(
                                currentQn = data.quality,
                                availableQn = data.available,
                            )
                        }

                        is PlaySource.Progressive -> {
                            player.setMediaItem(MediaItem.fromUri(data.url))
                            _state.value = _state.value.copy(currentQn = data.quality)
                        }
                    }
                    player.prepare()
                    player.playWhenReady = true
                    _state.value = _state.value.copy(loading = false, error = null)
                    scheduleHideControls()
                }
            }
        }
    }

    fun selectTab(index: Int) {
        _state.value = _state.value.copy(selectedTab = index)
    }

    fun seekBy(seconds: Int) {
        val target = (player.currentPosition + seconds * 1000L).coerceAtLeast(0L)
        player.seekTo(target)
        showControls()
    }

    /** 长按倍速：按下切到设定倍速，松手恢复 1.0。 */
    fun boost(on: Boolean) {
        val speed = if (on) _state.value.settings.longPressSpeed else 1.0f
        player.playbackParameters = PlaybackParameters(speed)
        _state.value = _state.value.copy(boosting = on)
    }

    fun setSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed)
        viewModelScope.launch { container.settings.setLongPressSpeed(speed) }
    }

    fun setSeekStep(seconds: Int) {
        viewModelScope.launch { container.settings.setSeekStep(seconds) }
    }

    fun setQuality(qn: Int) {
        viewModelScope.launch {
            container.settings.setPreferredQn(qn)
            val state = _state.value
            val episode = state.tabs.getOrNull(state.selectedTab)
                ?.episodes?.firstOrNull { it.epId == state.currentEpId }
            if (episode != null) playEpisode(episode.epId, episode.cid)
        }
    }

    fun toggleControls() {
        val visible = !_state.value.controlsVisible
        _state.value = _state.value.copy(controlsVisible = visible)
        if (visible) scheduleHideControls()
    }

    private fun showControls() {
        _state.value = _state.value.copy(controlsVisible = true)
        scheduleHideControls()
    }

    /** 3 秒无操作自动隐藏控件。 */
    private fun scheduleHideControls() {
        hideControlsJob?.cancel()
        hideControlsJob = viewModelScope.launch {
            delay(com.wobok.bibilili.ui.theme.PaperMotion.CONTROLS_AUTO_HIDE_MILLIS)
            _state.value = _state.value.copy(controlsVisible = false)
        }
    }

    fun toggleFullscreen() {
        _state.value = _state.value.copy(fullscreen = !_state.value.fullscreen)
    }

    fun setOverlay(overlay: PlayerOverlay) {
        val next = if (_state.value.overlay == overlay) PlayerOverlay.None else overlay
        _state.value = _state.value.copy(overlay = next, controlsVisible = true)
    }

    fun setDetail(show: Boolean) {
        _state.value = _state.value.copy(showDetail = show)
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}

private fun EpisodeDto.toDomain() = Episode(
    epId = id,
    cid = cid,
    aid = aid,
    shortTitle = title,
    longTitle = longTitle,
    cover = cover,
    durationMillis = duration,
    needsVip = badge.contains("会员"),
)

/** 接口没给类型名时按 `type` 反推，对不上就留空，不瞎猜。 */
private fun SeasonDto.seasonTypeName(): String = when (type) {
    1 -> "番剧"
    2 -> "电影"
    3 -> "纪录片"
    4 -> "国创"
    5 -> "电视剧"
    7 -> "综艺"
    else -> ""
}
