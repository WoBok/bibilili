package com.wobok.bibilili.core.bili.play

/**
 * `fnval` 是位掩码，决定接口返回哪些格式。
 * 不传足位就拿不到 4K / 杜比，传了也不代表账号有权限。
 */
object Fnval {
    const val DASH = 16
    const val HDR = 64
    const val FOUR_K = 128
    const val DOLBY_AUDIO = 256
    const val DOLBY_VISION = 512
    const val EIGHT_K = 1024
    const val AV1 = 2048

    /** 全都要 = 4048。本机播放用这个。 */
    const val ALL = DASH or HDR or FOUR_K or DOLBY_AUDIO or DOLBY_VISION or EIGHT_K or AV1

    /**
     * 投屏用。DLNA 的 `SetAVTransportURI` 推不了 DASH（音视频分离的自适应流），
     * 必须让接口返回整段 MP4/FLV 的 `durl`，所以这里**不能**带 [DASH] 位。
     */
    const val FOR_CAST = 0
}

/**
 * 清晰度档位。
 *
 * 关键事实：**4K 及以上只存在于 DASH**，而 DLNA 推不了 DASH，
 * 所以投屏的天花板是 1080P（`qn = 80`）——这与投屏到哪台设备无关。
 */
enum class PlayQuality(
    val qn: Int,
    val label: String,
    val needsLogin: Boolean,
    val needsVip: Boolean,
    /** 只有 DASH 才有这一档，投屏不可用。 */
    val dashOnly: Boolean,
) {
    Q_360(16, "360P 流畅", needsLogin = false, needsVip = false, dashOnly = false),
    Q_480(32, "480P 清晰", needsLogin = false, needsVip = false, dashOnly = false),
    Q_720(64, "720P 高清", needsLogin = true, needsVip = false, dashOnly = false),
    Q_720_60(74, "720P60 高帧率", needsLogin = true, needsVip = true, dashOnly = false),
    Q_1080(80, "1080P 高清", needsLogin = true, needsVip = false, dashOnly = false),
    Q_1080_PLUS(112, "1080P+ 高码率", needsLogin = true, needsVip = true, dashOnly = true),
    Q_1080_60(116, "1080P60 高帧率", needsLogin = true, needsVip = true, dashOnly = true),
    Q_4K(120, "4K 超清", needsLogin = true, needsVip = true, dashOnly = true),
    Q_HDR(125, "HDR 真彩", needsLogin = true, needsVip = true, dashOnly = true),
    Q_DOLBY(126, "杜比视界", needsLogin = true, needsVip = true, dashOnly = true),
    Q_8K(127, "8K 超高清", needsLogin = true, needsVip = true, dashOnly = true),
    ;

    companion object {
        private val byQn = entries.associateBy(PlayQuality::qn)

        fun from(qn: Int): PlayQuality? = byQn[qn]

        /** 本机播放可选的全部档位，从高到低。 */
        fun forLocalPlayback(): List<PlayQuality> = entries.sortedByDescending(PlayQuality::qn)

        /** 投屏可选的档位：排除只存在于 DASH 的档，上限 1080P。 */
        fun forCasting(): List<PlayQuality> =
            entries.filterNot(PlayQuality::dashOnly).sortedByDescending(PlayQuality::qn)

        /**
         * 请求失败后降一档。接口返回 `-10403`（地区或会员限制）时调用，
         * 一路降到最低档仍失败才算真的放不了。
         */
        fun downgradeFrom(quality: PlayQuality, castable: Boolean = false): PlayQuality? {
            val ladder = if (castable) forCasting() else forLocalPlayback()
            val index = ladder.indexOf(quality)
            return if (index < 0 || index == ladder.lastIndex) null else ladder[index + 1]
        }
    }
}
