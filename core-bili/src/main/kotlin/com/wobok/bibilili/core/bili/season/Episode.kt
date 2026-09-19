package com.wobok.bibilili.core.bili.season

data class Episode(
    val epId: Long,
    val cid: Long,
    val aid: Long,
    /** 短标题，通常是集数，如「3」。 */
    val shortTitle: String,
    /** 长标题，如「风蚀」。 */
    val longTitle: String,
    val cover: String,
    val durationMillis: Long,
    val needsVip: Boolean,
) {
    /** 「03 风蚀」；长标题为空时退化成「第 3 集」。 */
    fun displayTitle(): String = when {
        longTitle.isNotBlank() && shortTitle.isNotBlank() -> "$shortTitle $longTitle"
        longTitle.isNotBlank() -> longTitle
        shortTitle.isNotBlank() -> "第 $shortTitle 集"
        else -> "第 ? 集"
    }
}

/** `pgc/view/web/season` 的 `seasons[]`：同一部作品的其他季。 */
data class SeasonRef(
    val seasonId: Long,
    val title: String,
    val cover: String,
    /** 当前正在播放的那一季。 */
    val isCurrent: Boolean,
)

/** `section[]`：正片之外的分段，预告 / 花絮 / PV。 */
data class Section(
    val id: Long,
    val title: String,
    val episodes: List<Episode>,
)
