package com.wobok.bibilili.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.wobok.bibilili.data.local.FollowEntity
import com.wobok.bibilili.data.repo.FollowRepository
import com.wobok.bibilili.data.repo.HistoryRepository
import com.wobok.bibilili.data.repo.hasUnwatchedUpdate
import com.wobok.bibilili.ui.component.ContinueCard
import com.wobok.bibilili.ui.component.EmptyState
import com.wobok.bibilili.ui.component.PaperChip
import com.wobok.bibilili.ui.component.PosterCard
import com.wobok.bibilili.ui.component.SectionHeader
import com.wobok.bibilili.ui.theme.PaperTheme

/**
 * 首页三块：继续观看 / 我的收藏 / 追番更新。
 *
 * 「继续观看」的数据是历史过滤掉 UP 主视频之后剩下的影视和番剧，
 * 为空是允许的，给个简洁提示即可。
 */
@Composable
fun HomeScreen(
    historyRepo: HistoryRepository,
    followRepo: FollowRepository,
    loggedIn: Boolean,
    onOpenSeason: (Long) -> Unit,
    onGoChannel: () -> Unit,
    onGoLogin: () -> Unit,
    onGoHistory: () -> Unit,
    onGoFollowList: () -> Unit,
) {
    val continueWatching by historyRepo.observeContinueWatching().collectAsState(emptyList())
    val follows by followRepo.observe().collectAsState(emptyList())
    var favFilter by remember { mutableIntStateOf(0) }
    var syncError by remember { mutableIntStateOf(0) }

    LaunchedEffect(loggedIn) {
        if (!loggedIn) return@LaunchedEffect
        runCatching { historyRepo.sync() }.onFailure { syncError++ }
        runCatching { followRepo.sync() }
    }

    val colors = PaperTheme.colors
    val updates = remember(follows) { follows.filter(FollowEntity::hasUnwatchedUpdate).take(6) }
    val favourites = remember(follows, favFilter) {
        follows.filter { favFilter == 0 || it.followStatus == favFilter }.take(9)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            SectionHeader(
                title = "继续观看",
                action = "全部 ›",
                onAction = onGoHistory,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
            )
        }

        item {
            when {
                !loggedIn -> EmptyState("登录后显示你的观看记录", actionLabel = "去登录", onAction = onGoLogin)
                continueWatching.isEmpty() && syncError > 0 ->
                    EmptyState("没能加载观看记录", actionLabel = "重试") { syncError = 0 }
                continueWatching.isEmpty() ->
                    EmptyState("最近 30 天没有看过影视或番剧", actionLabel = "去影视看看", onAction = onGoChannel)
                else -> LazyRow(
                    contentPadding = PaddingValues(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(continueWatching, key = { it.seasonId }) { entry ->
                        ContinueCard(
                            title = entry.title,
                            subtitle = entry.episodeTitle.ifBlank { "继续观看" },
                            cover = entry.cover,
                            progress = entry.watchedRatio,
                            onClick = { onOpenSeason(entry.seasonId) },
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "我的收藏",
                action = "全部 ›",
                onAction = onGoFollowList,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 12.dp),
            )
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                items(FAV_FILTERS) { (value, label) ->
                    PaperChip(label, favFilter == value, { favFilter = value })
                }
            }
        }

        item {
            if (favourites.isEmpty()) {
                EmptyState(if (loggedIn) "还没有追的剧" else "登录后显示你的收藏")
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(favourites, key = { it.seasonId }) { item ->
                        PosterCard(
                            title = item.title,
                            subtitle = item.newEpIndexShow,
                            cover = item.cover,
                            score = item.score,
                            badge = "",
                            onClick = { onOpenSeason(item.seasonId) },
                            modifier = Modifier.size(width = 102.dp, height = 200.dp),
                        )
                    }
                }
            }
        }

        if (updates.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "追番更新",
                    action = "全部 ›",
                    onAction = onGoFollowList,
                    modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 12.dp),
                )
            }
            item {
                Column(
                    Modifier
                        .padding(horizontal = 22.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                ) {
                    updates.forEach { item ->
                        UpdateRow(item) { onOpenSeason(item.seasonId) }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateRow(item: FollowEntity, onClick: () -> Unit) {
    val colors = PaperTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = item.cover,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 44.dp, height = 62.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface2),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onBg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.newEpIndexShow,
                style = MaterialTheme.typography.bodySmall,
                color = colors.accent,
                modifier = Modifier.padding(top = 5.dp),
            )
            if (item.progressText.isNotBlank()) {
                Text(
                    text = item.progressText,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onBg2,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

private val FAV_FILTERS = listOf(
    0 to "全部",
    2 to "在看",
    1 to "想看",
    3 to "看过",
)
