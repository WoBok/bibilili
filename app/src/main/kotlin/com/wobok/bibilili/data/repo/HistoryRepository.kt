package com.wobok.bibilili.data.repo

import com.wobok.bibilili.core.bili.error.ApiResult
import com.wobok.bibilili.core.bili.history.HistoryBusiness
import com.wobok.bibilili.core.bili.history.HistorySyncPlanner
import com.wobok.bibilili.core.bili.history.PgcHistoryEntry
import com.wobok.bibilili.data.api.BiliApi
import com.wobok.bibilili.data.api.HistoryItemDto
import com.wobok.bibilili.data.local.HistoryDao
import com.wobok.bibilili.data.local.HistoryEntity
import com.wobok.bibilili.data.local.SettingsStore
import com.wobok.bibilili.data.network.runApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 观看历史。整个「只展现影视和番剧」的需求落在这里。
 *
 * 服务端返回的是混合流，`history.business == "pgc"` 才是剧集。
 * 纯客户端就能做到，不需要自建服务器。
 */
class HistoryRepository(
    private val api: BiliApi,
    private val dao: HistoryDao,
    private val settings: SettingsStore,
) {

    fun observeContinueWatching(limit: Int = 8): Flow<List<PgcHistoryEntry>> =
        dao.observeRecent(limit * 3).map { rows ->
            HistorySyncPlanner.continueWatching(rows.map(HistoryEntity::toDomain), limit)
        }

    /**
     * 增量同步。
     *
     * 只拉 30 天内；停止条件看**游标时间**而不是「这一页过滤后有没有内容」——
     * 一页 20 条里可能一条剧集都没有，看内容会导致同步提前中断。
     */
    suspend fun sync(): ApiResult<Int> {
        val localNewest = dao.newestViewAt()
        var max = 0L
        var viewAt = 0L
        var pages = 0
        var written = 0

        while (true) {
            val result = runApi { api.historyCursor(max = max, viewAt = viewAt) }
            val page = when (result) {
                is ApiResult.Failure -> return if (written > 0) ApiResult.Success(written) else result
                is ApiResult.Success -> result.data
            }
            pages++

            val pgc = page.list
                .filter { HistoryBusiness.from(it.history.business) == HistoryBusiness.PGC }
                .map(HistoryItemDto::toEntity)

            if (pgc.isNotEmpty()) {
                dao.upsertAll(pgc)
                written += pgc.size
            }

            val decision = HistorySyncPlanner.decide(
                page = HistorySyncPlanner.PageOutcome(
                    allViewAtSeconds = page.list.map(HistoryItemDto::viewAt),
                    hasMore = page.list.isNotEmpty() && page.cursor.max != 0L,
                ),
                pagesFetched = pages,
                localNewestViewAt = localNewest,
                nowSeconds = System.currentTimeMillis() / 1000,
            )
            if (decision is HistorySyncPlanner.Decision.Stop) break

            max = page.cursor.max
            viewAt = page.cursor.viewAt
            // 翻页之间留间隔，避免触发 -412。
            delay(HistorySyncPlanner.PAGE_DELAY_MILLIS)
        }

        maybePurge()
        return ApiResult.Success(written)
    }

    /** 每 7 天清一次超过 3 个月的记录。 */
    private suspend fun maybePurge() {
        val now = System.currentTimeMillis()
        val last = settings.lastPurgeAt.first()
        val interval = HistorySyncPlanner.CLEANUP_INTERVAL_DAYS * 24 * 60 * 60 * 1000
        if (now - last < interval) return
        dao.purgeBefore(HistorySyncPlanner.purgeBefore(now / 1000))
        settings.setLastPurgeAt(now)
    }

    /** 用户页的「近 90 天看过」与「观看时长」。 */
    suspend fun stats(): Pair<Int, Long> {
        val since = HistorySyncPlanner.purgeBefore(System.currentTimeMillis() / 1000)
        val count = dao.countSince(since)
        val seconds = dao.watchedSecondsSince(since) ?: 0L
        return count to seconds
    }

    suspend fun clear() = dao.clear()
}

private fun HistoryItemDto.toEntity() = HistoryEntity(
    seasonId = history.oid,
    epId = history.epid,
    cid = history.cid,
    title = title,
    episodeTitle = showTitle,
    cover = cover,
    viewAt = viewAt,
    progressSeconds = progress,
    durationSeconds = duration,
)

private fun HistoryEntity.toDomain() = PgcHistoryEntry(
    seasonId = seasonId,
    epId = epId,
    cid = cid,
    title = title,
    episodeTitle = episodeTitle,
    cover = cover,
    viewAt = viewAt,
    progressSeconds = progressSeconds,
    durationSeconds = durationSeconds,
)
