package com.wobok.bibilili.core.bili.history

/**
 * 历史增量同步的决策逻辑，纯函数，与网络层无关。
 *
 * 需求定下的策略：
 *  - 只拉 **30 天内**的记录，更早的不翻；
 *  - 本地库随使用自然沉淀，最多保留 **3 个月**；
 *  - 每 **7 天**清理一次超过 3 个月的记录。
 *
 * 这里要解决的是「稀疏分页」：历史接口按时间倒序返回**混合**内容，
 * 一页 20 条里可能一条 PGC 都没有。所以停止条件不能只看「这一页有没有拿到东西」，
 * 必须看**游标本身的时间**是否已经翻过了截止点。
 */
object HistorySyncPlanner {

    /** 只同步这个天数内的记录。 */
    const val SYNC_WINDOW_DAYS = 30L

    /** 本地保留上限，超出的由周期任务清理。 */
    const val LOCAL_RETENTION_DAYS = 90L

    /** 清理任务的周期。 */
    const val CLEANUP_INTERVAL_DAYS = 7L

    /** 单次同步最多翻多少页，兜住「30 天内全是 UP 视频」的极端情况。 */
    const val MAX_PAGES_PER_SYNC = 12

    /** 每页之间的间隔，避免触发 `-412`。 */
    const val PAGE_DELAY_MILLIS = 300L

    private const val SECONDS_PER_DAY = 24L * 60 * 60

    sealed interface Decision {
        /** 继续翻下一页。 */
        data object Continue : Decision

        /** 停止，并附上原因——排查同步为什么只拿到几条时全靠它。 */
        data class Stop(val reason: Reason) : Decision
    }

    enum class Reason {
        /** 游标已经翻过 30 天截止点。正常结束。 */
        REACHED_TIME_WINDOW,

        /** 已经追上本地最新的一条，再往前都是已有数据。增量同步的正常结束。 */
        CAUGHT_UP_WITH_LOCAL,

        /** 接口说没有更多了。 */
        NO_MORE_PAGES,

        /** 到达翻页上限。说明这段时间 PGC 记录确实很稀疏。 */
        PAGE_LIMIT,
    }

    data class PageOutcome(
        /** 本页**过滤前**的全部记录的 viewAt，用来判断游标位置。 */
        val allViewAtSeconds: List<Long>,
        /** 接口是否还给了下一页游标。 */
        val hasMore: Boolean,
    )

    /**
     * @param pagesFetched      含本页在内已经请求的页数
     * @param localNewestViewAt 本地库里最新一条的 viewAt；库为空传 `null`（首次全量）
     * @param nowSeconds        当前 Unix 秒
     */
    fun decide(
        page: PageOutcome,
        pagesFetched: Int,
        localNewestViewAt: Long?,
        nowSeconds: Long,
    ): Decision {
        val cutoff = nowSeconds - SYNC_WINDOW_DAYS * SECONDS_PER_DAY

        val oldestOnPage = page.allViewAtSeconds.minOrNull()
        if (oldestOnPage != null && oldestOnPage < cutoff) {
            return Decision.Stop(Reason.REACHED_TIME_WINDOW)
        }

        // 增量同步：本页出现了不新于本地最新记录的条目，说明已经接上了。
        // 首次同步（localNewestViewAt == null）不适用，必须翻满时间窗。
        if (localNewestViewAt != null &&
            page.allViewAtSeconds.any { it <= localNewestViewAt }
        ) {
            return Decision.Stop(Reason.CAUGHT_UP_WITH_LOCAL)
        }

        if (!page.hasMore) return Decision.Stop(Reason.NO_MORE_PAGES)
        if (pagesFetched >= MAX_PAGES_PER_SYNC) return Decision.Stop(Reason.PAGE_LIMIT)

        return Decision.Continue
    }

    /** 周期清理的删除边界：早于此时间的本地记录可以删。 */
    fun purgeBefore(nowSeconds: Long): Long =
        nowSeconds - LOCAL_RETENTION_DAYS * SECONDS_PER_DAY

    /**
     * 「继续观看」要展示的条目。
     *
     * 同一部剧只保留最近看的那一集（按 seasonId 去重），已经看完的不再出现在
     * 「继续观看」里——看完的归属是「我的收藏」，不是待续。
     */
    fun continueWatching(entries: List<PgcHistoryEntry>, limit: Int): List<PgcHistoryEntry> =
        entries.asSequence()
            .sortedByDescending(PgcHistoryEntry::viewAt)
            .distinctBy(PgcHistoryEntry::seasonId)
            .filterNot(PgcHistoryEntry::isFinished)
            .take(limit)
            .toList()
}
