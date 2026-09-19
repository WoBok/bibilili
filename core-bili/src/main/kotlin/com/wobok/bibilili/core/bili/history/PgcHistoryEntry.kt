package com.wobok.bibilili.core.bili.history

/**
 * 一条已经过滤好的 PGC 观看记录。这是接口报文映射到本地库之间的中间形态，
 * 刻意不带任何 Android / 序列化注解，方便直接测。
 */
data class PgcHistoryEntry(
    /** 剧集 id。同一部剧的不同集共享同一个 seasonId，去重要按它。 */
    val seasonId: Long,
    /** 具体某一集的 epId。 */
    val epId: Long,
    val cid: Long,
    /** 剧名，如「荒原纪事」。 */
    val title: String,
    /** 分集名，如「第 3 集 风蚀」。 */
    val episodeTitle: String,
    val cover: String,
    /** 观看时间，Unix 秒。游标翻页按它倒序。 */
    val viewAt: Long,
    /** 已观看秒数。`-1` 表示看完。 */
    val progressSeconds: Int,
    /** 本集总时长（秒）。为 0 表示接口没给。 */
    val durationSeconds: Int,
) {
    /** 看完的记录 `progress` 为 -1，直接按 100% 处理。 */
    val watchedRatio: Float
        get() = when {
            progressSeconds < 0 -> 1f
            durationSeconds <= 0 -> 0f
            else -> (progressSeconds.toFloat() / durationSeconds).coerceIn(0f, 1f)
        }

    val isFinished: Boolean
        get() = progressSeconds < 0 || watchedRatio >= FINISHED_RATIO

    companion object {
        /** 看到 95% 以上就算看完，片尾一般不会看完。 */
        const val FINISHED_RATIO = 0.95f
    }
}
