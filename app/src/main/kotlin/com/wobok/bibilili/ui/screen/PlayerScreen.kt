package com.wobok.bibilili.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import com.wobok.bibilili.core.bili.play.PlayQuality
import com.wobok.bibilili.core.bili.play.PlaybackSettings
import com.wobok.bibilili.core.bili.play.formatSpeedBadge
import com.wobok.bibilili.core.bili.season.Episode
import com.wobok.bibilili.player.GestureLayer
import com.wobok.bibilili.player.PlayerOverlay
import com.wobok.bibilili.player.PlayerUiState
import com.wobok.bibilili.player.PlayerViewModel
import com.wobok.bibilili.player.PreviewSprites
import com.wobok.bibilili.player.formatTime
import com.wobok.bibilili.ui.component.EmptyState
import com.wobok.bibilili.ui.theme.PaperColors
import com.wobok.bibilili.ui.theme.PaperMotion
import com.wobok.bibilili.ui.theme.PaperTheme
import kotlin.math.roundToInt

/** 覆盖层里用的文字色，压在画面上但不抢戏。 */
private val OverlayText = PaperColors.PlayerOnOverlay
private val OverlayTitle = PaperColors.PlayerOnOverlayTitle
private val OverlayAccent = PaperColors.PlayerAccent

/** 横向滑一整屏折算多少进度。两分钟是手感最稳的一档。 */
private const val SWIPE_SEEK_SPAN_MILLIS = 120_000f

@Composable
fun PlayerScreen(
    state: PlayerUiState,
    player: Player?,
    vm: PlayerViewModel,
    onExit: () -> Unit,
) {
    if (state.fullscreen) FullscreenPlayer(state, player, vm)
    else PortraitPlayer(state, player, vm, onExit)
}

// ---------------------------------------------------------------- 竖屏

@Composable
private fun PortraitPlayer(
    state: PlayerUiState,
    player: Player?,
    vm: PlayerViewModel,
    onExit: () -> Unit,
) {
    val colors = PaperTheme.colors

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            Surface(state, player, vm, onExit)

            val error = state.error
            if (error != null) {
                EmptyState(error, actionLabel = "返回") { onExit() }
                return@Column
            }
            if (state.loading && state.title.isBlank()) {
                EmptyState("正在加载剧集信息")
                return@Column
            }

            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                item { SynopsisCard(state) { vm.setDetail(true) } }

                if (state.tabs.isNotEmpty()) {
                    item { EpisodeHeader(state) }
                    item { EpisodeTabRow(state, vm::selectTab) }
                    item {
                        val episodes = state.tabs.getOrNull(state.selectedTab)?.episodes.orEmpty()
                        if (episodes.isEmpty()) {
                            EmptyState("正在取这一季的分集")
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 22.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(top = 14.dp),
                            ) {
                                items(episodes, key = { it.epId }) { ep ->
                                    EpisodeThumb(ep, ep.epId == state.currentEpId) { vm.playEpisode(ep) }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 详情从下往上滑出，不压半透明遮罩——那层灰看上去就是「多了一块阴影」
        AnimatedVisibility(
            visible = state.showDetail,
            enter = slideInVertically(tween(PaperMotion.DURATION_DETAIL_SHEET)) { it },
            exit = slideOutVertically(tween(PaperMotion.DURATION_DETAIL_SHEET)) { it },
        ) {
            DetailSheet(state) { vm.setDetail(false) }
        }
    }
}

@Composable
private fun EpisodeHeader(state: PlayerUiState) {
    val colors = PaperTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("选集", style = MaterialTheme.typography.titleLarge, color = colors.onBg)
        Spacer(Modifier.weight(1f))
        Text(
            text = "共 ${state.tabs.size} 组",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onBg2,
        )
    }
}

@Composable
private fun EpisodeTabRow(state: PlayerUiState, onSelect: (Int) -> Unit) {
    val colors = PaperTheme.colors
    LazyRow(
        contentPadding = PaddingValues(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        items(state.tabs.size) { index ->
            val selected = index == state.selectedTab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(index) },
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
            .clickable(onClick = onDetail)
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AsyncImage(
                model = state.cover,
                contentDescription = state.title,
                contentScale = ContentScale.Crop,
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
                        modifier = Modifier.padding(top = 4.dp),
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
                contentScale = ContentScale.Crop,
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

/**
 * 详情页。
 *
 * 下拉跟手：手指往下拖多少，整张纸就走多少；过半就放手关掉，没过半弹回去。
 * 用 `offset` 直接跟随位移，而不是做一个「拖过阈值再播放关闭动画」的两段式，
 * 后者手上会有一顿。
 */
@Composable
private fun DetailSheet(state: PlayerUiState, onClose: () -> Unit) {
    val colors = PaperTheme.colors
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var sheetHeight by remember { mutableIntStateOf(1) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .onSizeChanged { sheetHeight = it.height }
                .offset { IntOffset(0, dragOffset.roundToInt()) }
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(colors.bg)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragOffset > sheetHeight * 0.32f) onClose() else dragOffset = 0f
                        },
                        onDragCancel = { dragOffset = 0f },
                    ) { change, amount ->
                        change.consume()
                        dragOffset = (dragOffset + amount).coerceAtLeast(0f)
                    }
                }
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

            LazyColumn(
                contentPadding = PaddingValues(top = 18.dp, bottom = 40.dp),
            ) {
                item {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.onBg,
                    )
                }
                item {
                    Row(
                        Modifier.padding(top = 12.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column(Modifier.weight(1f)) {
                            state.meta.forEach {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onBg2,
                                )
                            }
                            if (state.originName.isNotBlank()) {
                                MetaRow("原名", state.originName)
                            }
                            if (state.alias.isNotBlank()) {
                                MetaRow("别名", state.alias)
                            }
                        }
                        if (state.score.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = state.score,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 30.sp,
                                    color = colors.accent,
                                )
                                Text(
                                    text = state.scoreCount,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onBg3,
                                )
                            }
                        }
                    }
                }
                if (state.styles.isNotEmpty()) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 16.dp),
                        ) {
                            items(state.styles.size) { i ->
                                Text(
                                    text = state.styles[i],
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onBg2,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colors.surface2)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
                item {
                    Text(
                        text = "简 介",
                        fontSize = 11.sp,
                        color = colors.onBg3,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
                item {
                    Text(
                        text = state.description.ifBlank { "这部还没有简介。" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onBg,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaRow(key: String, value: String) {
    val colors = PaperTheme.colors
    Row(
        Modifier.padding(top = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(key, style = MaterialTheme.typography.bodySmall, color = colors.onBg3, modifier = Modifier.width(34.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = colors.onBg)
    }
}

// ---------------------------------------------------------------- 画面

@Composable
private fun Surface(
    state: PlayerUiState,
    player: Player?,
    vm: PlayerViewModel,
    onExit: () -> Unit,
) {
    var widthPx by remember { mutableIntStateOf(1) }

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(PaperColors.PlayerBg)
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
    ) {
        VideoSurface(player)

        GestureLayer(
            enabled = true,
            onSingleTap = vm::toggleControls,
            onDoubleTapLeft = { vm.seekBy(-state.settings.seekStepSeconds) },
            onDoubleTapCenter = vm::togglePlay,
            onDoubleTapRight = { vm.seekBy(state.settings.seekStepSeconds) },
            onLongPressStart = { vm.boost(true) },
            onLongPressEnd = { vm.boost(false) },
            onDragStart = vm::startScrub,
            onDrag = { dx -> vm.scrubBy((dx / widthPx * SWIPE_SEEK_SPAN_MILLIS).toLong()) },
            onDragEnd = vm::endScrub,
        )

        SpeedBadge(state)
        SeekPreview(state, Modifier.align(Alignment.Center))

        AnimatedVisibility(
            visible = state.controlsVisible,
            enter = fadeIn(tween(PaperMotion.DURATION_CONTROLS)),
            exit = fadeOut(tween(PaperMotion.DURATION_CONTROLS)),
        ) {
            PortraitControls(state, vm, onExit)
        }
    }
}

@Composable
private fun VideoSurface(player: Player?) {
    if (player == null) return
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
private fun PortraitControls(state: PlayerUiState, vm: PlayerViewModel, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)))
        )
        PlayerIconButton(
            icon = Icons.Rounded.ArrowBackIosNew,
            description = "返回",
            modifier = Modifier.padding(start = 6.dp, top = 2.dp),
            onClick = onExit,
        )

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))))
                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerIconButton(
                icon = if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                description = if (state.playing) "暂停" else "播放",
                onClick = vm::togglePlay,
            )
            SeekBar(
                state = state,
                vm = vm,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )
            Text(
                text = "${formatTime(state.displayPositionMs)}/${formatTime(state.durationMs)}",
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            PlayerIconButton(Icons.Rounded.Fullscreen, "全屏", onClick = vm::toggleFullscreen)
        }
    }
}

// ---------------------------------------------------------------- 全屏

@Composable
private fun FullscreenPlayer(state: PlayerUiState, player: Player?, vm: PlayerViewModel) {
    var widthPx by remember { mutableIntStateOf(1) }

    Box(
        Modifier
            .fillMaxSize()
            .background(PaperColors.PlayerBg)
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
    ) {
        VideoSurface(player)

        // 菜单开着的时候整层不收事件：否则在倍速菜单上按住不放，速度会跟着乱跳
        GestureLayer(
            enabled = state.overlay == PlayerOverlay.None,
            onSingleTap = vm::toggleControls,
            onDoubleTapLeft = { vm.seekBy(-state.settings.seekStepSeconds) },
            onDoubleTapCenter = vm::togglePlay,
            onDoubleTapRight = { vm.seekBy(state.settings.seekStepSeconds) },
            onLongPressStart = { vm.boost(true) },
            onLongPressEnd = { vm.boost(false) },
            onDragStart = vm::startScrub,
            onDrag = { dx -> vm.scrubBy((dx / widthPx * SWIPE_SEEK_SPAN_MILLIS).toLong()) },
            onDragEnd = vm::endScrub,
        )

        SpeedBadge(state)
        SeekPreview(state, Modifier.align(Alignment.Center))

        AnimatedVisibility(
            visible = state.controlsVisible,
            enter = fadeIn(tween(PaperMotion.DURATION_CONTROLS)),
            exit = fadeOut(tween(PaperMotion.DURATION_CONTROLS)),
        ) {
            FullscreenControls(state, vm)
        }

        // 点画面其他地方就关菜单。这层只在菜单开着时存在，平时不挡手势。
        if (state.overlay != PlayerOverlay.None) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = vm::closeOverlay,
                    )
            )
        }

        AnimatedVisibility(
            visible = state.overlay != PlayerOverlay.None,
            enter = slideInHorizontally(tween(PaperMotion.DURATION_DRAWER)) { it },
            exit = slideOutHorizontally(tween(PaperMotion.DURATION_DRAWER)) { it },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            OverlayPanel(state, vm)
        }
    }
}

@Composable
private fun FullscreenControls(state: PlayerUiState, vm: PlayerViewModel) {
    // 横屏时刘海和手势条在左右两侧，只避让它们；顶部不留边距，
    // 否则标题会被推到画面中间去。
    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.displayCutout.union(WindowInsets.navigationBars))
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(88.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.66f), Color.Transparent)))
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerIconButton(Icons.Rounded.ArrowBackIosNew, "退出全屏", onClick = vm::toggleFullscreen)
            Text(
                text = listOf(state.title, state.episodeTitle).filter(String::isNotBlank).joinToString(" · "),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            PlayerIconButton(
                icon = if (state.favourited) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                description = if (state.favourited) "已收藏" else "收藏",
                tint = if (state.favourited) OverlayAccent else Color.White,
                onClick = vm::toggleFavourite,
            )
            PlayerIconButton(Icons.Rounded.Cast, "投屏") { vm.setOverlay(PlayerOverlay.Cast) }
            PlayerIconButton(Icons.Rounded.MoreVert, "播放设置") { vm.setOverlay(PlayerOverlay.Settings) }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.66f))))
                .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 10.dp),
        ) {
            Text(
                text = "${formatTime(state.displayPositionMs)}/${formatTime(state.durationMs)}",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            SeekBar(state, vm, Modifier.fillMaxWidth())
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerIconButton(
                    icon = if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    description = if (state.playing) "暂停" else "播放",
                    onClick = vm::togglePlay,
                )
                PlayerIconButton(Icons.Rounded.SkipNext, "下一集") { playNext(state, vm) }
                Spacer(Modifier.weight(1f))
                OverlayEntry("选集", state.overlay == PlayerOverlay.Episodes) {
                    vm.setOverlay(PlayerOverlay.Episodes)
                }
                Spacer(Modifier.width(26.dp))
                OverlayEntry("倍速", state.overlay == PlayerOverlay.Speed) {
                    vm.setOverlay(PlayerOverlay.Speed)
                }
                Spacer(Modifier.width(26.dp))
                OverlayEntry(
                    text = PlayQuality.from(state.currentQn)?.label ?: "清晰度",
                    active = state.overlay == PlayerOverlay.Quality,
                ) { vm.setOverlay(PlayerOverlay.Quality) }
            }
        }
    }
}

private fun playNext(state: PlayerUiState, vm: PlayerViewModel) {
    val episodes = state.tabs.getOrNull(state.selectedTab)?.episodes.orEmpty()
    val index = episodes.indexOfFirst { it.epId == state.currentEpId }
    episodes.getOrNull(index + 1)?.let(vm::playEpisode)
}

@Composable
private fun OverlayEntry(text: String, active: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (active) OverlayAccent else Color.White.copy(alpha = 0.92f),
        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        fontSize = 13.sp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    )
}

// ---------------------------------------------------------------- 进度条与预览

/**
 * 进度条。
 *
 * 轨道只有 3dp 高，但热区留到 22dp——3dp 的东西手指根本按不准。
 * 点一下是跳转，按住拖是连续调整，两种都会带出小窗预览。
 */
@Composable
private fun SeekBar(state: PlayerUiState, vm: PlayerViewModel, modifier: Modifier = Modifier) {
    val fraction = state.progressFraction
    val buffered = state.bufferedFraction

    Box(
        modifier
            .height(22.dp)
            .pointerInput(state.durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        vm.startScrub()
                        vm.scrubTo((offset.x / size.width * state.durationMs).toLong())
                    },
                    onDragEnd = { vm.endScrub() },
                    onDragCancel = { vm.endScrub() },
                ) { change, _ ->
                    change.consume()
                    vm.scrubTo((change.position.x / size.width * state.durationMs).toLong())
                }
            }
            .pointerInput(state.durationMs) {
                detectTapGestures { offset ->
                    vm.startScrub()
                    vm.scrubTo((offset.x / size.width * state.durationMs).toLong())
                    vm.endScrub()
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.26f))
        )
        Box(
            Modifier
                .fillMaxWidth(buffered)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.34f))
        )
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(OverlayAccent)
        )
        // 滑块跟着进度走。拖动时放大一圈，手上才知道自己抓住了东西。
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(1.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    Modifier
                        .size(if (state.scrubbing) 13.dp else 9.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                )
            }
        }
    }
}

/**
 * 拖动时的小窗预览。
 *
 * 画的是雪碧图里裁出来的一格，不是单独请求的一张图——
 * `x/player/videoshot` 一次给几百格，逐帧请求会把接口打爆。
 */
@Composable
private fun SeekPreview(state: PlayerUiState, modifier: Modifier = Modifier) {
    if (!state.scrubbing) return
    val frame: ImageBitmap? = state.previewFrame
    val cell: PreviewSprites.Cell? = state.previewCell
    val sprites = state.previewSprites

    Column(
        modifier.padding(bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (frame != null && cell != null && sprites != null) {
            Box(
                Modifier
                    .size(width = 168.dp, height = 96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val cellWidth = frame.width / sprites.xLen
                    val cellHeight = frame.height / sprites.yLen
                    if (cellWidth <= 0 || cellHeight <= 0) return@Canvas
                    drawImage(
                        image = frame,
                        srcOffset = IntOffset(cell.column * cellWidth, cell.row * cellHeight),
                        srcSize = IntSize(cellWidth, cellHeight),
                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                    )
                }
            }
        }
        Text(
            text = "${formatTime(state.scrubPositionMs)} / ${formatTime(state.durationMs)}",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.58f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

// ---------------------------------------------------------------- 覆盖层面板

/**
 * 覆盖层面板。
 *
 * 深色半透明 + 左缘渐隐，浮在画面上而不是把画面挤窄。
 * 内容一律可上下滚动——倍速有九档，固定高度会把 2.5X 和 3X 压在屏幕外面。
 * 往右一划就关，和它滑出来的方向是对称的。
 */
@Composable
private fun OverlayPanel(state: PlayerUiState, vm: PlayerViewModel) {
    val width = when (state.overlay) {
        PlayerOverlay.Episodes -> 330.dp
        PlayerOverlay.Speed -> 264.dp
        PlayerOverlay.Quality -> 288.dp
        else -> 310.dp
    }

    Column(
        Modifier
            .width(width)
            .fillMaxHeight()
            .background(
                Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.16f to Color(0xFF0A0E10).copy(alpha = 0.52f),
                    0.46f to Color(0xFF0A0E10).copy(alpha = 0.78f),
                    1f to Color(0xFF0A0E10).copy(alpha = 0.86f),
                )
            )
            .pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total > 90f) vm.closeOverlay() },
                ) { change, amount ->
                    change.consume()
                    total += amount
                }
            }
            .windowInsetsPadding(WindowInsets.displayCutout.union(WindowInsets.navigationBars))
            .padding(start = 34.dp, end = 22.dp, top = 22.dp, bottom = 18.dp)
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

        Box(Modifier.padding(top = 14.dp)) {
            when (state.overlay) {
                PlayerOverlay.Episodes -> EpisodesPanel(state, vm)
                PlayerOverlay.Speed -> SpeedPanel(state, vm)
                PlayerOverlay.Quality -> QualityPanel(state, vm)
                PlayerOverlay.Cast -> CastPanel(state, vm)
                else -> SettingsPanel(state, vm)
            }
        }
    }
}

@Composable
private fun EpisodesPanel(state: PlayerUiState, vm: PlayerViewModel) {
    Column {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(state.tabs.size) { index ->
                val selected = index == state.selectedTab
                Text(
                    text = state.tabs[index].title,
                    color = if (selected) Color(0xFF0E1416) else OverlayText.copy(alpha = 0.78f),
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) OverlayAccent else Color.Transparent)
                        .clickable { vm.selectTab(index) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        val episodes = state.tabs.getOrNull(state.selectedTab)?.episodes.orEmpty()
        if (episodes.isEmpty()) {
            Text(
                text = "正在取这一季的分集",
                color = OverlayText.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 18.dp),
            )
            return@Column
        }
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
                            if (current) OverlayAccent.copy(alpha = 0.22f)
                            else OverlayText.copy(alpha = 0.07f)
                        )
                        .clickable { vm.playEpisode(ep) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = ep.gridLabel(),
                        color = if (current) OverlayAccent else OverlayText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedPanel(state: PlayerUiState, vm: PlayerViewModel) {
    Column(
        Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PlaybackSettings.SPEED_OPTIONS.forEach { speed ->
            // 高亮看的是**当前播放速度**，不是「长按倍速」的设定值
            val current = speed == state.speed
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (current) OverlayAccent.copy(alpha = 0.22f) else Color.Transparent)
                    .clickable { vm.setSpeed(speed) }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = speedLabel(speed),
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
private fun QualityPanel(state: PlayerUiState, vm: PlayerViewModel) {
    val options: List<PlayQuality> = remember(state.availableQn) {
        val mapped = state.availableQn.mapNotNull { qn -> PlayQuality.from(qn) }
        mapped.ifEmpty { PlayQuality.forLocalPlayback() }
    }
    Column(
        Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { quality ->
            val current = quality.qn == state.currentQn
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (current) OverlayAccent.copy(alpha = 0.22f) else Color.Transparent)
                    .clickable { vm.setQuality(quality.qn) }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
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

/**
 * 投屏面板。
 *
 * 之前这里落到了「播放设置」分支上——点投屏弹出的是倍速调节，纯属分支漏写。
 */
@Composable
private fun CastPanel(state: PlayerUiState, vm: PlayerViewModel) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        val casting = state.castingTo
        if (casting != null) {
            Text("正在投到 $casting", color = OverlayAccent, fontSize = 13.sp)
            Text(
                text = "停止投屏",
                color = OverlayText,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(OverlayText.copy(alpha = 0.1f))
                    .clickable { vm.stopCast() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
            return@Column
        }

        when {
            state.castSearching -> Text(
                text = "正在找同一个 Wi-Fi 下的设备",
                color = OverlayText.copy(alpha = 0.66f),
                fontSize = 12.sp,
            )

            state.castDevices.isEmpty() -> Column {
                Text(
                    text = "没找到可投屏的设备",
                    color = OverlayText.copy(alpha = 0.66f),
                    fontSize = 12.sp,
                )
                Text(
                    text = "重新搜索",
                    color = OverlayAccent,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clickable { vm.searchCastDevices() },
                )
            }

            else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.castDevices.forEach { device ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { vm.castTo(device) }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = device.friendlyName,
                            color = OverlayText,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (device.looksLikeBiliTv) {
                            Text("TV 端", color = OverlayAccent, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        val error = state.castError
        if (error != null) {
            Text(
                text = error,
                color = OverlayAccent,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun SettingsPanel(state: PlayerUiState, vm: PlayerViewModel) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text("双击快进步长", color = OverlayText.copy(alpha = 0.56f), fontSize = 11.sp)
        Row(
            Modifier.padding(top = 8.dp, bottom = 16.dp),
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
                        .background(if (current) OverlayAccent.copy(alpha = 0.22f) else Color.Transparent)
                        .clickable { vm.setSeekStep(step) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
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
                    text = speedLabel(speed),
                    color = if (current) OverlayAccent else OverlayText,
                    fontSize = 11.sp,
                    fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (current) OverlayAccent.copy(alpha = 0.22f) else Color.Transparent)
                        .clickable { vm.setLongPressSpeed(speed) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }

        Text(
            text = "以上设置会被记住，并同时作用于竖屏播放页",
            color = OverlayText.copy(alpha = 0.52f),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 18.dp),
        )
    }
}

// ---------------------------------------------------------------- 小件

@Composable
private fun PlayerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(44.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(21.dp))
    }
}

private fun speedLabel(speed: Float): String =
    if (speed % 1f == 0f) "${speed.toInt()}.0X" else "${speed}X"
