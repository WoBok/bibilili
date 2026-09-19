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
import com.wobok.bibilili.data.repo.CatalogRepository
import com.wobok.bibilili.data.repo.ChannelEntry
import com.wobok.bibilili.data.repo.MediaCard
import com.wobok.bibilili.ui.component.EmptyState
import com.wobok.bibilili.ui.component.PaperChip
import com.wobok.bibilili.ui.component.PosterCard
import com.wobok.bibilili.ui.theme.PaperTheme

data class ChannelTab(val label: String, val seasonType: Int)

val MovieTabs = listOf(
    ChannelTab("电影", SeasonType.MOVIE),
    ChannelTab("电视剧", SeasonType.TV),
    ChannelTab("纪录片", SeasonType.DOCUMENTARY),
    ChannelTab("综艺", SeasonType.VARIETY),
)

val AnimeTabs = listOf(
    ChannelTab("番剧", SeasonType.BANGUMI),
    ChannelTab("国创", SeasonType.GUOCHUANG),
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
    repo: CatalogRepository,
    onOpenSeason: (Long) -> Unit,
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    var entryIndex by remember { mutableIntStateOf(0) }
    var cards by remember { mutableStateOf<List<MediaCard>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    val tab = tabs[tabIndex]
    val entries = remember(tab) { repo.entriesFor(tab.seasonType) }
    val entry = entries[entryIndex.coerceIn(entries.indices)]

    LaunchedEffect(tab.seasonType, entry.key) {
        loading = true
        failed = false
        when (val result = repo.channel(tab.seasonType, entry.kind, page = 1)) {
            is ApiResult.Success -> cards = result.data
            is ApiResult.Failure -> { cards = emptyList(); failed = true }
        }
        loading = false
    }

    val colors = PaperTheme.colors

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Row(
            Modifier.padding(start = 22.dp, end = 22.dp, top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
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

        when {
            loading -> EmptyState("正在加载")
            failed -> EmptyState("没能加载内容", "重试") { entryIndex = entryIndex }
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
