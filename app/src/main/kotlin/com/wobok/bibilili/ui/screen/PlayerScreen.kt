package com.wobok.bibilili.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.wobok.bibilili.core.bili.play.PlayQuality
import com.wobok.bibilili.core.bili.play.PlaybackSettings
import com.wobok.bibilili.core.bili.play.formatSpeedBadge
import com.wobok.bibilili.core.bili.season.Episode
import com.wobok.bibilili.player.GestureLayer
import com.wobok.bibilili.player.PlayerOverlay
import com.wobok.bibilili.player.PlayerUiState
import com.wobok.bibilili.ui.theme.PaperColors
import com.wobok.bibilili.ui.theme.PaperMotion
import com.wobok.bibilili.ui.theme.PaperTheme

/** 覆盖层里用的文字色，压在画面上但不抢戏。 */
private val OverlayText = PaperColors.PlayerOnOverlay
private val OverlayTitle = PaperColors.PlayerOnOverlayTitle
private val OverlayAccent = PaperColors.PlayerAccent

@Composable
fun PlayerScreen(
    state: PlayerUiState,
    player: Player?,
    onBack: () -> Unit,
    onToggleControls: () -> Unit,
    onSeekBy: (Int) -> Unit,
    onBoost: (Boolean) -> Unit,
    onToggleFullscreen: () -> Unit,
    onOverlay: (PlayerOverlay) -> Unit,
    onSelectTab: (Int) -> Unit,
    onSelectEpisode: (Episode) -> Unit,
    onSelectQuality: (Int) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectSeekStep: (Int) -> Unit,
    onToggleDetail: (Boolean) -> Unit,
) {
    if (state.fullscreen) {
        FullscreenPlayer(
            state, player, onBack, onToggleControls, onSeekBy, onBoost,
            onOverlay, onSelectTab, onSelectEpisode, onSelectQuality,
            onSelectSpeed, onSelectSeekStep,
        )
    } else {
        PortraitPlayer(
            state, player, onBack, onToggleControls, onSeekBy, onBoost,
            onToggleFullscreen, onSelectEpisode, onSelectTab, onToggleDetail,
        )
    }
}

// ---------------------------------------------------------------- 竖屏

@Composable
private fun PortraitPlayer(
    state: PlayerUiState,
    player: Player?,
    onBack: () -> Unit,
    onToggleControls: () -> Unit,
    onSeekBy: (Int) -> Unit,
    onBoost: (Boolean) -> Unit,
    onToggleFullscreen: () -> Unit,
    onSelectEpisode: (Episode) -> Unit,
    onSelectTab: (Int) -> Unit,
    onToggleDetail: (Boolean) -> Unit,
) {
    val colors = PaperTheme.colors

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(Modifier.fillMaxSize()) {
            Surface(state, player, onBack, onToggleControls, onSeekBy, onBoost, onToggleFullscreen)

            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                item { SynopsisCard(state) { onToggleDetail(true) } }

                if (state.tabs.isNotEmpty()) {
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("选集", style = MaterialTheme.typography.titleLarge, color = colors.onBg)
                            Box(Modifier.weight(1f))
                            Text(
                                text = "共 ${state.tabs.size} 组",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onBg2,
                            )
                        }
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 22.dp),
                            horizontalArrangement = Arrangement.spacedBy(22.dp),
                        ) {
                            items(state.tabs.size) { index ->
                                val selected = index == state.selectedTab
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { onSelectTab(index) },
                                ) {
                                    Text(
                                        text = state.tabs[index].title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) colors.onBg else colors.onBg2,
                                    )
                                    Box(
                                        Modifier
                                            .padding(top = 6.dp)
                                            .width(16.dp)
                                            .height(2.dp)
                                            .background(if (selected) colors.primary else colors.bg)
                                    )
                                }
                            }
                        }
                    }
                    item {
                        val episodes = state.tabs.getOrNull(state.selectedTab)?.episodes.orEmpty()
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 22.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(top = 14.dp),
                        ) {
                            items(episodes, key = { it.epId }) { ep ->
                                EpisodeThumb(ep, ep.epId == state.currentEpId) { onSelectEpisode(ep) }
                            }
                        }
                    }
                }
            }
        }

        // 详情覆盖层：从下滑入，盖住播放器下方的整个区域，播放不中断
        AnimatedVisibility(
            visible = state.showDetail,
            enter = fadeIn(tween(PaperMotion.DURATION_DETAIL_SHEET)),
            exit = fadeOut(tween(PaperMotion.DURATION_DETAIL_SHEET)),
        ) {
            DetailOverlay(state) { onToggleDetail(false) }
        }
    }
}

@Composable
private fun SynopsisCard(state: PlayerUiState, onDetail: () -> Unit) {
    val colors = PaperTheme.colors
    Column(
        Modifier
            .padding(horizontal = 14.dp)
            .padding(top = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AsyncImage(
                model = state.cover,
                contentDescription = state.title,
                modifier = Modifier
                    .size(width = 84.dp, height = 118.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface2),
            )
            Column(Modifier.weight(1f)) {
                // 「详情 ›」与标题顶部对齐：字号差一倍时基线对齐会让小字掉到大字底部
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.onBg,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "详情 ›",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onBg2,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable(onClick = onDetail),
                    )
                }
                Row(Modifier.padding(top = 11.dp)) {
                    Column(Modifier.weight(1f)) {
                        state.meta.forEach {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onBg2,
                            )
                        }
                    }
                    if (state.score.isNotBlank()) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = state.score,
                                fontWeight = FontWeight.Black,
                                fontSize = 27.sp,
                                color = colors.accent,
                            )
                            Text(
                                text = state.scoreCount,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onBg3,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        if (state.originName.isNotBlank() || state.alias.isNotBlank()) {
            Box(
                Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.outline)
            )
            if (state.originName.isNotBlank()) MetaRow("原名", state.originName)
            if (state.alias.isNotBlank()) MetaRow("别名", state.alias)
        }
    }
}

@Composable
private fun MetaRow(key: String, value: String) {
    val colors = PaperTheme.colors
    Row(
        Modifier.padding(top = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(key, style = MaterialTheme.typography.bodySmall, color = colors.onBg3, modifier = Modifier.width(34.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = colors.onBg)
    }
}

@Composable
private fun EpisodeThumb(episode: Episode, current: Boolean, onClick: () -> Unit) {
    val colors = PaperTheme.colors
    Column(
        Modifier
            .width(150.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .size(width = 150.dp, height = 85.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surface2)
        ) {
            AsyncImage(
                model = episode.cover,
                contentDescription = episode.displayTitle(),
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            text = episode.displayTitle(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (current) colors.accent else colors.onBg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 9.dp),
        )
    }
}

@Composable
private fun DetailOverlay(state: PlayerUiState, onClose: () -> Unit) {
    val colors = PaperTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.12f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(colors.bg)
                .padding(horizontal = 22.dp)
        ) {
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .align(Alignment.CenterHorizontally)
                    .size(width = 34.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.outline2)
            )
            Text(
                text = "详情",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBg,
                modifier = Modifier.padding(top = 18.dp),
            )
            LazyColumn(Modifier.padding(top = 14.dp)) {
                item {
                    Text(
                        text = state.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onBg2,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 播放面

@Composable
private fun Surface(
    state: PlayerUiState,
    player: Player?,
    onBack: () -> Unit,
    onToggleControls: () -> Unit,
    onSeekBy: (Int) -> Unit,
    onBoost: (Boolean) -> Unit,
    onToggleFullscreen: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(PaperColors.PlayerBg)
    ) {
        if (player != null) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        useController = false
                        this.player = player
                    }
                },
                update = { it.player = player },
                modifier = Modifier.fillMaxSize(),
            )
        }

        GestureLayer(
            onSingleTap = onToggleControls,
            onDoubleTapLeft = { onSeekBy(-state.settings.seekStepSeconds) },
            onDoubleTapRight = { onSeekBy(state.settings.seekStepSeconds) },
            onLongPressStart = { onBoost(true) },
            onLongPressEnd = { onBoost(false) },
        )

        SpeedBadge(state)

        AnimatedVisibility(
            visible = state.controlsVisible,
            enter = fadeIn(tween(PaperMotion.DURATION_CONTROLS)),
            exit = fadeOut(tween(PaperMotion.DURATION_CONTROLS)),
        ) {
            PortraitControls(state, onBack, onToggleFullscreen)
        }
    }
}

/** 长按倍速浮层：居上偏上，不居中，不带 emoji。 */
@Composable
private fun SpeedBadge(state: PlayerUiState) {
    AnimatedVisibility(
        visible = state.boosting,
        enter = fadeIn(tween(PaperMotion.DURATION_SPEED_BADGE_IN)),
        exit = fadeOut(tween(PaperMotion.DURATION_SPEED_BADGE_OUT)),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Text(
                text = formatSpeedBadge(state.settings.longPressSpeed),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 56.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = 18.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun PortraitControls(
    state: PlayerUiState,
    onBack: () -> Unit,
    onToggleFullscreen: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)))
        )
        Text(
            text = "‹",
            color = Color.White,
            fontSize = 26.sp,
            modifier = Modifier
                .padding(start = 14.dp, top = 8.dp)
                .clickable(onClick = onBack),
        )
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Text("❙❙", color = Color.White, fontSize = 13.sp)
            Box(
                Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.28f))
            )
            Text("全屏", color = Color.White, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onToggleFullscreen))
        }
    }
}

// ---------------------------------------------------------------- 全屏

@Composable
private fun FullscreenPlayer(
    state: PlayerUiState,
    player: Player?,
    onBack: () -> Unit,
    onToggleControls: () -> Unit,
    onSeekBy: (Int) -> Unit,
    onBoost: (Boolean) -> Unit,
    onOverlay: (PlayerOverlay) -> Unit,
    onSelectTab: (Int) -> Unit,
    onSelectEpisode: (Episode) -> Unit,
    onSelectQuality: (Int) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectSeekStep: (Int) -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(PaperColors.PlayerBg)
    ) {
        if (player != null) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        useController = false
                        this.player = player
                    }
                },
                update = { it.player = player },
                modifier = Modifier.fillMaxSize(),
            )
        }

        GestureLayer(
            onSingleTap = onToggleControls,
            onDoubleTapLeft = { onSeekBy(-state.settings.seekStepSeconds) },
            onDoubleTapRight = { onSeekBy(state.settings.seekStepSeconds) },
            onLongPressStart = { onBoost(true) },
            onLongPressEnd = { onBoost(false) },
        )

        SpeedBadge(state)

        AnimatedVisibility(
            visible = state.controlsVisible,
            enter = fadeIn(tween(PaperMotion.DURATION_CONTROLS)),
            exit = fadeOut(tween(PaperMotion.DURATION_CONTROLS)),
        ) {
            FullscreenControls(state, onBack, onOverlay)
        }

        // 四个菜单都是覆盖层：浮在画面之上，画面不被挤窄，左缘渐隐
        AnimatedVisibility(
            visible = state.overlay != PlayerOverlay.None,
            enter = slideInHorizontally(tween(PaperMotion.DURATION_DRAWER)) { it },
            exit = slideOutHorizontally(tween(PaperMotion.DURATION_DRAWER)) { it },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            OverlayPanel(
                state = state,
                onSelectTab = onSelectTab,
                onSelectEpisode = onSelectEpisode,
                onSelectQuality = onSelectQuality,
                onSelectSpeed = onSelectSpeed,
                onSelectSeekStep = onSelectSeekStep,
            )
        }
    }
}

@Composable
private fun FullscreenControls(
    state: PlayerUiState,
    onBack: () -> Unit,
    onOverlay: (PlayerOverlay) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.66f), Color.Transparent)))
        )
        Row(
            Modifier.padding(start = 22.dp, top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Text("‹", color = Color.White, fontSize = 24.sp, modifier = Modifier.clickable(onClick = onBack))
            Text(
                text = listOf(state.title, state.episodeTitle).filter(String::isNotBlank).joinToString(" · "),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            Modifier
                .align(Alignment.TopEnd)
                .padding(end = 22.dp, top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("♡", color = Color.White, fontSize = 18.sp)
            Text("▭", color = Color.White, fontSize = 18.sp, modifier = Modifier.clickable { onOverlay(PlayerOverlay.Cast) })
            Text("⋮", color = Color.White, fontSize = 18.sp, modifier = Modifier.clickable { onOverlay(PlayerOverlay.Settings) })
        }

        // 三个入口居右，和右上角图标同侧
        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            OverlayEntry("选集", state.overlay == PlayerOverlay.Episodes) { onOverlay(PlayerOverlay.Episodes) }
            OverlayEntry("倍速", state.overlay == PlayerOverlay.Speed) { onOverlay(PlayerOverlay.Speed) }
            OverlayEntry(
                text = PlayQuality.from(state.currentQn)?.label ?: "清晰度",
                active = state.overlay == PlayerOverlay.Quality,
            ) { onOverlay(PlayerOverlay.Quality) }
        }
    }
}

@Composable
private fun OverlayEntry(text: String, active: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (active) OverlayAccent else Color.White.copy(alpha = 0.92f),
        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        fontSize = 13.sp,
        modifier = Modifier.clickable(onClick = onClick),
    )
}

/**
 * 覆盖层面板。
 *
 * 深色半透明 + 左缘渐隐，浮在画面上而不是把画面挤窄。
 * 文字用米白不用纯白，压在画面上不抢戏。
 */
@Composable
private fun OverlayPanel(
    state: PlayerUiState,
    onSelectTab: (Int) -> Unit,
    onSelectEpisode: (Episode) -> Unit,
    onSelectQuality: (Int) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectSeekStep: (Int) -> Unit,
) {
    val width = when (state.overlay) {
        PlayerOverlay.Episodes -> 320.dp
        PlayerOverlay.Speed -> 264.dp
        PlayerOverlay.Quality -> 288.dp
        else -> 304.dp
    }

    Column(
        Modifier
            .width(width)
            .fillMaxHeight()
            .background(
                Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.16f to Color(0xFF0A0E10).copy(alpha = 0.52f),
                    0.46f to Color(0xFF0A0E10).copy(alpha = 0.76f),
                    1f to Color(0xFF0A0E10).copy(alpha = 0.82f),
                )
            )
            .padding(start = 34.dp, end = 22.dp, top = 24.dp, bottom = 22.dp)
    ) {
        Text(
            text = when (state.overlay) {
                PlayerOverlay.Episodes -> "选集"
                PlayerOverlay.Speed -> "倍速"
                PlayerOverlay.Quality -> "清晰度"
                PlayerOverlay.Cast -> "投屏到"
                else -> "播放设置"
            },
            color = OverlayTitle,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )

        Box(Modifier.padding(top = 16.dp)) {
            when (state.overlay) {
                PlayerOverlay.Episodes -> EpisodesPanel(state, onSelectTab, onSelectEpisode)
                PlayerOverlay.Speed -> SpeedPanel(state, onSelectSpeed)
                PlayerOverlay.Quality -> QualityPanel(state, onSelectQuality)
                else -> SettingsPanel(state, onSelectSeekStep, onSelectSpeed)
            }
        }
    }
}

@Composable
private fun EpisodesPanel(
    state: PlayerUiState,
    onSelectTab: (Int) -> Unit,
    onSelectEpisode: (Episode) -> Unit,
) {
    Column {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(state.tabs.size) { index ->
                val selected = index == state.selectedTab
                Text(
                    text = state.tabs[index].title,
                    color = if (selected) Color(0xFF0E1416) else OverlayText.copy(alpha = 0.78f),
                    fontSize = 11.5f.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) OverlayAccent else Color.Transparent)
                        .clickable { onSelectTab(index) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        val episodes = state.tabs.getOrNull(state.selectedTab)?.episodes.orEmpty()
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 14.dp),
        ) {
            items(episodes, key = { it.epId }) { ep ->
                val current = ep.epId == state.currentEpId
                Box(
                    Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (current) OverlayAccent.copy(alpha = 0.18f)
                            else OverlayText.copy(alpha = 0.07f)
                        )
                        .clickable { onSelectEpisode(ep) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = ep.shortTitle.ifBlank { "?" },
                        color = if (current) OverlayAccent else OverlayText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedPanel(state: PlayerUiState, onSelect: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PlaybackSettings.SPEED_OPTIONS.forEach { speed ->
            val current = speed == 1.0f
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (current) OverlayAccent.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelect(speed) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (speed % 1f == 0f) "${speed.toInt()}.0X" else "${speed}X",
                    color = if (current) OverlayAccent else OverlayText,
                    fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
                if (speed == state.settings.longPressSpeed) {
                    Text("长按", color = OverlayText.copy(alpha = 0.5f), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun QualityPanel(state: PlayerUiState, onSelect: (Int) -> Unit) {
    val options = remember(state.availableQn) {
        state.availableQn.mapNotNull(PlayQuality::from).ifEmpty { PlayQuality.forLocalPlayback() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { quality ->
            val current = quality.qn == state.currentQn
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (current) OverlayAccent.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelect(quality.qn) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = quality.label,
                    color = if (current) OverlayAccent else OverlayText,
                    fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                )
                if (quality.needsVip) {
                    Text(
                        text = "大会员",
                        color = Color(0xFF0E1416),
                        fontSize = 9.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(OverlayAccent)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    state: PlayerUiState,
    onSelectSeekStep: (Int) -> Unit,
    onSelectSpeed: (Float) -> Unit,
) {
    Column {
        Text("双击快进步长", color = OverlayText.copy(alpha = 0.56f), fontSize = 11.sp)
        Row(
            Modifier.padding(top = 8.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            PlaybackSettings.SEEK_STEP_OPTIONS.forEach { step ->
                val current = step == state.settings.seekStepSeconds
                Text(
                    text = "${step}s",
                    color = if (current) OverlayAccent else OverlayText,
                    fontSize = 11.sp,
                    fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (current) OverlayAccent.copy(alpha = 0.18f) else Color.Transparent)
                        .clickable { onSelectSeekStep(step) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }

        Text("长按倍速", color = OverlayText.copy(alpha = 0.56f), fontSize = 11.sp)
        Row(
            Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            PlaybackSettings.LONG_PRESS_SPEED_OPTIONS.forEach { speed ->
                val current = speed == state.settings.longPressSpeed
                Text(
                    text = if (speed % 1f == 0f) "${speed.toInt()}.0X" else "${speed}X",
                    color = if (current) OverlayAccent else OverlayText,
                    fontSize = 11.sp,
                    fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (current) OverlayAccent.copy(alpha = 0.18f) else Color.Transparent)
                        .clickable { onSelectSpeed(speed) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        Text(
            text = "以上设置会被记住，并同时作用于竖屏播放页",
            color = OverlayText.copy(alpha = 0.52f),
            fontSize = 10.5f.sp,
            modifier = Modifier.padding(top = 18.dp),
        )
    }
}
