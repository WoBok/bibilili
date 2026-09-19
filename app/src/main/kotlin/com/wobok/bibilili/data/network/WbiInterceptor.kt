package com.wobok.bibilili.data.network

import com.wobok.bibilili.core.bili.sign.WbiKeys
import com.wobok.bibilili.core.bili.sign.WbiSigner
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 给标了 [Wbi] 的请求加上 `wts` 与 `w_rid`。
 *
 * 两把 key 每日更替，所以由 [keyProvider] 负责缓存与刷新，这里只管签。
 * 注意：`w_rid` / `wts` 是**追加**在原始参数之后的，原始参数保持原样不排序，
 * 排序只发生在计算签名的那份拷贝里。
 */
class WbiInterceptor(
    private val keyProvider: WbiKeyProvider,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000 },
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header(HEADER_WBI) == null) return chain.proceed(request)

        val keys = keyProvider.blockingKeys()
            ?: return chain.proceed(request.newBuilder().removeHeader(HEADER_WBI).build())

        val original = request.url
        val params = buildMap {
            original.queryParameterNames.forEach { name ->
                original.queryParameter(name)?.let { put(name, it) }
            }
        }

        val signed = WbiSigner.sign(params, keys, nowSeconds())

        val url = original.newBuilder()
            .apply {
                // 用剔除过 !'()* 的值重建 query，否则发出去的值和签名对不上。
                original.queryParameterNames.forEach { removeAllQueryParameters(it) }
                signed.params.forEach { (k, v) -> addQueryParameter(k, v) }
                addQueryParameter("w_rid", signed.wRid)
            }
            .build()

        return chain.proceed(
            request.newBuilder().url(url).removeHeader(HEADER_WBI).build()
        )
    }

    companion object {
        /** Retrofit 用一个假的 header 来标记「这个接口要 WBI 签名」。 */
        const val HEADER_WBI = "X-Bibilili-Wbi"
    }
}

/** 提供并缓存当日的 WBI key。 */
interface WbiKeyProvider {
    /** 拿不到时返回 null——调用方应让请求原样发出去，由业务码暴露问题。 */
    fun blockingKeys(): WbiKeys?
}
