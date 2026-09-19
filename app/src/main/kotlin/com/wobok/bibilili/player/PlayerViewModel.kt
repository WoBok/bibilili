package com.wobok.bibilili.player

import android.app.Application
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.dash.DefaultDashChunkSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.wobok.bibilili.BibiliApp
import com.wobok.bibilili.cast.CastDevice
import com.wobok.bibilili.cast.DlnaDiscovery
import com.wobok.bibilili.core.bili.error.ApiResult
import com.wobok.bibilili.core.bili.error.BiliError
import com.wobok.bibilili.core.bili.season.Episode
import com.wobok.bibilili.core.bili.season.EpisodeTab
import com.wobok.bibilili.core.bili.season.Section
import com.wobok.bibilili.core.bili.season.SeasonGrouping
import com.wobok.bibilili.core.bili.season.SeasonRef
import com.wobok.bibilili.data.api.EpisodeDto
import com.wobok.bibilili.data.api.SeasonDto
import com.wobok.bibilili.data.network.BiliHeaderInterceptor
import com.wobok.bibilili.data.repo.PlaySource
import com.wobok.bibilili.data.repo.asHttps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as BibiliApp).container

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    /**
     * 视频直链必须带 Referer 与浏览器 UA，否则 CDN 直接 403。
     * ExoPlayer 自己发请求，不走 OkHttp 拦截器，所以要在这里单独配。
     */
    private val httpFactory = DefaultHttpDataSource.Factory()
        .setUserAgent(BiliHeaderInterceptor.BROWSER_UA)
        .setDefaultRequestProperties(mapOf("Referer" to BiliHeaderInterceptor.REFERER))
        .setAllowCrossProtocolRedirects(true)

    val player: ExoPlayer = ExoPlayer.Builder(app)
        .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
        .build()

    private var hideControlsJob: Job? = null
    private var previewJob: Job? = null

    /** 当前这一集的完整信息，上报进度和取预览图都要用到。 */
    private var currentEpisode: Episode? = null

    /** 按图片地址缓存解码好的雪碧图。一集通常只有几张。 */
    private val spriteCache = LinkedHashMap<String, ImageBitmap>()

    private var lastRecordedSecond = -1

    init {
        viewModelScope.launch {
            container.settings.playback.collect { settings ->
                _state.value = _state.value.copy(settings = settings)
            }
        }

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(playing = isPlaying)
            }

            override fun onPlayerError(error: PlaybackException) {
                // 播放地址是有时效的，挂到后台再回来经常就失效了。
                // 先静默重取一次，真取不到再把错误摆到界面上。
                retryCurrentEpisode(silent = true)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) onEpisodeEnded()
            }
        })

        // 播放位置没有回调，只能轮询。400ms 够进度条看上去连续，又不至于空转。
        viewModelScope.launch {
            while (isActive) {
                val duration = player.duration.takeIf { it > 0 } ?: 0L
                val position = player.currentPosition
                _state.value = _state.value.copy(
                    positionMs = position,
                    durationMs = duration,
                    bufferedMs = player.bufferedPosition,
                    playing = player.isPlaying,
                )
                maybeRecordProgress(position, duration)
                delay(POSITION_POLL_MILLIS)
            }
        }
    }

    // ------------------------------------------------------------ 载入

    fun load(seasonId: Long?, epId: Long?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            when (val result = container.catalogRepo.season(seasonId, epId)) {
                is ApiResult.Failure -> _state.value = _state.value.copy(
                    loading = false,
                    error = result.error.readable("没能加载剧集信息"),
                )

                is ApiResult.Success -> {
                    val season = result.data
                    applySeason(season)

                    // 续播点：服务端的最准（换设备也对得上），其次本地库
                    val serverProgress = season.userStatus?.progress
                    val local = container.historyRepo.resumePoint(season.seasonId)
                    val episodes = season.episodes.map(EpisodeDto::toDomain)

                    val resumeEpId = serverProgress?.lastEpId?.takeIf { it > 0 }
                        ?: local?.epId?.takeIf { it > 0 }
                    val resumeSeconds = serverProgress?.lastTime?.takeIf { it > 0 }
                        ?: local?.progressSeconds?.toLong()?.takeIf { it > 0 }
                        ?: 0L

                    val target = episodes.firstOrNull { it.epId == resumeEpId }
                        ?: episodes.firstOrNull()

                    if (target == null) {
                        _state.value = _state.value.copy(loading = false)
                    } else {
                        val startMs =
                            if (target.epId == resumeEpId) resumeSeconds * 1000 else 0L
                        playEpisode(target, startMs)
                    }
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
                SeasonRef(it.seasonId, it.seasonTitle, it.cover.asHttps(), it.seasonId == season.seasonId)
            },
            sections = season.section.map {
                Section(it.id, it.title, it.episodes.map(EpisodeDto::toDomain))
            },
        )

        _state.value = _state.value.copy(
            seasonId = season.seasonId,
            title = season.title,
            cover = season.cover.asHttps(),
            description = season.evaluate,
            originName = season.originName,
            alias = season.alias,
            styles = season.styles,
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
            favourited = season.userStatus?.follow == 1,
        )
    }

    fun playEpisode(episode: Episode, startMs: Long = 0) {
        currentEpisode = episode
        lastRecordedSecond = -1
        viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = true,
                error = null,
                currentEpId = episode.epId,
                episodeTitle = episode.displayTitle(),
                previewSprites = null,
                previewFrame = null,
            )
            val preferred = container.settings.playback.first().preferredQn

            when (val source = container.playbackRepo.localSource(episode.epId, episode.cid, preferred)) {
                is ApiResult.Failure -> _state.value = _state.value.copy(
                    loading = false,
                    error = source.error.readable("这一集暂时放不了"),
                )

                is ApiResult.Success -> {
                    applySource(source.data)
                    if (startMs > 0) player.seekTo(startMs)
                    player.prepare()
                    player.playWhenReady = true
                    _state.value = _state.value.copy(loading = false, error = null)
                    scheduleHideControls()
                    loadPreviewSprites(episode)
                    recordProgress(startMs / 1000, force = true)
                }
            }
        }
    }

    private fun applySource(data: PlaySource) {
        when (data) {
            is PlaySource.Dash -> {
                // 拼好的 MPD 落在缓存文件里，是个 `file://` 地址。
                // manifest 和分片必须用**两个**数据源：分片走 HTTP（要带 Referer），
                // manifest 走 DefaultDataSource 才读得了本地文件——
                // 早先两者共用 HTTP 工厂，结果每一部 DASH 片源都在打开 manifest 时就失败了。
                val factory = DashMediaSource.Factory(
                    DefaultDashChunkSource.Factory(httpFactory),
                    DefaultDataSource.Factory(getApplication<Application>(), httpFactory),
                )
                val item = MediaItem.fromUri(data.manifestFile.toURI().toString())
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
    }

    /** 播放出错或从后台回来时，按当前位置重新取一次播放地址。 */
    private fun retryCurrentEpisode(silent: Boolean) {
        val episode = currentEpisode ?: return
        val resumeAt = player.currentPosition
        viewModelScope.launch {
            val preferred = container.settings.playback.first().preferredQn
            when (val source = container.playbackRepo.localSource(episode.epId, episode.cid, preferred)) {
                is ApiResult.Success -> {
                    applySource(source.data)
                    player.seekTo(resumeAt)
                    player.prepare()
                    player.playWhenReady = true
                    _state.value = _state.value.copy(error = null)
                }

                is ApiResult.Failure -> if (!silent) {
                    _state.value = _state.value.copy(error = source.error.readable("这一集暂时放不了"))
                }
            }
        }
    }

    private fun onEpisodeEnded() {
        recordProgress(-1, force = true)
        if (!_state.value.settings.autoPlayNext) return
        val state = _state.value
        val episodes = state.tabs.getOrNull(state.selectedTab)?.episodes.orEmpty()
        val index = episodes.indexOfFirst { it.epId == state.currentEpId }
        val next = episodes.getOrNull(index + 1) ?: return
        playEpisode(next)
    }

    // ------------------------------------------------------------ 选集

    /**
     * 切 tab。跨季的 tab 只带着 season_id，分集要点开才去拉——
     * 不然一部有八季的剧一进来就是八个请求。
     */
    fun selectTab(index: Int) {
        val tab = _state.value.tabs.getOrNull(index) ?: return
        _state.value = _state.value.copy(selectedTab = index)
        if (tab.isLoaded) return

        val seasonId = tab.seasonId ?: return
        viewModelScope.launch {
            val result = container.catalogRepo.season(seasonId = seasonId)
            if (result !is ApiResult.Success) return@launch
            val episodes = result.data.episodes.map(EpisodeDto::toDomain)
            _state.value = _state.value.copy(
                tabs = _state.value.tabs.map {
                    if (it.key == tab.key) it.copy(episodes = episodes) else it
                }
            )
        }
    }

    // ------------------------------------------------------------ 播放控制

    fun togglePlay() {
        if (player.isPlaying) player.pause() else player.play()
        showControls()
    }

    fun seekBy(seconds: Int) {
        val target = (player.currentPosition + seconds * 1000L)
            .coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(target)
        showControls()
    }

    fun seekTo(millis: Long) {
        player.seekTo(millis.coerceIn(0L, player.duration.coerceAtLeast(0L)))
        showControls()
    }

    /** 开始拖动：进度条与左右滑动共用同一套状态，行为完全一致。 */
    fun startScrub() {
        hideControlsJob?.cancel()
        _state.value = _state.value.copy(
            scrubbing = true,
            scrubPositionMs = player.currentPosition,
            controlsVisible = true,
        )
    }

    fun scrubTo(millis: Long) {
        val duration = _state.value.durationMs.coerceAtLeast(0L)
        val target = millis.coerceIn(0L, duration)
        _state.value = _state.value.copy(scrubPositionMs = target)
        updatePreviewFrame(target)
    }

    fun scrubBy(deltaMillis: Long) = scrubTo(_state.value.scrubPositionMs + deltaMillis)

    fun endScrub() {
        val target = _state.value.scrubPositionMs
        _state.value = _state.value.copy(scrubbing = false, previewFrame = null, previewCell = null)
        player.seekTo(target)
        recordProgress(target / 1000, force = true)
        scheduleHideControls()
    }

    /** 长按倍速：按下切到设定倍速，松手恢复原速。 */
    fun boost(on: Boolean) {
        val state = _state.value
        val speed = if (on) state.settings.longPressSpeed else state.speed
        player.playbackParameters = PlaybackParameters(speed)
        _state.value = state.copy(boosting = on)
    }

    /** 倍速菜单选的是**当前播放速度**，和「长按倍速」是两回事。 */
    fun setSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed)
        _state.value = _state.value.copy(speed = speed)
    }

    fun setLongPressSpeed(speed: Float) {
        viewModelScope.launch { container.settings.setLongPressSpeed(speed) }
    }

    fun setSeekStep(seconds: Int) {
        viewModelScope.launch { container.settings.setSeekStep(seconds) }
    }

    fun setQuality(qn: Int) {
        viewModelScope.launch {
            container.settings.setPreferredQn(qn)
            val episode = currentEpisode ?: return@launch
            playEpisode(episode, player.currentPosition)
        }
    }

    // ------------------------------------------------------------ 控件与覆盖层

    fun toggleControls() {
        val visible = !_state.value.controlsVisible
        _state.value = _state.value.copy(controlsVisible = visible)
        if (visible) scheduleHideControls()
    }

    private fun showControls() {
        _state.value = _state.value.copy(controlsVisible = true)
        scheduleHideControls()
    }

    /** 无操作自动隐藏控件。覆盖层开着的时候不收，否则菜单会自己消失。 */
    private fun scheduleHideControls() {
        hideControlsJob?.cancel()
        hideControlsJob = viewModelScope.launch {
            delay(com.wobok.bibilili.ui.theme.PaperMotion.CONTROLS_AUTO_HIDE_MILLIS)
            if (_state.value.overlay == PlayerOverlay.None && !_state.value.scrubbing) {
                _state.value = _state.value.copy(controlsVisible = false)
            }
        }
    }

    fun toggleFullscreen() {
        _state.value = _state.value.copy(fullscreen = !_state.value.fullscreen, overlay = PlayerOverlay.None)
    }

    fun setOverlay(overlay: PlayerOverlay) {
        val next = if (_state.value.overlay == overlay) PlayerOverlay.None else overlay
        _state.value = _state.value.copy(overlay = next, controlsVisible = true)
        if (next == PlayerOverlay.Cast) searchCastDevices()
        if (next == PlayerOverlay.None) scheduleHideControls()
    }

    fun closeOverlay() {
        if (_state.value.overlay == PlayerOverlay.None) return
        _state.value = _state.value.copy(overlay = PlayerOverlay.None)
        scheduleHideControls()
    }

    fun setDetail(show: Boolean) {
        _state.value = _state.value.copy(showDetail = show)
    }

    // ------------------------------------------------------------ 收藏

    fun toggleFavourite() {
        val state = _state.value
        val seasonId = state.seasonId.takeIf { it > 0 } ?: return
        val target = !state.favourited
        // 先改界面，失败再翻回来——网络往返几百毫秒，星星不该卡在那儿
        _state.value = state.copy(favourited = target)
        viewModelScope.launch {
            val result = container.followRepo.setFollowed(seasonId, target)
            if (result is ApiResult.Failure) {
                _state.value = _state.value.copy(favourited = !target)
            }
        }
    }

    // ------------------------------------------------------------ 投屏

    fun searchCastDevices() {
        if (_state.value.castSearching) return
        _state.value = _state.value.copy(castSearching = true, castError = null)
        viewModelScope.launch {
            val devices = runCatching { DlnaDiscovery.search() }.getOrDefault(emptyList())
            _state.value = _state.value.copy(castDevices = devices, castSearching = false)
        }
    }

    fun castTo(device: CastDevice) {
        val episode = currentEpisode ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(castError = null)
            when (val source = container.playbackRepo.castSource(episode.epId, episode.cid)) {
                is ApiResult.Failure -> _state.value =
                    _state.value.copy(castError = source.error.readable("没有可投屏的片源"))

                is ApiResult.Success -> {
                    val title = listOf(_state.value.title, episode.displayTitle())
                        .filter(String::isNotBlank).joinToString(" · ")
                    val result = container.dlna.play(device, source.data.url, title)
                    if (result.isSuccess) {
                        player.pause()
                        _state.value = _state.value.copy(castingTo = device.friendlyName)
                    } else {
                        _state.value = _state.value.copy(castError = "这台设备没有接受投屏")
                    }
                }
            }
        }
    }

    fun stopCast() {
        val name = _state.value.castingTo ?: return
        val device = _state.value.castDevices.firstOrNull { it.friendlyName == name }
        _state.value = _state.value.copy(castingTo = null)
        if (device != null) viewModelScope.launch { container.dlna.stop(device) }
    }

    // ------------------------------------------------------------ 小窗预览

    private fun loadPreviewSprites(episode: Episode) {
        previewJob?.cancel()
        if (episode.aid <= 0 || episode.cid <= 0) return
        previewJob = viewModelScope.launch {
            val dto = runCatching { container.biliApi.videoShot(episode.aid, episode.cid) }
                .getOrNull()?.payload ?: return@launch
            if (dto.image.isEmpty() || dto.xLen <= 0 || dto.yLen <= 0) return@launch
            _state.value = _state.value.copy(
                previewSprites = PreviewSprites(
                    images = dto.image.map(String::asHttps),
                    secondsIndex = dto.index,
                    xLen = dto.xLen,
                    yLen = dto.yLen,
                )
            )
        }
    }

    private fun updatePreviewFrame(positionMs: Long) {
        val sprites = _state.value.previewSprites ?: return
        val cell = sprites.cellAt(positionMs / 1000, _state.value.durationMs / 1000) ?: return
        if (cell == _state.value.previewCell) return

        _state.value = _state.value.copy(previewCell = cell)
        spriteCache[cell.imageUrl]?.let {
            _state.value = _state.value.copy(previewFrame = it)
            return
        }
        viewModelScope.launch {
            val bitmap = fetchSprite(cell.imageUrl) ?: return@launch
            spriteCache[cell.imageUrl] = bitmap
            while (spriteCache.size > SPRITE_CACHE_SIZE) {
                spriteCache.remove(spriteCache.keys.first())
            }
            if (_state.value.previewCell?.imageUrl == cell.imageUrl) {
                _state.value = _state.value.copy(previewFrame = bitmap)
            }
        }
    }

    /**
     * 雪碧图直接用图片客户端拉下来自己解码。
     * 走 Coil 反而绕：需要的是能按像素区间裁的原图，不是一个绘制好的 Painter。
     */
    private suspend fun fetchSprite(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).build()
            container.imageOkHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.byteStream()?.let(BitmapFactory::decodeStream)?.asImageBitmap()
            }
        }.getOrNull()
    }

    // ------------------------------------------------------------ 生命周期

    /** 切到后台就暂停。这是需求明确要求的，也省流量。 */
    fun onEnterBackground() {
        if (player.isPlaying) player.pause()
        recordProgress(player.currentPosition / 1000, force = true)
    }

    /**
     * 回到前台。
     *
     * 播放地址带时效，在后台待久了回来就是 IDLE 或直接报错，
     * 这时必须重新取地址，光 `prepare()` 是救不回来的。
     */
    fun onEnterForeground() {
        if (currentEpisode == null) return
        if (player.playbackState == Player.STATE_IDLE || player.playerError != null) {
            retryCurrentEpisode(silent = false)
        }
    }

    // ------------------------------------------------------------ 进度记录

    private fun maybeRecordProgress(positionMs: Long, durationMs: Long) {
        if (durationMs <= 0 || !player.isPlaying) return
        val second = (positionMs / 1000).toInt()
        if (lastRecordedSecond >= 0 && second - lastRecordedSecond < RECORD_INTERVAL_SECONDS) return
        recordProgress(second.toLong(), force = false)
    }

    private fun recordProgress(seconds: Long, force: Boolean) {
        val episode = currentEpisode ?: return
        val state = _state.value
        if (state.seasonId <= 0) return
        if (!force && seconds.toInt() == lastRecordedSecond) return
        lastRecordedSecond = seconds.toInt()

        viewModelScope.launch {
            container.historyRepo.record(
                seasonId = state.seasonId,
                epId = episode.epId,
                cid = episode.cid,
                aid = episode.aid,
                title = state.title,
                episodeTitle = episode.displayTitle(),
                cover = episode.cover.ifBlank { state.cover },
                progressSeconds = seconds.toInt(),
                durationSeconds = (state.durationMs / 1000).toInt()
                    .takeIf { it > 0 } ?: (episode.durationMillis / 1000).toInt(),
            )
        }
    }

    override fun onCleared() {
        recordProgress(player.currentPosition / 1000, force = true)
        player.release()
        super.onCleared()
    }

    private companion object {
        const val POSITION_POLL_MILLIS = 400L
        const val RECORD_INTERVAL_SECONDS = 15
        const val SPRITE_CACHE_SIZE = 3
    }
}

private fun BiliError.readable(fallback: String): String = when (this) {
    is BiliError.Api -> "$fallback（${code}）"
    is BiliError.Network -> "网络不通"
    is BiliError.Malformed -> fallback
    BiliError.NotLoggedIn -> "需要先登录"
}

private fun EpisodeDto.toDomain() = Episode(
    epId = id,
    cid = cid,
    aid = aid,
    shortTitle = title,
    longTitle = longTitle,
    cover = cover.asHttps(),
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
