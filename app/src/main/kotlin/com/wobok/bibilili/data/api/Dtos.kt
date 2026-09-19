package com.wobok.bibilili.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------------- 观看历史 ----------------

@Serializable
data class HistoryCursorDto(
    @SerialName("cursor") val cursor: CursorDto = CursorDto(),
    @SerialName("list") val list: List<HistoryItemDto> = emptyList(),
)

@Serializable
data class CursorDto(
    @SerialName("max") val max: Long = 0,
    @SerialName("view_at") val viewAt: Long = 0,
    @SerialName("ps") val ps: Int = 20,
)

@Serializable
data class HistoryItemDto(
    @SerialName("title") val title: String = "",
    @SerialName("cover") val cover: String = "",
    @SerialName("history") val history: HistoryRefDto = HistoryRefDto(),
    @SerialName("videos") val videos: Int = 0,
    @SerialName("show_title") val showTitle: String = "",
    @SerialName("duration") val duration: Int = 0,
    @SerialName("progress") val progress: Int = 0,
    @SerialName("view_at") val viewAt: Long = 0,
)

@Serializable
data class HistoryRefDto(
    @SerialName("oid") val oid: Long = 0,
    @SerialName("epid") val epid: Long = 0,
    @SerialName("cid") val cid: Long = 0,
    /** archive / pgc / live / article —— 整个「只看影视番剧」靠它。 */
    @SerialName("business") val business: String = "",
)

// ---------------- 追番 / 追剧 ----------------

@Serializable
data class FollowListDto(
    @SerialName("list") val list: List<FollowItemDto> = emptyList(),
    @SerialName("total") val total: Int = 0,
)

@Serializable
data class FollowItemDto(
    @SerialName("season_id") val seasonId: Long = 0,
    @SerialName("title") val title: String = "",
    @SerialName("cover") val cover: String = "",
    @SerialName("season_type_name") val seasonTypeName: String = "",
    @SerialName("is_finish") val isFinish: Int = 0,
    @SerialName("follow_status") val followStatus: Int = 0,
    @SerialName("progress") val progress: String = "",
    @SerialName("new_ep") val newEp: NewEpDto? = null,
    @SerialName("rating") val rating: RatingDto? = null,
)

@Serializable
data class NewEpDto(
    @SerialName("index_show") val indexShow: String = "",
    @SerialName("pub_time") val pubTime: String = "",
)

@Serializable
data class RatingDto(
    @SerialName("score") val score: Double = 0.0,
    @SerialName("count") val count: Int = 0,
)

// ---------------- 用户统计 ----------------

@Serializable
data class NavNumDto(
    /** 追番数，无视隐私设置。 */
    @SerialName("bangumi") val bangumi: Int = 0,
    /** 追剧数，无视隐私设置。 */
    @SerialName("cinema") val cinema: Int = 0,
)

@Serializable
data class SpaceInfoDto(
    @SerialName("mid") val mid: Long = 0,
    @SerialName("name") val name: String = "",
    @SerialName("face") val face: String = "",
    @SerialName("sign") val sign: String = "",
)

// ---------------- 剧集索引 / 榜单 / 时间表 ----------------

@Serializable
data class IndexResultDto(
    @SerialName("list") val list: List<IndexItemDto> = emptyList(),
    @SerialName("has_next") val hasNext: Int = 0,
    @SerialName("num") val num: Int = 1,
    @SerialName("total") val total: Int = 0,
)

@Serializable
data class IndexItemDto(
    @SerialName("season_id") val seasonId: Long = 0,
    @SerialName("title") val title: String = "",
    @SerialName("cover") val cover: String = "",
    @SerialName("index_show") val indexShow: String = "",
    @SerialName("order") val order: String = "",
    @SerialName("is_finish") val isFinish: Int = 0,
    @SerialName("badge") val badge: String = "",
)

@Serializable
data class RankListDto(
    @SerialName("list") val list: List<RankItemDto> = emptyList(),
)

@Serializable
data class RankItemDto(
    @SerialName("season_id") val seasonId: Long = 0,
    @SerialName("title") val title: String = "",
    @SerialName("cover") val cover: String = "",
    @SerialName("new_ep") val newEp: NewEpDto? = null,
    @SerialName("rating") val rating: String = "",
    @SerialName("badge") val badge: String = "",
)

@Serializable
data class TimelineDayDto(
    @SerialName("date") val date: String = "",
    @SerialName("date_ts") val dateTs: Long = 0,
    @SerialName("day_of_week") val dayOfWeek: Int = 0,
    @SerialName("is_today") val isToday: Int = 0,
    @SerialName("episodes") val episodes: List<TimelineEpisodeDto> = emptyList(),
)

@Serializable
data class TimelineEpisodeDto(
    @SerialName("season_id") val seasonId: Long = 0,
    @SerialName("title") val title: String = "",
    @SerialName("cover") val cover: String = "",
    @SerialName("pub_index") val pubIndex: String = "",
    @SerialName("pub_time") val pubTime: String = "",
    @SerialName("follow") val follow: Int = 0,
)

// ---------------- 剧集详情 ----------------

@Serializable
data class SeasonDto(
    @SerialName("season_id") val seasonId: Long = 0,
    @SerialName("title") val title: String = "",
    @SerialName("season_title") val seasonTitle: String = "",
    @SerialName("cover") val cover: String = "",
    @SerialName("evaluate") val evaluate: String = "",
    @SerialName("type") val type: Int = 0,
    @SerialName("total") val total: Int = 0,
    @SerialName("rating") val rating: RatingDto? = null,
    @SerialName("stat") val stat: SeasonStatDto? = null,
    @SerialName("episodes") val episodes: List<EpisodeDto> = emptyList(),
    @SerialName("seasons") val seasons: List<SeasonRefDto> = emptyList(),
    @SerialName("section") val section: List<SectionDto> = emptyList(),
    @SerialName("areas") val areas: List<AreaDto> = emptyList(),
    @SerialName("publish") val publish: PublishDto? = null,
    @SerialName("alias") val alias: String = "",
    @SerialName("origin_name") val originName: String = "",
    @SerialName("styles") val styles: List<String> = emptyList(),
    @SerialName("user_status") val userStatus: UserStatusDto? = null,
)

@Serializable
data class SeasonStatDto(
    @SerialName("views") val views: Long = 0,
    @SerialName("follow") val follow: Long = 0,
)

@Serializable
data class UserStatusDto(
    @SerialName("follow") val follow: Int = 0,
)

@Serializable
data class AreaDto(@SerialName("name") val name: String = "")

@Serializable
data class PublishDto(
    @SerialName("pub_time") val pubTime: String = "",
    @SerialName("release_date_show") val releaseDateShow: String = "",
)

@Serializable
data class EpisodeDto(
    @SerialName("id") val id: Long = 0,
    @SerialName("aid") val aid: Long = 0,
    @SerialName("cid") val cid: Long = 0,
    @SerialName("title") val title: String = "",
    @SerialName("long_title") val longTitle: String = "",
    @SerialName("cover") val cover: String = "",
    @SerialName("duration") val duration: Long = 0,
    @SerialName("badge") val badge: String = "",
    @SerialName("skip") val skip: SkipDto? = null,
)

/** 跳过片头片尾。接口不一定给，给不出就只能手动或置灰。 */
@Serializable
data class SkipDto(
    @SerialName("op") val op: SkipRangeDto? = null,
    @SerialName("ed") val ed: SkipRangeDto? = null,
)

@Serializable
data class SkipRangeDto(
    @SerialName("start") val start: Long = 0,
    @SerialName("end") val end: Long = 0,
)

@Serializable
data class SeasonRefDto(
    @SerialName("season_id") val seasonId: Long = 0,
    @SerialName("season_title") val seasonTitle: String = "",
    @SerialName("cover") val cover: String = "",
)

@Serializable
data class SectionDto(
    @SerialName("id") val id: Long = 0,
    @SerialName("title") val title: String = "",
    @SerialName("episodes") val episodes: List<EpisodeDto> = emptyList(),
)

// ---------------- 播放地址 ----------------

@Serializable
data class PlayUrlDto(
    @SerialName("code") val code: Int = 0,
    @SerialName("message") val message: String = "",
    @SerialName("result") val result: PlayUrlResultDto? = null,
    // 部分返回把内容直接铺在顶层
    @SerialName("dash") val dash: DashDto? = null,
    @SerialName("durl") val durl: List<DurlDto> = emptyList(),
    @SerialName("quality") val quality: Int = 0,
    @SerialName("accept_quality") val acceptQuality: List<Int> = emptyList(),
    @SerialName("accept_description") val acceptDescription: List<String> = emptyList(),
)

@Serializable
data class PlayUrlResultDto(
    @SerialName("dash") val dash: DashDto? = null,
    @SerialName("durl") val durl: List<DurlDto> = emptyList(),
    @SerialName("quality") val quality: Int = 0,
    @SerialName("accept_quality") val acceptQuality: List<Int> = emptyList(),
    @SerialName("accept_description") val acceptDescription: List<String> = emptyList(),
)

@Serializable
data class DashDto(
    @SerialName("duration") val duration: Long = 0,
    @SerialName("video") val video: List<DashTrackDto> = emptyList(),
    @SerialName("audio") val audio: List<DashTrackDto> = emptyList(),
)

@Serializable
data class DashTrackDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("baseUrl") val baseUrl: String = "",
    @SerialName("base_url") val baseUrlAlt: String = "",
    @SerialName("backupUrl") val backupUrl: List<String> = emptyList(),
    @SerialName("backup_url") val backupUrlAlt: List<String> = emptyList(),
    @SerialName("bandwidth") val bandwidth: Int = 0,
    @SerialName("mimeType") val mimeType: String = "",
    @SerialName("mime_type") val mimeTypeAlt: String = "",
    @SerialName("codecs") val codecs: String = "",
    @SerialName("width") val width: Int = 0,
    @SerialName("height") val height: Int = 0,
    @SerialName("frameRate") val frameRate: String = "",
    @SerialName("frame_rate") val frameRateAlt: String = "",
    @SerialName("segment_base") val segmentBase: SegmentBaseDto? = null,
    @SerialName("SegmentBase") val segmentBaseAlt: SegmentBaseDto? = null,
) {
    val url: String get() = baseUrl.ifBlank { baseUrlAlt }
    val backups: List<String> get() = backupUrl.ifEmpty { backupUrlAlt }
    val mime: String get() = mimeType.ifBlank { mimeTypeAlt }
    val fps: String get() = frameRate.ifBlank { frameRateAlt }
    val segment: SegmentBaseDto? get() = segmentBase ?: segmentBaseAlt
}

@Serializable
data class SegmentBaseDto(
    @SerialName("initialization") val initialization: String = "",
    @SerialName("Initialization") val initializationAlt: String = "",
    @SerialName("index_range") val indexRange: String = "",
    @SerialName("indexRange") val indexRangeAlt: String = "",
) {
    val init: String get() = initialization.ifBlank { initializationAlt }
    val index: String get() = indexRange.ifBlank { indexRangeAlt }
}

@Serializable
data class DurlDto(
    @SerialName("order") val order: Int = 0,
    @SerialName("length") val length: Long = 0,
    @SerialName("url") val url: String = "",
    @SerialName("backup_url") val backupUrl: List<String> = emptyList(),
)

// ---------------- 搜索 ----------------

@Serializable
data class SearchTypeDto(
    @SerialName("result") val result: List<SearchItemDto> = emptyList(),
    @SerialName("numPages") val numPages: Int = 0,
    @SerialName("page") val page: Int = 1,
)

@Serializable
data class SearchItemDto(
    @SerialName("season_id") val seasonId: Long = 0,
    @SerialName("title") val title: String = "",
    @SerialName("cover") val cover: String = "",
    @SerialName("media_score") val mediaScore: MediaScoreDto? = null,
    @SerialName("areas") val areas: String = "",
    @SerialName("styles") val styles: String = "",
    @SerialName("pubtime") val pubtime: Long = 0,
    @SerialName("index_show") val indexShow: String = "",
    @SerialName("season_type_name") val seasonTypeName: String = "",
    @SerialName("desc") val desc: String = "",
    @SerialName("goto_url") val gotoUrl: String = "",
)

@Serializable
data class MediaScoreDto(
    @SerialName("score") val score: Double = 0.0,
    @SerialName("user_count") val userCount: Int = 0,
)
