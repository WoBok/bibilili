package com.wobok.bibilili.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.wobok.bibilili.AppContainer
import com.wobok.bibilili.ui.theme.PaperTheme
import kotlinx.coroutines.launch

data class UserStats(
    val name: String = "",
    val face: String = "",
    val sign: String = "",
    val isVip: Boolean = false,
    val level: Int = 0,
    val vipDaysLeft: Int = 0,
    val bangumi: Int = 0,
    val cinema: Int = 0,
    val watchedCount: Int = 0,
    val watchedHours: Int = 0,
)

/**
 * 用户页。不放任何设置项——播放相关的设置都在全屏的三点菜单里，
 * 数据层是同一份 DataStore，改完两边都生效。
 */
@Composable
fun UserScreen(
    container: AppContainer,
    onLoggedOut: () -> Unit,
) {
    var stats by remember { mutableStateOf(UserStats()) }
    val scope = rememberCoroutineScopeCompat()

    LaunchedEffect(Unit) {
        val mid = container.credentialStore.current.value?.dedeUserId ?: return@LaunchedEffect

        runCatching { container.authApi.nav() }.getOrNull()?.data?.let { nav ->
            val daysLeft = nav.vip?.dueDate
                ?.takeIf { it > 0 }
                ?.let { ((it - System.currentTimeMillis()) / 86_400_000L).toInt().coerceAtLeast(0) }
                ?: 0
            stats = stats.copy(
                name = nav.uname,
                face = nav.face,
                isVip = nav.vipStatus == 1,
                level = nav.levelInfo?.currentLevel ?: 0,
                vipDaysLeft = daysLeft,
            )
        }

        // 一次请求同时拿到追番数与追剧数，不需要 WBI，也不用翻列表
        runCatching { container.biliApi.navNum(mid) }.getOrNull()?.data?.let {
            stats = stats.copy(bangumi = it.bangumi, cinema = it.cinema)
        }

        runCatching { container.biliApi.spaceInfo(mid) }.getOrNull()?.data?.let {
            stats = stats.copy(sign = it.sign)
        }

        // 后两项由本机观看记录统计，口径和官方 App 不同，界面上有标注
        val (count, seconds) = container.historyRepo.stats()
        stats = stats.copy(watchedCount = count, watchedHours = (seconds / 3600).toInt())
    }

    val colors = PaperTheme.colors

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.bg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = stats.face,
            contentDescription = stats.name,
            modifier = Modifier
                .padding(top = 74.dp)
                .size(112.dp)
                .clip(CircleShape)
                .background(colors.surface2),
        )

        Row(
            Modifier.padding(top = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                text = stats.name.ifBlank { "未登录" },
                fontWeight = FontWeight.Black,
                fontSize = 23.sp,
                color = colors.onBg,
            )
            if (stats.isVip) {
                Text(
                    text = "大会员",
                    fontSize = 9.5f.sp,
                    color = colors.bg,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.accent)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }

        Text(
            text = buildList {
                if (stats.level > 0) add("Lv.${stats.level}")
                if (stats.vipDaysLeft > 0) add("大会员剩余 ${stats.vipDaysLeft} 天")
            }.joinToString("　·　"),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onBg2,
            modifier = Modifier.padding(top = 11.dp),
        )

        Box(
            Modifier
                .padding(top = 22.dp)
                .width(26.dp)
                .height(2.dp)
                .background(colors.accent)
        )

        Column(
            Modifier
                .padding(top = 28.dp)
                .width(294.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .padding(vertical = 8.dp)
        ) {
            Row(Modifier.fillMaxWidth()) {
                StatCell("追番", stats.bangumi.toString(), "部", Modifier.weight(1f), false)
                StatCell("追剧", stats.cinema.toString(), "部", Modifier.weight(1f), false)
            }
            Row(Modifier.fillMaxWidth()) {
                StatCell("近 90 天看过", stats.watchedCount.toString(), "部", Modifier.weight(1f), true)
                StatCell("近 90 天观看", stats.watchedHours.toString(), "小时", Modifier.weight(1f), true)
            }
            Text(
                text = "下两项按本机观看记录统计",
                fontSize = 9.5f.sp,
                color = colors.onBg3,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 11.dp),
            )
        }

        if (stats.sign.isNotBlank()) {
            Column(
                Modifier
                    .padding(top = 16.dp)
                    .width(294.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .padding(18.dp)
            ) {
                Text("个 人 简 介", fontSize = 11.sp, color = colors.onBg3)
                Text(
                    text = stats.sign,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onBg,
                    modifier = Modifier.padding(top = 11.dp),
                )
            }
        }

        Box(Modifier.weight(1f))

        Text(
            text = "注销登录",
            style = MaterialTheme.typography.titleSmall,
            color = colors.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(bottom = 64.dp)
                .width(294.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, colors.accent, RoundedCornerShape(10.dp))
                .clickable {
                    scope.launch {
                        container.logout()
                        onLoggedOut()
                    }
                }
                .padding(vertical = 15.dp),
        )
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    local: Boolean,
) {
    val colors = PaperTheme.colors
    Column(
        modifier.padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = value,
                fontWeight = FontWeight.Black,
                fontSize = 25.sp,
                color = if (local) colors.accent else colors.onBg,
            )
            Text(unit, fontSize = 10.5f.sp, color = colors.onBg3, modifier = Modifier.padding(bottom = 3.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onBg2,
            modifier = Modifier.padding(top = 9.dp),
        )
    }
}

@Composable
private fun rememberCoroutineScopeCompat() = androidx.compose.runtime.rememberCoroutineScope()
