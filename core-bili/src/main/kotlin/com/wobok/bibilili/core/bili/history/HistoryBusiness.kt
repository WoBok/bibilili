package com.wobok.bibilili.core.bili.history

/**
 * `history.business` 字段——整个「只看影视和番剧」需求就靠它成立。
 *
 * 观看历史接口 `x/web-interface/history/cursor` 把 UP 主投稿、剧集、直播、专栏混在
 * 一条时间线里，靠这个字段区分。我们只要 [PGC]。
 */
enum class HistoryBusiness(val raw: String) {
    /** UP 主投稿视频。要过滤掉的就是这一类。 */
    ARCHIVE("archive"),

    /** 番剧 / 影视 / 纪录片 / 综艺，即 PGC 剧集。 */
    PGC("pgc"),

    LIVE("live"),
    ARTICLE("article"),
    UNKNOWN("");

    companion object {
        private val byRaw = entries.associateBy(HistoryBusiness::raw)

        /** 未知取值一律归入 [UNKNOWN] 并被过滤掉，宁可漏也不要让 UP 视频混进来。 */
        fun from(raw: String?): HistoryBusiness =
            raw?.let { byRaw[it] } ?: UNKNOWN
    }
}
