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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.wobok.bibilili.core.bili.error.ApiResult
import com.wobok.bibilili.data.repo.CatalogRepository
import com.wobok.bibilili.data.repo.MediaCard
import com.wobok.bibilili.ui.component.EmptyState
import com.wobok.bibilili.ui.theme.PaperTheme
import kotlinx.coroutines.delay

/**
 * 搜索。
 *
 * `media_ft` / `media_bangumi` 天然只返回剧集，UP 主视频不会混进来，
 * 所以不需要像历史那样再过滤一遍。
 */
@Composable
fun SearchScreen(
    repo: CatalogRepository,
    onOpenSeason: (Long) -> Unit,
) {
    var keyword by remember { mutableStateOf("") }
    var bangumi by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<MediaCard>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(keyword, bangumi) {
        if (keyword.isBlank()) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(300) // 输入防抖
        loading = true
        when (val r = repo.search(keyword, bangumi, page = 1)) {
            is ApiResult.Success -> results = r.data
            is ApiResult.Failure -> results = emptyList()
        }
        loading = false
    }

    val colors = PaperTheme.colors

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Box(
            Modifier
                .padding(horizontal = 22.dp, vertical = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surface2)
                .padding(horizontal = 13.dp, vertical = 11.dp)
        ) {
            BasicTextField(
                value = keyword,
                onValueChange = { keyword = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onBg),
                cursorBrush = SolidColor(colors.primary),
                modifier = Modifier.fillMaxWidth(),
            )
            if (keyword.isEmpty()) {
                Text(
                    text = "搜索影视或番剧",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onBg3,
                )
            }
        }

        Row(
            Modifier.padding(start = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            listOf(false to "影视", true to "番剧").forEach { (value, label) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { bangumi = value },
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (value == bangumi) FontWeight.Bold else FontWeight.Normal,
                        color = if (value == bangumi) colors.onBg else colors.onBg2,
                    )
                    Box(
                        Modifier
                            .padding(top = 6.dp)
                            .width(16.dp)
                            .height(2.dp)
                            .background(if (value == bangumi) colors.primary else colors.bg)
                    )
                }
            }
        }

        Box(
            Modifier
                .padding(horizontal = 22.dp)
                .padding(top = 14.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.outline)
        )

        when {
            keyword.isBlank() -> EmptyState("搜点什么")
            loading -> EmptyState("正在搜索")
            results.isEmpty() -> EmptyState("没有找到相关影视或番剧")
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(results, key = { it.seasonId }) { card ->
                    SearchRow(card) { onOpenSeason(card.seasonId) }
                }
            }
        }
    }
}

@Composable
private fun SearchRow(card: MediaCard, onClick: () -> Unit) {
    val colors = PaperTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        AsyncImage(
            model = card.cover,
            contentDescription = card.title,
            modifier = Modifier
                .size(width = 84.dp, height = 118.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface2),
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onBg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (card.score.isNotBlank()) {
                    Text(
                        text = card.score,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.accent,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            Text(
                text = card.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onBg2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 9.dp),
            )
        }
    }
}
