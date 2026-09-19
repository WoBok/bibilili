package com.wobok.bibilili.core.bili.sign

/**
 * 一组 WBI 实时口令。两把 key 全站统一，**每日更替**，取自 `x/web-interface/nav` 的
 * `wbi_img.img_url` / `wbi_img.sub_url`，需要缓存并按日刷新。
 */
data class WbiKeys(
    val imgKey: String,
    val subKey: String,
) {
    init {
        require(imgKey.isNotEmpty() && subKey.isNotEmpty()) { "WBI key 不能为空" }
    }
}

/** 签名后的参数：原始参数 + `wts` + `w_rid`。 */
data class SignedParams(
    val params: Map<String, String>,
    val wts: Long,
    val wRid: String,
)

/**
 * WBI 签名。
 *
 * 流程：`img_key + sub_key` → 按 [MIXIN_KEY_ENC_TAB] 重排取前 32 位得到 `mixin_key`
 * → 参数加上 `wts`、按 key 升序、value 过滤掉 `!'()*` 后编码成 query
 * → `w_rid = md5(query + mixin_key)`。
 *
 * 注意最后一步：`w_rid` / `wts` 是**追加**到原始参数后面的，原始参数本身不需要排序，
 * 排序只发生在计算签名的那份拷贝上。
 */
object WbiSigner {

    private val MIXIN_KEY_ENC_TAB = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
        33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40,
        61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11,
        36, 20, 34, 44, 52,
    )

    private const val MIXIN_KEY_LENGTH = 32

    /** 计算签名前需要从 value 中剔除的字符。 */
    private const val FORBIDDEN_IN_VALUE = "!'()*"

    /**
     * 从 `wbi_img.img_url` 这类地址里取出 key。
     *
     * `https://i0.hdslb.com/bfs/wbi/7cd084941338484aae1ad9425b84077c.png`
     *   → `7cd084941338484aae1ad9425b84077c`
     */
    fun keyFromUrl(url: String): String =
        url.substringAfterLast('/').substringBefore('.')

    /** 打乱重排得到 `mixin_key`。 */
    fun mixinKey(keys: WbiKeys): String {
        val raw = keys.imgKey + keys.subKey
        require(raw.length > MIXIN_KEY_ENC_TAB.max()) {
            "img_key + sub_key 长度为 ${raw.length}，不足以覆盖重排表，两把 key 可能没取全"
        }
        val out = StringBuilder(MIXIN_KEY_LENGTH)
        for (index in MIXIN_KEY_ENC_TAB) {
            out.append(raw[index])
            if (out.length == MIXIN_KEY_LENGTH) break
        }
        return out.toString()
    }

    /** value 中的 `!'()*` 在参与签名前会被剔除，实际发出的请求也要用剔除后的值。 */
    fun sanitizeValue(value: String): String =
        value.filterNot { it in FORBIDDEN_IN_VALUE }

    /**
     * @param params 原始请求参数，不含 `wts` / `w_rid`
     * @param wts    Unix 时间戳（**秒**）
     */
    fun sign(params: Map<String, String>, keys: WbiKeys, wts: Long): SignedParams {
        val forSigning = buildMap(params.size + 1) {
            params.forEach { (key, value) -> put(key, sanitizeValue(value)) }
            put("wts", wts.toString())
        }
        val query = UrlCodec.sortedQuery(forSigning)
        val wRid = Md5.hex(query + mixinKey(keys))
        return SignedParams(params = forSigning, wts = wts, wRid = wRid)
    }
}
