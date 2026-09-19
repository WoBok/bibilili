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
    /** 短标题是不是纯集数。电影的分段常常直接把片名放在这里。 */
    private val shortIsNumber: Boolean
        get() = shortTitle.isNotBlank() && shortTitle.all(Char::isDigit)

    /**
     * 「03 风蚀」。
     *
     * 短标题不是数字时**原样用**——电影特辑的 `title` 可能是「地球最后的导演」，
     * 硬套「第 N 集」会出现「第 地球最后的导演 集」这种句子。
     */
    fun displayTitle(): String = when {
        longTitle.isNotBlank() && shortTitle.isNotBlank() -> "$shortTitle $longTitle"
        longTitle.isNotBlank() -> longTitle
        shortIsNumber -> "第 $shortTitle 集"
        shortTitle.isNotBlank() -> shortTitle
        else -> "第 ? 集"
    }

    /** 选集九宫格里的一格，位置很窄，能用数字就用数字。 */
    fun gridLabel(): String = when {
        shortIsNumber -> shortTitle
        shortTitle.isNotBlank() -> shortTitle
        longTitle.isNotBlank() -> longTitle
        else -> "?"
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
