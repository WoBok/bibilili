package com.wobok.bibilili.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.wobok.bibilili.ui.theme.PaperTheme

/** 区块标题：衬线 + 右侧「全部 ›」。 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = PaperTheme.colors.onBg)
        Box(Modifier.weight(1f))
        if (action != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.bodySmall,
                color = PaperTheme.colors.onBg2,
                modifier = Modifier.clickable(enabled = onAction != null) { onAction?.invoke() },
            )
        }
    }
}

/** 竖版海报卡片。 */
@Composable
fun PosterCard(
    title: String,
    subtitle: String,
    cover: String,
    score: String,
    badge: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(102f / 150f)
                .clip(RoundedCornerShape(12.dp))
                .background(PaperTheme.colors.surface2)
        ) {
            AsyncImage(
                model = cover,
                contentDescription = title,
                // 海报的比例和格子并不总是一致，默认的 Fit 会在上下留白，
                // 看上去就是「封面没占满格子」。一律裁切填满。
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (score.isNotBlank()) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = score,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                    )
                }
            }
            if (badge.isNotBlank()) {
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 9.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(
                            PaperTheme.colors.accent,
                            RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
                        )
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = PaperTheme.colors.onBg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 9.dp),
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = PaperTheme.colors.onBg2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

/** 继续观看的横向卡片，底部带进度条。 */
@Composable
fun ContinueCard(
    title: String,
    subtitle: String,
    cover: String,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .width(210.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PaperTheme.colors.surface)
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(118.dp)
                .background(PaperTheme.colors.surface2)
        ) {
            AsyncImage(
                model = cover,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.5f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(PaperTheme.colors.accent)
                )
            }
        }
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 11.dp, bottom = 13.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = PaperTheme.colors.onBg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = PaperTheme.colors.onBg2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** 芯片。选中态用主色实底。 */
@Composable
fun PaperChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PaperTheme.colors
    Box(
        modifier
            .clip(RoundedCornerShape(17.dp))
            .then(
                if (selected) Modifier.background(colors.primary)
                else Modifier.border(BorderStroke(1.dp, colors.outline2), RoundedCornerShape(17.dp))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.bg else colors.onBg,
        )
    }
}

/**
 * 空态：一行文案 + 一个操作。
 *
 * 原先文案上方有个细线方框当插画位，实际看上去就是「凭空多出来一个框」，去掉了。
 * 「继续观看」为空是允许的，给个简洁提示就好。
 */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = PaperTheme.colors.onBg2,
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = PaperTheme.colors.primary,
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}

/** 顶栏渐隐，让内容滚动时自然淡出。 */
@Composable
fun BottomFade(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, PaperTheme.colors.bg)
                )
            )
    )
}
