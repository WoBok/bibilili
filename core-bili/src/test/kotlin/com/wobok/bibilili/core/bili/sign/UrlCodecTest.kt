package com.wobok.bibilili.core.bili.sign

import kotlin.test.Test
import kotlin.test.assertEquals

class UrlCodecTest {

    /** 官方说明里给出的编码示例，直接当基准。 */
    @Test
    fun `排序并编码后与基准值一致`() {
        val query = UrlCodec.sortedQuery(
            mapOf(
                "foo" to "one one four",
                "bar" to "五一四",
                "baz" to "1919810",
            )
        )
        assertEquals("bar=%E4%BA%94%E4%B8%80%E5%9B%9B&baz=1919810&foo=one%20one%20four", query)
    }

    @Test
    fun `空格编码为 %20 而不是加号`() {
        assertEquals("one%20one%20four", UrlCodec.encodeComponent("one one four"))
    }

    @Test
    fun `十六进制字母大写`() {
        // 小写实现会得到 %e4%ba%94，签名随即对不上。
        assertEquals("%E4%BA%94", UrlCodec.encodeComponent("五"))
    }

    @Test
    fun `encodeURIComponent 不转义的字符原样保留`() {
        val unreserved = "AZaz09-_.!~*'()"
        assertEquals(unreserved, UrlCodec.encodeComponent(unreserved))
    }

    @Test
    fun `保留字符会被转义`() {
        assertEquals("%26", UrlCodec.encodeComponent("&"))
        assertEquals("%3D", UrlCodec.encodeComponent("="))
        assertEquals("%2F", UrlCodec.encodeComponent("/"))
        assertEquals("%2B", UrlCodec.encodeComponent("+"))
    }

    @Test
    fun `按 key 升序而不是插入序`() {
        val query = UrlCodec.sortedQuery(linkedMapOf("z" to "1", "a" to "2", "m" to "3"))
        assertEquals("a=2&m=3&z=1", query)
    }
}
