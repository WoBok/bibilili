package com.wobok.bibilili.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * 素笺的字体分工：标题衬线（宋体），正文无衬线（黑体）。
 *
 * 系统自带的 `FontFamily.Serif` 在中文环境下会落到思源宋体/Noto Serif CJK，
 * 效果够用；要完全可控就得打包字体文件，但那会让 APK 大出十几兆，
 * 对一个自用 App 不划算。
 */
val SerifCn = FontFamily.Serif
val SansCn = FontFamily.SansSerif

val PaperTypography = Typography(
    // 页面大标题：首页 / 影视 / 番剧 这一行
    headlineMedium = TextStyle(
        fontFamily = SerifCn, fontWeight = FontWeight.Black,
        fontSize = 20.sp, lineHeight = 27.sp, letterSpacing = 0.5.sp,
    ),
    // 区块标题：继续观看 / 我的收藏 / 追番更新
    titleLarge = TextStyle(
        fontFamily = SerifCn, fontWeight = FontWeight.Bold,
        fontSize = 17.sp, lineHeight = 23.sp, letterSpacing = 1.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = SerifCn, fontWeight = FontWeight.Bold,
        fontSize = 15.sp, lineHeight = 21.sp, letterSpacing = 1.sp,
    ),
    // 卡片标题
    titleSmall = TextStyle(
        fontFamily = SerifCn, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 19.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = SansCn, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 25.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = SansCn, fontWeight = FontWeight.Normal,
        fontSize = 12.5f.sp, lineHeight = 24.sp,
    ),
    // 次要信息：集数、时长、人数
    bodySmall = TextStyle(
        fontFamily = SansCn, fontWeight = FontWeight.Normal,
        fontSize = 11.sp, lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = SansCn, fontWeight = FontWeight.Medium,
        fontSize = 11.5f.sp, lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = SansCn, fontWeight = FontWeight.Normal,
        fontSize = 10.5f.sp, lineHeight = 15.sp,
    ),
)

/**
 * 数字专用：评分、时长、进度。
 * 必须等宽，否则播放进度每秒跳动时整行会左右抖。
 */
val NumericStyle = TextStyle(
    fontFamily = SerifCn,
    fontWeight = FontWeight.Black,
    fontFeatureSettings = "tnum",
    textAlign = TextAlign.End,
)
