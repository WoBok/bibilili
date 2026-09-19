package com.wobok.bibilili.core.bili.play

/**
 * 把 playurl 返回的 DASH 轨道拼成一份内存中的 MPD。
 *
 * 为什么必须这么做：接口给的是 `SegmentBase` 形态（一个完整文件 + init/index 字节区间），
 * 没有现成的 .mpd 地址。ExoPlayer 的 `DashMediaSource` 需要一份 manifest，
 * 用两个 `ProgressiveMediaSource` 拼视频和音频**是错的**——那样没有音画同步保证，
 * 也拿不到自适应切换。
 */
object DashMpdBuilder {

    data class Track(
        val id: String,
        val baseUrl: String,
        /** 如 `avc1.640028`、`mp4a.40.2`。 */
        val codecs: String,
        val bandwidth: Int,
        val mimeType: String,
        /** `"0-1234"` 形式的字节区间。 */
        val initRange: String,
        val indexRange: String,
        val width: Int? = null,
        val height: Int? = null,
        /** 如 `"25"` 或 `"30000/1001"`。 */
        val frameRate: String? = null,
        val audioSampleRate: Int? = null,
    )

    /**
     * @param durationSeconds 视频总时长（秒）
     * @param video           视频轨，同一 AdaptationSet 内可多档以支持自适应
     * @param audio           音频轨
     */
    fun build(
        durationSeconds: Double,
        video: List<Track>,
        audio: List<Track>,
    ): String {
        require(video.isNotEmpty()) { "至少要有一条视频轨" }

        return buildString {
            append("""<?xml version="1.0" encoding="utf-8"?>""")
            append(
                """<MPD xmlns="urn:mpeg:dash:schema:mpd:2011" """ +
                    """profiles="urn:mpeg:dash:profile:isoff-on-demand:2011" """ +
                    """type="static" """ +
                    """mediaPresentationDuration="${isoDuration(durationSeconds)}" """ +
                    """minBufferTime="PT1.5S">"""
            )
            append("<Period>")
            if (video.isNotEmpty()) append(adaptationSet("video", video))
            if (audio.isNotEmpty()) append(adaptationSet("audio", audio))
            append("</Period>")
            append("</MPD>")
        }
    }

    private fun adaptationSet(contentType: String, tracks: List<Track>): String = buildString {
        append("""<AdaptationSet contentType="$contentType" """)
        append("""segmentAlignment="true" subsegmentAlignment="true" """)
        append("""subsegmentStartsWithSAP="1" startWithSAP="1" bitstreamSwitching="false">""")
        tracks.forEach { append(representation(it)) }
        append("</AdaptationSet>")
    }

    private fun representation(track: Track): String = buildString {
        append("""<Representation id="${escape(track.id)}" """)
        append("""codecs="${escape(track.codecs)}" """)
        append("""mimeType="${escape(track.mimeType)}" """)
        append("""bandwidth="${track.bandwidth}" """)
        track.width?.let { append("""width="$it" """) }
        track.height?.let { append("""height="$it" """) }
        track.frameRate?.let { append("""frameRate="${escape(it)}" """) }
        append(""">""")
        track.audioSampleRate?.let {
            append(
                """<AudioChannelConfiguration """ +
                    """schemeIdUri="urn:mpeg:dash:23003:3:audio_channel_configuration:2011" """ +
                    """value="2"/>"""
            )
        }
        // 视频直链带一堆 query 参数，& 必须转义，否则 MPD 不是合法 XML。
        append("<BaseURL>${escape(track.baseUrl)}</BaseURL>")
        append("""<SegmentBase indexRange="${escape(track.indexRange)}">""")
        append("""<Initialization range="${escape(track.initRange)}"/>""")
        append("</SegmentBase>")
        append("</Representation>")
    }

    /** `3600.5` → `PT3600.5S`。 */
    internal fun isoDuration(seconds: Double): String {
        val rounded = (seconds * 1000).toLong() / 1000.0
        return if (rounded % 1.0 == 0.0) "PT${rounded.toLong()}S" else "PT${rounded}S"
    }

    private fun escape(value: String): String = buildString(value.length) {
        value.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(char)
            }
        }
    }
}
