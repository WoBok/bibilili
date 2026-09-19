package com.wobok.bibilili.core.bili.history

import com.wobok.bibilili.core.bili.history.HistorySyncPlanner.Decision
import com.wobok.bibilili.core.bili.history.HistorySyncPlanner.PageOutcome
import com.wobok.bibilili.core.bili.history.HistorySyncPlanner.Reason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HistoryBusinessTest {

    @Test
    fun `已知取值正确映射`() {
        assertEquals(HistoryBusiness.PGC, HistoryBusiness.from("pgc"))
        assertEquals(HistoryBusiness.ARCHIVE, HistoryBusiness.from("archive"))
        assertEquals(HistoryBusiness.LIVE, HistoryBusiness.from("live"))
        assertEquals(HistoryBusiness.ARTICLE, HistoryBusiness.from("article"))
    }

    @Test
    fun `未知与缺失一律归为 UNKNOWN`() {
        // 接口哪天新增一种 business，也绝不能让它漏进「只看影视番剧」的列表。
        assertEquals(HistoryBusiness.UNKNOWN, HistoryBusiness.from("something-new"))
        assertEquals(HistoryBusiness.UNKNOWN, HistoryBusiness.from(null))
        assertEquals(HistoryBusiness.UNKNOWN, HistoryBusiness.from(""))
    }
}

class PgcHistoryEntryTest {

    private fun entry(progress: Int, duration: Int, seasonId: Long = 1L, viewAt: Long = 0L) =
        PgcHistoryEntry(
            seasonId = seasonId, epId = 1, cid = 1, title = "t", episodeTitle = "e",
            cover = "", viewAt = viewAt, progressSeconds = progress, durationSeconds = duration,
        )

    @Test
    fun `progress 为 -1 表示看完`() {
        val e = entry(progress = -1, duration = 2880)
        assertEquals(1f, e.watchedRatio)
        assertTrue(e.isFinished)
    }

    @Test
    fun `按比例计算观看进度`() {
        assertEquals(0.5f, entry(progress = 50, duration = 100).watchedRatio)
    }

    @Test
    fun `时长缺失时进度为 0 而不是除零`() {
        assertEquals(0f, entry(progress = 50, duration = 0).watchedRatio)
    }

    @Test
    fun `看到 95% 以上算看完`() {
        assertTrue(entry(progress = 96, duration = 100).isFinished)
        assertTrue(!entry(progress = 94, duration = 100).isFinished)
    }

    @Test
    fun `进度不会超过 1`() {
        assertEquals(1f, entry(progress = 200, duration = 100).watchedRatio)
    }
}

class HistorySyncPlannerTest {

    private val now = 1_700_000_000L
    private val day = 24L * 60 * 60

    private fun page(vararg viewAt: Long, hasMore: Boolean = true) =
        PageOutcome(allViewAtSeconds = viewAt.toList(), hasMore = hasMore)

    @Test
    fun `页内还在 30 天窗口内则继续翻`() {
        val decision = HistorySyncPlanner.decide(
            page = page(now - day, now - 2 * day),
            pagesFetched = 1,
            localNewestViewAt = null,
            nowSeconds = now,
        )
        assertEquals(Decision.Continue, decision)
    }

    @Test
    fun `游标翻过 30 天即停止`() {
        val decision = HistorySyncPlanner.decide(
            page = page(now - 29 * day, now - 31 * day),
            pagesFetched = 1,
            localNewestViewAt = null,
            nowSeconds = now,
        )
        assertEquals(Decision.Stop(Reason.REACHED_TIME_WINDOW), decision)
    }

    @Test
    fun `增量同步追上本地最新记录即停止`() {
        val local = now - 5 * day
        val decision = HistorySyncPlanner.decide(
            page = page(now - 4 * day, local),
            pagesFetched = 1,
            localNewestViewAt = local,
            nowSeconds = now,
        )
        assertEquals(Decision.Stop(Reason.CAUGHT_UP_WITH_LOCAL), decision)
    }

    @Test
    fun `首次同步不因为本地为空而提前停止`() {
        // localNewestViewAt 为 null 时不能走「追上本地」那条分支。
        val decision = HistorySyncPlanner.decide(
            page = page(now - day),
            pagesFetched = 1,
            localNewestViewAt = null,
            nowSeconds = now,
        )
        assertEquals(Decision.Continue, decision)
    }

    @Test
    fun `接口说没有更多就停止`() {
        val decision = HistorySyncPlanner.decide(
            page = page(now - day, hasMore = false),
            pagesFetched = 1,
            localNewestViewAt = null,
            nowSeconds = now,
        )
        assertEquals(Decision.Stop(Reason.NO_MORE_PAGES), decision)
    }

    @Test
    fun `达到翻页上限时停止`() {
        val decision = HistorySyncPlanner.decide(
            page = page(now - day),
            pagesFetched = HistorySyncPlanner.MAX_PAGES_PER_SYNC,
            localNewestViewAt = null,
            nowSeconds = now,
        )
        assertEquals(Decision.Stop(Reason.PAGE_LIMIT), decision)
    }

    @Test
    fun `整页都是 UP 视频也不会提前停止`() {
        // 稀疏分页：这一页过滤后一条 PGC 都不剩，但游标还在窗口内，必须继续翻。
        val decision = HistorySyncPlanner.decide(
            page = page(now - day, now - day - 100, now - day - 200),
            pagesFetched = 3,
            localNewestViewAt = null,
            nowSeconds = now,
        )
        assertEquals(Decision.Continue, decision)
    }

    @Test
    fun `清理边界为 90 天前`() {
        assertEquals(now - 90 * day, HistorySyncPlanner.purgeBefore(now))
    }

    private fun entry(seasonId: Long, viewAt: Long, progress: Int = 50, duration: Int = 100) =
        PgcHistoryEntry(
            seasonId = seasonId, epId = seasonId * 10, cid = 1, title = "剧 $seasonId",
            episodeTitle = "", cover = "", viewAt = viewAt,
            progressSeconds = progress, durationSeconds = duration,
        )

    @Test
    fun `继续观看按剧集去重只留最近一集`() {
        val result = HistorySyncPlanner.continueWatching(
            listOf(
                entry(seasonId = 1, viewAt = 100),
                entry(seasonId = 1, viewAt = 300),
                entry(seasonId = 2, viewAt = 200),
            ),
            limit = 10,
        )
        assertEquals(listOf(1L, 2L), result.map(PgcHistoryEntry::seasonId))
        assertEquals(300L, result.first().viewAt, "同一部剧应保留最近观看的那一条")
    }

    @Test
    fun `继续观看排除已看完的`() {
        val result = HistorySyncPlanner.continueWatching(
            listOf(
                entry(seasonId = 1, viewAt = 300, progress = -1),
                entry(seasonId = 2, viewAt = 200),
            ),
            limit = 10,
        )
        assertEquals(listOf(2L), result.map(PgcHistoryEntry::seasonId))
    }

    @Test
    fun `继续观看遵守数量上限`() {
        val entries = (1L..10L).map { entry(seasonId = it, viewAt = it * 100) }
        assertEquals(3, HistorySyncPlanner.continueWatching(entries, limit = 3).size)
    }

    @Test
    fun `继续观看为空是允许的`() {
        assertTrue(HistorySyncPlanner.continueWatching(emptyList(), limit = 6).isEmpty())
    }
}
