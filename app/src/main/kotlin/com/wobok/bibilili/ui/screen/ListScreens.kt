package com.wobok.bibilili.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.wobok.bibilili.ui.component.EmptyState
import com.wobok.bibilili.ui.component.PaperChip
import com.wobok.bibilili.ui.component.PosterCard
import com.wobok.bibilili.ui.theme.PaperTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 二级页共用的返回栏。 */
@Composable
private fun PageBar(title: String, onBack: () -> Unit, trailing: String = "") {
    val colors = PaperTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 22.dp, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.ArrowBackIosNew, "返回", tint = colors.onBg, modifier = Modifier.size(18.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.onBg)
        Spacer(Modifier.weight(1f))
        if (trailing.isNotBlank()) {
            Text(trailing, style = MaterialTheme.typography.bodySmall, color = colors.onBg2)
        }
    }
}

/**
 * 全部观看记录。
 *
 * 只有影视和番剧——UP 主的视频在写进本地库之前就被 `history.business` 滤掉了。
 * 本地按需求保留三个月，所以这一页就是「最近三个月看过的剧集」。
 */
@Composable
fun HistoryScreen(
    repo: HistoryRepository,
    onBack: () -> Unit,
    onOpenSeason: (Long) -> Unit,
) {
    val entries by repo.observeAll().collectAsState(emptyList())
    val colors = PaperTheme.colors

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        PageBar("观看记录", onBack, if (entries.isEmpty()) "" else "${entries.size} 部")

        if (entries.isEmpty()) {
            EmptyState("最近三个月没有看过影视或番剧")
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(entries, key = { it.seasonId }) { entry ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSeason(entry.seasonId) },
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    Box(
                        Modifier
                            .size(width = 118.dp, height = 68.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surface2)
                    ) {
                        AsyncImage(
                            model = entry.cover,
                            contentDescription = entry.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(colors.outline2)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(entry.watchedRatio.coerceIn(0f, 1f))
                                    .height(2.dp)
                                    .background(colors.accent)
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onBg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (entry.episodeTitle.isNotBlank()) {
                            Text(
                                text = entry.episodeTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onBg2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                        Text(
                            text = formatViewAt(entry.viewAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onBg3,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 我的追番 / 追剧。首页的「我的收藏」和「追番更新」两个「全部」都落到这里。 */
@Composable
fun FollowScreen(
    repo: FollowRepository,
    onBack: () -> Unit,
    onOpenSeason: (Long) -> Unit,
) {
    var status by remember { mutableIntStateOf(0) }
    var type by remember { mutableIntStateOf(0) }
    val all by repo.observe().collectAsState(emptyList())
    val colors = PaperTheme.colors

    val shown: List<FollowEntity> = remember(all, status, type) {
        all.filter { (status == 0 || it.followStatus == status) && (type == 0 || it.type == type) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        PageBar("我的追番", onBack, if (shown.isEmpty()) "" else "${shown.size} 部")

        LazyRow(
            contentPadding = PaddingValues(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 6.dp),
        ) {
            items(TYPE_FILTERS) { (value, label) ->
                PaperChip(label, type == value, { type = value })
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
        ) {
            items(STATUS_FILTERS) { (value, label) ->
                PaperChip(label, status == value, { status = value })
            }
        }

        if (shown.isEmpty()) {
            EmptyState("这里还没有内容")
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(shown, key = { it.seasonId }) { item ->
                PosterCard(
                    title = item.title,
                    subtitle = item.newEpIndexShow,
                    cover = item.cover,
                    score = item.score,
                    badge = "",
                    onClick = { onOpenSeason(item.seasonId) },
                )
            }
        }
    }
}

private val TYPE_FILTERS = listOf(0 to "全部", 1 to "番剧", 2 to "影视")
private val STATUS_FILTERS = listOf(0 to "不限", 2 to "在看", 1 to "想看", 3 to "看过")

/** 今天的记录只显示时间，更早的带上日期。 */
private fun formatViewAt(seconds: Long): String {
    if (seconds <= 0) return ""
    val millis = seconds * 1000
    val now = System.currentTimeMillis()
    val pattern = if (now - millis < 24 * 60 * 60 * 1000L) "今天 HH:mm" else "M 月 d 日 HH:mm"
    return SimpleDateFormat(pattern, Locale.CHINA).format(Date(millis))
}
