package com.wobok.bibilili.data.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 补齐 B 站接口必需的请求头。少一个就是 `-412`。
 *
 *  - `Referer` 必须在 `.bilibili.com` 下，视频直链尤其严格，缺了直接 403；
 *  - `User-Agent` 必须像浏览器，**且不能含敏感子串**（搜索接口会查）；
 *  - `Origin` 部分写接口要。
 */
class BiliHeaderInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", BROWSER_UA)
            .header("Referer", REFERER)
            .header("Origin", ORIGIN)
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "zh-CN,zh;q=0.9")
            .build()
        return chain.proceed(request)
    }

    companion object {
        /**
         * 普通 Chrome UA。不要在里面塞应用名——接口会把不认识的 UA 当成脚本。
         */
        const val BROWSER_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        const val REFERER = "https://www.bilibili.com"
        const val ORIGIN = "https://www.bilibili.com"
    }
}
