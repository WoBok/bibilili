package com.wobok.bibilili.core.bili.sign

import java.security.MessageDigest

internal object Md5 {
    fun hex(input: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        val out = StringBuilder(digest.size * 2)
        for (byte in digest) {
            val code = byte.toInt() and 0xFF
            out.append(HEX[code shr 4]).append(HEX[code and 0x0F])
        }
        return out.toString()
    }

    private val HEX = "0123456789abcdef".toCharArray()
}
