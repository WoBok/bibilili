package com.wobok.bibilili.core.bili.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CredentialsTest {

    private val setCookie = listOf(
        "SESSDATA=abc%2Cdef%2Cghi; Path=/; Domain=bilibili.com; Expires=Wed, 01 Jan 2027 00:00:00 GMT; HttpOnly; Secure",
        "bili_jct=deadbeef; Path=/; Domain=bilibili.com",
        "DedeUserID=12345678; Path=/; Domain=bilibili.com",
        "DedeUserID__ckMd5=aabbcc; Path=/",
        "sid=xyz; Path=/",
    )

    @Test
    fun `从 Set-Cookie 里取出凭证`() {
        val c = parseCredentials(setCookie)!!
        assertEquals("abc%2Cdef%2Cghi", c.sessData)
        assertEquals("deadbeef", c.biliJct)
        assertEquals("12345678", c.dedeUserId)
    }

    @Test
    fun `属性不会被误当成 cookie 名`() {
        // Path / Domain / HttpOnly 都在同一行的分号之后，只取第一段才不会串味。
        val c = parseCredentials(setCookie)!!
        assertTrue("Path" !in c.sessData)
        assertTrue("Domain" !in c.biliJct)
    }

    @Test
    fun `缺 SESSDATA 视为失败`() {
        assertNull(parseCredentials(listOf("DedeUserID=1; Path=/")))
    }

    @Test
    fun `缺 DedeUserID 视为失败`() {
        assertNull(parseCredentials(listOf("SESSDATA=x; Path=/")))
    }

    @Test
    fun `空响应头视为失败`() {
        assertNull(parseCredentials(emptyList()))
    }

    @Test
    fun `没有 bili_jct 仍可用于观看`() {
        val c = parseCredentials(listOf("SESSDATA=x; Path=/", "DedeUserID=9; Path=/"))!!
        assertEquals("", c.biliJct)
        assertTrue(c.isUsable)
    }

    @Test
    fun `Cookie 头按顺序拼好`() {
        val c = Credentials("s", "j", "1", "b")
        assertEquals("SESSDATA=s; bili_jct=j; DedeUserID=1; buvid3=b", c.asCookieHeader())
    }

    @Test
    fun `buvid3 为空时不出现在 Cookie 头里`() {
        val c = Credentials("s", "j", "1", "")
        assertEquals("SESSDATA=s; bili_jct=j; DedeUserID=1", c.asCookieHeader())
    }

    @Test
    fun `toString 不泄露 SESSDATA`() {
        val c = Credentials("super-secret-session", "jct", "42", "buvid")
        val text = c.toString()
        assertTrue("super-secret-session" !in text, "日志里绝不能出现 SESSDATA")
        assertTrue("jct" !in text)
        assertTrue("42" in text, "mid 保留，便于排查是哪个账号")
    }
}
