package com.wobok.bibilili.core.bili.sign

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppSignerTest {

    private val pair = AppKeyPair("appkey123", "secret456")

    @Test
    fun `签名结果带上 appkey 与 sign`() {
        val signed = AppSigner.sign(mapOf("mid" to "1"), pair)
        assertEquals("appkey123", signed["appkey"])
        assertTrue(signed.containsKey("sign"))
        assertEquals(32, signed["sign"]!!.length, "MD5 十六进制应为 32 位")
    }

    @Test
    fun `sign 等于 md5 的排序 query 加 appsec`() {
        val signed = AppSigner.sign(mapOf("mid" to "1"), pair)
        val expected = Md5.hex(UrlCodec.sortedQuery(mapOf("appkey" to "appkey123", "mid" to "1")) + "secret456")
        assertEquals(expected, signed["sign"])
    }

    @Test
    fun `参数顺序不影响签名`() {
        val a = AppSigner.sign(linkedMapOf("b" to "2", "a" to "1"), pair)
        val b = AppSigner.sign(linkedMapOf("a" to "1", "b" to "2"), pair)
        assertEquals(a["sign"], b["sign"])
    }
}
