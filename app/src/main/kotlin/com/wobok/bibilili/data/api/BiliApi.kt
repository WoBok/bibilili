package com.wobok.bibilili.data.api

import com.wobok.bibilili.data.network.BiliResponse
import com.wobok.bibilili.data.network.WbiInterceptor
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

/**
 * 所有接口都挂在 `https://api.bilibili.com/` 下。
 *
 * 标了 `X-Bibilili-Wbi` 的走 WBI 签名，由 [WbiInterceptor] 在发出前替换成
 * `wts` + `w_rid`，并摘掉这个内部 header。
 */
interface BiliApi {

    // ---- 历史：只看影视番剧的关键入口 ----

    /**
     * 观看历史，按 `view_at` 倒序的游标分页。
     * 返回的是 UP 视频 / 剧集 / 直播 / 专栏的混合流，靠 `history.business` 区分。
     */
    @GET("x/web-interface/history/cursor")
    suspend fun historyCursor(
        @Query("max") max: Long = 0,
        @Query("view_at") viewAt: Long = 0,
        @Query("ps") ps: Int = 20,
        /** 传 archive 反而会把剧集滤掉，所以不传 type，拿混合流自己筛。 */
        @Query("business") business: String? = null,
    ): BiliResponse<HistoryCursorDto>

    // ---- 追番 / 追剧 ----

    @GET("x/space/bangumi/follow/list")
    suspend fun followList(
        @Query("vmid") vmid: String,
        /** 1=追番 2=追剧 */
        @Query("type") type: Int,
        @Query("pn") pn: Int = 1,
        @Query("ps") ps: Int = 30,
        /** 0=全部 1=想看 2=在看 3=看过。未在公开文档的参数表里，取不到时退回全部。 */
        @Query("follow_status") followStatus: Int? = null,
    ): BiliResponse<FollowListDto>

    // ---- 用户 ----

    /** 一次拿到追番数与追剧数，不需要 WBI，也不需要翻列表。 */
    @GET("x/space/navnum")
    suspend fun navNum(@Query("mid") mid: String): BiliResponse<NavNumDto>

    @Headers(WbiInterceptor.HEADER_WBI + ": 1")
    @GET("x/space/wbi/acc/info")
    suspend fun spaceInfo(@Query("mid") mid: String): BiliResponse<SpaceInfoDto>

    // ---- 频道内容 ----

    /**
     * 剧集索引。影视与番剧的「高分」「找片」「厂牌」「地区」都由它驱动。
     * season_type：番剧1 电影2 纪录片3 国创4 电视剧5 综艺7
     */
    @GET("pgc/season/index/result")
    suspend fun indexResult(
        @Query("season_type") seasonType: Int,
        @Query("type") type: Int = 1,
        @Query("page") page: Int = 1,
        @Query("pagesize") pageSize: Int = 20,
        /** 更新0 弹幕1 播放2 追剧3 评分4 开播5 上映6 */
        @Query("order") order: Int = 2,
        @Query("sort") sort: Int = 0,
        @Query("st") st: Int = 0,
        @Query("area") area: String? = null,
        @Query("style_id") styleId: Int? = null,
        @Query("producer_id") producerId: Int? = null,
        @Query("release_date") releaseDate: String? = null,
        @Query("year") year: String? = null,
        @Query("season_month") seasonMonth: Int? = null,
        @Query("is_finish") isFinish: Int? = null,
    ): BiliResponse<IndexResultDto>

    /** 排行榜。day=3 为三日榜。 */
    @GET("pgc/web/rank/list")
    suspend fun rankList(
        @Query("season_type") seasonType: Int,
        @Query("day") day: Int = 3,
    ): BiliResponse<RankListDto>

    /** 时间表。types 只接受 1=番剧 3=电影 4=国创，其余频道没有时间表。 */
    @GET("pgc/web/timeline")
    suspend fun timeline(
        @Query("types") types: Int = 1,
        @Query("before") before: Int = 3,
        @Query("after") after: Int = 7,
    ): BiliResponse<List<TimelineDayDto>>

    // ---- 剧集详情 ----

    @GET("pgc/view/web/season")
    suspend fun season(
        @Query("season_id") seasonId: Long? = null,
        @Query("ep_id") epId: Long? = null,
    ): BiliResponse<SeasonDto>

    // ---- 播放地址 ----

    /**
     * PGC 的播放地址。
     *
     * `fnval=4048` 才拿得到 4K/HDR/杜比；投屏要 `durl` 所以另传 `fnval=0`。
     * 这个接口的返回把内容放在 `result` 里而不是 `data`，所以整体反序列化成
     * [PlayUrlDto] 后要走 `result ?: 顶层` 的兼容。
     */
    @GET("pgc/player/web/playurl")
    suspend fun playUrl(
        @Query("ep_id") epId: Long,
        @Query("cid") cid: Long,
        @Query("qn") qn: Int,
        @Query("fnval") fnval: Int,
        @Query("fnver") fnver: Int = 0,
        @Query("fourk") fourk: Int = 1,
    ): PlayUrlDto

    // ---- 搜索 ----

    /**
     * 分类搜索。`search_type` 取 `media_ft`（影视）或 `media_bangumi`（番剧），
     * **天然只返回剧集，UP 主视频不会混进来**，不需要二次过滤。
     */
    @Headers(WbiInterceptor.HEADER_WBI + ": 1")
    @GET("x/web-interface/wbi/search/type")
    suspend fun searchByType(
        @Query("search_type") searchType: String,
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1,
    ): BiliResponse<SearchTypeDto>
}

object SearchType {
    const val MOVIE_AND_TV = "media_ft"
    const val BANGUMI = "media_bangumi"
}

object SeasonType {
    const val BANGUMI = 1
    const val MOVIE = 2
    const val DOCUMENTARY = 3
    const val GUOCHUANG = 4
    const val TV = 5
    const val VARIETY = 7
}
