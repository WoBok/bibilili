package com.wobok.bibilili.core.bili.auth

/**
 * Web 端登录凭证。
 *
 * 只存在 EncryptedSharedPreferences 里：**不入 Room、不写日志、不进崩溃上报**。
 * [toString] 被刻意重写成只暴露 mid，防止随手打日志时把 SESSDATA 漏出去。
 */
data class Credentials(
    val sessData: String,
    val biliJct: String,
    val dedeUserId: String,
    val buvid3: String,
) {
    fun asCookieHeader(): String = buildString {
        append("SESSDATA=").append(sessData)
        append("; bili_jct=").append(biliJct)
        append("; DedeUserID=").append(dedeUserId)
        if (buvid3.isNotEmpty()) append("; buvid3=").append(buvid3)
    }

    override fun toString(): String = "Credentials(mid=$dedeUserId, 其余已隐去)"

    /** `bili_jct` 只有写操作（追番、上报进度）才需要，缺了不影响观看。 */
    val isUsable: Boolean
        get() = sessData.isNotBlank() && dedeUserId.isNotBlank()
}

/**
 * 从 `Set-Cookie` 响应头里取出凭证。
 *
 * 扫码登录成功时凭证在**响应头**里，不在 body——这点很容易看漏。
 */
fun parseCredentials(setCookieLines: List<String>): Credentials? {
    val jar = mutableMapOf<String, String>()
    setCookieLines.forEach { line ->
        val pair = line.substringBefore(';')
        val name = pair.substringBefore('=').trim()
        val value = pair.substringAfter('=', "").trim()
        if (name.isNotEmpty() && value.isNotEmpty()) jar[name] = value
    }
    val sessData = jar["SESSDATA"] ?: return null
    val mid = jar["DedeUserID"] ?: return null
    return Credentials(
        sessData = sessData,
        biliJct = jar["bili_jct"].orEmpty(),
        dedeUserId = mid,
        buvid3 = jar["buvid3"].orEmpty(),
    ).takeIf { it.isUsable }
}
