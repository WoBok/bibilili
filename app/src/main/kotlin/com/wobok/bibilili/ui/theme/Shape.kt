package com.wobok.bibilili.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val PaperShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),    // 角标
    small = RoundedCornerShape(8.dp),         // 集数方格
    medium = RoundedCornerShape(12.dp),       // 卡片、海报、输入框
    large = RoundedCornerShape(16.dp),        // 弹层（仅上两角时单独写）
    extraLarge = RoundedCornerShape(17.dp),   // 芯片
)

object PaperDimens {
    /** 页边距。 */
    val PagePadding = 22.dp
    /** 区块之间的纵向节奏。 */
    val SectionGap = 26.dp
    val CardPadding = 16.dp
    /** 触摸目标下限。 */
    val MinTouchTarget = 44.dp
    /** 竖版海报比例，3:2 更接近真实剧集封面。 */
    const val PosterAspect = 102f / 150f
}
