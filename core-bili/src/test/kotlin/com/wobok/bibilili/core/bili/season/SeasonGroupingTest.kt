package com.wobok.bibilili.core.bili.season

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SeasonGroupingTest {

    private fun ep(n: Int) = Episode(
        epId = n.toLong(), cid = n.toLong(), aid = n.toLong(),
        shortTitle = n.toString(), longTitle = "第 $n 话",
        cover = "", durationMillis = 0, needsVip = false,
    )

    private fun seasonRef(id: Long, title: String, current: Boolean) =
        SeasonRef(seasonId = id, title = title, cover = "", isCurrent = current)

    @Test
    fun `单季只出一个 tab`() {
        val tabs = SeasonGrouping.build(
            currentSeasonId = 1, currentSeasonTitle = "荒原纪事",
            episodes = listOf(ep(1), ep(2)),
            seasons = listOf(seasonRef(1, "荒原纪事", true)),
            sections = emptyList(),
        )
        assertEquals(1, tabs.size)
        assertEquals(EpisodeTab.Kind.MAIN, tabs.single().kind)
        assertTrue(tabs.single().selected)
    }

    @Test
    fun `seasons 为空也能退化成单季`() {
        val tabs = SeasonGrouping.build(1, "某电影", listOf(ep(1)), emptyList(), emptyList())
        assertEquals(1, tabs.size)
        assertEquals("某电影", tabs.single().title)
    }

    @Test
    fun `多季保持 seasons 的原始顺序`() {
        val tabs = SeasonGrouping.build(
            currentSeasonId = 2, currentSeasonTitle = "第二季",
            episodes = listOf(ep(1)),
            seasons = listOf(
                seasonRef(1, "第一季", false),
                seasonRef(2, "第二季", true),
                seasonRef(3, "第三季", false),
            ),
            sections = emptyList(),
        )
        // 当前季不能被提到最前面，否则「第一季 / 第二季」的顺序会乱。
        assertEquals(listOf("第一季", "第二季", "第三季"), tabs.map(EpisodeTab::title))
        assertEquals(1, tabs.count(EpisodeTab::selected))
        assertEquals("第二季", tabs.single(EpisodeTab::selected).title)
    }

    @Test
    fun `只有当前季带分集数据`() {
        val tabs = SeasonGrouping.build(
            currentSeasonId = 2, currentSeasonTitle = "第二季",
            episodes = listOf(ep(1), ep(2)),
            seasons = listOf(seasonRef(1, "第一季", false), seasonRef(2, "第二季", true)),
            sections = emptyList(),
        )
        assertEquals(0, tabs.first { it.title == "第一季" }.episodes.size)
        assertEquals(2, tabs.first { it.title == "第二季" }.episodes.size)
        assertTrue(!tabs.first { it.title == "第一季" }.isLoaded, "其他季点开才拉，先标记为未加载")
    }

    @Test
    fun `section 排在所有季之后`() {
        val tabs = SeasonGrouping.build(
            currentSeasonId = 1, currentSeasonTitle = "第一季",
            episodes = listOf(ep(1)),
            seasons = listOf(seasonRef(1, "第一季", true), seasonRef(2, "第二季", false)),
            sections = listOf(Section(id = 9, title = "预告花絮", episodes = listOf(ep(99)))),
        )
        assertEquals(listOf("第一季", "第二季", "预告花絮"), tabs.map(EpisodeTab::title))
        assertEquals(EpisodeTab.Kind.SECTION, tabs.last().kind)
    }

    @Test
    fun `空的 section 不出 tab`() {
        val tabs = SeasonGrouping.build(
            1, "第一季", listOf(ep(1)),
            listOf(seasonRef(1, "第一季", true)),
            listOf(Section(id = 9, title = "预告", episodes = emptyList())),
        )
        assertEquals(1, tabs.size)
    }

    @Test
    fun `单集电影不显示选集区`() {
        val tabs = SeasonGrouping.build(1, "某电影", listOf(ep(1)), emptyList(), emptyList())
        assertTrue(!SeasonGrouping.shouldShowPicker(tabs))
    }

    @Test
    fun `多集或多 tab 时显示选集区`() {
        val multiEp = SeasonGrouping.build(1, "剧", listOf(ep(1), ep(2)), emptyList(), emptyList())
        assertTrue(SeasonGrouping.shouldShowPicker(multiEp))

        val withSection = SeasonGrouping.build(
            1, "某电影", listOf(ep(1)), emptyList(),
            listOf(Section(9, "花絮", listOf(ep(99)))),
        )
        assertTrue(SeasonGrouping.shouldShowPicker(withSection))
    }

    @Test
    fun `分集标题回退`() {
        assertEquals("3 第 3 话", ep(3).displayTitle())
        assertEquals("风蚀", ep(1).copy(shortTitle = "", longTitle = "风蚀").displayTitle())
        assertEquals("第 5 集", ep(1).copy(shortTitle = "5", longTitle = "").displayTitle())
        assertEquals("第 ? 集", ep(1).copy(shortTitle = "", longTitle = "").displayTitle())
    }
}
