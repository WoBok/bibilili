package com.wobok.bibilili.data.repo

import com.wobok.bibilili.core.bili.error.ApiResult
import com.wobok.bibilili.core.bili.error.BiliError
import com.wobok.bibilili.core.bili.play.DashMpdBuilder
import com.wobok.bibilili.core.bili.play.Fnval
import com.wobok.bibilili.core.bili.play.PlayQuality
import com.wobok.bibilili.data.api.BiliApi
import com.wobok.bibilili.data.api.DashTrackDto
import com.wobok.bibilili.data.api.PlayUrlDto
import java.io.File

/** 播放源：本机走 DASH，投屏走整段 durl。 */
sealed interface PlaySource {
    /** 内存里拼好的 MPD 落到缓存文件，交给 DashMediaSource。 */
    data class Dash(val manifestFile: File, val quality: Int, val available: List<Int>) : PlaySource
    data class Progressive(val url: String, val quality: Int) : PlaySource
}

class PlaybackRepository(
    private val api: BiliApi,
    private val cacheDir: File,
) {

    /**
     * 取本机播放源。
     *
     * `-10403`（地区或会员限制）时自动降一档重试，一路降到最低档仍失败才算真放不了。
     */
    suspend fun localSource(epId: Long, cid: Long, preferredQn: Int): ApiResult<PlaySource> {
        var quality = PlayQuality.from(preferredQn) ?: PlayQuality.Q_1080

        while (true) {
            val dto = try {
                api.playUrl(epId = epId, cid = cid, qn = quality.qn, fnval = Fnval.ALL)
            } catch (e: java.io.IOException) {
                return ApiResult.Failure(BiliError.Network(e))
            }

            if (dto.code != 0) {
                val next = PlayQuality.downgradeFrom(quality)
                if (dto.code == CODE_LIMITED && next != null) {
                    quality = next
                    continue
                }
                return ApiResult.Failure(BiliError.Api(dto.code, dto.message))
            }

            val dash = dto.result?.dash ?: dto.dash
            val actualQn = dto.result?.quality ?: dto.quality
            val accept = dto.result?.acceptQuality ?: dto.acceptQuality

            if (dash != null && dash.video.isNotEmpty()) {
                val mpd = DashMpdBuilder.build(
                    durationSeconds = dash.duration.toDouble(),
                    video = dash.video.map { it.toTrack() },
                    audio = dash.audio.map { it.toTrack() },
                )
                val file = File(cacheDir, "manifest_${epId}_${cid}.mpd").apply { writeText(mpd) }
                return ApiResult.Success(PlaySource.Dash(file, actualQn, accept))
            }

            val durl = (dto.result?.durl ?: dto.durl).firstOrNull()
            if (durl != null) return ApiResult.Success(PlaySource.Progressive(durl.url, actualQn))

            val next = PlayQuality.downgradeFrom(quality)
                ?: return ApiResult.Failure(
                    BiliError.Malformed(IllegalStateException("既没有 dash 也没有 durl"))
                )
            quality = next
        }
    }

    /**
     * 取投屏源。
     *
     * **不带 DASH 位**——DLNA 的 SetAVTransportURI 推不了音视频分离的自适应流，
     * 必须让接口返回整段的 durl。这也是投屏上限 1080P 的原因：
     * 4K 与杜比只存在于 DASH，和投给哪台设备无关。
     */
    suspend fun castSource(epId: Long, cid: Long): ApiResult<PlaySource.Progressive> {
        var quality = PlayQuality.forCasting().first()

        while (true) {
            val dto = try {
                api.playUrl(epId = epId, cid = cid, qn = quality.qn, fnval = Fnval.FOR_CAST)
            } catch (e: java.io.IOException) {
                return ApiResult.Failure(BiliError.Network(e))
            }

            val durl = (dto.result?.durl ?: dto.durl).firstOrNull()
            if (dto.code == 0 && durl != null) {
                return ApiResult.Success(
                    PlaySource.Progressive(durl.url, dto.result?.quality ?: dto.quality)
                )
            }

            quality = PlayQuality.downgradeFrom(quality, castable = true)
                ?: return ApiResult.Failure(
                    BiliError.Api(dto.code, dto.message.ifBlank { "没有可投屏的片源" })
                )
        }
    }

    private companion object {
        const val CODE_LIMITED = -10403
    }
}

private fun DashTrackDto.toTrack() = DashMpdBuilder.Track(
    id = id.toString(),
    baseUrl = url,
    codecs = codecs,
    bandwidth = bandwidth,
    mimeType = mime.ifBlank { if (height > 0) "video/mp4" else "audio/mp4" },
    initRange = segment?.init.orEmpty().ifBlank { "0-0" },
    indexRange = segment?.index.orEmpty().ifBlank { "0-0" },
    width = width.takeIf { it > 0 },
    height = height.takeIf { it > 0 },
    frameRate = fps.takeIf { it.isNotBlank() },
    audioSampleRate = if (height == 0) 44100 else null,
)
