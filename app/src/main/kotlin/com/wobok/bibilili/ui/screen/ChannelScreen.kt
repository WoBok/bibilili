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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.size
import coil3.compose.AsyncImage
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wobok.bibilili.core.bili.error.ApiResult
import com.wobok.bibilili.data.api.SeasonType
import com.wobok.bibilili.AppContainer
import com.wobok.bibilili.data.api.TimelineDayDto
import com.wobok.bibilili.data.local.FollowEntity
import com.wobok.bibilili.data.repo.CatalogRepository
import com.wobok.bibilili.data.repo.ChannelEntry
import com.wobok.bibilili.data.repo.MediaCard
import com.wobok.bibilili.ui.component.EmptyState
import com.wobok.bibilili.ui.component.PaperChip
import com.wobok.bibilili.ui.component.PosterCard
import com.wobok.bibilili.ui.theme.PaperTheme

/**
 * 频道的二级 tab。
 * [seasonType] 为 0 表示这不是一个剧集索引页，而是时间表或我的追番。
 */
data class ChannelTab(val label: String, val seasonType: Int, val kind: Kind = Kind.INDEX) {
    enum class Kind { INDEX, TIMELINE, MY_FOLLOW }
}

val MovieTabs = listOf(
    ChannelTab("电影", SeasonType.MOVIE),
    ChannelTab("电视剧", SeasonType.TV),
    ChannelTab("纪录片", SeasonType.DOCUMENTARY),
    ChannelTab("综艺", SeasonType.VARIETY),
)

val AnimeTabs = listOf(
    ChannelTab("番剧", SeasonType.BANGUMI),
    ChannelTab("国创", SeasonType.GUOCHUANG),
    ChannelTab("时间表", SeasonType.BANGUMI, ChannelTab.Kind.TIMELINE),
    ChannelTab("我的追番", 0, ChannelTab.Kind.MY_FOLLOW),
)

/**
 * 影视 / 番剧频道。
 *
 * 入口一行用的是**我们自己定义的筛选预设**，全部由索引与榜单接口驱动，
 * 不依赖官方运营位——那些随运营变动，而且有的只能跳 H5。
 */
@Composable
fun ChannelScreen(
    tabs: List<ChannelTab>,
    container: AppContainer,
    onOpenSeason: (Long) -> Unit,
) {
    val repo = container.catalogRepo
    var tabIndex by remember { mutableIntStateOf(0) }
    var entryIndex by remember { mutableIntStateOf(0) }
    var reload by remember { mutableIntStateOf(0) }
    var cards by remember { mutableStateOf<List<MediaCard>>(emptyList()) }
    var timeline by remember { mutableStateOf<List<TimelineDayDto>>(emptyList()) }
    var official by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    val tab = tabs[tabIndex.coerceIn(tabs.indices)]
    val entries = remember(tab) { repo.entriesFor(tab.seasonType) }
    val entry = entries[entryIndex.coerceIn(entries.indices)]
    val follows by container.followRepo.observe().collectAsState(emptyList())
    val context = LocalContext.current

    LaunchedEffect(tab.label, entry.key, reload) {
        loading = true
        failed = false
        when (tab.kind) {
            ChannelTab.Kind.TIMELINE -> {
                when (val r = repo.timeline(tab.seasonType)) {
                    is ApiResult.Success -> timeline = r.data
                    is ApiResult.Failure -> { timeline = emptyList(); failed = true }
                }
            }

            ChannelTab.Kind.MY_FOLLOW -> Unit

            ChannelTab.Kind.INDEX -> {
                when (val r = repo.channel(tab.seasonType, entry.kind, page = 1)) {
                    is ApiResult.Success -> cards = r.data
                    is ApiResult.Failure -> { cards = emptyList(); failed = true }
                }
            }
        }
        loading = false
    }

    // 官方运营入口：取不到就整行不显示，不影响页面其余部分
    LaunchedEffect(tab.label) {
        official = if (tab.kind == ChannelTab.Kind.INDEX) {
            repo.officialEntries(tab.seasonType.asCinemaTabName())
        } else emptyList()
    }

    val colors = PaperTheme.colors

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Row(
            Modifier.padding(start = 22.dp, end = 22.dp, top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            tabs.forEachIndexed { index, item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        tabIndex = index
                        entryIndex = 0
                    },
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (index == tabIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (index == tabIndex) colors.onBg else colors.onBg2,
                    )
                    Box(
                        Modifier
                            .padding(top = 6.dp)
                            .width(16.dp)
                            .height(2.dp)
                            .background(if (index == tabIndex) colors.primary else colors.bg)
                    )
                }
            }
        }

        if (tab.kind == ChannelTab.Kind.INDEX) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 18.dp),
            ) {
                items(entries.size) { index ->
                    PaperChip(
                        text = entries[index].label,
                        selected = index == entryIndex,
                        onClick = { entryIndex = index },
                    )
                }
            }

            if (official.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    items(official.size) { index ->
                        val (title, link) = official[index]
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onBg2,
                            modifier = Modifier
                                .clip(RoundedCornerShape(15.dp))
                                .background(colors.surface2)
                                .clickable { context.openLink(link) }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }

        when {
            tab.kind == ChannelTab.Kind.MY_FOLLOW -> when {
                follows.isEmpty() -> EmptyState("还没有追的番")
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(follows, key = { it.seasonId }) { item ->
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

            loading -> EmptyState("正在加载")
            failed -> EmptyState("没能加载内容", actionLabel = "重试") { reload++ }

            tab.kind == ChannelTab.Kind.TIMELINE -> TimelineList(timeline, onOpenSeason)

            cards.isEmpty() -> EmptyState("这里还没有内容")

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(cards, key = { it.seasonId }) { card ->
                    PosterCard(
                        title = card.title,
                        subtitle = card.subtitle,
                        cover = card.cover,
                        score = card.score,
                        badge = card.badge,
                        onClick = { onOpenSeason(card.seasonId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineList(days: List<TimelineDayDto>, onOpenSeason: (Long) -> Unit) {
    val colors = PaperTheme.colors
    if (days.isEmpty()) {
        EmptyState("时间表暂时取不到")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(days.size) { index ->
            val day = days[index]
            Column {
                Text(
                    text = buildString {
                        if (day.isToday == 1) append("今天 · ")
                        append(day.date)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = if (day.isToday == 1) colors.accent else colors.onBg,
                )
                Column(
                    Modifier
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                ) {
                    day.episodes.forEach { ep ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenSeason(ep.seasonId) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = ep.pubTime,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onBg3,
                                modifier = Modifier.width(42.dp),
                            )
                            AsyncImage(
                                model = ep.cover,
                                contentDescription = ep.title,
                                modifier = Modifier
                                    .size(width = 40.dp, height = 56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surface2),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = ep.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.onBg,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = ep.pubIndex,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onBg2,
                                    modifier = Modifier.padding(top = 5.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 频道页模块接口按名字取，和 season_type 是两套编号。 */
private fun Int.asCinemaTabName(): String = when (this) {
    SeasonType.MOVIE -> "movie"
    SeasonType.TV -> "tv"
    SeasonType.DOCUMENTARY -> "documentary"
    SeasonType.VARIETY -> "variety"
    else -> "bangumi"
}

private fun android.content.Context.openLink(link: String) {
    val uri = android.net.Uri.parse(link)
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}
