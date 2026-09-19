package com.wobok.bibilili.data.repo

import com.wobok.bibilili.core.bili.error.ApiResult
import com.wobok.bibilili.data.api.BiliApi
import com.wobok.bibilili.data.api.FollowItemDto
import com.wobok.bibilili.data.auth.CredentialStore
import com.wobok.bibilili.data.local.FollowDao
import com.wobok.bibilili.data.local.FollowEntity
import com.wobok.bibilili.data.network.runApi
import kotlinx.coroutines.flow.Flow

/**
 * 追番 / 追剧。首页的「我的收藏」和番剧页的「我的追番」同源，
 * 区别只在展示：首页混排最近的一小撮，番剧页是完整列表 + 状态筛选。
 */
class FollowRepository(
    private val api: BiliApi,
    private val dao: FollowDao,
    private val credentials: CredentialStore,
) {

    /** @param type 0=全部 1=番剧 2=影视；@param status 0=全部 1=想看 2=在看 3=看过 */
    fun observe(type: Int = 0, status: Int = 0): Flow<List<FollowEntity>> = dao.observe(type, status)

    suspend fun sync(): ApiResult<Int> {
        val mid = credentials.current.value?.dedeUserId
            ?: return ApiResult.Failure(com.wobok.bibilili.core.bili.error.BiliError.NotLoggedIn)

        var total = 0
        val all = mutableListOf<FollowEntity>()

        for (type in listOf(1, 2)) {
            var page = 1
            while (page <= MAX_PAGES) {
                val result = runApi { api.followList(vmid = mid, type = type, pn = page) }
                val data = when (result) {
                    is ApiResult.Failure -> break
                    is ApiResult.Success -> result.data
                }
                if (data.list.isEmpty()) break
                all += data.list.map { it.toEntity(type) }
                total += data.list.size
                if (all.size >= data.total) break
                page++
            }
        }

        if (all.isNotEmpty()) {
            dao.clear()
            dao.upsertAll(all)
        }
        return ApiResult.Success(total)
    }

    /**
     * 追番更新：有新集、且我还没看到那一集的。
     * 只用已经拉下来的字段判断，不额外请求。
     */
    fun observeUpdates(): Flow<List<FollowEntity>> = dao.observe(0, 0)

    suspend fun clear() = dao.clear()

    private companion object {
        const val MAX_PAGES = 20
    }
}

private fun FollowItemDto.toEntity(type: Int) = FollowEntity(
    seasonId = seasonId,
    type = type,
    title = title,
    cover = cover,
    seasonTypeName = seasonTypeName,
    newEpIndexShow = newEp?.indexShow.orEmpty(),
    progressText = progress,
    followStatus = followStatus,
    isFinish = isFinish == 1,
    newEpPubTime = newEp?.pubTime?.toEpochOrZero() ?: 0L,
    score = rating?.score?.takeIf { it > 0 }?.toString().orEmpty(),
)

/** 接口给的是 `2026-09-19 22:00:00` 这种串，解析失败就当 0，不让它中断同步。 */
private fun String.toEpochOrZero(): Long = runCatching {
    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.CHINA)
        .parse(this)?.time ?: 0L
}.getOrDefault(0L)

/** 有没有没看的新集：拿「更新至第 N 话」和「看到第 M 话」里的数字比。 */
fun FollowEntity.hasUnwatchedUpdate(): Boolean {
    val latest = newEpIndexShow.firstNumber() ?: return false
    val mine = progressText.firstNumber() ?: return true
    return latest > mine
}

private fun String.firstNumber(): Int? =
    Regex("\\d+").find(this)?.value?.toIntOrNull()
