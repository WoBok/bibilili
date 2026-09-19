package com.wobok.bibilili.data.repo

import com.wobok.bibilili.core.bili.error.ApiResult
import com.wobok.bibilili.core.bili.error.map
import com.wobok.bibilili.data.api.BiliApi
import com.wobok.bibilili.data.api.SearchType
import com.wobok.bibilili.data.api.SeasonDto
import com.wobok.bibilili.data.api.SeasonType
import com.wobok.bibilili.data.api.TimelineDayDto
import com.wobok.bibilili.data.network.runApi

/** 一张卡片需要的最小信息，各个来源（索引/榜单/搜索）统一映射到它。 */
data class MediaCard(
    val seasonId: Long,
    val title: String,
    val cover: String,
    val subtitle: String,
    val score: String,
    val badge: String,
)

/** 频道里的一个预设入口。全部由索引与榜单接口驱动，不依赖运营位。 */
data class ChannelEntry(
    val key: String,
    val label: String,
    val kind: Kind,
) {
    enum class Kind { RANK, HIGH_SCORE, INDEX, LATEST, HOT }
}

class CatalogRepository(private val api: BiliApi) {

    fun entriesFor(seasonType: Int): List<ChannelEntry> = listOf(
        ChannelEntry("rank", "排行榜", ChannelEntry.Kind.RANK),
        ChannelEntry("score", "高分", ChannelEntry.Kind.HIGH_SCORE),
        ChannelEntry("hot", "最热", ChannelEntry.Kind.HOT),
        ChannelEntry("new", "最新", ChannelEntry.Kind.LATEST),
        ChannelEntry("index", "找片", ChannelEntry.Kind.INDEX),
    )

    suspend fun channel(
        seasonType: Int,
        entry: ChannelEntry.Kind,
        page: Int,
    ): ApiResult<List<MediaCard>> = when (entry) {
        ChannelEntry.Kind.RANK -> runApi { api.rankList(seasonType) }.map { dto ->
            dto.list.map {
                MediaCard(
                    seasonId = it.seasonId,
                    title = it.title,
                    cover = it.cover,
                    subtitle = it.newEp?.indexShow.orEmpty(),
                    score = it.rating,
                    badge = it.badge,
                )
            }
        }

        else -> {
            val order = when (entry) {
                ChannelEntry.Kind.HIGH_SCORE -> ORDER_SCORE
                ChannelEntry.Kind.LATEST -> ORDER_UPDATE
                else -> ORDER_PLAY
            }
            runApi {
                api.indexResult(seasonType = seasonType, page = page, order = order, st = seasonType)
            }.map { dto ->
                dto.list.map {
                    MediaCard(
                        seasonId = it.seasonId,
                        title = it.title,
                        cover = it.cover,
                        subtitle = it.indexShow,
                        score = it.order.takeIf { s -> s.endsWith("分") }.orEmpty(),
                        badge = it.badge,
                    )
                }
            }
        }
    }

    /** 时间表只有番剧/电影/国创三类，其余频道没有这个接口。 */
    suspend fun timeline(types: Int = SeasonType.BANGUMI): ApiResult<List<TimelineDayDto>> =
        runApi { api.timeline(types = types) }

    suspend fun season(seasonId: Long? = null, epId: Long? = null): ApiResult<SeasonDto> =
        runApi { api.season(seasonId = seasonId, epId = epId) }

    /**
     * 搜索。`media_ft` / `media_bangumi` 天然只返回剧集，
     * UP 主视频不会出现，不需要像历史那样再过滤一遍。
     */
    suspend fun search(keyword: String, bangumi: Boolean, page: Int): ApiResult<List<MediaCard>> =
        runApi {
            api.searchByType(
                searchType = if (bangumi) SearchType.BANGUMI else SearchType.MOVIE_AND_TV,
                keyword = keyword,
                page = page,
            )
        }.map { dto ->
            dto.result.map {
                MediaCard(
                    seasonId = it.seasonId,
                    title = it.title.stripHighlight(),
                    cover = it.cover.fixProtocol(),
                    subtitle = listOf(it.seasonTypeName, it.areas, it.indexShow)
                        .filter(String::isNotBlank).joinToString(" · "),
                    score = it.mediaScore?.score?.takeIf { s -> s > 0 }?.toString().orEmpty(),
                    badge = "",
                )
            }
        }

    private companion object {
        const val ORDER_PLAY = 2
        const val ORDER_SCORE = 4
        const val ORDER_UPDATE = 0
    }
}

/** 搜索结果的标题带 `<em class="keyword">` 高亮标签，展示前要去掉。 */
internal fun String.stripHighlight(): String =
    replace(Regex("</?em[^>]*>"), "")

/** 部分封面地址是 `//i0.hdslb.com/...` 的协议相对写法。 */
internal fun String.fixProtocol(): String =
    if (startsWith("//")) "https:$this" else this
