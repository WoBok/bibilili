package com.wobok.bibilili.core.bili.play

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FnvalTest {

    @Test
    fun `本机播放的 fnval 为 4048`() {
        assertEquals(4048, Fnval.ALL)
    }

    @Test
    fun `投屏不带 DASH 位`() {
        // 带了 DASH 位就拿不到 durl，DLNA 也就推不了。
        assertEquals(0, Fnval.FOR_CAST and Fnval.DASH)
    }
}

class PlayQualityTest {

    @Test
    fun `投屏档位排除只存在于 DASH 的档`() {
        val castable = PlayQuality.forCasting()
        assertTrue(castable.none(PlayQuality::dashOnly))
        assertTrue(PlayQuality.Q_4K !in castable)
        assertTrue(PlayQuality.Q_DOLBY !in castable)
    }

    @Test
    fun `投屏上限是 1080P`() {
        assertEquals(PlayQuality.Q_1080, PlayQuality.forCasting().first())
        assertEquals(80, PlayQuality.forCasting().first().qn)
    }

    @Test
    fun `本机播放上限是 8K`() {
        assertEquals(PlayQuality.Q_8K, PlayQuality.forLocalPlayback().first())
    }

    @Test
    fun `降档走的是本机梯子`() {
        assertEquals(PlayQuality.Q_DOLBY, PlayQuality.downgradeFrom(PlayQuality.Q_8K))
        assertEquals(PlayQuality.Q_HDR, PlayQuality.downgradeFrom(PlayQuality.Q_DOLBY))
    }

    @Test
    fun `投屏降档沿投屏梯子逐级下降且始终可投`() {
        var current: PlayQuality? = PlayQuality.forCasting().first()
        var previous = current!!
        var steps = 0
        while (true) {
            current = PlayQuality.downgradeFrom(previous, castable = true) ?: break
            assertTrue(!current.dashOnly, "降档结果必须仍然可投屏，不能落到 DASH 专属档")
            assertTrue(current.qn < previous.qn, "每降一档 qn 必须严格变小")
            previous = current
            steps++
        }
        assertEquals(PlayQuality.forCasting().size - 1, steps, "应能一路降到投屏梯子的最低档")
    }

    @Test
    fun `降到最低档后返回 null`() {
        assertNull(PlayQuality.downgradeFrom(PlayQuality.Q_360))
    }

    @Test
    fun `按 qn 反查`() {
        assertEquals(PlayQuality.Q_1080, PlayQuality.from(80))
        assertNull(PlayQuality.from(999))
    }
}

class DashMpdBuilderTest {

    private val video = DashMpdBuilder.Track(
        id = "30080",
        baseUrl = "https://cn-example.bilivideo.com/v.m4s?e=ig8&oi=1&deadline=1&uipk=5",
        codecs = "avc1.640032", bandwidth = 1_500_000, mimeType = "video/mp4",
        initRange = "0-1023", indexRange = "1024-2047",
        width = 1920, height = 1080, frameRate = "25",
    )

    private val audio = DashMpdBuilder.Track(
        id = "30280",
        baseUrl = "https://cn-example.bilivideo.com/a.m4s?e=ig8&oi=2",
        codecs = "mp4a.40.2", bandwidth = 128_000, mimeType = "audio/mp4",
        initRange = "0-767", indexRange = "768-1535",
        audioSampleRate = 44100,
    )

    @Test
    fun `直链里的 and 号被转义否则 MPD 不是合法 XML`() {
        val mpd = DashMpdBuilder.build(3600.0, listOf(video), listOf(audio))
        assertTrue("&amp;" in mpd)
        assertTrue(Regex("&(?!amp;|lt;|gt;|quot;|apos;)").find(mpd) == null, "不应存在未转义的 & ")
    }

    @Test
    fun `视频与音频各自成组`() {
        val mpd = DashMpdBuilder.build(3600.0, listOf(video), listOf(audio))
        assertTrue("""contentType="video"""" in mpd)
        assertTrue("""contentType="audio"""" in mpd)
        assertEquals(2, Regex("<AdaptationSet").findAll(mpd).count())
    }

    @Test
    fun `SegmentBase 带上 init 与 index 区间`() {
        val mpd = DashMpdBuilder.build(3600.0, listOf(video), listOf(audio))
        assertTrue("""<SegmentBase indexRange="1024-2047">""" in mpd)
        assertTrue("""<Initialization range="0-1023"/>""" in mpd)
    }

    @Test
    fun `时长按 ISO 8601 输出`() {
        assertEquals("PT3600S", DashMpdBuilder.isoDuration(3600.0))
        assertEquals("PT90.5S", DashMpdBuilder.isoDuration(90.5))
    }

    @Test
    fun `没有视频轨直接失败`() {
        val error = runCatching { DashMpdBuilder.build(10.0, emptyList(), listOf(audio)) }
        assertTrue(error.isFailure)
    }

    @Test
    fun `没有音频轨仍能生成`() {
        val mpd = DashMpdBuilder.build(10.0, listOf(video), emptyList())
        assertEquals(1, Regex("<AdaptationSet").findAll(mpd).count())
    }

    @Test
    fun `多档视频进同一个 AdaptationSet`() {
        val low = video.copy(id = "30016", bandwidth = 400_000, width = 640, height = 360)
        val mpd = DashMpdBuilder.build(10.0, listOf(video, low), listOf(audio))
        assertEquals(2, Regex("<AdaptationSet").findAll(mpd).count())
        assertEquals(3, Regex("<Representation ").findAll(mpd).count())
    }
}

class PlaybackSettingsTest {

    @Test
    fun `默认值符合需求`() {
        val s = PlaybackSettings()
        assertEquals(10, s.seekStepSeconds)
        assertEquals(2.0f, s.longPressSpeed)
        assertTrue(s.autoPlayNext)
        assertTrue(s.skipOpeningEnding)
    }

    @Test
    fun `倍速九档`() {
        assertEquals(
            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f),
            PlaybackSettings.SPEED_OPTIONS,
        )
    }

    @Test
    fun `长按倍速只取大于 1 倍的档`() {
        assertTrue(PlaybackSettings.LONG_PRESS_SPEED_OPTIONS.all { it > 1.0f })
        assertTrue(2.0f in PlaybackSettings.LONG_PRESS_SPEED_OPTIONS)
    }

    @Test
    fun `双击步长四档`() {
        assertEquals(listOf(5, 10, 15, 30), PlaybackSettings.SEEK_STEP_OPTIONS)
    }

    @Test
    fun `倍速浮层文案不带 emoji`() {
        assertEquals("2.0X 倍速播放中", formatSpeedBadge(2.0f))
        assertEquals("1.5X 倍速播放中", formatSpeedBadge(1.5f))
        val badge = formatSpeedBadge(2.0f)
        assertTrue(badge.all { it.code < 0x1F000 }, "不应含 emoji")
    }

    @Test
    fun `定时关闭五个选项`() {
        assertEquals(5, SleepTimer.OPTIONS.size)
        assertTrue(SleepTimer.Off in SleepTimer.OPTIONS)
        assertTrue(SleepTimer.EndOfEpisode in SleepTimer.OPTIONS)
    }
}
