package com.wobok.bibilili.core.bili.sign

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 测试向量取自 B 站 WBI 签名的公开说明，是真实可复现的一组值。
 * 签名错一个字符整条链路就废，所以这里对着基准值逐步校验。
 */
class WbiSignerTest {

    private val keys = WbiKeys(
        imgKey = "7cd084941338484aae1ad9425b84077c",
        subKey = "4932caff0ff746eab6f01bf08b70ac45",
    )

    @Test
    fun `重排表是 0 到 63 的一个排列`() {
        val table = WbiSigner::class.java.getDeclaredField("MIXIN_KEY_ENC_TAB")
            .apply { isAccessible = true }
            .get(WbiSigner) as IntArray
        assertEquals(64, table.size, "重排表应有 64 项")
        assertEquals((0..63).toSet(), table.toSet(), "重排表应恰好覆盖 0..63，不重不漏")
    }

    @Test
    fun `mixin_key 与基准值一致`() {
        assertEquals("ea1db124af3c7062474693fa704f4ff8", WbiSigner.mixinKey(keys))
    }

    @Test
    fun `mixin_key 长度为 32`() {
        assertEquals(32, WbiSigner.mixinKey(keys).length)
    }

    @Test
    fun `w_rid 与基准值一致`() {
        val signed = WbiSigner.sign(
            params = mapOf("foo" to "114", "bar" to "514", "zab" to "1919810"),
            keys = keys,
            wts = 1702204169L,
        )
        assertEquals("8f6f2b5b3d485fe1886cec6a0be8c5d4", signed.wRid)
        assertEquals(1702204169L, signed.wts)
        assertEquals("1702204169", signed.params["wts"])
    }

    @Test
    fun `从 wbi_img 地址里取 key`() {
        assertEquals(
            "7cd084941338484aae1ad9425b84077c",
            WbiSigner.keyFromUrl("https://i0.hdslb.com/bfs/wbi/7cd084941338484aae1ad9425b84077c.png"),
        )
        assertEquals(
            "4932caff0ff746eab6f01bf08b70ac45",
            WbiSigner.keyFromUrl("https://i0.hdslb.com/bfs/wbi/4932caff0ff746eab6f01bf08b70ac45.png"),
        )
    }

    @Test
    fun `value 中的特殊字符会被剔除`() {
        assertEquals("abc", WbiSigner.sanitizeValue("a!b'c"))
        assertEquals("", WbiSigner.sanitizeValue("!'()*"))
        assertEquals("你好 world", WbiSigner.sanitizeValue("你好 world"))
    }

    @Test
    fun `签名用的参数里也是剔除后的值`() {
        val signed = WbiSigner.sign(mapOf("keyword" to "a(b)c"), keys, 1L)
        assertEquals("abc", signed.params["keyword"], "发出的请求必须用剔除后的值，否则和签名对不上")
    }

    @Test
    fun `key 为空时立即失败`() {
        assertFailsWith<IllegalArgumentException> { WbiKeys("", "x") }
    }

    @Test
    fun `key 长度不足以覆盖重排表时报错而不是静默截断`() {
        val short = WbiKeys("abc", "def")
        val error = assertFailsWith<IllegalArgumentException> { WbiSigner.mixinKey(short) }
        assertTrue(error.message!!.contains("重排表"), "错误信息应指出是 key 没取全")
    }
}
