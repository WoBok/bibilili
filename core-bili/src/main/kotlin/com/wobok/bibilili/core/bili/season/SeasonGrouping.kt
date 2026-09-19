package com.wobok.bibilili.core.bili.season

/**
 * 把 `pgc/view/web/season` 的三块数据拼成选集 tab。
 *
 * 需求里点名要处理「某个连续剧或者纪录片、电影可能有多季」，接口把它拆成三处：
 *  - `episodes[]`  当前季的正片
 *  - `seasons[]`   同一作品的所有季（含当前季）
 *  - `section[]`   预告、花絮、PV
 *
 * 规则：
 *  - 只有一季且没有 section 的（典型如单片电影）→ 不出 tab，直接平铺，
 *    UI 层据此隐藏整个 tab 行；
 *  - 多季时当前季排在它在 `seasons[]` 里的原始位置，不提到最前面，
 *    否则「第一季/第二季」的顺序会乱；
 *  - section 一律排在所有季之后。
 */
object SeasonGrouping {

    fun build(
        currentSeasonId: Long,
        currentSeasonTitle: String,
        episodes: List<Episode>,
        seasons: List<SeasonRef>,
        sections: List<Section>,
    ): List<EpisodeTab> {
        val seasonTabs = if (seasons.size <= 1) {
            listOf(
                EpisodeTab(
                    key = "season-$currentSeasonId",
                    title = currentSeasonTitle,
                    kind = EpisodeTab.Kind.MAIN,
                    episodes = episodes,
                    seasonId = currentSeasonId,
                    selected = true,
                )
            )
        } else {
            seasons.map { season ->
                val isCurrent = season.seasonId == currentSeasonId
                EpisodeTab(
                    key = "season-${season.seasonId}",
                    title = season.title,
                    kind = if (isCurrent) EpisodeTab.Kind.MAIN else EpisodeTab.Kind.OTHER_SEASON,
                    episodes = if (isCurrent) episodes else emptyList(),
                    seasonId = season.seasonId,
                    selected = isCurrent,
                )
            }
        }

        val sectionTabs = sections
            .filter { it.episodes.isNotEmpty() }
            .map { section ->
                EpisodeTab(
                    key = "section-${section.id}",
                    title = section.title,
                    kind = EpisodeTab.Kind.SECTION,
                    episodes = section.episodes,
                )
            }

        return seasonTabs + sectionTabs
    }

    /**
     * 选集区是否值得显示。
     * 单集电影（一季、一集、无花絮）显示一个只有一格的选集区是噪音。
     */
    fun shouldShowPicker(tabs: List<EpisodeTab>): Boolean {
        if (tabs.size > 1) return true
        val only = tabs.singleOrNull() ?: return false
        return only.episodes.size > 1
    }
}
