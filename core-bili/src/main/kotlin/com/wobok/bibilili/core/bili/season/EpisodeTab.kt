package com.wobok.bibilili.core.bili.season

/** 选集区的一个 tab。 */
data class EpisodeTab(
    val key: String,
    val title: String,
    val kind: Kind,
    val episodes: List<Episode>,
    /** 跨季 tab 只有 id，点开才去拉那一季的分集。 */
    val seasonId: Long? = null,
    val selected: Boolean = false,
) {
    enum class Kind {
        /** 当前季的正片。 */
        MAIN,
        /** 另一季，内容需要另取。 */
        OTHER_SEASON,
        /** 预告 / 花絮 / PV。 */
        SECTION,
    }

    val isLoaded: Boolean get() = kind != Kind.OTHER_SEASON || episodes.isNotEmpty()
}
