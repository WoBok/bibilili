package com.wobok.bibilili.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 本地观看记录。
 *
 * 只存 PGC（影视/番剧），UP 主视频在写入前就被过滤掉了。
 * 这张表是「随使用自然沉淀」的：每次同步只拉 30 天内的，
 * 三个月后库里自然攒到三个月，再由周期任务清掉更早的。
 */
@Entity(tableName = "history")
data class HistoryEntity(
    /** 按剧集去重，同一部剧只留最近看的那一集。 */
    @PrimaryKey val seasonId: Long,
    val epId: Long,
    val cid: Long,
    val title: String,
    val episodeTitle: String,
    val cover: String,
    val viewAt: Long,
    val progressSeconds: Int,
    val durationSeconds: Int,
)

/** 追番 / 追剧。`type` 1=番剧 2=影视。 */
@Entity(tableName = "follow")
data class FollowEntity(
    @PrimaryKey val seasonId: Long,
    val type: Int,
    val title: String,
    val cover: String,
    val seasonTypeName: String,
    /** 「更新至第 12 话」，接口原样给的展示串。 */
    val newEpIndexShow: String,
    /** 「看到第 5 话」。 */
    val progressText: String,
    val followStatus: Int,
    val isFinish: Boolean,
    val newEpPubTime: Long,
    val score: String,
)
