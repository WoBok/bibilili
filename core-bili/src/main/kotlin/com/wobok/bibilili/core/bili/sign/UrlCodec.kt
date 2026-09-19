package com.wobok.bibilili.core.bili.sign

/**
 * 百分号编码，语义与 JavaScript 的 `encodeURIComponent` 对齐。
 *
 * WBI 签名对编码结果敏感，两处常见错误会直接导致 `w_rid` 不匹配：
 *  - 十六进制字母必须**大写**（部分库输出小写）；
 *  - 空格必须编码为 `%20`，**不是** `+`（`application/x-www-form-urlencoded` 的约定在这里是错的）。
 *
 * 因此不能用 `java.net.URLEncoder`。
 */
internal object UrlCodec {

    /** `encodeURIComponent` 不转义的字符集合。 */
    private const val UNRESERVED =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.!~*'()"

    private val HEX = "0123456789ABCDEF".toCharArray()

    fun encodeComponent(value: String): String {
        val bytes = value.toByteArray(Charsets.UTF_8)
        val out = StringBuilder(bytes.size)
        for (byte in bytes) {
            val code = byte.toInt() and 0xFF
            val char = code.toChar()
            if (code < 0x80 && UNRESERVED.indexOf(char) >= 0) {
                out.append(char)
            } else {
                out.append('%').append(HEX[code shr 4]).append(HEX[code and 0x0F])
            }
        }
        return out.toString()
    }

    /** 按 key 升序排列后拼成 query string。 */
    fun sortedQuery(params: Map<String, String>): String =
        params.entries
            .sortedBy { it.key }
            .joinToString("&") { (k, v) -> "${encodeComponent(k)}=${encodeComponent(v)}" }
}
